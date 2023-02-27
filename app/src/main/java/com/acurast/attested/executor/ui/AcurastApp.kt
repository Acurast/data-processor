package com.acurast.attested.executor.ui

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.acurast.attested.executor.R
import com.acurast.attested.executor.ui.addresses.AddressesView
import com.acurast.attested.executor.ui.home.Overview
import com.acurast.attested.executor.ui.settings.Settings

@Composable
fun AcurastApp(appState: AcurastAppState = rememberAcurastAppState()) {
    NavHost(
        navController = appState.navController,
        startDestination = Screen.Overview.route
    ) {
        composable(Screen.Overview.route) { backStackEntry ->
            Overview(
                navigateToSettings = {
                    appState.navigateToSettings(backStackEntry)
                },
                navigateToAddresses = {
                    appState.navigateToAddresses(backStackEntry)
                }
            )
        }
        composable(Screen.Addresses.route) { _ ->
            AddressesView(
                onBackPress = {
                    appState.navigateBack()
                }
            )
        }
        composable(Screen.Settings.route) { _ ->
            Settings(
                onBackPress = {
                    appState.navigateBack()
                }
            )
        }
    }

    // TODO: rework the connection status
    //if (!appState.isOnline) {
    //    OfflineDialog { appState.refreshOnline() }
    //}
}

@Composable
fun OfflineDialog(onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.connection_error_title)) },
        text = { Text(text = stringResource(R.string.connection_error_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry_label))
            }
        }
    )
}