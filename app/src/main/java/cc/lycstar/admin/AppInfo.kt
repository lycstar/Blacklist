package cc.lycstar.admin

/**
 * 应用信息条目。
 *
 * @param packageName 包名
 * @param label 应用显示名称(读取失败时为包名)
 * @param isSystem 是否为系统预装应用
 * @param installed 是否已安装(历史隐藏记录对应的包可能已被卸载)
 * @param hidden 隐藏状态:null = 未知(无法验证,如缺少设备所有者权限)
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean = false,
    val installed: Boolean = true,
    val hidden: Boolean? = null,
)
