package io.github.luposolitario.immundanoctis.ui.adventure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.luposolitario.immundanoctis.data.Skill

@Composable
fun SkillDialog(title: String, skills: List<Skill>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(skills) { skill ->
                        SkillCard(skill = skill)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: Skill) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = skill.icon, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = skill.name, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                RatingStars(currentLevel = skill.level, maxLevel = skill.maxLevel)
                Spacer(modifier = Modifier.height(8.dp))
                skill.effects.forEachIndexed { index, effect ->
                    val levelRequired = index + 1
                    val unlocked = skill.level >= levelRequired
                    Text(
                        text = "Liv $levelRequired: $effect",
                        fontSize = 14.sp,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStars(currentLevel: Int, maxLevel: Int) {
    Row {
        for (i in 1..maxLevel) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Livello $i",
                tint = if (i <= currentLevel) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DiceDialog(onDismiss: () -> Unit) {
    var diceCount by remember { mutableStateOf("2") }
    var modifier by remember { mutableStateOf("0") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Lancia i Dadi", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Icon(Icons.Default.Casino, contentDescription = "Dado", modifier = Modifier.size(64.dp))
                    Icon(Icons.Default.Casino, contentDescription = "Dado", modifier = Modifier.size(64.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = diceCount, onValueChange = { diceCount = it }, label = { Text("N. Dadi") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = modifier, onValueChange = { modifier = it }, label = { Text("Modificatore") }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { /* Logica da aggiungere */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("TIRA")
                }
            }
        }
    }
}
