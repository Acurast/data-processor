package com.acurast.attested.executor.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acurast.attested.executor.App
import com.acurast.attested.executor.R
import com.acurast.attested.executor.utils.Device

@Composable
fun Settings(onBackPress: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        SettingsContent(onBackPress)
    }
}

@Composable
private fun SettingsAppBar(onBackPress: () -> Unit) {
    // Draw a scrim over the status bar which matches the app bar
    Spacer(
        Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackPress) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )
        }
        Text(
            text = "Overview",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun SettingsContent(onBackPress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
            .background(MaterialTheme.colors.secondary)
    ) {
        SettingsAppBar(onBackPress)

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Divider(thickness = 20.dp)
            UpdateApplicationButton()
            Divider(thickness = 10.dp)
            ClearCacheButton()
            Divider(thickness = 10.dp)
            ResetDeviceButton()
        }
    }
}

@Composable
fun ClearCacheButton() {
    Button(
        onClick = { App.clearSharedPreferences() },
        elevation = ButtonDefaults.elevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp
        ),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = null,
                modifier = Modifier.heightIn(max = 24.dp)
            )
            Text(
                text = "Clear cache",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ResetDeviceButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            Device.factoryReset(context)
        },
        elevation = ButtonDefaults.elevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp
        ),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.heightIn(max = 24.dp),
                tint = Color.Red,
            )
            Text(
                text = "Wipe data",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun UpdateApplicationButton() {
    val activity = LocalContext.current as Activity
    Button(
        onClick = {
            Device.updateApp(activity)
        },
        elevation = ButtonDefaults.elevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp
        ),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
        modifier = Modifier.fillMaxWidth(),

        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_system_update_24),
                contentDescription = null,
                modifier = Modifier.heightIn(max = 24.dp),
            )
            Text(
                text = "Version update",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}