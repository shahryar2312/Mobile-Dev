package com.example.scentcast

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/** Screen 3: Add / Edit Perfume. Edit mode is chosen by an extra on the explicit intent. */
class AddPerfumeActivity : LoggedActivity() {

    // Key for the perfume id passed on the explicit intent from CollectionActivity.
    // It starts with the package name so it cannot clash with another app's extras.
    companion object {
        const val EXTRA_PERFUME_ID = "com.example.scentcast.PERFUME_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // -1 means no extra was sent, so this is the "add" case.
        val perfumeId = intent.getIntExtra(EXTRA_PERFUME_ID, -1)
        val existing = PerfumeRepository.get(perfumeId)

        setContent {
            ScentCastTheme {
                AddPerfumeScreen(existing = existing, onDone = { finish() })
            }
        }
    }
}

/**
 * Layout 3 of 3: a scrolling form with a picture area, text fields, radio buttons and chips.
 *  is the perfume being edited, or null when adding a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerfumeScreen(existing: Perfume?, onDone: () -> Unit) {
    val context = LocalContext.current

    // Form state. It starts from the existing perfume in edit mode, or from defaults
    // in add mode, and rememberSaveable keeps what the user typed across rotation.
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var brand by rememberSaveable { mutableStateOf(existing?.brand ?: "") }
    var scent by rememberSaveable { mutableStateOf(existing?.scentType ?: ScentType.FRESH) }
    var weather by rememberSaveable { mutableStateOf(existing?.weather ?: Weather.MILD) }
    // Bitmaps are too big to save in the instance state, so the photo resets on rotation.
    var photo by remember { mutableStateOf(existing?.photo) }

    // Implicit intent 3: ACTION_IMAGE_CAPTURE, any camera app returns a thumbnail.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) photo = bitmap
    }

    // Implicit intent 4: ACTION_GET_CONTENT, any gallery or file app returns an image Uri.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) photo = loadBitmap(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existing == null) R.string.add_perfume else R.string.edit_perfume
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val current = photo
                if (current != null) {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                } else if (existing?.imageRes != null) {
                    // No photo of its own yet, so show the bundled picture.
                    Image(
                        painter = painterResource(existing.imageRes),
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.White)
                            .padding(8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_photo),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            cameraLauncher.launch(null)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_take_photo))
                }
                OutlinedButton(
                    onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_gallery))
                }
            }

            // User interaction: text input. Each field writes straight into its state variable.
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text(stringResource(R.string.field_brand)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.label_scent_type),
                style = MaterialTheme.typography.titleSmall
            )
            // One radio button per scent type. The whole row is selectable, not just the
            // small circle, which gives a bigger touch target and works with TalkBack.
            ScentType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = scent == type,
                            onClick = { scent = type },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = scent == type, onClick = null)
                    Text(
                        text = stringResource(type.labelRes),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.label_suits),
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weather.entries.forEach { option ->
                    FilterChip(
                        selected = weather == option,
                        onClick = { weather = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // Save: builds a Perfume from the form and stores it. An existing perfume keeps
            // its id, so save() replaces it; a new one gets a fresh id and is added.
            // The button is disabled until a name is typed (simple input validation).
            Button(
                onClick = {
                    PerfumeRepository.save(
                        Perfume(
                            id = existing?.id ?: PerfumeRepository.newId(),
                            name = name.trim(),
                            brand = brand.trim(),
                            scentType = scent,
                            weather = weather,
                            photo = photo,
                            imageRes = existing?.imageRes
                        )
                    )
                    Toast.makeText(context, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                    onDone()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_save))
            }

            // Delete only makes sense for a perfume that already exists, so it is hidden in add mode.
            if (existing != null) {
                OutlinedButton(
                    onClick = {
                        PerfumeRepository.delete(existing.id)
                        Toast.makeText(context, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            }
        }
    }
}

/** Longest side, in pixels, a gallery photo is scaled down to. */
private const val MAX_PHOTO_SIDE = 1024

/**
 * Decodes a gallery image, shrinking it only when it is bigger than [MAX_PHOTO_SIDE]
 * so large photos stay small in memory and small ones stay sharp.
 */
private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width
        val height = info.size.height
        val longest = maxOf(width, height)
        if (longest > MAX_PHOTO_SIDE) {
            val scale = MAX_PHOTO_SIDE.toFloat() / longest
            decoder.setTargetSize(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1)
            )
        }
    }
} catch (e: Exception) {
    null
}
