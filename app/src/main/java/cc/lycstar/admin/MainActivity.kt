package cc.lycstar.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import cc.lycstar.admin.ui.AppRoot
import cc.lycstar.admin.ui.BlacklistTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BlacklistViewModel by viewModels()

    /** 系统设备管理员授权页返回后刷新状态 */
    private val adminRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 在组合开始前强制初始化 ViewModel,避免其 Compose 状态诞生于组合快照中
        val vm = viewModel
        setContent {
            BlacklistTheme {
                AppRoot(
                    viewModel = vm,
                    onAdminFlowRequested = {
                        adminRequestLauncher.launch(vm.adminRequestIntent())
                    },
                )
            }
        }
    }
}
