package com.filotheque.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchAddSheet(
    onDismiss: () -> Unit,
    onSubmit: (brandId: String, rawCodes: String) -> Unit
) {
    var selectedBrand by remember { mutableStateOf("DMC") }
    var rawCodes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Ajout rapide par codes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tapez plusieurs numéros séparés par des virgules ou espaces pour ajouter +1 échevette à chacun.",
                fontSize = 13.sp
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
                placeholder = { Text("Ex : 310, 666, 798, B5200") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (rawCodes.isNotBlank()) {
                        onSubmit(selectedBrand, rawCodes)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Ajouter à l'inventaire")
            }
        }
    }
}
