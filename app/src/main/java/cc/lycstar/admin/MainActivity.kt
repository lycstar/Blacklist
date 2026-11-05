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
        setContent {
            BlacklistTheme {
                AppRoot(
                    viewModel = viewModel,
                    onAdminFlowRequested = {
                        adminRequestLauncher.launch(viewModel.adminRequestIntent())
                    },
                )
            }
        }
    }
}
