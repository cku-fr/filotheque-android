package com.filotheque.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import com.filotheque.app.ui.theme.OutlineSoft
import com.filotheque.app.ui.theme.SecondaryCoral
import com.filotheque.app.ui.theme.TextPrimary

fun parseComposeColor(hex: String): Color {
    val (r, g, b) = ColorUtils.parseHex(hex)
    return Color(r, g, b)
}

fun swatchBrush(thread: ThreadEntity): Brush {
    val parts = thread.gradientColors
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { parseComposeColor(it) }
    return if (parts.size >= 2) {
        Brush.linearGradient(colors = parts)
    } else {
        SolidColor(parseComposeColor(thread.hexColor))
    }
}

fun threadDisplayName(thread: ThreadEntity): String = buildString {
    if (thread.isGlitter) append("✨ ")
    if (thread.gradientColors.isNotBlank()) append("🌈 ")
    append(thread.name)
}

@Composable
fun GlitterOverlay(
    isLightBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sparkleColor = if (isLightBackground) Color(0xFFFFC107) else Color.White
    val secondaryColor = if (isLightBackground) Color.White else Color(0xFFFFF3B0)
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dots = listOf(
            Triple(0.14f, 0.24f, 2.2f),
            Triple(0.35f, 0.72f, 1.8f),
            Triple(0.55f, 0.22f, 2.4f),
            Triple(0.78f, 0.65f, 2.0f),
            Triple(0.88f, 0.28f, 1.6f),
            Triple(0.24f, 0.52f, 1.5f),
            Triple(0.66f, 0.80f, 1.6f)
        )
        dots.forEachIndexed { idx, (fx, fy, r) ->
            val cx = w * fx
            val cy = h * fy
            val color = if (idx % 2 == 0) sparkleColor else secondaryColor
            val alpha = if (idx % 2 == 0) 0.85f else 0.7f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = r.dp.toPx() * 0.55f,
                center = Offset(cx, cy)
            )
        }
        val stars = listOf(
            Triple(0.28f, 0.30f, 4.5f),
            Triple(0.72f, 0.44f, 5.2f)
        )
        stars.forEach { (fx, fy, arm) ->
            val cx = w * fx
            val cy = h * fy
            val len = arm.dp.toPx()
            val stroke = 1.2.dp.toPx()
            drawLine(
                color = sparkleColor.copy(alpha = 0.9f),
                start = Offset(cx - len, cy),
                end = Offset(cx + len, cy),
                strokeWidth = stroke
            )
            drawLine(
                color = sparkleColor.copy(alpha = 0.9f),
                start = Offset(cx, cy - len),
                end = Offset(cx, cy + len),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
fun ThreadGridCard(
    thread: ThreadEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchColor = parseComposeColor(thread.hexColor)
    val onSwatchColor = if (ColorUtils.isLightColor(thread.hexColor)) TextPrimary else Color.White

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineSoft)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatchBrush(thread))
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            ) {
                if (thread.isGlitter) {
                    GlitterOverlay(isLightBackground = ColorUtils.isLightColor(thread.hexColor))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                ) {
                    if (thread.isOnShoppingList) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart),
                            shape = RoundedCornerShape(8.dp),
                            color = SecondaryCoral
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "À acheter",
                                tint = Color.White,
                                modifier = Modifier.padding(3.dp).size(12.dp)
                            )
                        }
                    }
                    if (thread.quantity > 0) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = "×${thread.quantity}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = thread.code,
                        color = onSwatchColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = thread.brandName.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = threadDisplayName(thread),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ThreadListRow(
    thread: ThreadEntity,
    onClick: () -> Unit,
    onAdjustQuantity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchColor = parseComposeColor(thread.hexColor)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatchBrush(thread))
                    .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            ) {
                if (thread.isGlitter) {
                    GlitterOverlay(isLightBackground = ColorUtils.isLightColor(thread.hexColor))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${thread.brandName} ${thread.code}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (thread.isOnShoppingList) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "À acheter",
                            tint = SecondaryCoral,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = threadDisplayName(thread),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onAdjustQuantity(-1) },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Retirer", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = thread.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                IconButton(
                    onClick = { onAdjustQuantity(1) },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
