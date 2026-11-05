package cc.lycstar.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyAdmin : DeviceAdminReceiver() {

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        showToast(context, context.getString(R.string.admin_toast_enabled))
    }

    override fun onDisabled(context: Context, intent: Intent) {
        showToast(context, context.getString(R.string.admin_toast_disabled))
    }

    /** 用户在系统设置中尝试停用管理员时的确认提示 */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)
}
