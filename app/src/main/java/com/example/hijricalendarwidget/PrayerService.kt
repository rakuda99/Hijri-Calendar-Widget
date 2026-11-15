package com.example.hijricalendarwidget

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents

/**
 * Prayer calculation engine backed by Adhan Kotlin (batoulapps/adhan2, MIT).
 * Replaces the legacy PrayTime.java (LGPL v3) engine.
 *
 * All prayer times are returned as UTC epoch milliseconds for storage and display.
 */
object PrayerService {

    enum class CalcMethod {
        UMML_QURA, MWL, ISNA, EGYPT, KARACHI, TEHRAN, JAFARI
    }

    data class PrayerTimesResult(
        val fajrMs: Long,
        val sunriseMs: Long,
        val dhuhrMs: Long,
        val asrMs: Long,
        val maghribMs: Long,
        val ishaMs: Long
    )

    /**
     * Calculates prayer times for a given Gregorian date and location using Adhan Kotlin.
     *
     * @param latitude  decimal degrees
     * @param longitude decimal degrees
     * @param year      Gregorian year
     * @param month     Gregorian month (1-12)
     * @param day       Gregorian day of month
     * @param method    calculation method (maps to the app's existing spinner indices)
     * @param madhab    Asr juristic method (SHAFI or HANAFI)
     */
    @JvmOverloads
    fun calculate(
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int,
        day: Int,
        method: CalcMethod,
        madhab: Madhab = Madhab.SHAFI
    ): PrayerTimesResult {
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents(year, month, day)
        val parameters = calculationParameters(method, madhab)
        val prayerTimes = PrayerTimes(coordinates, date, parameters)

        return PrayerTimesResult(
            fajrMs = prayerTimes.fajr.toEpochMilliseconds(),
            sunriseMs = prayerTimes.sunrise.toEpochMilliseconds(),
            dhuhrMs = prayerTimes.dhuhr.toEpochMilliseconds(),
            asrMs = prayerTimes.asr.toEpochMilliseconds(),
            maghribMs = prayerTimes.maghrib.toEpochMilliseconds(),
            ishaMs = prayerTimes.isha.toEpochMilliseconds()
        )
    }

    private fun calculationParameters(method: CalcMethod, madhab: Madhab): CalculationParameters {
        return when (method) {
            CalcMethod.UMML_QURA -> CalculationMethod.UMM_AL_QURA.parameters.copy(madhab = madhab)
            CalcMethod.MWL -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(madhab = madhab)
            CalcMethod.ISNA -> CalculationMethod.NORTH_AMERICA.parameters.copy(madhab = madhab)
            CalcMethod.EGYPT -> CalculationMethod.EGYPTIAN.parameters.copy(madhab = madhab)
            CalcMethod.KARACHI -> CalculationMethod.KARACHI.parameters.copy(madhab = madhab)
            // Tehran and Jafari are not in Adhan Kotlin's built-in methods;
            // use custom parameters matching the legacy PrayTime values.
            CalcMethod.TEHRAN -> CalculationParameters(
                fajrAngle = 17.7,
                ishaAngle = 14.0,
                madhab = madhab
            )
            CalcMethod.JAFARI -> CalculationParameters(
                fajrAngle = 16.0,
                ishaAngle = 14.0,
                madhab = madhab
            )
        }
    }

    /** Maps the app's legacy spinner index (0..6) to a CalcMethod. */
    fun fromLegacyIndex(index: Int): CalcMethod {
        return when (index) {
            0 -> CalcMethod.UMML_QURA
            1 -> CalcMethod.MWL
            2 -> CalcMethod.ISNA
            3 -> CalcMethod.EGYPT
            4 -> CalcMethod.KARACHI
            5 -> CalcMethod.TEHRAN
            6 -> CalcMethod.JAFARI
            else -> CalcMethod.UMML_QURA
        }
    }
}
