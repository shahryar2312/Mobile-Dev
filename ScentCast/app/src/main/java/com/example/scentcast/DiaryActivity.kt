package com.example.scentcast

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme
import java.util.Date

/** Screen 4: Wear Diary. A list of the times you wore a perfume, each with a 1-5 longevity rating. */
class DiaryActivity : LoggedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScentCastTheme {
                DiaryScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(onBack: () -> Unit) {
    val wears = PerfumeRepository.wears

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_title)) },
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
        if (wears.isEmpty()) {
            Text(
                text = stringResource(R.string.diary_empty),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wears, key = WearLog::id) { wear ->
                    WearCard(wear)
                }
            }
        }
    }
}

@Composable
fun WearCard(wear: WearLog) {
    val context = LocalContext.current
    val date = DateFormat.getMediumDateFormat(context).format(Date(wear.timeMillis))
    val clock = DateFormat.getTimeFormat(context).format(Date(wear.timeMillis))
    val period = stringResource(
        if (wear.timeOfDay == TimeOfDay.NIGHT) R.string.tod_night else R.string.tod_day
    )
    val temp = if (wear.tempC != null) " · " + stringResource(R.string.diary_temp, wear.tempC) else ""

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(wear.perfumeName, style = MaterialTheme.typography.titleMedium)
            Text(wear.perfumeBrand, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$date, $clock · $period$temp · ${stringResource(R.string.diary_sprays, wear.sprays)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.diary_rate_prompt),
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (value in 1..5) {
                    FilterChip(
                        selected = wear.rating == value,
                        onClick = { PerfumeRepository.rateWear(wear.id, value) },
                        label = { Text(value.toString()) }
                    )
                }
            }
        }
    }
}
