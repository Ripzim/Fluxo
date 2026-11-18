@file:OptIn(ExperimentalMaterial3Api::class)

package app.fluxo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

private data class ColorField(
    val key: String,
    val label: String,
    val value: String,
    val build: (String) -> FxTheme,
)

@Composable
fun SettingsContent(
    theme: FxTheme,
    saved: List<FxTheme>,
    blockShorts: Boolean,
    onBlockShorts: (Boolean) -> Unit,
    onTheme: (FxTheme) -> Unit,
    onSave: (String) -> Unit,
    onDeleteSaved: (FxTheme) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var saveName by remember { mutableStateOf("") }

    fun edit(t: FxTheme) = onTheme(t.copy(name = FxTheme.CUSTOM))

    val fields = listOf(
        ColorField("bg", "Fundo", theme.bg) { theme.copy(bg = it) },
        ColorField("surface", "Superfície (menus, cartões)", theme.surface) { theme.copy(surface = it) },
        ColorField("text", "Texto", theme.text) { theme.copy(text = it) },
        ColorField("text2", "Texto secundário", theme.textSecondary) { theme.copy(textSecondary = it) },
        ColorField("accent", "Destaque", theme.accent) { theme.copy(accent = it) },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Fluxo", style = MaterialTheme.typography.titleLarge)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bloquear Shorts")
                Text(
                    "Remove prateleiras, aba e links de Shorts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = blockShorts, onCheckedChange = onBlockShorts)
        }

        SectionTitle("Temas prontos")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FxTheme.presets.forEach { p ->
                PresetChip(p, selected = p.name == theme.name) { onTheme(p) }
            }
        }

        SectionTitle("Cores")
        fields.forEach { f ->
            ColorRow(
                label = f.label,
                hex = f.value,
                expanded = expanded == f.key,
                onToggle = { expanded = if (expanded == f.key) null else f.key },
                onChange = { edit(f.build(it)) },
            )
        }

        SectionTitle("Fonte e densidade")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sans" to "Padrão", "serif" to "Serifa", "mono" to "Mono").forEach { (key, label) ->
                FilterChip(
                    selected = theme.font == key,
                    onClick = { edit(theme.copy(font = key)) },
                    label = { Text(label) },
                )
            }
        }
        LabeledSlider(
            label = "Tamanho do texto",
            valueText = "${(theme.textScale * 100).roundToInt()}%",
            value = theme.textScale,
            range = 0.8f..1.4f,
        ) { edit(theme.copy(textScale = it)) }
        LabeledSlider(
            label = "Arredondamento das miniaturas",
            valueText = "${theme.radius}px",
            value = theme.radius.toFloat(),
            range = 0f..28f,
        ) { edit(theme.copy(radius = it.roundToInt())) }
        LabeledSlider(
            label = "Densidade (experimental)",
            valueText = "${(theme.density * 100).roundToInt()}%",
            value = theme.density,
            range = 0.8f..1.2f,
        ) { edit(theme.copy(density = it)) }

        SectionTitle("Meus temas")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = saveName,
                onValueChange = { saveName = it.take(30) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Nome do tema") },
            )
            Button(onClick = { onSave(saveName); saveName = "" }) { Text("Salvar") }
        }
        saved.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PresetChip(s, selected = s.name == theme.name, modifier = Modifier.weight(1f)) { onTheme(s) }
                IconButton(onClick = { onDeleteSaved(s) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar tema ${s.name}")
                }
            }
        }

        SectionTitle("Importar e exportar")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Copiar tema") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Colar tema") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun PresetChip(
    theme: FxTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = Color(theme.accent.toColorInt())
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color(theme.bg.toColorInt()))
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(theme.name, color = Color(theme.text.toColorInt()), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorRow(
    label: String,
    hex: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (String) -> Unit,
) {
    val rgb = remember(hex) { hexToRgb(hex) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(hex.toColorInt()))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Text(label, Modifier.weight(1f))
            HexField(hex, onChange)
        }
        if (expanded) {
            listOf("R", "G", "B").forEachIndexed { i, channel ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(channel, Modifier.width(18.dp))
                    Slider(
                        value = rgb[i].toFloat(),
                        onValueChange = { v ->
                            val next = rgb.copyOf()
                            next[i] = v.roundToInt()
                            onChange(rgbToHex(next))
                        },
                        valueRange = 0f..255f,
                    )
                }
            }
        }
    }
}

@Composable
private fun HexField(hex: String, onChange: (String) -> Unit) {
    var text by remember(hex) { mutableStateOf(hex) }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v.take(7)
            if (HEX_REGEX.matches(v)) onChange(v.uppercase())
        },
        singleLine = true,
        modifier = Modifier.width(120.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun hexToRgb(hex: String): IntArray = intArrayOf(
    hex.substring(1, 3).toInt(16),
    hex.substring(3, 5).toInt(16),
    hex.substring(5, 7).toInt(16),
)

private fun rgbToHex(c: IntArray): String = String.format("#%02X%02X%02X", c[0], c[1], c[2])
