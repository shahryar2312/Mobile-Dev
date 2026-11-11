package com.example.scentcast

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/** Screen 1: Today's Pick (explicit intent out to the collection, implicit intents for share and maps). */
class MainActivity : LoggedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScentCastTheme {
                TodaysPickScreen()
            }
        }
    }
}

/** Starts an intent, and shows a message instead of crashing if no app can handle it. */
fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Layout 1 of 3: a scrolling Column with weather chips, a Card for the pick and buttons.
 * Everything on this screen is a standard Compose / Material 3 component.
 */
@Composable
fun TodaysPickScreen() {
    val context = LocalContext.current

    // rememberSaveable keeps these values when the activity is destroyed and
    // recreated, for example on rotation. Only the id of the pick is saved, not the
    // whole perfume, so the screen always shows the latest version from the repository.
    var weather by rememberSaveable { mutableStateOf(Weather.MILD) }
    var pickId by rememberSaveable { mutableStateOf(PerfumeRepository.suggestFor(Weather.MILD)?.id) }
    val pick = pickId?.let { PerfumeRepository.get(it) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.home_weather_label),
                style = MaterialTheme.typography.bodyLarge
            )

            // User interaction: choose the weather, which changes the suggestion.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weather.entries.forEach { option ->
                    FilterChip(
                        selected = weather == option,
                        onClick = {
                            weather = option
                            pickId = PerfumeRepository.suggestFor(option)?.id
                        },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // The suggestion card. It is redrawn whenever the weather or the pick changes.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pick != null) {
                        // Bottle picture: the user's photo, or the bundled picture.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PerfumePicture(pick, Modifier.fillMaxSize())
                        }
                        Text(text = pick.name, style = MaterialTheme.typography.headlineSmall)
                        // Brand is optional, so the separator is only shown when there is a brand.
                        val scentLabel = stringResource(pick.scentType.labelRes)
                        Text(
                            text = if (pick.brand.isBlank()) scentLabel else "${pick.brand} · $scentLabel",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(weather.reasonRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    // No perfume in the collection suits this weather (e.g. all were deleted).
                    } else {
                        Text(text = stringResource(R.string.pick_none))
                    }
                }
            }

            // User interaction: ask for a different perfume. skipId avoids repeating the current one.
            Button(
                onClick = { pickId = PerfumeRepository.suggestFor(weather, skipId = pickId)?.id },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_another))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Implicit intent 1: ACTION_SEND, the system lets the user choose any sharing app.
                OutlinedButton(
                    onClick = {
                        if (pick != null) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    context.getString(R.string.share_text, pick.name, pick.brand)
                                )
                            }
                            startSafely(context, Intent.createChooser(send, null))
                        }
                    },
                    enabled = pick != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_share))
                }

                // Implicit intent 2: ACTION_VIEW with a geo: URI, opens any maps app.
                OutlinedButton(
                    onClick = {
                        val query = Uri.encode(context.getString(R.string.shops_query))
                        val map = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                        startSafely(context, map)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_shops))
                }
            }

            // Explicit intent: we name the exact activity to start.
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(context, CollectionActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_collection))
            }
        }
    }
}
