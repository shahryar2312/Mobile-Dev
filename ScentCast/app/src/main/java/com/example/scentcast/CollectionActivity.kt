package com.example.scentcast

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/** Screen 2: My Collection, a grid of bottles with a scent-type filter. */
class CollectionActivity : LoggedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScentCastTheme {
                CollectionScreen { finish() }
            }
        }
    }
}

/**
 * Layout 2 of 3: a top bar, a row of filter chips and a two-column LazyVerticalGrid.
 * The grid is lazy, so only the cards on screen are composed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // null means "All". The filter is saved across rotation.
    // PerfumeRepository.perfumes is Compose state, so this screen redraws by itself
    // when a perfume is added, edited or deleted on the form screen.
    var filter by rememberSaveable { mutableStateOf<ScentType?>(null) }
    val shown = PerfumeRepository.perfumes.filter { (_, _, _, scentType) ->
        (filter == null) || (scentType == filter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collection_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // Explicit intent: open the form in "add" mode (no extra attached).
            FloatingActionButton(
                onClick = {
                    context.startActivity(Intent(context, AddPerfumeActivity::class.java))
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_perfume),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // User interaction: filter chips. The row scrolls sideways on narrow screens.
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
                ScentType.entries.forEach { type ->
                    FilterChip(
                        selected = filter == type,
                        onClick = { filter = type },
                        label = { Text(stringResource(type.labelRes)) },
                    )
                }
            }

            // Empty state, so the user is told why the grid is blank.
            if (shown.isEmpty()) {
                Text(
                    text = stringResource(R.string.collection_empty),
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = Perfume::id) { perfume ->
                        PerfumeCard(perfume) {
                            // Explicit intent with data: open the form in "edit" mode.
                            val edit = Intent(context, AddPerfumeActivity::class.java)
                                .putExtra(AddPerfumeActivity.EXTRA_PERFUME_ID, perfume.id)
                            context.startActivity(edit)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Picture for a perfume. The user's own photo comes first, then the bundled
 * picture from res/drawable-nodpi, and the first letter of the name if there is neither.
 * Call it inside a Box that centres its content.
 */
@Composable
fun PerfumePicture(perfume: Perfume, modifier: Modifier = Modifier) {
    when {
        perfume.photo != null -> Image(
            bitmap = perfume.photo.asImageBitmap(),
            contentDescription = perfume.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )

        perfume.imageRes != null -> Image(
            painter = painterResource(perfume.imageRes),
            contentDescription = perfume.name,
            contentScale = ContentScale.Fit,
            // The bundled pictures have a white background, so the area behind them is white too.
            modifier = modifier.background(Color.White).padding(8.dp)
        )

        else -> Text(
            text = perfume.name.take(1).uppercase(),
            style = MaterialTheme.typography.displayMedium
        )
    }
}

/** One cell of the grid: a square picture with the name, brand and scent type under it. */
@Composable
fun PerfumeCard(perfume: Perfume, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PerfumePicture(perfume, Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = perfume.name, style = MaterialTheme.typography.titleMedium)
            // Brand is optional. Skipping the Text avoids an empty line on the card.
            if (perfume.brand.isNotBlank()) {
                Text(text = perfume.brand, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = stringResource(perfume.scentType.labelRes),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
