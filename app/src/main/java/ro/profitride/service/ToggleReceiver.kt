package ro.profitride.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Primește apăsarea butonului Pornește/Oprește din notificare. */
class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ProfitAccessibilityService.setActive(intent.getBooleanExtra(EXTRA_ACTIVE, true))
    }

    companion object {
        const val EXTRA_ACTIVE = "active"
    }
}
