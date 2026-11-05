package cc.lycstar.admin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.lycstar.admin.AppFilter
import cc.lycstar.admin.AppInfo
import cc.lycstar.admin.BlacklistViewModel
import cc.lycstar.admin.R

/**
 * 主界面:统一应用列表(名称+包名两行),支持搜索、筛选、多选批量隐藏/恢复。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel,
    onAdminFlowRequested: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val filtered = remember(viewModel.apps, viewModel.query, viewModel.filter) {
        filterApps(viewModel.apps, viewModel.query, viewModel.filter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Text(text = "⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_manual_input)) },
                            onClick = {
                                menuOpen = false
                                showManualDialog = true
                            },
                        )
                        if (viewModel.adminActive) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_disable_admin)) },
                                onClick = {
                                    menuOpen = false
                                    showDisableDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {

            // 授权未完成时显示引导条
            if (!viewModel.adminActive || !viewModel.deviceOwner) {
                SetupBanner(onClick = onOpenSetup)
            }

            // 搜索框
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.main_search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.onQueryChange("") },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Text("✕") }
                    }
                },
            )

            if (viewModel.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 筛选 Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = viewModel.filter == AppFilter.All,
                    onClick = { viewModel.filter = AppFilter.All },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
                FilterChip(
                    selected = viewModel.filter == AppFilter.Hidden,
                    onClick = { viewModel.filter = AppFilter.Hidden },
                    label = { Text(stringResource(R.string.filter_hidden)) },
                )
                FilterChip(
                    selected = viewModel.filter == AppFilter.Shown,
                    onClick = { viewModel.filter = AppFilter.Shown },
                    label = { Text(stringResource(R.string.filter_shown)) },
                )
            }

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            selected = app.packageName in viewModel.selection,
                            onToggle = { viewModel.toggleSelect(app.packageName) },
                        )
                    }
                }
            }

            // 多选底部操作栏
            AnimatedVisibility(
                visible = viewModel.selection.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.selected_count, viewModel.selection.size),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            TextButton(
                                onClick = viewModel::clearSelection,
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.action_clear_selection),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Button(
                            onClick = viewModel::hideSelected,
                            enabled = viewModel.adminActive && viewModel.deviceOwner,
                        ) { Text(stringResource(R.string.action_hide)) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = viewModel::unhideSelected,
                            enabled = viewModel.adminActive && viewModel.deviceOwner,
                        ) { Text(stringResource(R.string.action_unhide)) }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        ManualInputDialog(
            onDismiss = { showManualDialog = false },
            onHide = { pkg ->
                showManualDialog = false
                viewModel.hide(pkg)
            },
            onUnhide = { pkg ->
                showManualDialog = false
                viewModel.unhide(pkg)
            },
        )
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
}

private fun filterApps(
    apps: List<AppInfo>,
    query: String,
    filter: AppFilter,
): List<AppInfo> {
    val q = query.trim()
    return apps.filter { app ->
        val matchQuery = q.isEmpty() ||
            app.label.contains(q, ignoreCase = true) ||
            app.packageName.contains(q, ignoreCase = true)
        val matchFilter = when (filter) {
            AppFilter.All -> true
            AppFilter.Hidden -> app.hidden == true
            AppFilter.Shown -> app.installed && app.hidden != true
        }
        matchQuery && matchFilter
    }
}

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(R.string.main_banner_setup),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AppRow(app: AppInfo, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (app.isSystem) {
                    stringResource(R.string.label_with_system_tag, app.label)
                } else {
                    app.label
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!app.installed) {
            StatusBadge(text = stringResource(R.string.badge_not_installed), level = StatusLevel.Neutral)
            Spacer(Modifier.width(8.dp))
        } else if (app.hidden == true) {
            StatusBadge(text = stringResource(R.string.badge_hidden), level = StatusLevel.Warn)
            Spacer(Modifier.width(8.dp))
        }
    }
}

private enum class StatusLevel { Ok, Warn, Bad, Neutral }

private fun statusColor(scheme: ColorScheme, level: StatusLevel) = when (level) {
    StatusLevel.Ok -> scheme.primary
    StatusLevel.Warn -> scheme.tertiary
    StatusLevel.Bad -> scheme.error
    StatusLevel.Neutral -> scheme.outline
}

@Composable
private fun StatusBadge(text: String, level: StatusLevel) {
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
private fun ManualInputDialog(
    onDismiss: () -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
) {
    var pkg by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_dialog_title)) },
        text = {
            OutlinedTextField(
                value = pkg,
                onValueChange = { pkg = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.pkg_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { onUnhide(pkg.trim()) },
                    enabled = pkg.isNotBlank(),
                ) { Text(stringResource(R.string.action_unhide)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onHide(pkg.trim()) },
                    enabled = pkg.isNotBlank(),
                ) { Text(stringResource(R.string.action_hide)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
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
