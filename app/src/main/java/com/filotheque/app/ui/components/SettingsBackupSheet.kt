package com.filotheque.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.data.update.OtaUpdater
import com.filotheque.app.data.update.UpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupSheet(
    allThreads: List<ThreadEntity>,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember { OtaUpdater.getCurrentVersionName(context) }

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadProgress by remember { mutableIntStateOf(-1) }

    val ownedColors = allThreads.count { it.quantity > 0 }
    val totalSkeins = allThreads.sumOf { it.quantity }
    val dmcTotal = allThreads.count { it.brandId == "DMC" }
    val dmcOwned = allThreads.count { it.brandId == "DMC" && it.quantity > 0 }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Statistiques & Sauvegarde", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("$ownedColors couleurs en stock · $totalSkeins échevettes au total", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Nuancier DMC : $dmcOwned / $dmcTotal couleurs", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version installée : v$currentVersion", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onDismiss()
                    onExportClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Exporter mon inventaire (.json)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onImportClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Importer une sauvegarde (.json)")
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (downloadProgress >= 0) {
                Text("Téléchargement : $downloadProgress %", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (availableUpdate != null) {
                Button(
                    onClick = {
                        val info = availableUpdate ?: return@Button
                        downloadProgress = 0
                        scope.launch {
                            try {
                                OtaUpdater.downloadAndInstall(context, info.apkUrl) { pct ->
                                    downloadProgress = pct
                                }
                            } catch (e: Exception) {
                                downloadProgress = -1
                                updateStatus = "Erreur de téléchargement"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Installer la mise à jour (v${availableUpdate!!.latestVersion})")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        updateStatus = "Vérification en cours..."
                        scope.launch {
                            val info = try {
                                OtaUpdater.checkForUpdate(currentVersion)
                            } catch (_: Exception) {
                                null
                            }
                            if (info != null) {
                                availableUpdate = info
                                updateStatus = "Nouvelle version v${info.latestVersion} disponible !"
                            } else {
                                updateStatus = "Vous avez déjà la dernière version (v$currentVersion)"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Rechercher une mise à jour (OTA)")
                }
            }

            updateStatus?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
