package com.acurast.attested.executor.ui.home

import acurast.codec.extensions.blake2b
import acurast.codec.extensions.fromSS58
import acurast.codec.extensions.hexToBa
import acurast.codec.type.AccountId32
import acurast.codec.type.CurveKind
import acurast.codec.type.MultiSignature
import acurast.rpc.RPC
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acurast.attested.executor.App
import com.acurast.attested.executor.BuildConfig
import com.acurast.attested.executor.Constants
import com.acurast.attested.executor.R
import com.acurast.attested.executor.crypto.Attestation
import com.acurast.attested.executor.protocol.acurast.AcurastRPC
import com.acurast.attested.executor.ui.theme.AcurastTheme
import com.acurast.attested.executor.utils.Notification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

@Composable
fun Overview(navigateToSettings: () -> Unit, navigateToAddresses: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        OverviewContent(
            navigateToSettings,
            navigateToAddresses
        )
    }
}

@Composable
fun OverviewAppBar(
    modifier: Modifier = Modifier,
    navigateToSettings: () -> Unit,
    navigateToAddresses: () -> Unit
) {
    val appBarColor = MaterialTheme.colors.surface

    // Draw a scrim over the status bar which matches the app bar
    Spacer(
        Modifier
            .background(appBarColor)
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
    )

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.app_title),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        backgroundColor = appBarColor,
        actions = {
            IconButton(
                onClick = navigateToAddresses
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_qr_code_24),
                    contentDescription = stringResource(R.string.qr_code),
                    modifier = Modifier.heightIn(max = 24.dp)
                )
            }
            IconButton(
                onClick = navigateToSettings
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.heightIn(max = 24.dp)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun OverviewContent(
    navigateToSettings: () -> Unit,
    navigateToAddresses: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
            .background(MaterialTheme.colors.secondary)
    ) {
        OverviewAppBar(
            modifier = Modifier.fillMaxWidth(),
            navigateToSettings,
            navigateToAddresses
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.primary
            )
            Divider(thickness = 20.dp)
            DeviceLoadCard()
            Divider(thickness = 10.dp)
            ConnectionStatusCard()
            Divider(thickness = 10.dp)
            MarketplaceCard()
            Divider(thickness = 10.dp)
            VersionCard()
        }
    }
}

@Composable
fun DeviceLoadCard() {
    AcurastTheme {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surface.copy(alpha = 0.09f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Device Load",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.secondary
    ) {
        val viewModel = viewModel<AttestationViewModel>()
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "Connection",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Divider(color = Color.Black, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manager: ",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    text = viewModel.managerId.collectAsState().value?.toString() ?: "not set",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray
                )
            }
            if (viewModel.isLoading.collectAsState().value) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attestation: ",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "loading...",
                        style = MaterialTheme.typography.caption,
                        color = Color.LightGray
                    )
                }
            } else if (viewModel.isAttested.collectAsState().value) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attestation: ",
                        style = MaterialTheme.typography.caption
                    )
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.heightIn(max = 18.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attestation: ",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "not attested",
                        style = MaterialTheme.typography.caption,
                        color = Color.Red
                    )
                }
                Divider(thickness = 10.dp)
                Button(
                    onClick = { viewModel.attestDevice() },
                    border = BorderStroke(1.dp, Color.Blue),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 10.dp,
                        pressedElevation = 15.dp,
                        disabledElevation = 0.dp
                    ),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Submit Attestation",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun MarketplaceCard() {
    AcurastTheme {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surface.copy(alpha = 0.09f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_storefront_24),
                        contentDescription = stringResource(R.string.settings),
                        tint = Color.Blue,
                        modifier = Modifier.heightIn(max = 18.dp)
                    )
                    Text(
                        text = "Marketplace",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                }
                // TODO: Remove in favor of manager dashboard
                if (Constants.SHOW_ADVERTISEMENT_BUTTON) {
                    Divider(thickness = 10.dp)
                    Button(
                        onClick = {
                            AcurastRPC.extrinsic.advertise(22) {}
                        },
                        border = BorderStroke(1.dp, Color.Blue),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 15.dp,
                            disabledElevation = 0.dp
                        ),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Advertise resources",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun VersionCard() {
    AcurastTheme {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.surface.copy(alpha = 0.09f)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

class AttestationViewModel : ViewModel() {
    private var _loading = MutableStateFlow(true)
    val isLoading = _loading.asStateFlow()
    private var _isAttested = MutableStateFlow(false)
    val isAttested = _isAttested.asStateFlow()
    private var _managerId = MutableStateFlow<Int?>(null)
    val managerId = _managerId.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    _loading.value = true
                    _isAttested.value = isAttested()
                    _managerId.value = AcurastRPC.queries.managerIdentifier()
                    _loading.value = false
                    delay(30_000)
                } catch (e: Throwable) {
                    Log.e("AttestationViewModel", e.toString())
                }
            }
        }
    }

    /**
     * Submit attestation to validate the mobile device.
     */
    fun attestDevice() {
        viewModelScope.launch {
            try {
                AcurastRPC.extrinsic.submitAttestation()
                Notification.notify("Success", "The device is now attested.")
            } catch (e: Throwable) {
                Log.e("Attestation", Log.getStackTraceString(e))
                Notification.notify("Failure", "Failed to submit attestation.")
            }
        }
    }

    /**
     * Verify if the account associated to the device is attested
     */
    private suspend fun isAttested(): Boolean {
        val rpc = RPC(Constants.ACURAST_RPC)
        val accountId = App.Protocol.acurast.getSigner().getPublicKey().blake2b(256)

        return rpc.isAttested(accountId)
    }
}