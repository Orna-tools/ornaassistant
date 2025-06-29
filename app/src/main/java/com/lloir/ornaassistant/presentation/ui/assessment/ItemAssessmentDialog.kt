package com.lloir.ornaassistant.presentation.ui.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lloir.ornaassistant.domain.model.AssessmentResult
import com.lloir.ornaassistant.utils.ItemAssessmentTester

/**
 * A dialog for testing item assessment functionality.
 * 
 * This dialog allows users to input item details (name, level, and attributes)
 * and see the assessment results without relying on the overlay UI.
 */
@Composable
fun ItemAssessmentDialog(
    itemAssessmentTester: ItemAssessmentTester,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemLevel by remember { mutableStateOf("10") }
    
    // Attribute states
    var attack by remember { mutableStateOf("") }
    var magic by remember { mutableStateOf("") }
    var defense by remember { mutableStateOf("") }
    var resistance by remember { mutableStateOf("") }
    var dexterity by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }
    var mana by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    
    // Assessment result state
    var assessmentResult by remember { mutableStateOf<AssessmentResult?>(null) }
    var isAssessing by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Item Assessment Tester",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Item name input
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Item level input
                OutlinedTextField(
                    value = itemLevel,
                    onValueChange = { itemLevel = it },
                    label = { Text("Item Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Item Attributes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Attribute inputs in a grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    AttributeInput(
                        label = "Attack",
                        value = attack,
                        onValueChange = { attack = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AttributeInput(
                        label = "Magic",
                        value = magic,
                        onValueChange = { magic = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    AttributeInput(
                        label = "Defense",
                        value = defense,
                        onValueChange = { defense = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AttributeInput(
                        label = "Resistance",
                        value = resistance,
                        onValueChange = { resistance = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    AttributeInput(
                        label = "Dexterity",
                        value = dexterity,
                        onValueChange = { dexterity = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AttributeInput(
                        label = "HP",
                        value = hp,
                        onValueChange = { hp = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    AttributeInput(
                        label = "Mana",
                        value = mana,
                        onValueChange = { mana = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AttributeInput(
                        label = "Ward",
                        value = ward,
                        onValueChange = { ward = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Assess button
                Button(
                    onClick = {
                        if (itemName.isNotBlank()) {
                            isAssessing = true
                            
                            // Build attributes map
                            val attributes = buildMap {
                                attack.toIntOrNull()?.let { put("Att", it) }
                                magic.toIntOrNull()?.let { put("Mag", it) }
                                defense.toIntOrNull()?.let { put("Def", it) }
                                resistance.toIntOrNull()?.let { put("Res", it) }
                                dexterity.toIntOrNull()?.let { put("Dex", it) }
                                hp.toIntOrNull()?.let { put("HP", it) }
                                mana.toIntOrNull()?.let { put("Mana", it) }
                                ward.toIntOrNull()?.let { put("Ward", it) }
                            }
                            
                            // Assess the item
                            itemAssessmentTester.assessItem(
                                itemName = itemName,
                                level = itemLevel.toIntOrNull() ?: 10,
                                attributes = attributes
                            ) { result ->
                                assessmentResult = result
                                isAssessing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAssessing && itemName.isNotBlank()
                ) {
                    Text(if (isAssessing) "Assessing..." else "Assess Item")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Assessment result
                assessmentResult?.let { result ->
                    AssessmentResultView(result)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun AttributeInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun AssessmentResultView(result: AssessmentResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = "Assessment Result",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (result.assessmentFailed) {
            Text(
                text = "Assessment Failed",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text("Possible reasons:")
            Text("1. Adornments not properly subtracted")
            Text("2. Wrong item level detected")
            Text("3. Item name parsing issues")
        } else {
            // Quality
            val qualityColor = when {
                result.quality >= 1.8 -> Color.Green
                result.quality >= 1.5 -> Color(0xFFFFD700) // Gold
                else -> Color.White
            }
            
            Row {
                Text("Quality: ")
                Text(
                    text = String.format("%.2f", result.quality),
                    color = qualityColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats
            Text(
                text = "Stats:",
                fontWeight = FontWeight.Bold
            )
            
            result.stats.forEach { (statName, values) ->
                Row {
                    Text("$statName: ")
                    Text(values.joinToString(" → "))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Materials
            if (result.materials.isNotEmpty()) {
                Text(
                    text = "Materials:",
                    fontWeight = FontWeight.Bold
                )
                
                Text("10★: ${result.materials.getOrNull(0) ?: 0}")
                Text("MF: ${result.materials.getOrNull(1) ?: 0}")
                Text("DF: ${result.materials.getOrNull(2) ?: 0}")
                Text("GF: ${result.materials.getOrNull(3) ?: 0}")
            }
        }
    }
}