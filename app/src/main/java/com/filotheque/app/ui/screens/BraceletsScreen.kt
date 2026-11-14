package com.filotheque.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.filotheque.app.data.local.entity.ProjectWithThreads
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.ui.components.parseComposeColor
import com.filotheque.app.ui.theme.DangerBg
import com.filotheque.app.ui.theme.DangerRed
import com.filotheque.app.ui.theme.OutlineSoft
import com.filotheque.app.ui.theme.SuccessBg
import com.filotheque.app.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BraceletsScreen(
    projects: List<ProjectWithThreads>,
    allThreads: List<ThreadEntity>,
    selectedProjectId: Long?,
    onSelectProject: (Long?) -> Unit,
    onCreateProject: (name: String, source: String, threadIds: List<Long>) -> Unit,
    onAddThreadToProject: (Long, Long) -> Unit,
    onRemoveThreadFromProject: (Long, Long) -> Unit,
    onDeleteProject: (ProjectWithThreads) -> Unit,
    onAddMissingToShoppingList: (ProjectWithThreads) -> Unit,
    onOpenThreadDetail: (ThreadEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (projects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aucun projet de bracelet", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Créez un bracelet pour vérifier en un coup d'œil quels fils vous avez déjà !",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(projects, key = { it.project.id }) { pwt ->
            val owned = pwt.threads.count { it.quantity > 0 }
            val total = pwt.threads.size
            val allReady = total > 0 && owned == total
            val progress = if (total == 0) 0f else owned.toFloat() / total.toFloat()

            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectProject(pwt.project.id) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, OutlineSoft)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pwt.project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (pwt.project.patternSource.isNotBlank()) {
                                Text(
                                    pwt.project.patternSource,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (allReady) SuccessBg else DangerBg
                        ) {
                            Text(
                                text = if (allReady) "Prêt !" else "${total - owned} manquant(s)",
                                color = if (allReady) SuccessGreen else DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pwt.threads.forEach { t ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseComposeColor(t.hexColor))
                                    .border(
                                        width = if (t.quantity == 0) 2.dp else 1.dp,
                                        color = if (t.quantity == 0) DangerRed else Color.Black.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$owned / $total fils en stock",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showCreateSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nouveau bracelet")
            }
        }
    }

    val selectedProject = projects.find { it.project.id == selectedProjectId }
    if (selectedProject != null) {
        var threadSearch by remember { mutableStateOf("") }
        val missingCount = selectedProject.threads.count { it.quantity == 0 }

        ModalBottomSheet(onDismissRequest = { onSelectProject(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedProject.project.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (selectedProject.project.patternSource.isNotBlank()) {
                            Text(
                                selectedProject.project.patternSource,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteProject(selectedProject) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer le projet", tint = DangerRed)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                selectedProject.threads.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenThreadDetail(t) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(parseComposeColor(t.hexColor))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${t.brandName} ${t.code}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(t.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (t.quantity > 0) SuccessBg else DangerBg
                        ) {
                            Text(
                                text = if (t.quantity > 0) "×${t.quantity}" else "Manquant",
                                color = if (t.quantity > 0) SuccessGreen else DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(onClick = { onRemoveThreadFromProject(selectedProject.project.id, t.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Retirer", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (missingCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onAddMissingToShoppingList(selectedProject) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter les $missingCount manquant(s) aux courses")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = threadSearch,
                    onValueChange = { threadSearch = it },
                    label = { Text("Ajouter un fil au bracelet (code ou nom)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (threadSearch.isNotBlank()) {
                    val existingIds = selectedProject.threads.map { it.id }.toSet()
                    val suggestions = allThreads.filter {
                        it.id !in existingIds &&
                            (it.code.contains(threadSearch, true) || it.name.contains(threadSearch, true))
                    }.take(4)
                    suggestions.forEach { cand ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddThreadToProject(selectedProject.project.id, cand.id)
                                    threadSearch = ""
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseComposeColor(cand.hexColor))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${cand.brandName} ${cand.code} — ${cand.name}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        var name by remember { mutableStateOf("") }
        var source by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Nouveau bracelet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du bracelet") },
                    placeholder = { Text("Ex : Bracelet Épi Pastel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Modèle / Source (optionnel)") },
                    placeholder = { Text("Ex : BraceletBook #45120") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateProject(name, source, emptyList())
                            showCreateSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Créer le bracelet")
                }
            }
        }
    }
}
