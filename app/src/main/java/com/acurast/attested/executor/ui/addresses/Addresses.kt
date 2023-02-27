package com.acurast.attested.executor.ui.addresses

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acurast.attested.executor.App
import com.acurast.attested.executor.crypto.CryptoLegacy
import com.acurast.attested.executor.utils.QR
import com.acurast.attested.executor.utils.toHex

@Composable
fun AddressesView(onBackPress: () -> Unit) {
    Surface(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.secondary)) {
        AddressesContent(onBackPress)
    }
}

@Composable
private fun TopAppBar(onBackPress: () -> Unit) {
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
fun AddressesContent(onBackPress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.secondary)
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
    ) {
        TopAppBar(onBackPress)

        val acurastPub = App.Signer.SECP_256_R1.getPublicKey(compressed = false).toHex()
        val qrCode = QR.encodeAsBitmap(acurastPub)
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Public Key",
                style = MaterialTheme.typography.overline,
                color = MaterialTheme.colors.primary
            )
            Image(
                painter = BitmapPainter(qrCode.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .width(256.dp)
            )
        }

        // Compute addresses for supported protocols
        val acurastAddress = App.Protocol.acurast.getAddress()
        val tezosAddress = App.Protocol.tezos.getAddress()
        val ethereumAddress = App.Protocol.ethereum.getAddress()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            AddressCard(chain = "Tezos", address = tezosAddress)
            Divider(thickness = 10.dp)
            AddressCard(chain = "Acurast", address = acurastAddress)
            Divider(thickness = 10.dp)
            AddressCard(chain = "Ethereum", address = ethereumAddress)
        }
    }
}


@Composable
fun AddressCard(chain: String, address: String) {
    Column {
        Text(
            text = chain,
            style = MaterialTheme.typography.overline,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
        Divider(thickness=5.dp)
        SelectionContainer {
            Text(
                text = address,
                style = MaterialTheme.typography.caption,
                overflow = TextOverflow.Ellipsis,
                fontSize = 8.sp,
                softWrap = false,
                color = Color.Gray
            )
        }
    }
}