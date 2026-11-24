package com.example.scentcast

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf

/** Set by the light sensor (or the clock if the phone has no light sensor). */
enum class TimeOfDay { DAY, NIGHT }

/**
 * The four scent families. labelRes points at strings.xml, so no text is hard-coded here.
 * bestAt is the time of day this scent suits most, or null if it works at any time.
 */
enum class ScentType(val labelRes: Int, val bestAt: TimeOfDay?) {
    FRESH(R.string.scent_fresh, TimeOfDay.DAY),
    WOODY(R.string.scent_woody, null),
    SWEET(R.string.scent_sweet, TimeOfDay.NIGHT),
    SPICY(R.string.scent_spicy, TimeOfDay.NIGHT)
}

/** Taken from today's date. */
enum class Season(val labelRes: Int) {
    SPRING(R.string.season_spring),
    SUMMER(R.string.season_summer),
    AUTUMN(R.string.season_autumn),
    WINTER(R.string.season_winter)
}

/** Simple weather groups, filled from Open-Meteo (GPS) or chosen by hand. */
enum class Weather(val labelRes: Int, val reasonRes: Int, val defaultSeasons: Set<Season>) {
    COLD(R.string.weather_cold, R.string.reason_cold, setOf(Season.AUTUMN, Season.WINTER)),
    MILD(R.string.weather_mild, R.string.reason_mild, setOf(Season.SPRING, Season.AUTUMN)),
    HOT(R.string.weather_hot, R.string.reason_hot, setOf(Season.SUMMER, Season.SPRING))
}

/** A cheaper perfume with a similar style. Prices are rough, in euro. */
data class Dupe(val name: String, val brand: String, val priceEur: Int)

/** One bottle in the collection. A data class, so two perfumes with the same values are equal. */
data class Perfume(
    val id: Int,
    val name: String,
    val brand: String,
    val scentType: ScentType,
    val weather: Weather,
    // A photo the user took or chose. It wins over the bundled picture.
    val photo: Bitmap? = null,
    // A bundled picture from res/drawable-nodpi, used by the starter perfumes.
    val imageRes: Int? = null,
    val sizeMl: Int = 100,
    val seasons: Set<Season> = weather.defaultSeasons,
    // Notes are comma separated text, so they are easy to type on the form.
    val topNotes: String = "",
    val heartNotes: String = "",
    val baseNotes: String = "",
    val accords: List<Pair<String, Int>> = emptyList(), // name and strength 0 to 100
    val howToWear: String = "",
    val avoidWhen: String = "",
    val priceEur: Int? = null,
    val dupe: Dupe? = null
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

    val perfumes = mutableStateListOf<Perfume>().apply { addAll(starterPerfumes) }

    val wears = mutableStateListOf<WearLog>()

    // Ids for perfumes the user adds start at 100, so they never clash with the starter ids 1 to 6.
    private var nextId = 100
    private var nextWearId = 1

    fun newId(): Int = nextId++

    fun get(id: Int): Perfume? = perfumes.firstOrNull { it.id == id }

    /** Replaces the perfume with the same id, or adds it if the id is new. */
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
     * Picks a random perfume for the weather. Among those, it prefers ones that suit the
     * time of day, then the season, and avoids skipId (the current pick) when there is a choice.
     */
    fun suggestFor(
        weather: Weather,
        time: TimeOfDay,
        season: Season? = null,
        skipId: Int? = null
    ): Perfume? {
        val matches = perfumes.filter { it.weather == weather }
        val byTime = matches.filter { fitsTime(it, time) }.ifEmpty { matches }
        val best = byTime.filter { season == null || season in it.seasons }.ifEmpty { byTime }
        val pool = best.filter { it.id != skipId }.ifEmpty { best }
        return pool.randomOrNull()
    }

    /** The season for a date, using the usual meteorological months (Dec to Feb is winter). */
    fun currentSeason(): Season {
        return when (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) {
            java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> Season.SPRING
            java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> Season.SUMMER
            java.util.Calendar.SEPTEMBER, java.util.Calendar.OCTOBER, java.util.Calendar.NOVEMBER -> Season.AUTUMN
            else -> Season.WINTER
        }
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
