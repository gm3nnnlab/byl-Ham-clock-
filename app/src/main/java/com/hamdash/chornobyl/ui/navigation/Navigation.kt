package com.hamdash.chornobyl.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector

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
}

enum class TopLevel(val route: String, val label: String, val icon: ImageVector, val routes: Set<String>) {
    Dashboard(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard, setOf(Routes.DASHBOARD)),
    Map(Routes.MAP, "Map", Icons.Filled.Map, setOf(Routes.MAP)),
    Repeaters(Routes.REPEATERS, "Repeaters", Icons.Filled.CellTower, setOf(Routes.REPEATERS)),
    Dmr(Routes.DMR, "DMR", Icons.Filled.Radio, setOf(Routes.DMR)),
    More(
        Routes.MORE,
        "More",
        Icons.Filled.MoreHoriz,
        setOf(Routes.MORE, Routes.BANDS, Routes.DIGITAL, Routes.PROPAGATION, Routes.SETTINGS, Routes.ABOUT),
    ),
}
