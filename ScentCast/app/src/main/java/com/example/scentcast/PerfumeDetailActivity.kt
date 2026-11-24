package com.example.scentcast

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/**
 * Perfume details, in the style of a fragrance website: notes, accords chart, best time and
 * season, how to wear it (and when not to), a budget alternative, and map links.
 */
class PerfumeDetailActivity : LoggedActivity() {

    companion object {
        const val EXTRA_PERFUME_ID = "com.example.scentcast.DETAIL_PERFUME_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val perfumeId = intent.getIntExtra(EXTRA_PERFUME_ID, -1)
        setContent {
            ScentCastTheme {
                // Read inside setContent, so the page updates after coming back from Edit.
                val perfume = PerfumeRepository.get(perfumeId)
                if (perfume == null) {
                    finish()
                } else {
                    PerfumeDetailScreen(perfume) { finish() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfumeDetailScreen(perfume: Perfume, onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(perfume.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(perfume.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${perfume.brand} · ${stringResource(perfume.scentType.labelRes)}",
                style = MaterialTheme.typography.titleMedium
            )

            // ---- Notes ----
            if (perfume.topNotes.isNotBlank() || perfume.heartNotes.isNotBlank() ||
                perfume.baseNotes.isNotBlank()
            ) {
                SectionCard(stringResource(R.string.detail_notes)) {
                    NoteLine(stringResource(R.string.notes_top), perfume.topNotes)
                    NoteLine(stringResource(R.string.notes_heart), perfume.heartNotes)
                    NoteLine(stringResource(R.string.notes_base), perfume.baseNotes)
                }
            } else {
                SectionCard(stringResource(R.string.detail_notes)) {
                    Text(stringResource(R.string.detail_no_notes))
                }
            }

            // ---- Accords chart ("scent map") ----
            if (perfume.accords.isNotEmpty()) {
                SectionCard(stringResource(R.string.detail_accords)) {
                    // Strongest first, stacked tightly like the website chart.
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        perfume.accords.sortedByDescending { it.second }
                            .forEach { (name, percent) -> AccordBar(name, percent) }
                    }
                }
            }

            // ---- When to wear ----
            SectionCard(stringResource(R.string.detail_when)) {
                val time = perfume.scentType.bestAt
                val timeText = when (time) {
                    TimeOfDay.DAY -> stringResource(R.string.tod_day)
                    TimeOfDay.NIGHT -> stringResource(R.string.tod_night)
                    null -> stringResource(R.string.time_any)
                }
                Text(stringResource(R.string.detail_best_time, timeText))
                val seasons = perfume.seasons.joinToString(", ") { context.getString(it.labelRes) }
                Text(stringResource(R.string.detail_seasons, seasons))
                Text(
                    stringResource(
                        R.string.detail_weather,
                        stringResource(perfume.weather.labelRes)
                    )
                )
            }

            // ---- How to use it, and when not to ----
            if (perfume.howToWear.isNotBlank() || perfume.avoidWhen.isNotBlank()) {
                SectionCard(stringResource(R.string.detail_how)) {
                    if (perfume.howToWear.isNotBlank()) {
                        Text(stringResource(R.string.detail_do, perfume.howToWear))
                    }
                    if (perfume.avoidWhen.isNotBlank()) {
                        Text(stringResource(R.string.detail_dont, perfume.avoidWhen))
                    }
                }
            }

            // ---- Budget alternative ----
            val dupe = perfume.dupe
            if (dupe != null) {
                SectionCard(stringResource(R.string.detail_budget)) {
                    Text(
                        text = "${dupe.name} · ${dupe.brand}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val original = perfume.priceEur
                    if (original != null) {
                        Text(stringResource(R.string.detail_price_compare, dupe.priceEur, original))
                    } else {
                        Text(stringResource(R.string.detail_price, dupe.priceEur))
                    }
                    Text(
                        text = stringResource(R.string.detail_dupe_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ---- Maps: implicit intents with a geo: URI ----
            FilledTonalButton(
                onClick = {
                    openMap(context, "${perfume.name} ${perfume.brand} perfume shop")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_find_perfume))
            }
            if (dupe != null) {
                OutlinedButton(
                    onClick = { openMap(context, "${dupe.name} ${dupe.brand} perfume shop") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_find_dupe))
                }
            }

            // ---- Explicit intent to the edit form ----
            OutlinedButton(
                onClick = {
                    val edit = Intent(context, AddPerfumeActivity::class.java)
                        .putExtra(AddPerfumeActivity.EXTRA_PERFUME_ID, perfume.id)
                    context.startActivity(edit)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_perfume))
            }
        }
    }
}

private fun openMap(context: Context, query: String) {
    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
    startSafely(context, Intent(Intent.ACTION_VIEW, uri))
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NoteLine(label: String, notes: String) {
    if (notes.isNotBlank()) {
        Text("$label: $notes")
    }
}

/** Bar colours, picked to look like a fragrance website. Unknown accords get a grey-purple. */
private fun accordColor(name: String): Color = when (name.lowercase()) {
    "sweet" -> Color(0xFFEF3340)
    "fruity" -> Color(0xFFDC4325)
    "powdery" -> Color(0xFFB5ABA3)
    "vanilla" -> Color(0xFFC5C593)
    "musky", "musk" -> Color(0xFFA89FB0)
    "yellow floral" -> Color(0xFFBBA010)
    "woody" -> Color(0xFF5B3A1A)
    "rose" -> Color(0xFFB00B52)
    "floral" -> Color(0xFFA64360)
    "citrus" -> Color(0xFFF9D308)
    "fresh", "fresh spicy" -> Color(0xFF7CC242)
    "aromatic" -> Color(0xFF48A07C)
    "aquatic" -> Color(0xFF4C9FD6)
    "amber" -> Color(0xFFC0521F)
    "warm spicy" -> Color(0xFFB5381E)
    "leather" -> Color(0xFF7B3F28)
    "tobacco" -> Color(0xFF7A4B20)
    else -> Color(0xFF8E8499)
}

/**
 * One accord as a coloured bar with its name inside. The stronger the accord, the longer the
 * bar. Bars never get shorter than 45% so the name always fits.
 */
@Composable
private fun AccordBar(name: String, percent: Int) {
    val color = accordColor(name)
    val textColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth(0.45f + 0.55f * percent.coerceIn(0, 100) / 100f)
            .height(30.dp)
            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.lowercase(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
