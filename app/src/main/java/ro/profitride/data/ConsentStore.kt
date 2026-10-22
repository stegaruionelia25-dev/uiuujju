package ro.profitride.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Acordul pentru Termeni și Politica de confidențialitate: versiunea acceptată și data, doar pe telefon. */
class ConsentStore(context: Context) {
    private val sp = context.getSharedPreferences("consent", Context.MODE_PRIVATE)

    private val _accepted = MutableStateFlow(isAccepted())
    val accepted: StateFlow<Boolean> = _accepted.asStateFlow()

    private fun isAccepted(): Boolean = sp.getInt("version", 0) >= CURRENT_VERSION

    fun acceptedAtMillis(): Long = sp.getLong("at", 0L)

    fun accept(nowMillis: Long = System.currentTimeMillis()) {
        sp.edit().putInt("version", CURRENT_VERSION).putLong("at", nowMillis).apply()
        _accepted.value = true
    }

    fun withdraw() {
        sp.edit().clear().apply()
        _accepted.value = false
    }

    companion object {
        /** Se mărește când Termenii sau Politica se schimbă important; utilizatorul e întrebat din nou. */
        const val CURRENT_VERSION = 1
    }
}
