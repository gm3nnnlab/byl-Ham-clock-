package com.hamdash.chornobyl.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector
import com.hamdash.chornobyl.R

object Routes {
    const val DASHBOARD = "dashboard"
    const val MAP = "map"
    const val REPEATERS = "repeaters"
    const val DMR = "dmr"
    const val MORE = "more"
    const val BANDS = "bands"
    const val DIGITAL = "digital"
    const val PROPAGATION = "propagation"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val WEATHER = "weather"
    const val FPV = "fpv"
    const val ZONE = "zone"
    const val RADIATION = "radiation"
}

enum class TopLevel(val route: String, @StringRes val labelRes: Int, val icon: ImageVector, val routes: Set<String>) {
    Dashboard(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, setOf(Routes.DASHBOARD)),
    Map(Routes.MAP, R.string.nav_map, Icons.Filled.Map, setOf(Routes.MAP)),
    Repeaters(Routes.REPEATERS, R.string.nav_repeaters, Icons.Filled.CellTower, setOf(Routes.REPEATERS)),
    Dmr(Routes.DMR, R.string.nav_dmr, Icons.Filled.Radio, setOf(Routes.DMR)),
    More(
        Routes.MORE,
        R.string.nav_more,
        Icons.Filled.MoreHoriz,
        setOf(
            Routes.MORE, Routes.BANDS, Routes.DIGITAL, Routes.PROPAGATION, Routes.SETTINGS, Routes.ABOUT,
            Routes.WEATHER, Routes.FPV, Routes.ZONE, Routes.RADIATION,
        ),
    ),
}
