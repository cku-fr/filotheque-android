package com.filotheque.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.ui.components.ThreadGridCard
import com.filotheque.app.ui.components.ThreadListRow
import com.filotheque.app.ui.viewmodel.SortMode
import com.filotheque.app.ui.viewmodel.ViewMode

val COLOR_FAMILIES = listOf(
    "Toutes", "Blanc", "Noir/Gris", "Rouge", "Rose",
    "Orange", "Jaune", "Vert", "Bleu", "Violet", "Marron"
)

@Composable
fun ThreadCollectionView(
    threads: List<ThreadEntity>,
    viewMode: ViewMode,
    sortMode: SortMode,
    selectedColorFamily: String,
    emptyMessage: String,
    onSelectColorFamily: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleSortMode: () -> Unit,
    onThreadClick: (ThreadEntity) -> Unit,
    onAdjustQuantity: (Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSkeins = threads.sumOf { it.quantity }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(COLOR_FAMILIES) { family ->
                FilterChip(
                    selected = selectedColorFamily == family,
                    onClick = { onSelectColorFamily(family) },
                    label = { Text(family, fontSize = 12.sp) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${threads.size} couleurs · $totalSkeins échevettes",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggleSortMode) {
                    Icon(
                        imageVector = if (sortMode == SortMode.BY_SHADE) Icons.Default.Palette else Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sortMode == SortMode.BY_SHADE) "Tri : Nuances" else "Tri : Code",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Changer la vue"
                    )
                }
            }
        }

        if (threads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(threads, key = { it.id }) { thread ->
                    ThreadGridCard(
                        thread = thread,
                        onClick = { onThreadClick(thread) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(threads, key = { it.id }) { thread ->
                    ThreadListRow(
                        thread = thread,
                        onClick = { onThreadClick(thread) },
                        onAdjustQuantity = { delta -> onAdjustQuantity(thread.id, delta) }
                    )
                }
            }
        }
    }
}
