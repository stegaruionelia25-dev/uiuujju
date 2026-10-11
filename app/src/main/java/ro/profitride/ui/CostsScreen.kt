package ro.profitride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.profitride.R

/** Aceleași câmpuri ca la instalare; orice modificare se aplică imediat pe următoarea ofertă. */
@Composable
fun CostsScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.costs_title), stringResource(R.string.costs_sub))
        CostPerKmCard(profile)
        PrCard {
            Label(stringResource(R.string.costs_section_car))
            CountrySection(profile, vm::updateProfile)
            CarSection(profile, vm::updateProfile)
        }
        PrCard {
            Label(stringResource(R.string.costs_section_costs))
            CostSection(profile, vm::updateProfile)
        }
        PrCard {
            Label(stringResource(R.string.costs_section_earn))
            EarningsSection(profile, vm::updateProfile)
        }
    }
}
