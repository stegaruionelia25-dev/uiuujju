package ro.profitride.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import ro.profitride.ProfitRideApp
import ro.profitride.service.ProfitAccessibilityService
import ro.profitride.service.StatusNotification
import java.io.File

/** Drepturile GDPR puse în practică: export, ștergere, retragerea acordului. Totul se întâmplă local. */
object DataRights {

    private fun dump(sp: SharedPreferences): JSONObject {
        val o = JSONObject()
        for ((k, v) in sp.all) {
            when (v) {
                is Set<*> -> o.put(k, JSONArray(v.map { it.toString() }))
                null -> Unit
                else -> o.put(k, v)
            }
        }
        return o
    }

    fun exportJson(context: Context): String {
        val app = ProfitRideApp.instance
        val root = JSONObject()
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("app", "Netomax")
        root.put("profileAndSettings", dump(context.getSharedPreferences("profitride", Context.MODE_PRIVATE)))
        root.put("auto", dump(context.getSharedPreferences("auto", Context.MODE_PRIVATE)))
        val consent = JSONObject()
        consent.put("termsVersion", context.getSharedPreferences("consent", Context.MODE_PRIVATE).getInt("version", 0))
        consent.put("acceptedAtMillis", app.consentStore.acceptedAtMillis())
        root.put("consent", consent)
        val rides = JSONArray()
        for (r in app.rideStore.recent(1_000_000)) {
            rides.put(
                JSONObject()
                    .put("id", r.id).put("timestamp", r.timestamp).put("platform", r.platform.name)
                    .put("price", r.price).put("km", r.km).put("minutes", r.minutes)
                    .put("net", r.net).put("rating", r.rating.name).put("accepted", r.accepted),
            )
        }
        root.put("rides", rides)
        val manual = JSONArray()
        for (m in app.rideStore.manualSince(0L)) {
            manual.put(JSONObject().put("id", m.id).put("timestamp", m.timestampMillis).put("amount", m.amount).put("note", m.note))
        }
        root.put("manualIncome", manual)
        return root.toString(2)
    }

    /** Scrie fișierul în cache și deschide fereastra de partajare (tu alegi unde ajunge). */
    fun export(context: Context): Boolean = try {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, "netomax-export.json")
        file.writeText(exportJson(context))
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/json")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun deleteRides() {
        ProfitRideApp.instance.rideStore.clear()
    }

    fun deleteAll(context: Context) {
        val app = ProfitRideApp.instance
        ProfitAccessibilityService.setActive(false)
        app.rideStore.clear()
        app.autoStore.resetAll()
        app.profileStore.resetAll()
        app.consentStore.withdraw()
        File(context.cacheDir, "export").deleteRecursively()
        StatusNotification.cancel(context)
    }

    fun withdrawConsent(context: Context) {
        ProfitAccessibilityService.setActive(false)
        ProfitRideApp.instance.consentStore.withdraw()
        StatusNotification.cancel(context)
    }
}
