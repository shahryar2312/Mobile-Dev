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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.scentcast.ui.theme.ScentCastTheme

/** Screen 3: Add / Edit Perfume. Edit mode is chosen by an extra on the explicit intent. */
class AddPerfumeActivity : LoggedActivity() {

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerfumeScreen(existing: Perfume?, onDone: () -> Unit) {
    val context = LocalContext.current

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

            Button(
                onClick = {
                    PerfumeRepository.save(
                        Perfume(
                            id = existing?.id ?: PerfumeRepository.newId(),
                            name = name.trim(),
                            brand = brand.trim(),
                            scentType = scent,
                            weather = weather,
                            photo = photo
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

/** Decodes a gallery image at a quarter of its size so it stays small in memory. */
private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSampleSize(4)
    }
} catch (e: Exception) {
    null
}
