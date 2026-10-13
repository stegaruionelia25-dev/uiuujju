package ro.profitride.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ro.profitride.ProfitRideApp
import ro.profitride.core.Analysis
import ro.profitride.core.AnalysisRide
import ro.profitride.core.DayStats
import ro.profitride.core.ManualEntry
import ro.profitride.core.Period
import ro.profitride.core.ProfitCalculator
import ro.profitride.core.RideOffer
import ro.profitride.core.DriverProfile
import ro.profitride.core.OfferParser
import ro.profitride.core.OverlaySettings
import ro.profitride.core.ParseResult
import ro.profitride.core.Platform
import ro.profitride.core.ScreenText
import ro.profitride.core.Verdict
import ro.profitride.core.Stats
import ro.profitride.data.RideRow
import ro.profitride.service.ProfitAccessibilityService

data class HomeStats(
    val today: DayStats = DayStats(0.0, 0, null),
    val offersToday: Int = 0,
    val avgPerHour: Double? = null,
    val bestHour: Int? = null,
)

/** Rezultatul citirii unei poze cu o ofertă, alese din galerie (testul din Setări). */
data class ImageTest(
    val busy: Boolean = false,
    val result: ParseResult? = null,
    val verdict: Verdict? = null,
    val failed: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ProfitRideApp

    val profile: StateFlow<DriverProfile> = app.profileStore.profile
    val overlay: StateFlow<OverlaySettings> = app.profileStore.overlay

    private val _serviceEnabled = MutableStateFlow(false)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    private val _rides = MutableStateFlow<List<RideRow>>(emptyList())
    val rides: StateFlow<List<RideRow>> = _rides.asStateFlow()

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    private val _analysisRides = MutableStateFlow<List<AnalysisRide>>(emptyList())
    val analysisRides: StateFlow<List<AnalysisRide>> = _analysisRides.asStateFlow()

    private val _manual = MutableStateFlow<List<ManualEntry>>(emptyList())
    val manual: StateFlow<List<ManualEntry>> = _manual.asStateFlow()

    fun updateProfile(transform: (DriverProfile) -> DriverProfile) = app.profileStore.updateProfile(transform)

    fun updateOverlay(transform: (OverlaySettings) -> OverlaySettings) = app.profileStore.updateOverlay(transform)

    /** Se apelează la fiecare revenire în aplicație. */
    fun refresh() {
        _serviceEnabled.value = AccessibilityStatus.isEnabled(app)
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val recent = app.rideStore.recent(200)
            val accepted = app.rideStore.acceptedRides(now - THIRTY_DAYS_MS)
            val today = Stats.today(accepted, now)
            val workDayStart = now - 18 * 3_600_000L
            _rides.value = recent
            val yearStart = Analysis.periodStart(Period.YEAR, now)
            _analysisRides.value = app.rideStore.acceptedForAnalysis(yearStart)
            _manual.value = app.rideStore.manualSince(yearStart)
            _stats.value = HomeStats(
                today = today,
                offersToday = recent.count { it.timestamp >= workDayStart },
                avgPerHour = Stats.averagePerHour(accepted, now),
                bestHour = Stats.bestWindowStartHour(accepted),
            )
        }
    }

    fun setAccepted(id: Long, accepted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.setAccepted(id, accepted)
            refresh()
        }
    }

    /** Corectează prețul, kilometrii sau minutele unei oferte citite greșit; profitul se recalculează. */
    fun editRide(ride: RideRow, price: Double, km: Double, minutes: Double) {
        val verdict = ProfitCalculator.evaluate(
            RideOffer(ride.platform, price, 0.0, km, 0.0, minutes, ride.priceNet),
            profile.value,
        )
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.updateRide(ride.id, price, km, minutes, verdict.net, verdict.rating)
            refresh()
        }
    }

    fun addManualIncome(amount: Double, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.addManual(amount, note.trim())
            refresh()
        }
    }

    fun deleteManual(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.deleteManual(id)
            refresh()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            app.rideStore.clear()
            refresh()
        }
    }

    /** Arată scurt o bulă de EXEMPLU, ca șoferul să-i regleze poziția. Fără efect dacă serviciul e oprit. */
    private val _imageTest = MutableStateFlow<ImageTest?>(null)
    val imageTest: StateFlow<ImageTest?> = _imageTest.asStateFlow()

    /** Citește o poză cu o ofertă (captură de ecran) direct în aplicație. Poza nu se salvează. */
    fun analyzeImage(uri: Uri) {
        _imageTest.value = ImageTest(busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val outcome = runCatching {
                val bitmap = decodeBitmap(uri) ?: error("decode")
                val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
                bitmap.recycle()
                text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                    line.boundingBox?.let { ScreenText(line.text, it.top, it.left) }
                }
            }
            recognizer.close()
            val lines = outcome.getOrNull()
            if (lines == null) {
                _imageTest.value = ImageTest(failed = true)
                return@launch
            }
            val parsed = OfferParser.parse(lines)
            val platform = if (lines.any { it.text.contains("uber", ignoreCase = true) }) Platform.UBER else Platform.BOLT
            val p = profile.value
            val verdict = (parsed as? ParseResult.Complete)
                ?.takeIf { p.isUsable }
                ?.let { ProfitCalculator.evaluate(it.offer.toOffer(platform), p) }
            _imageTest.value = ImageTest(result = parsed, verdict = verdict)
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val resolver = app.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_IMAGE_WIDTH) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    fun setActive(active: Boolean) {
        ProfitAccessibilityService.setActive(active)
    }

    fun startTest() {
        ProfitAccessibilityService.instance?.startTest()
    }

    fun previewOverlay() {
        ProfitAccessibilityService.instance?.previewDemo()
    }

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 3_600_000
        const val MAX_IMAGE_WIDTH = 1600
    }
}

object AccessibilityStatus {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(context, ProfitAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Pe Android 13+, aplicațiile instalate din afara magazinului au setări restricționate; se permit de aici. */
    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
