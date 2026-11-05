package cc.lycstar.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cc.lycstar.admin.BlacklistViewModel

/**
 * 应用根路由:
 * - 尚未成为设备所有者且未跳过向导时,显示初始化设置页
 * - 主界面权限卡片可重新打开向导
 * - 应用回到前台时自动重新检测权限状态(如在电脑上执行完 adb 后回到应用)
 */
@Composable
fun AppRoot(viewModel: BlacklistViewModel, onAdminFlowRequested: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showSetup by remember { mutableStateOf(false) }

    val forceSetup = showSetup || (!viewModel.deviceOwner && !viewModel.setupDismissed)
    if (forceSetup) {
        SetupScreen(
            viewModel = viewModel,
            onAdminFlowRequested = onAdminFlowRequested,
            onClose = { showSetup = false },
        )
    } else {
        BlacklistScreen(
            viewModel = viewModel,
            onAdminFlowRequested = onAdminFlowRequested,
            onOpenSetup = { showSetup = true },
        )
    }
}
