package com.hamdash.chornobyl.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.ui.dashboard.DashboardScreen
import com.hamdash.chornobyl.ui.dmr.DmrScreen
import com.hamdash.chornobyl.ui.map.MapScreen
import com.hamdash.chornobyl.ui.more.AboutScreen
import com.hamdash.chornobyl.ui.more.BandsScreen
import com.hamdash.chornobyl.ui.more.DigitalScreen
import com.hamdash.chornobyl.ui.more.MoreScreen
import com.hamdash.chornobyl.ui.more.PropagationScreen
import com.hamdash.chornobyl.ui.navigation.Routes
import com.hamdash.chornobyl.ui.navigation.TopLevel
import com.hamdash.chornobyl.ui.radiation.RadiationScreen
import com.hamdash.chornobyl.ui.repeaters.RepeatersScreen
import com.hamdash.chornobyl.ui.settings.SettingsScreen
import com.hamdash.chornobyl.ui.weather.FpvScreen
import com.hamdash.chornobyl.ui.weather.WeatherScreen
import com.hamdash.chornobyl.ui.zone.ZoneScreen

private fun NavHostController.goTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

@Composable
fun HamDashRoot(@Suppress("UNUSED_PARAMETER") container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TopLevel.entries.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute in item.routes,
                        onClick = { navController.goTopLevel(item.route) },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenMap = { navController.goTopLevel(Routes.MAP) },
                    onOpenRepeaters = { navController.goTopLevel(Routes.REPEATERS) },
                    onOpenDmr = { navController.goTopLevel(Routes.DMR) },
                    onOpenPropagation = { navController.navigate(Routes.PROPAGATION) { launchSingleTop = true } },
                    onOpenWeather = { navController.navigate(Routes.WEATHER) { launchSingleTop = true } },
                    onOpenFpv = { navController.navigate(Routes.FPV) { launchSingleTop = true } },
                    onOpenZone = { navController.navigate(Routes.ZONE) { launchSingleTop = true } },
                )
            }
            composable(Routes.MAP) { MapScreen() }
            composable(Routes.REPEATERS) { RepeatersScreen() }
            composable(Routes.DMR) { DmrScreen() }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenBands = { navController.navigate(Routes.BANDS) { launchSingleTop = true } },
                    onOpenDigital = { navController.navigate(Routes.DIGITAL) { launchSingleTop = true } },
                    onOpenPropagation = { navController.navigate(Routes.PROPAGATION) { launchSingleTop = true } },
                    onOpenWeather = { navController.navigate(Routes.WEATHER) { launchSingleTop = true } },
                    onOpenFpv = { navController.navigate(Routes.FPV) { launchSingleTop = true } },
                    onOpenZone = { navController.navigate(Routes.ZONE) { launchSingleTop = true } },
                    onOpenRadiation = { navController.navigate(Routes.RADIATION) { launchSingleTop = true } },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) { launchSingleTop = true } },
                )
            }
            composable(Routes.BANDS) { BandsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.DIGITAL) { DigitalScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PROPAGATION) { PropagationScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.WEATHER) {
                WeatherScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFpv = { navController.navigate(Routes.FPV) { launchSingleTop = true } },
                )
            }
            composable(Routes.RADIATION) {
                RadiationScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMap = { navController.goTopLevel(Routes.MAP) },
                )
            }
            composable(Routes.FPV) { FpvScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ZONE) {
                ZoneScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMap = { navController.goTopLevel(Routes.MAP) },
                )
            }
        }
    }
}
