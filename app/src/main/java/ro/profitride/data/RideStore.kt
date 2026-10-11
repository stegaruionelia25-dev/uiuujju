package ro.profitride.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ro.profitride.core.LoggedRide
import ro.profitride.core.Platform
import ro.profitride.core.Rating
import ro.profitride.core.Verdict

data class RideRow(
    val id: Long,
    val timestamp: Long,
    val platform: Platform,
    val price: Double,
    val km: Double,
    val minutes: Double,
    val net: Double,
    val rating: Rating,
    val accepted: Boolean,
)

/**
 * Istoricul ofertelor văzute. Rămâne pe telefon și se șterge singur după 90 de zile.
 * Doar cursele marcate de șofer ca „acceptate” intră în statistici.
 */
class RideStore(context: Context) : SQLiteOpenHelper(context, "rides.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE rides (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                platform TEXT NOT NULL,
                price REAL NOT NULL,
                km REAL NOT NULL,
                minutes REAL NOT NULL,
                net REAL NOT NULL,
                rating TEXT NOT NULL,
                accepted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_rides_ts ON rides(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.delete("rides", "ts < ?", arrayOf((System.currentTimeMillis() - RETENTION_MS).toString()))
    }

    /** Salvează oferta; aceeași ofertă citită de mai multe ori în câteva secunde se salvează o singură dată. */
    @Synchronized
    fun logOffer(verdict: Verdict, nowMillis: Long = System.currentTimeMillis()): Long {
        val o = verdict.offer
        writableDatabase.query("rides", arrayOf("id", "ts", "platform", "price", "km"), null, null, null, null, "ts DESC", "1")
            .use { c ->
                if (c.moveToFirst()) {
                    val same = c.getString(2) == o.platform.name &&
                        kotlin.math.abs(c.getDouble(3) - o.price) < 0.005 &&
                        kotlin.math.abs(c.getDouble(4) - o.totalKm) < 0.05 &&
                        nowMillis - c.getLong(1) < DEDUPE_MS
                    if (same) return c.getLong(0)
                }
            }
        val values = ContentValues().apply {
            put("ts", nowMillis)
            put("platform", o.platform.name)
            put("price", o.price)
            put("km", o.totalKm)
            put("minutes", o.totalMin)
            put("net", verdict.net)
            put("rating", verdict.rating.name)
            put("accepted", 0)
        }
        return writableDatabase.insert("rides", null, values)
    }

    @Synchronized
    fun setAccepted(id: Long, accepted: Boolean) {
        val values = ContentValues().apply { put("accepted", if (accepted) 1 else 0) }
        writableDatabase.update("rides", values, "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun recent(limit: Int = 100): List<RideRow> {
        val out = ArrayList<RideRow>()
        readableDatabase.query(
            "rides", null, null, null, null, null, "ts DESC", limit.toString(),
        ).use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iTs = c.getColumnIndexOrThrow("ts")
            val iPlatform = c.getColumnIndexOrThrow("platform")
            val iPrice = c.getColumnIndexOrThrow("price")
            val iKm = c.getColumnIndexOrThrow("km")
            val iMin = c.getColumnIndexOrThrow("minutes")
            val iNet = c.getColumnIndexOrThrow("net")
            val iRating = c.getColumnIndexOrThrow("rating")
            val iAccepted = c.getColumnIndexOrThrow("accepted")
            while (c.moveToNext()) {
                val platform = Platform.entries.firstOrNull { it.name == c.getString(iPlatform) } ?: continue
                val rating = Rating.entries.firstOrNull { it.name == c.getString(iRating) } ?: Rating.MEDIUM
                out += RideRow(
                    id = c.getLong(iId),
                    timestamp = c.getLong(iTs),
                    platform = platform,
                    price = c.getDouble(iPrice),
                    km = c.getDouble(iKm),
                    minutes = c.getDouble(iMin),
                    net = c.getDouble(iNet),
                    rating = rating,
                    accepted = c.getInt(iAccepted) == 1,
                )
            }
        }
        return out
    }

    @Synchronized
    fun acceptedRides(sinceMillis: Long): List<LoggedRide> {
        val out = ArrayList<LoggedRide>()
        readableDatabase.query(
            "rides", arrayOf("ts", "net", "minutes"), "accepted = 1 AND ts >= ?",
            arrayOf(sinceMillis.toString()), null, null, "ts ASC",
        ).use { c ->
            while (c.moveToNext()) out += LoggedRide(c.getLong(0), c.getDouble(1), c.getDouble(2))
        }
        return out
    }

    @Synchronized
    fun clear() {
        writableDatabase.delete("rides", null, null)
    }

    private companion object {
        const val RETENTION_MS = 90L * 24 * 3_600_000
        const val DEDUPE_MS = 45_000L
    }
}
