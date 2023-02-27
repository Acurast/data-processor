package com.acurast.attested.executor.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AcurastTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = AcurastColors,
        typography = AcurastTypography,
        shapes = AcurastShapes,
        content = content
    )
}