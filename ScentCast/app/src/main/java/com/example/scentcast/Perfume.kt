package com.example.scentcast

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf

/** Set by the light sensor (or the clock if the phone has no light sensor). */
enum class TimeOfDay { DAY, NIGHT }

/** bestAt is the time of day this scent suits most, or null if it works at any time. */
enum class ScentType(val labelRes: Int, val bestAt: TimeOfDay?) {
    FRESH(R.string.scent_fresh, TimeOfDay.DAY),
    WOODY(R.string.scent_woody, null),
    SWEET(R.string.scent_sweet, TimeOfDay.NIGHT),
    SPICY(R.string.scent_spicy, TimeOfDay.NIGHT)
}

/** Simple weather groups, filled from Open-Meteo (GPS) or chosen by hand. */
enum class Weather(val labelRes: Int, val reasonRes: Int) {
    COLD(R.string.weather_cold, R.string.reason_cold),
    MILD(R.string.weather_mild, R.string.reason_mild),
    HOT(R.string.weather_hot, R.string.reason_hot)
}

data class Perfume(
    val id: Int,
    val name: String,
    val brand: String,
    val scentType: ScentType,
    val weather: Weather,
    val photo: Bitmap? = null,
    val sizeMl: Int = 100
)

/** One time you wore a perfume. tempC comes from the GPS weather reading, if there was one. */
data class WearLog(
    val id: Int,
    val perfumeId: Int,
    val perfumeName: String,
    val perfumeBrand: String,
    val timeMillis: Long,
    val tempC: Double?,
    val timeOfDay: TimeOfDay,
    val sprays: Int,
    val rating: Int? = null // longevity: 1 (faded) to 5 (strong)
)

/**
 * In-memory store shared by all the screens. The lists are Compose state,
 * so screens update when they change. Room replaces this in a later milestone.
 */
object PerfumeRepository {

    val perfumes = mutableStateListOf(
        Perfume(1, "Sauvage", "Dior", ScentType.SPICY, Weather.COLD),
        Perfume(2, "Spicebomb", "Viktor & Rolf", ScentType.SPICY, Weather.COLD),
        Perfume(3, "Bleu de Chanel", "Chanel", ScentType.WOODY, Weather.MILD),
        Perfume(4, "1 Million", "Paco Rabanne", ScentType.SWEET, Weather.MILD),
        Perfume(5, "Acqua di Gio", "Armani", ScentType.FRESH, Weather.HOT),
        Perfume(6, "Light Blue", "Dolce & Gabbana", ScentType.FRESH, Weather.HOT)
    )

    val wears = mutableStateListOf<WearLog>()

    private var nextId = 100
    private var nextWearId = 1

    fun newId(): Int = nextId++

    fun get(id: Int): Perfume? = perfumes.firstOrNull { it.id == id }

    fun save(perfume: Perfume) {
        val index = perfumes.indexOfFirst { it.id == perfume.id }
        if (index >= 0) perfumes[index] = perfume else perfumes.add(perfume)
    }

    fun delete(id: Int) {
        perfumes.removeAll { it.id == id }
    }

    // ---- Suggestions ----

    /** True if the scent type suits this time of day (or suits any time). */
    fun fitsTime(perfume: Perfume, time: TimeOfDay): Boolean {
        val best = perfume.scentType.bestAt
        return best == null || best == time
    }

    /** True if the perfume is a good answer for the weather and time of day right now. */
    fun isGoodFor(perfume: Perfume, weather: Weather, time: TimeOfDay): Boolean {
        return perfume.weather == weather && fitsTime(perfume, time)
    }

    /**
     * Picks a random perfume for the weather. It prefers ones that also suit the time of
     * day, and avoids skipId (the current pick) when there is a choice.
     */
    fun suggestFor(weather: Weather, time: TimeOfDay, skipId: Int? = null): Perfume? {
        val matches = perfumes.filter { it.weather == weather }
        val best = matches.filter { fitsTime(it, time) }.ifEmpty { matches }
        val pool = best.filter { it.id != skipId }.ifEmpty { best }
        return pool.randomOrNull()
    }

    // ---- Wear diary ----

    fun logWear(perfume: Perfume, tempC: Double?, time: TimeOfDay): WearLog {
        val log = WearLog(
            id = nextWearId++,
            perfumeId = perfume.id,
            perfumeName = perfume.name,
            perfumeBrand = perfume.brand,
            timeMillis = System.currentTimeMillis(),
            tempC = tempC,
            timeOfDay = time,
            sprays = Settings.spraysPerWear
        )
        wears.add(0, log) // newest first
        return log
    }

    fun rateWear(id: Int, rating: Int) {
        val index = wears.indexOfFirst { it.id == id }
        if (index >= 0) wears[index] = wears[index].copy(rating = rating)
    }

    // ---- Stats ----

    fun wearCount(perfumeId: Int): Int = wears.count { it.perfumeId == perfumeId }

    /** Average longevity rating, or null if this perfume was never rated. */
    fun averageRating(perfumeId: Int): Double? {
        val ratings = wears.filter { it.perfumeId == perfumeId }.mapNotNull { it.rating }
        return if (ratings.isEmpty()) null else ratings.average()
    }

    /** Estimate only: bottle size minus about 0.1 ml for every spray logged. */
    fun mlLeft(perfume: Perfume): Double {
        val sprays = wears.filter { it.perfumeId == perfume.id }.sumOf { it.sprays }
        return (perfume.sizeMl - sprays * ML_PER_SPRAY).coerceAtLeast(0.0)
    }

    private const val ML_PER_SPRAY = 0.1
}
