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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 应用列表筛选条件 */
enum class AppFilter { All, Hidden, Shown }

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
    var setupDismissed by mutableStateOf(false)
        private set

    /** 已安装应用 + 历史隐藏记录合并后的统一列表 */
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
    var filter by mutableStateOf(AppFilter.All)
    var selection by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        setupDismissed = prefs.getBoolean(KEY_SETUP_DISMISSED, false)
        refresh()
    }

    /** 重新读取权限状态并重建应用列表 */
    fun refresh() {
        adminActive = dpm.isAdminActive(admin)
        deviceOwner = try {
            dpm.isDeviceOwnerApp(getApplication<Application>().packageName)
        } catch (e: Exception) {
            false
        }
        rebuildApps()
    }

    fun onQueryChange(value: String) {
        query = value
    }

    fun toggleSelect(pkg: String) {
        selection = if (pkg in selection) selection - pkg else selection + pkg
    }

    fun clearSelection() {
        selection = emptySet()
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

    /** 单个应用隐藏/恢复(手动输入包名场景) */
    fun hide(pkg: String) = setHidden(pkg, hidden = true)

    fun unhide(pkg: String) = setHidden(pkg, hidden = false)

    /** 批量隐藏/恢复已选应用 */
    fun hideSelected() = batchSetHidden(hidden = true)

    fun unhideSelected() = batchSetHidden(hidden = false)

    private fun batchSetHidden(hidden: Boolean) {
        val app = getApplication<Application>()
        val targets = selection.toList()
        if (targets.isEmpty()) return
        when {
            !adminActive -> toast(R.string.toast_need_admin)
            !deviceOwner -> toast(R.string.toast_need_device_owner)

            else -> {
                var ok = 0
                var fail = 0
                for (pkg in targets) {
                    if (pkg == app.packageName) {
                        fail++
                        continue
                    }
                    try {
                        dpm.setApplicationHidden(admin, pkg, hidden)
                        updateHiddenSet(pkg, hidden)
                        ok++
                    } catch (e: Exception) {
                        fail++
                    }
                }
                clearSelection()
                toast(
                    if (hidden) R.string.toast_batch_hide else R.string.toast_batch_unhide,
                    ok,
                    fail,
                )
            }
        }
        refresh()
    }

    private fun setHidden(pkg: String, hidden: Boolean) {
        val app = getApplication<Application>()
        when {
            pkg.isEmpty() -> toast(R.string.toast_pkg_empty)
            pkg == app.packageName -> toast(R.string.toast_self_hide_denied)
            !adminActive -> toast(R.string.toast_need_admin)

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
        refresh()
    }

    /**
     * 重建统一应用列表:已安装应用 + 仅存在于本地隐藏记录中的包(已卸载但被预设置隐藏)。
     * 包含数百个应用的标签读取与 DPM 状态查询,放到后台线程执行。
     *
     * 注意:后台线程禁止读取任何 Compose 状态(它们可能诞生于尚未 apply 的组合快照),
     * 因此权限标志先在主线程读取为普通参数再传入。
     */
    private fun rebuildApps() {
        val canVerify = adminActive && deviceOwner
        loading = true
        viewModelScope.launch(Dispatchers.Default) {
            val list = computeApps(canVerify)
            withContext(Dispatchers.Main) {
                apps = list
                loading = false
            }
        }
    }

    private fun computeApps(canVerify: Boolean): List<AppInfo> {
        val app = getApplication<Application>()
        val pm = app.packageManager
        val saved = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()

        val installed = pm.getInstalledApplications(0)
        val installedPkgs = installed.mapTo(mutableSetOf()) { it.packageName }

        val result = mutableListOf<AppInfo>()
        for (info in installed) {
            val pkg = info.packageName
            if (pkg == app.packageName) continue
            val label = try {
                info.loadLabel(pm).toString()
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
            result += AppInfo(
                packageName = pkg,
                label = label,
                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                installed = true,
                hidden = hidden,
            )
        }

        // 仅有本地隐藏记录、当前未安装的包(隐藏状态会在安装后自动生效)
        for (pkg in saved) {
            if (pkg == app.packageName || pkg in installedPkgs) continue
            val hidden = if (canVerify) {
                try {
                    dpm.isApplicationHidden(admin, pkg)
                } catch (e: Exception) {
                    null
                }
            } else {
                true
            }
            result += AppInfo(
                packageName = pkg,
                label = pkg,
                isSystem = false,
                installed = false,
                hidden = hidden,
            )
        }

        return result.sortedWith(
            compareBy(
                { it.installed },                    // 未安装的隐藏记录排最前(需要关注)
                { it.isSystem },                     // 用户应用在前
                { it.label.lowercase() },
            )
        )
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
