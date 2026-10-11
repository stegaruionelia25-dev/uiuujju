package ro.profitride.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.profitride.R

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfitRideTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PrColors.Bg),
                ) {
                    Root(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

private enum class Tab(val icon: ImageVector, val label: Int) {
    HOME(Icons.Filled.Home, R.string.tab_home),
    RIDES(Icons.Filled.Menu, R.string.tab_rides),
    COSTS(Icons.Filled.Build, R.string.tab_costs),
    SETTINGS(Icons.Filled.Settings, R.string.tab_settings),
}

@Composable
private fun Root(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    if (!profile.onboardingDone) {
        OnboardingScreen(vm)
        return
    }

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showCalibration by rememberSaveable { mutableStateOf(false) }
    val tab = Tab.entries[tabIndex]

    BackHandler(enabled = showCalibration) { showCalibration = false }
    BackHandler(enabled = !showCalibration && tabIndex != 0) { tabIndex = 0 }

    Scaffold(
        containerColor = PrColors.Bg,
        bottomBar = {
            NavigationBar(containerColor = PrColors.Surface) {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = index == tabIndex && !showCalibration,
                        onClick = {
                            showCalibration = false
                            tabIndex = index
                            vm.refresh()
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrColors.Accent,
                            selectedTextColor = PrColors.Accent,
                            indicatorColor = PrColors.Accent.copy(alpha = 0.14f),
                            unselectedIconColor = PrColors.Faint,
                            unselectedTextColor = PrColors.Faint,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            if (showCalibration) {
                CalibrationScreen(vm, onBack = { showCalibration = false })
            } else {
                when (tab) {
                    Tab.HOME -> HomeScreen(vm, onOpenRides = { tabIndex = Tab.RIDES.ordinal })
                    Tab.RIDES -> RidesScreen(vm)
                    Tab.COSTS -> CostsScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm, onOpenCalibration = { showCalibration = true })
                }
            }
        }
    }
}
