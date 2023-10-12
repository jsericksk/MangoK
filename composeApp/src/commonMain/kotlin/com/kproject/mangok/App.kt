package com.kproject.mangok

import androidx.compose.runtime.Composable
import com.kproject.mangok.theme.AppTheme

@Composable
internal fun App() = AppTheme {
    MainScreen()
}

internal expect fun getPlatform(): String