package com.example.scentcast

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme
import java.util.Calendar
import kotlin.math.sqrt

/** Everything the sensors report. It is Compose state, so the screen redraws on each change. */
class SensorState {
    var lux by mutableStateOf<Float?>(null)        // light sensor
    var shakes by mutableIntStateOf(0)             // accelerometer
    var reading by mutableStateOf<WeatherReading?>(null) // GPS + Open-Meteo
    var loading by mutableStateOf(false)
    var errorRes by mutableStateOf<Int?>(null)
}

/**
 * Screen 1: Today's Pick. The three sensors from the proposal feed the suggestion:
 *  - light sensor: daytime or night-time scent
 *  - accelerometer: shake for another pick
 *  - GPS + Open-Meteo: the weather where you are
 * The light sensor and accelerometer listen only between onResume and onPause.
 */
class MainActivity : LoggedActivity(), SensorEventListener {

    private val state = SensorState()
    private lateinit var sensorManager: SensorManager
    private lateinit var weatherHelper: WeatherHelper
    private var lightSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) loadWeather() else state.errorRes = R.string.err_no_permission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        weatherHelper = WeatherHelper(this)

        setContent {
            ScentCastTheme {
                TodaysPickScreen(
                    state = state,
                    hasLightSensor = lightSensor != null,
                    onRefreshWeather = { requestWeather() }
                )
            }
        }

        // Ask for the weather once when the app opens (not again after a rotation).
        if (savedInstanceState == null) requestWeather()
    }

    private fun requestWeather() {
        if (weatherHelper.hasPermission()) {
            loadWeather()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadWeather() {
        state.loading = true
        state.errorRes = null
        weatherHelper.load(
            onSuccess = {
                state.reading = it
                state.loading = false
            },
            onError = {
                state.errorRes = it
                state.loading = false
            }
        )
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this) // release the sensors when not on screen
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            state.lux = event.values[0]
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Total pull in g. Resting is about 1 g, a shake spikes well above that.
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            val now = SystemClock.elapsedRealtime()
            if (g > 2.5f && now - lastShakeTime > 1000) {
                lastShakeTime = now
                state.shakes++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

/** Starts an intent, and shows a message instead of crashing if no app can handle it. */
fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
    }
}

/** Night is 19:00 to 07:00. Used only until the light sensor gives a reading. */
private fun timeOfDayFromClock(): TimeOfDay {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (hour < 7 || hour >= 19) TimeOfDay.NIGHT else TimeOfDay.DAY
}

// Two limits with a gap between them, so the answer does not flicker near the boundary.
private const val DARK_BELOW_LUX = 30f
private const val BRIGHT_ABOVE_LUX = 80f

/**
 * A scrolling Column. Sensor values arrive as state, and the LaunchedEffects below
 * turn each new reading into a new pick.
 */
@Composable
fun TodaysPickScreen(state: SensorState, hasLightSensor: Boolean, onRefreshWeather: () -> Unit) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // rememberSaveable keeps these values when the activity is recreated, for example on
    // rotation. Only the id of the pick is saved, so the screen always shows the latest version.
    var weather by rememberSaveable { mutableStateOf(Weather.MILD) }
    var timeOfDay by rememberSaveable { mutableStateOf(timeOfDayFromClock()) }
    var pickId by rememberSaveable { mutableStateOf<Int?>(null) }
    val pick = pickId?.let { PerfumeRepository.get(it) }
    var budget by rememberSaveable { mutableStateOf(false) }
    val season = remember { PerfumeRepository.currentSeason() }
    // In budget mode the card shows the cheaper alternative, when the pick has one.
    val dupe = if (budget) pick?.dupe else null

    // New GPS weather: use it as the weather for the suggestion.
    LaunchedEffect(state.reading) {
        state.reading?.let { weather = it.toWeather() }
    }

    // New light reading: switch between day and night.
    LaunchedEffect(state.lux) {
        val lux = state.lux
        if (lux != null) {
            if (lux < DARK_BELOW_LUX) timeOfDay = TimeOfDay.NIGHT
            if (lux > BRIGHT_ABOVE_LUX) timeOfDay = TimeOfDay.DAY
        }
    }

    // Weather or time of day changed: pick again only if the current pick no longer fits.
    LaunchedEffect(weather, timeOfDay) {
        if (pick == null || !PerfumeRepository.isGoodFor(pick, weather, timeOfDay)) {
            pickId = PerfumeRepository.suggestFor(weather, timeOfDay, season)?.id
        }
    }

    // Each shake gives another pick.
    LaunchedEffect(state.shakes) {
        if (state.shakes > 0) {
            pickId = PerfumeRepository.suggestFor(weather, timeOfDay, season, skipId = pickId)?.id
        }
    }

    val timeLabel = stringResource(
        if (timeOfDay == TimeOfDay.NIGHT) R.string.tod_night else R.string.tod_day
    )

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

            // Live sensor readings, so the changing data is visible.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sensors_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val reading = state.reading
                    if (state.loading) {
                        Text(stringResource(R.string.live_loading))
                    } else if (reading != null) {
                        Text(stringResource(R.string.live_weather, reading.tempC, reading.humidity))
                        Text(
                            stringResource(R.string.live_position, reading.latitude, reading.longitude)
                        )
                    } else {
                        Text(stringResource(R.string.live_none))
                    }

                    val errorRes = state.errorRes
                    if (errorRes != null) {
                        Text(
                            text = stringResource(errorRes),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    val lux = state.lux
                    if (hasLightSensor && lux != null) {
                        Text(stringResource(R.string.light_value, lux.toInt(), timeLabel))
                    } else {
                        Text(stringResource(R.string.light_none, timeLabel))
                    }

                    Text(stringResource(R.string.season_value, stringResource(season.labelRes)))
                    Text(stringResource(R.string.shake_count, state.shakes))
                    Text(
                        text = stringResource(R.string.shake_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedButton(
                onClick = onRefreshWeather,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_live_weather))
            }

            Text(
                text = stringResource(R.string.home_weather_label),
                style = MaterialTheme.typography.bodyLarge
            )

            // The weather can still be chosen by hand, for example if location is off.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weather.entries.forEach { option ->
                    FilterChip(
                        selected = weather == option,
                        onClick = { weather = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // Budget option: swaps the pick for a cheaper perfume in a similar style.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.budget_switch),
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = budget, onCheckedChange = { budget = it })
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pick == null) {
                        Text(text = stringResource(R.string.pick_none))
                    } else {
                        if (dupe != null) {
                            Text(
                                text = stringResource(R.string.budget_label),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(text = dupe.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "${dupe.brand} · " +
                                    stringResource(R.string.price_about, dupe.priceEur),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.budget_similar, pick.name, pick.brand),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
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
                            // Brand is optional, so the separator is only shown when there is one.
                            val scentLabel = stringResource(pick.scentType.labelRes)
                            Text(
                                text = if (pick.brand.isBlank()) scentLabel else "${pick.brand} · $scentLabel",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (budget) {
                                Text(
                                    text = stringResource(R.string.budget_none),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // The weather and time reasons apply to either perfume.
                        Text(
                            text = stringResource(weather.reasonRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val timeReason =
                            if (timeOfDay == TimeOfDay.NIGHT) R.string.reason_night else R.string.reason_day
                        Text(
                            text = stringResource(timeReason),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (pick != null) {
                // Explicit intent with an extra: opens the notes, accords chart and maps page.
                OutlinedButton(
                    onClick = {
                        val details = Intent(context, PerfumeDetailActivity::class.java)
                            .putExtra(PerfumeDetailActivity.EXTRA_PERFUME_ID, pick.id)
                        context.startActivity(details)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_details))
                }
            }

            Button(
                onClick = {
                    pickId = PerfumeRepository.suggestFor(weather, timeOfDay, season, skipId = pickId)?.id
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_another))
            }

            // Saves a diary entry, with the temperature from the GPS weather reading.
            FilledTonalButton(
                onClick = {
                    if (pick != null) {
                        val wear = PerfumeRepository.logWear(pick, state.reading?.tempC, timeOfDay)
                        Toast.makeText(context, R.string.toast_logged, Toast.LENGTH_SHORT).show()
                        if (Settings.remindersOn) {
                            Reminders.schedule(context, wear)
                            // Android 13+ needs the user's OK before notifications can show.
                            val needsAsk = Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsAsk) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
                // In budget mode the shown perfume is not in your collection, so no logging.
                enabled = pick != null && dupe == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_wore))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Implicit intent 1: ACTION_SEND, the system lets the user choose a sharing app.
                OutlinedButton(
                    onClick = {
                        if (pick != null) {
                            val send = Intent(Intent.ACTION_SEND)
                            send.type = "text/plain"
                            send.putExtra(
                                Intent.EXTRA_TEXT,
                                context.getString(R.string.share_text, pick.name, pick.brand)
                            )
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
                        startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_shops))
                }
            }

            // Explicit intents: we name the exact activity to start.
            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, CollectionActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_collection))
            }
            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, DiaryActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_diary))
            }
            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, StatsActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_stats))
            }
        }
    }
}
