package com.convx.music.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.modulehost.DeclarativeModuleHostViewModel
import com.convx.modulehost.DeclarativeModuleHostLifecycle
import com.convx.modulehost.ModuleState
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarativeModuleHostSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: DeclarativeModuleHostViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var pendingRemove by rememberSaveable { mutableStateOf<String?>(null) }

    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.installPackage(context.contentResolver, it) }
    }

    val started = snapshot.lifecycle == DeclarativeModuleHostLifecycle.STARTED

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            )
        )
        Material3SettingsGroup(
            title = stringResource(R.string.declarative_modules),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.add),
                        title = { Text(stringResource(R.string.module_host_install)) },
                        description = { Text(stringResource(R.string.module_host_install_hint)) },
                        enabled = started && !busy,
                        onClick = { installLauncher.launch(arrayOf("application/octet-stream")) },
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = {
                            Text(
                                stringResource(
                                    if (started) R.string.module_host_ready else R.string.module_host_not_started,
                                )
                            )
                        },
                        description = { Text(stringResource(R.string.module_host_version, snapshot.convxVersion)) },
                        enabled = false,
                    )
                )
            },
        )

        if (snapshot.modules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(27.dp))
            Material3SettingsGroup(
                title = stringResource(R.string.module_host_installed_modules),
                items = snapshot.modules.map { module ->
                    // Row tap toggles state only where the registry allows the
                    // transition; removal is the trailing trash button alone, so
                    // tapping it never also toggles the module.
                    val rowAction: (() -> Unit)? = when (module.state) {
                        ModuleState.INSTALLED, ModuleState.DISABLED -> {
                            { viewModel.enable(module.manifest.id) }
                        }
                        ModuleState.ENABLED -> {
                            { viewModel.disable(module.manifest.id) }
                        }
                        else -> null
                    }
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(module.manifest.name) },
                        description = {
                            Text(
                                stringResource(
                                    R.string.module_host_module_state,
                                    module.manifest.version,
                                    stringResource(module.state.stringRes()),
                                )
                            )
                        },
                        onClick = rowAction,
                        trailingContent = {
                            IconButton(
                                onClick = { pendingRemove = module.manifest.id },
                                onLongClick = { pendingRemove = module.manifest.id },
                            ) {
                                Icon(
                                    painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.module_host_remove),
                                )
                            }
                        },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    val removing = pendingRemove?.let { id -> snapshot.modules.firstOrNull { it.manifest.id == id } }
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text(stringResource(R.string.module_host_remove_confirm_title)) },
            text = { Text(stringResource(R.string.module_host_remove_confirm_body, removing.manifest.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(removing.manifest.id)
                    pendingRemove = null
                }) {
                    Text(stringResource(R.string.module_host_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    val message = errorMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            icon = { Icon(painterResource(R.drawable.error), null) },
            title = { Text(stringResource(R.string.module_host_error)) },
            text = { Text(message) },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.declarative_modules)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        windowInsets = appTopBarWindowInsets(),
        scrollBehavior = scrollBehavior,
    )
}

private fun ModuleState.stringRes(): Int = when (this) {
    ModuleState.DISCOVERED -> R.string.module_state_discovered
    ModuleState.VALIDATED -> R.string.module_state_validated
    ModuleState.STAGED -> R.string.module_state_staged
    ModuleState.INSTALLED -> R.string.module_state_installed
    ModuleState.ENABLED -> R.string.module_state_enabled
    ModuleState.DISABLED -> R.string.module_state_disabled
    ModuleState.INVALID -> R.string.module_state_invalid
    ModuleState.INCOMPATIBLE -> R.string.module_state_incompatible
    ModuleState.FAILED -> R.string.module_state_failed
    ModuleState.ROLLED_BACK -> R.string.module_state_rolled_back
}
