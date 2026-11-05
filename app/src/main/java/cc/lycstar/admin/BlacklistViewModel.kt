package cc.lycstar.admin

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** 目标应用的实时状态 */
sealed interface TargetStatus {
    data object Empty : TargetStatus
    data class NotInstalled(val pkg: String) : TargetStatus
    data object Hidden : TargetStatus
    data object Shown : TargetStatus
    data object Unknown : TargetStatus
}

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {

    private val dpm =
        application.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(application, MyAdmin::class.java)
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var adminActive by mutableStateOf(false)
        private set
    var deviceOwner by mutableStateOf(false)
        private set
    var pkgInput by mutableStateOf("")
        private set
    var targetStatus by mutableStateOf<TargetStatus>(TargetStatus.Empty)
        private set
    var hiddenApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var setupDismissed by mutableStateOf(false)
        private set

    init {
        setupDismissed = prefs.getBoolean(KEY_SETUP_DISMISSED, false)
        refresh()
    }

    /** 重新读取权限状态、目标应用状态与已隐藏列表 */
    fun refresh() {
        adminActive = dpm.isAdminActive(admin)
        deviceOwner = try {
            dpm.isDeviceOwnerApp(getApplication<Application>().packageName)
        } catch (e: Exception) {
            false
        }
        updateTargetStatus()
        refreshHiddenList()
    }

    fun onPkgChange(value: String) {
        pkgInput = value
        updateTargetStatus()
    }

    /** 发起系统设备管理员授权页 */
    fun adminRequestIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getApplication<Application>().getString(R.string.admin_explanation),
            )
        }

    fun disableAdmin() {
        dpm.removeActiveAdmin(admin)
        toast(R.string.toast_admin_removed)
        refresh()
    }

    /** 跳过初始化向导(记入本地,不再强制弹出) */
    fun dismissSetup() {
        setupDismissed = true
        prefs.edit().putBoolean(KEY_SETUP_DISMISSED, true).apply()
    }

    /** 把内置脚本资产导出到用户选择的位置(.bat 自动转换为 CRLF 换行) */
    fun exportAsset(assetName: String, uri: android.net.Uri) {
        val app = getApplication<Application>()
        try {
            app.contentResolver.openOutputStream(uri)?.use { out ->
                app.assets.open(assetName).use { input ->
                    if (assetName.endsWith(".bat")) {
                        val text = input.bufferedReader().readText()
                            .replace("\r\n", "\n")
                            .replace("\n", "\r\n")
                        out.write(text.toByteArray(Charsets.UTF_8))
                    } else {
                        input.copyTo(out)
                    }
                }
            } ?: throw IllegalStateException("无法打开输出位置")
            toast(R.string.setup_toast_export_ok)
        } catch (e: Exception) {
            toast(R.string.setup_toast_export_fail)
        }
    }

    fun hide(pkg: String) = setHidden(pkg, hidden = true)

    fun unhide(pkg: String) = setHidden(pkg, hidden = false)

    /** 读取已安装应用:用户应用在前、系统应用在后,均按名称排序;排除本应用(自身不可隐藏) */
    fun installedApps(): List<AppInfo> = try {
        val pm = getApplication<Application>().packageManager
        pm.getInstalledApplications(0)
            .filter { it.packageName != getApplication<Application>().packageName }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = it.loadLabel(pm).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedWith(compareBy({ !it.isSystem }, { it.label.lowercase() }))
    } catch (e: Exception) {
        emptyList()
    }

    private fun setHidden(pkg: String, hidden: Boolean) {
        val app = getApplication<Application>()
        when {
            pkg.isEmpty() -> toast(R.string.toast_pkg_empty)
            pkg == app.packageName -> toast(R.string.toast_self_hide_denied)
            !adminActive -> {
                toast(R.string.toast_need_admin)
            }

            else -> try {
                dpm.setApplicationHidden(admin, pkg, hidden)
                updateHiddenSet(pkg, hidden)
                toast(if (hidden) R.string.toast_hide_success else R.string.toast_unhide_success, pkg)
            } catch (e: SecurityException) {
                toast(R.string.toast_need_device_owner)
            } catch (e: Exception) {
                toast(R.string.toast_op_failed, e.message ?: e.javaClass.simpleName)
            }
        }
        updateTargetStatus()
        refreshHiddenList()
    }

    /** 实时显示目标应用的安装与隐藏状态 */
    private fun updateTargetStatus() {
        val app = getApplication<Application>()
        val pkg = pkgInput.trim()
        if (pkg.isEmpty()) {
            targetStatus = TargetStatus.Empty
            return
        }
        val installed = try {
            app.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (!installed) {
            targetStatus = TargetStatus.NotInstalled(pkg)
            return
        }
        targetStatus = if (adminActive && deviceOwner) {
            try {
                if (dpm.isApplicationHidden(admin, pkg)) TargetStatus.Hidden else TargetStatus.Shown
            } catch (e: Exception) {
                TargetStatus.Unknown
            }
        } else {
            TargetStatus.Unknown
        }
    }

    /** 刷新"已隐藏应用"列表;记录来自本地 SharedPreferences,并尽量实时验证 */
    private fun refreshHiddenList() {
        val app = getApplication<Application>()
        val saved = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()
        val canVerify = adminActive && deviceOwner
        hiddenApps = saved.map { pkg ->
            val label = try {
                app.packageManager.getApplicationInfo(pkg, 0).loadLabel(app.packageManager).toString()
            } catch (e: Exception) {
                pkg
            }
            val hidden = if (canVerify) {
                try {
                    dpm.isApplicationHidden(admin, pkg)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            AppInfo(
                packageName = pkg,
                label = label,
                isHidden = hidden ?: true,
                hiddenVerified = hidden != null,
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun updateHiddenSet(pkg: String, hidden: Boolean) {
        val set = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (hidden) set.add(pkg) else set.remove(pkg)
        prefs.edit().putStringSet(KEY_HIDDEN, set).apply()
    }

    private fun toast(res: Int, vararg args: Any?) {
        Toast.makeText(
            getApplication<Application>(),
            getApplication<Application>().getString(res, *args),
            Toast.LENGTH_SHORT,
        ).show()
    }

    companion object {
        private const val PREFS_NAME = "hidden_apps"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_SETUP_DISMISSED = "setup_dismissed"
    }
}
