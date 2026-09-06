package com.convx.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.modulehost.DeclarativeModuleHostViewModel
import com.convx.modulehost.DeclarativeModuleHostLifecycle
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
    val snapshot = viewModel.snapshot
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
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.link),
                    title = {
                        Text(
                            stringResource(
                                if (started) R.string.module_host_ready else R.string.module_host_not_started,
                            )
                        )
                    },
                    description = {
                        Text(stringResource(R.string.module_host_version, snapshot.convxVersion))
                    },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = {
                        Text(stringResource(R.string.module_host_registered_modules, snapshot.modules.size))
                    },
                    description = {
                        Text(stringResource(R.string.module_host_no_installation))
                    },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = { Text(stringResource(R.string.module_host_scope)) },
                    enabled = false,
                ),
            ),
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
