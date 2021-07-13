package com.kpstv.vpn.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kpstv.vpn.data.db.repository.VpnLoadState
import com.kpstv.vpn.data.models.asVpnConfiguration
import com.kpstv.vpn.extensions.SlideTop
import com.kpstv.vpn.ui.viewmodels.VpnViewModel
import com.kpstv.navigation.compose.ComposeNavigator
import com.kpstv.navigation.compose.Fade
import com.kpstv.navigation.compose.Route
import com.kpstv.navigation.compose.SlideRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.parcelize.Parcelize

sealed class NavigationRoute : Route {
  @Parcelize
  @Immutable
  data class Main(private val noArg: String = "") : NavigationRoute()

  @Parcelize
  @Immutable
  data class Server(private val noArg: String = "") : NavigationRoute()

  @Parcelize
  @Immutable
  data class Import(private val noArg: String = "") : NavigationRoute()

  companion object {
    val key = NavigationRoute::class
  }
}

private class Load(val refresh: Boolean = false)

@Composable
fun NavigationScreen(
  navigator: ComposeNavigator,
  viewModel: VpnViewModel = viewModel()
) {
  val shouldRefresh = remember { mutableStateOf(Load(), policy = referentialEqualityPolicy()) }
  val vpnCollectJob = remember(shouldRefresh.value) { SupervisorJob() }

  val location = viewModel.publicIp.collectAsState()
  val currentConfig = viewModel.currentVpn.collectAsState()
  val vpnLoadState = viewModel.fetchServers(shouldRefresh.value.refresh)
    .collectAsState(initial = VpnLoadState.Loading(), context = vpnCollectJob + Dispatchers.IO)

  val connectivityStatus = viewModel.connectivityStatus.collectAsState()

  navigator.Setup(key = NavigationRoute.key, initial = NavigationRoute.Main()) { controller, dest ->
    when (dest) {
      is NavigationRoute.Main -> MainScreen(
        publicIp = location.value?.query,
        configuration = currentConfig.value,
        connectivityStatus = connectivityStatus.value,
        onChangeServer = {
          controller.navigateTo(NavigationRoute.Server()) {
            withAnimation {
              target = SlideRight
              current = Fade
            }
          }
        },
        onConnectClick = {
          viewModel.connect()
        },
        onDisconnect = {
          viewModel.disconnect()
        }
      )
      is NavigationRoute.Server -> ServerScreen(
        vpnState = vpnLoadState.value,
        onBackButton = { controller.goBack() },
        onRefresh = {
          vpnCollectJob.cancel()
          shouldRefresh.value = Load(refresh = true)
        },
        onImportButton = {
          controller.navigateTo(NavigationRoute.Import()) {
            withAnimation {
              target = SlideTop
              current = Fade
            }
          }
        },
        onItemClick = { config ->
          viewModel.changeServer(config)
          controller.goBack()
        }
      )
      is NavigationRoute.Import -> ImportScreen(
        onItemClick = { config ->
          viewModel.changeServer(config.asVpnConfiguration())

          controller.navigateTo(NavigationRoute.Main()) {
            popUpTo(NavigationRoute.Main())
            withAnimation {
              target = Fade
              current = Fade
            }
          }
        },
        goBack = { controller.goBack() }
      )
    }
  }
}