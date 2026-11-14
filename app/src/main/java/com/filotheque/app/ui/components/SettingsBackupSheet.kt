package com.filotheque.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupSheet(
    allThreads: List<ThreadEntity>,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
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
        }
    }
}
