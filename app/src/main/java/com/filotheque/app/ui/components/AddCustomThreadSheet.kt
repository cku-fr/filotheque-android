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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import com.filotheque.app.ui.theme.TextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomThreadSheet(
    allThreads: List<ThreadEntity>,
    closestDmc: ThreadEntity?,
    onColorChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (brand: String, code: String, name: String, hex: String) -> Unit
) {
    var brand by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPhotoPicker by remember { mutableStateOf(false) }

    var r by remember { mutableFloatStateOf(152f) }
    var g by remember { mutableFloatStateOf(226f) }
    var b by remember { mutableFloatStateOf(198f) }

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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Ajouter un fil de marque tierce", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(previewColor)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${brand.ifBlank { "Marque" }} · $hex",
                    color = onPreviewColor,
                    fontWeight = FontWeight.Bold
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
                            fontSize = 12.sp
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
                    singleLine = true
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    placeholder = { Text("ACT-12") },
                    modifier = Modifier.weight(0.7f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom de la couleur") },
                placeholder = { Text("Menthe douce") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    onSave(brand.ifBlank { "Perso" }, code.ifBlank { "01" }, name.ifBlank { "Couleur perso" }, hex)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enregistrer dans l'inventaire")
            }
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
