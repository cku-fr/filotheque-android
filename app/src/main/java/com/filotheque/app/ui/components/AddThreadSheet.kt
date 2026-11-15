package com.filotheque.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import com.filotheque.app.ui.theme.TextPrimary
import kotlin.math.roundToInt

enum class AddThreadTab(val label: String) {
    CODES("DMC / Anchor"),
    PHOTO("Par photo"),
    CUSTOM("Marque tierce")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddThreadSheet(
    allThreads: List<ThreadEntity>,
    closestDmc: ThreadEntity?,
    initialTab: AddThreadTab = AddThreadTab.CODES,
    initialBrandId: String = "DMC",
    onColorChanged: (String) -> Unit,
    onBatchSubmit: (brandId: String, rawCodes: String) -> Unit,
    onPhotoThreadSelected: (ThreadEntity) -> Unit,
    onCustomSave: (brand: String, code: String, name: String, hex: String, isGlitter: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Ajouter un fil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AddThreadTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AddThreadTab.entries.size
                        ),
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeTab) {
                AddThreadTab.CODES -> {
                    BatchCodesContent(
                        initialBrandId = initialBrandId,
                        onSubmit = { brandId, rawCodes ->
                            onBatchSubmit(brandId, rawCodes)
                            onDismiss()
                        }
                    )
                }

                AddThreadTab.PHOTO -> {
                    PhotoColorPickerContent(
                        allThreads = allThreads,
                        pickColorOnly = false,
                        onDismiss = onDismiss,
                        onThreadSelected = onPhotoThreadSelected
                    )
                }

                AddThreadTab.CUSTOM -> {
                    CustomThreadContent(
                        allThreads = allThreads,
                        closestDmc = closestDmc,
                        onColorChanged = onColorChanged,
                        onSave = { brand, code, name, hex, isGlitter ->
                            onCustomSave(brand, code, name, hex, isGlitter)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchCodesContent(
    initialBrandId: String,
    onSubmit: (brandId: String, rawCodes: String) -> Unit
) {
    var selectedBrand by remember { mutableStateOf(if (initialBrandId == "ANCHOR") "ANCHOR" else "DMC") }
    var rawCodes by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tapez un ou plusieurs codes séparés par des virgules ou espaces pour ajouter +1 échevette à chacun (y compris les fils spéciaux E334, C310, S321…).",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedBrand == "DMC",
                onClick = { selectedBrand = "DMC" },
                label = { Text("DMC") }
            )
            FilterChip(
                selected = selectedBrand == "ANCHOR",
                onClick = { selectedBrand = "ANCHOR" },
                label = { Text("Anchor") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = rawCodes,
            onValueChange = { rawCodes = it },
            label = { Text("Codes des fils") },
            placeholder = { Text("Ex : 310, 666, E334, B5200") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (rawCodes.isNotBlank()) {
                    onSubmit(selectedBrand, rawCodes)
                }
            },
            enabled = rawCodes.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Ajouter à l'inventaire")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomThreadSheet(
    thread: ThreadEntity,
    allThreads: List<ThreadEntity>,
    closestDmc: ThreadEntity?,
    onColorChanged: (String) -> Unit,
    onSave: (brand: String, code: String, name: String, hex: String, isGlitter: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Modifier le fil personnalisé",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            CustomThreadContent(
                allThreads = allThreads,
                closestDmc = closestDmc,
                initialBrand = thread.brandName,
                initialCode = thread.code,
                initialName = thread.name,
                initialHex = thread.hexColor,
                initialIsGlitter = thread.isGlitter,
                submitLabel = "Enregistrer les modifications",
                onColorChanged = onColorChanged,
                onSave = { brand, code, name, hex, isGlitter ->
                    onSave(brand, code, name, hex, isGlitter)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun CustomThreadContent(
    allThreads: List<ThreadEntity>,
    closestDmc: ThreadEntity?,
    initialBrand: String = "",
    initialCode: String = "",
    initialName: String = "",
    initialHex: String = "#98E2C6",
    initialIsGlitter: Boolean = false,
    submitLabel: String = "Enregistrer dans l'inventaire",
    onColorChanged: (String) -> Unit,
    onSave: (brand: String, code: String, name: String, hex: String, isGlitter: Boolean) -> Unit
) {
    var brand by remember(initialBrand) { mutableStateOf(initialBrand) }
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var isGlitter by remember(initialIsGlitter) { mutableStateOf(initialIsGlitter) }
    var showPhotoPicker by remember { mutableStateOf(false) }

    val initialRgb = remember(initialHex) { ColorUtils.parseHex(initialHex) }
    var r by remember(initialHex) { mutableFloatStateOf(initialRgb.r.toFloat()) }
    var g by remember(initialHex) { mutableFloatStateOf(initialRgb.g.toFloat()) }
    var b by remember(initialHex) { mutableFloatStateOf(initialRgb.b.toFloat()) }

    val hex = remember(r, g, b) {
        String.format("#%02X%02X%02X", r.roundToInt(), g.roundToInt(), b.roundToInt())
    }

    LaunchedEffect(hex) {
        onColorChanged(hex)
    }

    val previewColor = Color(r.roundToInt(), g.roundToInt(), b.roundToInt())
    val onPreviewColor = if (ColorUtils.isLightColor(hex)) TextPrimary else Color.White

    val presets = listOf(
        "#E31D42", "#FFA4BE", "#FF8B00", "#FDED54",
        "#7FB335", "#30C2EC", "#466A8E", "#C8A2C8", "#CB9051", "#1C1B1F"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(previewColor)
                .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            if (isGlitter) {
                GlitterOverlay(isLightBackground = ColorUtils.isLightColor(hex))
            }
            Text(
                text = "${brand.ifBlank { "Marque" }} · $hex${if (isGlitter) " ✨" else ""}",
                color = onPreviewColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(12.dp)
            )
        }

        if (closestDmc != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(parseComposeColor(closestDmc.hexColor))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Teinte DMC la plus proche : ${closestDmc.code} (${closestDmc.name})",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { showPhotoPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pipette depuis une photo")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            presets.forEach { presetHex ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parseComposeColor(presetHex))
                        .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                        .clickable {
                            val rgb = ColorUtils.parseHex(presetHex)
                            r = rgb.r.toFloat()
                            g = rgb.g.toFloat()
                            b = rgb.b.toFloat()
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Rouge (${r.roundToInt()})", fontSize = 11.sp)
        Slider(value = r, onValueChange = { r = it }, valueRange = 0f..255f)
        Text("Vert (${g.roundToInt()})", fontSize = 11.sp)
        Slider(value = g, onValueChange = { g = it }, valueRange = 0f..255f)
        Text("Bleu (${b.roundToInt()})", fontSize = 11.sp)
        Slider(value = b, onValueChange = { b = it }, valueRange = 0f..255f)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marque") },
                placeholder = { Text("Action, Sostrene...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code") },
                placeholder = { Text("ACT-12") },
                modifier = Modifier.weight(0.7f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom de la couleur") },
            placeholder = { Text("Menthe douce") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fil à paillettes ✨", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = isGlitter,
                onCheckedChange = { isGlitter = it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                onSave(
                    brand.ifBlank { "Perso" },
                    code.ifBlank { "01" },
                    name.ifBlank { "Couleur perso" },
                    hex,
                    isGlitter
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(submitLabel)
        }
    }

    if (showPhotoPicker) {
        PhotoColorPickerSheet(
            allThreads = allThreads,
            pickColorOnly = true,
            onDismiss = { showPhotoPicker = false },
            onColorPicked = { pickedHex ->
                val rgb = ColorUtils.parseHex(pickedHex)
                r = rgb.r.toFloat()
                g = rgb.g.toFloat()
                b = rgb.b.toFloat()
            }
        )
    }
}
