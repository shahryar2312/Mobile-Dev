package com.example.scentcast

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/** Screen 5: Stats & Settings. */
class StatsActivity : LoggedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScentCastTheme {
                StatsScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context: Context = LocalContext.current
    val perfumes = PerfumeRepository.perfumes
    PerfumeRepository.wears.size // read, so this screen redraws when a wear is added or rated

    // Most and least worn. Most only counts perfumes that were worn at least once.
    val mostWorn = perfumes.filter { PerfumeRepository.wearCount(it.id) > 0 }
        .maxByOrNull { PerfumeRepository.wearCount(it.id) }
    val leastWorn = perfumes.minByOrNull { PerfumeRepository.wearCount(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Stats ----
            Text(
                text = stringResource(R.string.stats_heading),
                style = MaterialTheme.typography.titleLarge
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (mostWorn == null) {
                        Text(stringResource(R.string.stats_no_wears))
                    } else {
                        Text(
                            stringResource(
                                R.string.stats_most,
                                mostWorn.name,
                                PerfumeRepository.wearCount(mostWorn.id)
                            )
                        )
                    }
                    if (leastWorn != null) {
                        Text(
                            stringResource(
                                R.string.stats_least,
                                leastWorn.name,
                                PerfumeRepository.wearCount(leastWorn.id)
                            )
                        )
                    }
                }
            }

            // One row per bottle: wears, average longevity and an estimate of the ml left.
            perfumes.forEach { perfume ->
                val average = PerfumeRepository.averageRating(perfume.id)
                val longevity = if (average == null) {
                    stringResource(R.string.stats_not_rated)
                } else {
                    String.format(java.util.Locale.US, "%.1f / 5", average)
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(perfume.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                R.string.stats_row,
                                PerfumeRepository.wearCount(perfume.id),
                                longevity,
                                PerfumeRepository.mlLeft(perfume),
                                perfume.sizeMl
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ---- Settings ----
            Text(
                text = stringResource(R.string.settings_heading),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(context, Settings.THEME_SYSTEM, R.string.theme_system)
                ThemeChip(context, Settings.THEME_LIGHT, R.string.theme_light)
                ThemeChip(context, Settings.THEME_DARK, R.string.theme_dark)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_reminders),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = Settings.remindersOn,
                    onCheckedChange = { Settings.saveRemindersOn(context, it) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_demo),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = Settings.demoReminders,
                    onCheckedChange = { Settings.saveDemoReminders(context, it) }
                )
            }

            Text(stringResource(R.string.settings_sprays), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (count in 2..6) {
                    FilterChip(
                        selected = Settings.spraysPerWear == count,
                        onClick = { Settings.saveSprays(context, count) },
                        label = { Text(count.toString()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(context: Context, value: Int, labelRes: Int) {
    FilterChip(
        selected = Settings.theme == value,
        onClick = { Settings.saveTheme(context, value) },
        label = { Text(stringResource(labelRes)) }
    )
}
