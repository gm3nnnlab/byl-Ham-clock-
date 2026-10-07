package com.hamdash.chornobyl.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hamdash.chornobyl.HamDashApplication
import com.hamdash.chornobyl.ui.dashboard.DashboardViewModel
import com.hamdash.chornobyl.ui.dmr.DmrViewModel
import com.hamdash.chornobyl.ui.map.MapViewModel
import com.hamdash.chornobyl.ui.more.PropagationViewModel
import com.hamdash.chornobyl.ui.more.ReferenceViewModel
import com.hamdash.chornobyl.ui.radiation.RadiationViewModel
import com.hamdash.chornobyl.ui.repeaters.RepeatersViewModel
import com.hamdash.chornobyl.ui.settings.SettingsViewModel
import com.hamdash.chornobyl.ui.weather.WeatherViewModel
import com.hamdash.chornobyl.ui.zone.ZoneViewModel

private fun CreationExtras.container() =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HamDashApplication).container

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(container()) }
        initializer { MapViewModel(container()) }
        initializer { RepeatersViewModel(container()) }
        initializer { DmrViewModel(container()) }
        initializer { PropagationViewModel(container()) }
        initializer { ReferenceViewModel(container()) }
        initializer { SettingsViewModel(container()) }
        initializer { WeatherViewModel(container()) }
        initializer { ZoneViewModel(container()) }
        initializer { RadiationViewModel(container()) }
    }
}
