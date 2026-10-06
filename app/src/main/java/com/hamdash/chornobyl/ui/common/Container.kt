package com.hamdash.chornobyl.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.HamDashApplication

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as HamDashApplication).container }
}
