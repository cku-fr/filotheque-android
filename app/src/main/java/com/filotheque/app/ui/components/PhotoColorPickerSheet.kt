package com.filotheque.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import com.filotheque.app.ui.theme.SuccessBg
import com.filotheque.app.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoColorPickerSheet(
    allThreads: List<ThreadEntity>,
    pickColorOnly: Boolean,
    onDismiss: () -> Unit,
    onColorPicked: (String) -> Unit = {},
    onThreadSelected: (ThreadEntity) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            PhotoColorPickerContent(
                allThreads = allThreads,
                pickColorOnly = pickColorOnly,
                onDismiss = onDismiss,
                onColorPicked = onColorPicked,
                onThreadSelected = onThreadSelected
            )
        }
    }
}

@Composable
fun PhotoColorPickerContent(
    allThreads: List<ThreadEntity>,
    pickColorOnly: Boolean,
    onDismiss: () -> Unit,
    onColorPicked: (String) -> Unit = {},
    onThreadSelected: (ThreadEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var reticlePos by remember { mutableStateOf<Offset?>(null) }
    var sampledHex by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun loadUri(uri: Uri) {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { decodeScaledBitmap(context, uri, 1024) }
            bitmap = loaded
            reticlePos = null
            sampledHex = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { loadUri(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { loadUri(it) }
    }

    fun updateSampleAt(pos: Offset) {
        val bmp = bitmap ?: return
        if (imageSize.width == 0 || imageSize.height == 0) return
        val clampedX = pos.x.coerceIn(0f, imageSize.width.toFloat() - 1f)
        val clampedY = pos.y.coerceIn(0f, imageSize.height.toFloat() - 1f)
        reticlePos = Offset(clampedX, clampedY)

        val bx = ((clampedX / imageSize.width) * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
        val by = ((clampedY / imageSize.height) * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
        sampledHex = sampleAverageColor(bmp, bx, by, radius = 4) // 9x9 px window
    }

    val dmcMatches = remember(sampledHex, allThreads) {
        sampledHex?.let { hex ->
            ColorUtils.findTopMatches(hex, allThreads.filter { it.brandId == "DMC" }, limit = 3)
        } ?: emptyList()
    }
    val anchorMatches = remember(sampledHex, allThreads) {
        sampledHex?.let { hex ->
            ColorUtils.findTopMatches(hex, allThreads.filter { it.brandId == "ANCHOR" }, limit = 3)
        } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Reconnaissance couleur par photo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prenez une photo à la lumière naturelle puis touchez le fil pour détecter sa teinte (moyenne 9×9 px).",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val uri = createTempImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galerie")
                }
            }

            bitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f))
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .onSizeChanged { imageSize = it }
                        .pointerInput(bmp) {
                            detectTapGestures { offset -> updateSampleAt(offset) }
                        }
                        .pointerInput(bmp) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                updateSampleAt(change.position)
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Photo du fil",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    reticlePos?.let { pos ->
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White,
                                radius = 22f,
                                center = pos,
                                style = Stroke(width = 5f)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 24f,
                                center = pos,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }

            sampledHex?.let { hex ->
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parseComposeColor(hex))
                                .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Couleur détectée : $hex", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Famille : ${ColorUtils.classifyColorFamily(hex)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (pickColorOnly) {
                            Button(
                                onClick = {
                                    onColorPicked(hex)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Utiliser")
                            }
                        }
                    }
                }

                if (dmcMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Top 3 DMC les plus proches", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    dmcMatches.forEach { match ->
                        MatchRow(
                            match = match,
                            onClick = {
                                if (pickColorOnly) {
                                    onColorPicked(hex)
                                } else {
                                    onThreadSelected(match.thread)
                                }
                                onDismiss()
                            }
                        )
                    }
                }

                if (anchorMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Top 3 Anchor les plus proches", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    anchorMatches.forEach { match ->
                        MatchRow(
                            match = match,
                            onClick = {
                                if (pickColorOnly) {
                                    onColorPicked(hex)
                                } else {
                                    onThreadSelected(match.thread)
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(
    match: ColorUtils.ThreadMatch,
    onClick: () -> Unit
) {
    val t = match.thread
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(parseComposeColor(t.hexColor))
                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${t.brandName} ${t.code} — ${t.name}", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text("ΔE = ${String.format("%.1f", match.deltaE)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (t.quantity > 0) {
            Surface(shape = RoundedCornerShape(8.dp), color = SuccessBg) {
                Text(
                    text = "×${t.quantity}",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
        sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
}

private fun sampleAverageColor(bmp: Bitmap, cx: Int, cy: Int, radius: Int): String {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0
    val xStart = (cx - radius).coerceAtLeast(0)
    val xEnd = (cx + radius).coerceAtMost(bmp.width - 1)
    val yStart = (cy - radius).coerceAtLeast(0)
    val yEnd = (cy + radius).coerceAtMost(bmp.height - 1)

    for (y in yStart..yEnd) {
        for (x in xStart..xEnd) {
            val px = bmp.getPixel(x, y)
            rSum += android.graphics.Color.red(px)
            gSum += android.graphics.Color.green(px)
            bSum += android.graphics.Color.blue(px)
            count++
        }
    }
    if (count == 0) return "#808080"
    return String.format("#%02X%02X%02X", (rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
}
