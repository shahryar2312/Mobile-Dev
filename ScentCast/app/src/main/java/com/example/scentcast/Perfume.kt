package com.example.scentcast

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf

/** The four scent families. labelRes points at strings.xml, so no text is hard-coded here. */
enum class ScentType(val labelRes: Int) {
    FRESH(R.string.scent_fresh),
    WOODY(R.string.scent_woody),
    SWEET(R.string.scent_sweet),
    SPICY(R.string.scent_spicy)
}

/** Simplified weather buckets. Later milestones will fill these from Open-Meteo. */
enum class Weather(val labelRes: Int, val reasonRes: Int) {
    COLD(R.string.weather_cold, R.string.reason_cold),
    MILD(R.string.weather_mild, R.string.reason_mild),
    HOT(R.string.weather_hot, R.string.reason_hot)
}

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
    val imageRes: Int? = null
)

/**
 * In-memory store shared by all three activities (Milestone 1 only).
 * The list is Compose state, so screens update when it changes.
 * Later this is replaced by a Room database.
 */
object PerfumeRepository {

    val perfumes = mutableStateListOf(
        Perfume(1, "Sauvage", "Dior", ScentType.SPICY, Weather.COLD, imageRes = R.drawable.photo_sauvage),
        Perfume(2, "Spicebomb", "Viktor & Rolf", ScentType.SPICY, Weather.COLD, imageRes = R.drawable.photo_spicebomb),
        Perfume(3, "Bleu de Chanel", "Chanel", ScentType.WOODY, Weather.MILD, imageRes = R.drawable.photo_bleu),
        Perfume(4, "1 Million", "Paco Rabanne", ScentType.SWEET, Weather.MILD, imageRes = R.drawable.photo_million),
        Perfume(5, "Acqua di Gio", "Armani", ScentType.FRESH, Weather.HOT, imageRes = R.drawable.photo_acqua),
        Perfume(6, "Light Blue", "Dolce & Gabbana", ScentType.FRESH, Weather.HOT, imageRes = R.drawable.photo_lightblue)
    )

    // Ids for perfumes the user adds start at 100, so they never clash with the starter ids 1 to 6.
    private var nextId = 100

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

    /** Picks a random perfume that suits [weather], avoiding [skipId] when there is a choice. */
    fun suggestFor(weather: Weather, skipId: Int? = null): Perfume? {
        val matches = perfumes.filter { it.weather == weather }
        val pool = matches.filter { it.id != skipId }.ifEmpty { matches }
        return pool.randomOrNull()
    }
}
