package cc.lycstar.admin.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.lycstar.admin.BlacklistViewModel
import cc.lycstar.admin.R

/**
 * 初始化向导(Shizuku 风格):分步引导用户激活设备管理员、
 * 在电脑上执行授权命令(复制按钮提供精确命令),并自动检测授权结果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: BlacklistViewModel,
    onAdminFlowRequested: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val pkg = context.packageName
    val ownerCommand = "adb shell dpm set-device-owner $pkg/.MyAdmin"
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.setup_title)) },
            actions = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.setup_close)) }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            // ---------- 第 1 步:激活设备管理员 ----------
            StepCard(step = 1, title = stringResource(R.string.setup_step1_title)) {
                Text(
                    text = stringResource(R.string.setup_step1_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (viewModel.adminActive) {
                    StepDoneText(stringResource(R.string.setup_step1_done))
                } else {
                    Button(
                        onClick = onAdminFlowRequested,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_grant_admin)) }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ---------- 第 2 步:授权设备所有者 ----------
            StepCard(step = 2, title = stringResource(R.string.setup_step2_title)) {
                Text(
                    text = stringResource(R.string.setup_step2_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                BulletText(stringResource(R.string.setup_prereq_debug))
                BulletText(stringResource(R.string.setup_prereq_connect))
                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.setup_command_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                CodeBox(text = ownerCommand)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(ownerCommand))
                        Toast.makeText(context, R.string.setup_toast_copied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.setup_action_copy)) }
            }
            Spacer(Modifier.height(16.dp))

            // ---------- 机型兼容性提示 ----------
            CompatCard()

            Spacer(Modifier.height(16.dp))

            // ---------- 第 3 步:确认授权状态 ----------
            StepCard(step = 3, title = stringResource(R.string.setup_step3_title)) {
                Text(
                    text = stringResource(R.string.setup_step3_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                StatusRow(
                    label = stringResource(R.string.perm_admin),
                    ok = viewModel.adminActive,
                    okText = stringResource(R.string.status_active),
                    badText = stringResource(R.string.status_inactive),
                )
                Spacer(Modifier.height(8.dp))
                StatusRow(
                    label = stringResource(R.string.perm_owner),
                    ok = viewModel.deviceOwner,
                    okText = stringResource(R.string.status_yes),
                    badText = stringResource(R.string.status_no),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.setup_action_detect)) }
            }

            if (!viewModel.deviceOwner) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        viewModel.dismissSetup()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.setup_action_skip)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CompatCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_compat_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_compat_dual_app),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_compat_miui),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_compat_other),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun StepDoneText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BulletText(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusRow(label: String, ok: Boolean, okText: String, badText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (ok) okText else badText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun CodeBox(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(12.dp),
        )
    }
}
