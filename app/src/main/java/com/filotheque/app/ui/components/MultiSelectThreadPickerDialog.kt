package com.filotheque.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import com.filotheque.app.ui.screens.COLOR_FAMILIES
import com.filotheque.app.ui.theme.OutlineSoft
import com.filotheque.app.ui.theme.TextPrimary

private enum class PickerFilter(val label: String) {
    ALL("Tous"),
    IN_STOCK("En stock"),
    DMC("DMC"),
    ANCHOR("Anchor"),
    CUSTOM("Tiers")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectThreadPickerDialog(
    allThreads: List<ThreadEntity>,
    initialSelectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(PickerFilter.ALL) }
    var activeColorFamily by remember { mutableStateOf("Toutes") }

    val filteredThreads = remember(allThreads, searchQuery, activeFilter, activeColorFamily) {
        val q = searchQuery.trim().lowercase()
        allThreads
            .filter { t ->
                val matchQ = q.isEmpty() ||
                    t.code.lowercase().contains(q) ||
                    t.name.lowercase().contains(q) ||
                    t.brandName.lowercase().contains(q)
                val matchFilter = when (activeFilter) {
                    PickerFilter.ALL -> true
                    PickerFilter.IN_STOCK -> t.quantity > 0
                    PickerFilter.DMC -> t.brandId == "DMC"
                    PickerFilter.ANCHOR -> t.brandId == "ANCHOR"
                    PickerFilter.CUSTOM -> t.brandId != "DMC" && t.brandId != "ANCHOR"
                }
                val matchFam = activeColorFamily == "Toutes" || t.colorFamily == activeColorFamily
                matchQ && matchFilter && matchFam
            }
            .sortedBy { ColorUtils.colorSortKey(it.hexColor) }
    }

    val selectedThreads = remember(allThreads, selectedIds) {
        allThreads.filter { it.id in selectedIds }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    title = {
                        Text(
                            text = "Choisir les fils (${selectedIds.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        Button(
                            onClick = { onConfirm(selectedIds) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Valider")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedThreads.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        items(selectedThreads, key = { it.id }) { t ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseComposeColor(t.hexColor))
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .clickable { selectedIds = selectedIds - t.id },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Retirer ${t.code}",
                                    tint = if (ColorUtils.isLightColor(t.hexColor)) TextPrimary else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un code ou une couleur...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(PickerFilter.entries) { f ->
                        FilterChip(
                            selected = activeFilter == f,
                            onClick = { activeFilter = f },
                            label = { Text(f.label, fontSize = 12.sp) }
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(COLOR_FAMILIES) { fam ->
                        FilterChip(
                            selected = activeColorFamily == fam,
                            onClick = { activeColorFamily = fam },
                            label = { Text(fam, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = "${filteredThreads.size} couleurs disponibles",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredThreads, key = { it.id }) { thread ->
                        val isSelected = thread.id in selectedIds
                        val swatchColor = parseComposeColor(thread.hexColor)
                        val onSwatch = if (ColorUtils.isLightColor(thread.hexColor)) TextPrimary else Color.White

                        Card(
                            modifier = Modifier.clickable {
                                selectedIds = if (isSelected) selectedIds - thread.id else selectedIds + thread.id
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else OutlineSoft
                            )
                        ) {
                            Column(modifier = Modifier.padding(7.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(swatchColor)
                                        .padding(5.dp)
                                ) {
                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopStart).size(20.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Sélectionné",
                                                tint = Color.White,
                                                modifier = Modifier.padding(3.dp)
                                            )
                                        }
                                    }
                                    if (thread.quantity > 0) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.9f)
                                        ) {
                                            Text(
                                                text = "×${thread.quantity}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = thread.code,
                                        color = onSwatch,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = thread.brandName.uppercase(),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Text(
                                    text = thread.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
