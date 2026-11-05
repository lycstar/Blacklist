package cc.lycstar.admin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.lycstar.admin.AppInfo
import cc.lycstar.admin.BlacklistViewModel
import cc.lycstar.admin.R
import cc.lycstar.admin.TargetStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel,
    onAdminFlowRequested: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    var showDisableDialog by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            PermissionCard(
                adminActive = viewModel.adminActive,
                deviceOwner = viewModel.deviceOwner,
                onToggleAdmin = {
                    if (viewModel.adminActive) showDisableDialog = true else onAdminFlowRequested()
                },
                onOpenSetup = onOpenSetup,
            )
            Spacer(Modifier.height(16.dp))

            TargetCard(
                pkgInput = viewModel.pkgInput,
                status = viewModel.targetStatus,
                onPkgChange = viewModel::onPkgChange,
                onHide = { viewModel.hide(viewModel.pkgInput.trim()) },
                onUnhide = { viewModel.unhide(viewModel.pkgInput.trim()) },
                onPickApp = { showPicker = true },
            )
            Spacer(Modifier.height(16.dp))

            HiddenAppsCard(
                apps = viewModel.hiddenApps,
                onSelect = { viewModel.onPkgChange(it.packageName) },
                onRestore = { viewModel.unhide(it.packageName) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDisableDialog) {
        DisableAdminDialog(
            onConfirm = {
                showDisableDialog = false
                viewModel.disableAdmin()
            },
            onDismiss = { showDisableDialog = false },
        )
    }
    if (showPicker) {
        AppPickerDialog(
            apps = remember { viewModel.installedApps() },
            onSelect = { pkg ->
                viewModel.onPkgChange(pkg)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** 状态等级:正常=primary,警告=tertiary,异常=error,未知=outline */
private enum class StatusLevel { Ok, Warn, Bad, Neutral }

private fun statusColor(scheme: ColorScheme, level: StatusLevel): Color = when (level) {
    StatusLevel.Ok -> scheme.primary
    StatusLevel.Warn -> scheme.tertiary
    StatusLevel.Bad -> scheme.error
    StatusLevel.Neutral -> scheme.outline
}

@Composable
private fun StatusText(text: String, level: StatusLevel) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = statusColor(MaterialTheme.colorScheme, level),
    )
}

@Composable
private fun PermissionCard(
    adminActive: Boolean,
    deviceOwner: Boolean,
    onToggleAdmin: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_permission)) {
        PermissionRow(
            label = stringResource(R.string.perm_admin),
            status = stringResource(if (adminActive) R.string.status_active else R.string.status_inactive),
            level = if (adminActive) StatusLevel.Ok else StatusLevel.Bad,
        )
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            label = stringResource(R.string.perm_owner),
            status = stringResource(if (deviceOwner) R.string.status_yes else R.string.status_no),
            level = if (deviceOwner) StatusLevel.Ok else StatusLevel.Warn,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(if (deviceOwner) R.string.owner_hint_ok else R.string.owner_hint_no),
            style = MaterialTheme.typography.bodySmall,
            color = statusColor(
                MaterialTheme.colorScheme,
                if (deviceOwner) StatusLevel.Ok else StatusLevel.Warn,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onToggleAdmin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (adminActive) R.string.action_disable_admin else R.string.action_grant_admin
                )
            )
        }
        if (!deviceOwner) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onOpenSetup,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.main_open_setup)) }
        }
    }
}

@Composable
private fun PermissionRow(label: String, status: String, level: StatusLevel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        StatusText(text = status, level = level)
    }
}

@Composable
private fun TargetCard(
    pkgInput: String,
    status: TargetStatus,
    onPkgChange: (String) -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onPickApp: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_target)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pkgInput,
                onValueChange = onPkgChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.pkg_hint)) },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onPickApp) {
                Text(stringResource(R.string.pkg_pick))
            }
        }
        Spacer(Modifier.height(8.dp))
        val (statusText, level) = when (val s = status) {
            TargetStatus.Empty ->
                stringResource(R.string.pkg_status_hint) to StatusLevel.Neutral

            is TargetStatus.NotInstalled ->
                stringResource(R.string.pkg_status_not_installed, s.pkg) to StatusLevel.Warn

            TargetStatus.Hidden ->
                stringResource(R.string.pkg_status_hidden_normal) to StatusLevel.Bad

            TargetStatus.Shown ->
                stringResource(R.string.pkg_status_shown_normal) to StatusLevel.Ok

            TargetStatus.Unknown ->
                stringResource(R.string.pkg_status_unknown) to StatusLevel.Neutral
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = statusColor(MaterialTheme.colorScheme, level),
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = onHide,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_hide)) }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onUnhide,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_unhide)) }
        }
    }
}

@Composable
private fun HiddenAppsCard(
    apps: List<AppInfo>,
    onSelect: (AppInfo) -> Unit,
    onRestore: (AppInfo) -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_hidden)) {
        Text(
            text = stringResource(R.string.hidden_count, apps.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        if (apps.isEmpty()) {
            Text(
                text = stringResource(R.string.hidden_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            apps.forEach { app ->
                AppRow(
                    app = app,
                    mode = AppRowMode.HiddenList,
                    onClick = { onSelect(app) },
                    onRestore = { onRestore(app) },
                )
            }
        }
    }
}

private enum class AppRowMode { HiddenList, Picker }

@Composable
private fun AppRow(
    app: AppInfo,
    mode: AppRowMode,
    onClick: () -> Unit,
    onRestore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (app.isSystem) {
                    stringResource(R.string.label_with_system_tag, app.label)
                } else {
                    app.label
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (mode == AppRowMode.HiddenList) {
            val (tagText, level) = when {
                app.hiddenVerified && app.isHidden ->
                    stringResource(R.string.hidden_tag) to StatusLevel.Warn

                app.hiddenVerified ->
                    stringResource(R.string.hidden_tag_shown) to StatusLevel.Ok

                else -> stringResource(R.string.hidden_tag_unverified) to StatusLevel.Neutral
            }
            TagPill(text = tagText, level = level)
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onRestore?.invoke() },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            ) { Text(stringResource(R.string.action_restore)) }
        }
    }
}

@Composable
private fun TagPill(text: String, level: StatusLevel) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = statusColor(scheme, level).copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor(scheme, level),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DisableAdminDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_disable_admin_title)) },
        text = { Text(stringResource(R.string.dialog_disable_admin_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun AppPickerDialog(
    apps: List<AppInfo>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_picker_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dialog_picker_search_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            mode = AppRowMode.Picker,
                            onClick = { onSelect(app.packageName) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
