package cc.lycstar.admin

/**
 * 应用信息条目。
 *
 * @param packageName 包名
 * @param label 应用显示名称(读取失败时为包名)
 * @param isSystem 是否为系统预装应用
 * @param isHidden 是否处于隐藏状态(仅"已隐藏应用"列表使用)
 * @param hiddenVerified 隐藏状态是否已通过 DevicePolicyManager 实时验证
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean = false,
    val isHidden: Boolean = false,
    val hiddenVerified: Boolean = false,
)
