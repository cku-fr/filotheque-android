package com.filotheque.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.filotheque.app.ui.theme.SecondaryCoral
import com.filotheque.app.ui.theme.SuccessBg
import com.filotheque.app.ui.theme.SuccessGreen
import com.filotheque.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailSheet(
    thread: ThreadEntity,
    equivalents: List<ThreadEntity>,
    onDismiss: () -> Unit,
    onAdjustQuantity: (Int) -> Unit,
    onToggleShoppingList: () -> Unit,
    onUpdateNote: (String) -> Unit
) {
    val swatchColor = parseComposeColor(thread.hexColor)
    val onSwatchColor = if (ColorUtils.isLightColor(thread.hexColor)) TextPrimary else Color.White
    var noteText by remember(thread.id) { mutableStateOf(thread.note) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(swatchColor)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = thread.brandName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSwatchColor.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "${thread.code} — ${thread.name}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSwatchColor
                    )
                }
                Text(
                    text = thread.colorFamily,
                    fontSize = 12.sp,
                    color = onSwatchColor.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quantité en stock", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustQuantity(-1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Retirer")
                    }
                    Text(
                        text = thread.quantity.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { onAdjustQuantity(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    }
                }
            }

            if (equivalents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Équivalence(s) DMC ↔ Anchor",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        equivalents.forEach { eq ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(parseComposeColor(eq.hexColor))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${eq.brandName} ${eq.code} (${eq.name})", fontSize = 13.sp)
                                }
                                if (eq.quantity > 0) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = SuccessBg) {
                                        Text(
                                            text = "×${eq.quantity} en stock",
                                            color = SuccessGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    onUpdateNote(it)
                },
                label = { Text("Note personnelle") },
                placeholder = { Text("Ex : boîte du haut, fil entamé...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onToggleShoppingList,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (thread.isOnShoppingList) SecondaryCoral else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (thread.isOnShoppingList) "Retirer de la liste de courses"
                    else "Ajouter à la liste de courses"
                )
            }
        }
    }
}
