package ro.profitride.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import ro.profitride.core.AutoData
import ro.profitride.core.CarDoc
import ro.profitride.core.DocType
import ro.profitride.core.Expense
import ro.profitride.core.ExpenseType
import ro.profitride.core.MaintKind
import ro.profitride.core.Maintenance

/** Datele din meniul Auto, salvate doar pe telefon. */
class AutoStore(context: Context) {
    private val sp = context.getSharedPreferences("auto", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AutoData> = _state.asStateFlow()

    fun update(transform: (AutoData) -> AutoData) {
        val next = transform(_state.value)
        if (next == _state.value) return
        save(next)
        _state.value = next
    }

    fun notifiedKeys(): Set<String> = sp.getStringSet("notified", emptySet())?.toSet() ?: emptySet()

    fun markNotified(keys: Collection<String>) {
        if (keys.isEmpty()) return
        sp.edit().putStringSet("notified", (notifiedKeys() + keys).toSet()).apply()
    }

    private fun load(): AutoData {
        val docs = runCatching {
            val arr = JSONArray(sp.getString("docs", "[]"))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val type = enumOrNull<DocType>(o.getString("type")) ?: return@mapNotNull null
                val days = o.getJSONArray("notify")
                CarDoc(type, o.getLong("expiry"), o.optDouble("cost", 0.0), (0 until days.length()).map { days.getInt(it) }.toSet())
            }
        }.getOrDefault(emptyList())
        val maint = runCatching {
            val arr = JSONArray(sp.getString("maint", "[]"))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val kind = enumOrNull<MaintKind>(o.getString("kind")) ?: return@mapNotNull null
                Maintenance(kind, o.getLong("lastKm"), o.getLong("interval"), o.optString("note"), o.optLong("lastDay"))
            }
        }.getOrDefault(emptyList())
        val expenses = runCatching {
            val arr = JSONArray(sp.getString("expenses", "[]"))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val type = enumOrNull<ExpenseType>(o.getString("type")) ?: return@mapNotNull null
                Expense(o.getLong("id"), type, o.getDouble("amount"), o.optDouble("liters", 0.0), o.getLong("day"), o.optString("note"))
            }
        }.getOrDefault(emptyList())
        return AutoData(
            plate = sp.getString("plate", "").orEmpty(),
            odometer = sp.getLong("odometer", 0),
            tutorialSeen = sp.getBoolean("tutorial_seen", false),
            docs = docs,
            maintenance = maint,
            expenses = expenses,
        )
    }

    private fun save(d: AutoData) {
        val docs = JSONArray().apply {
            d.docs.forEach {
                put(JSONObject().put("type", it.type.name).put("expiry", it.expiryDay).put("cost", it.cost).put("notify", JSONArray(it.notifyDays.toList())))
            }
        }
        val maint = JSONArray().apply {
            d.maintenance.forEach {
                put(JSONObject().put("kind", it.kind.name).put("lastKm", it.lastKm).put("interval", it.intervalKm).put("note", it.note).put("lastDay", it.lastDay))
            }
        }
        val expenses = JSONArray().apply {
            d.expenses.forEach {
                put(JSONObject().put("id", it.id).put("type", it.type.name).put("amount", it.amount).put("liters", it.liters).put("day", it.day).put("note", it.note))
            }
        }
        sp.edit()
            .putString("plate", d.plate)
            .putLong("odometer", d.odometer)
            .putBoolean("tutorial_seen", d.tutorialSeen)
            .putString("docs", docs.toString())
            .putString("maint", maint.toString())
            .putString("expenses", expenses.toString())
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
}
