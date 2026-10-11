package ro.profitride.overlay

import android.content.Context
import ro.profitride.R
import ro.profitride.core.Countries
import ro.profitride.core.DriverProfile
import ro.profitride.core.IdleMessage
import ro.profitride.core.Rating
import ro.profitride.core.VerdictBasis
import ro.profitride.util.Format

/** Texte afișate de bulă și de aplicație, din date reale + resursele de limbă. */
object OverlayText {

    fun ratingColor(rating: Rating): Int = when (rating) {
        Rating.GOOD -> OverlayColors.GREEN
        Rating.MEDIUM -> OverlayColors.AMBER
        Rating.POOR -> OverlayColors.RED
    }

    fun ratingLabel(context: Context, rating: Rating): String = context.getString(
        when (rating) {
            Rating.GOOD -> R.string.rating_good
            Rating.MEDIUM -> R.string.rating_medium
            Rating.POOR -> R.string.rating_poor
        },
    )

    fun currency(profile: DriverProfile): String =
        Format.currencySymbol(Countries.byCode(profile.countryCode).currency)

    /** „lei/km net”, „lei/h net” sau „lei net”, după criteriul ales. */
    fun basisUnit(context: Context, basis: VerdictBasis, profile: DriverProfile): String {
        val cur = currency(profile)
        val unit = Countries.byCode(profile.countryCode).unit
        val raw = when (basis) {
            VerdictBasis.PER_KM -> "$cur/${unit.symbol}"
            VerdictBasis.PER_HOUR -> "$cur/h"
            VerdictBasis.NET -> cur
        }
        return context.getString(R.string.net_suffix, raw)
    }

    fun idleMessage(context: Context, message: IdleMessage, profile: DriverProfile): String {
        val cur = currency(profile)
        return when (message) {
            is IdleMessage.Threshold -> context.getString(
                R.string.idle_threshold,
                Format.number(message.value, if (message.basis == VerdictBasis.PER_KM) 2 else 0),
                basisUnit(context, message.basis, profile),
            )
            is IdleMessage.Today -> context.getString(
                R.string.idle_today,
                "${Format.number(message.net, 0)} $cur",
                context.resources.getQuantityString(R.plurals.rides_count, message.rides, message.rides),
            )
            is IdleMessage.ToTarget -> context.getString(
                R.string.idle_to_target,
                "${Format.number(message.remaining, 0)} $cur",
            )
            IdleMessage.TargetReached -> context.getString(R.string.idle_target_reached)
            is IdleMessage.BestWindow -> context.getString(R.string.idle_best_window, Format.hour(message.startHour))
        }
    }
}
