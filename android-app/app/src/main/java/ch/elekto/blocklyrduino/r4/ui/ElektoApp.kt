package ch.elekto.blocklyrduino.r4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.elekto.blocklyrduino.r4.data.ProjectStore
import ch.elekto.blocklyrduino.r4.engine.ArduinoCodeGenerator
import ch.elekto.blocklyrduino.r4.engine.IssueLevel
import ch.elekto.blocklyrduino.r4.engine.ProgramValidator
import ch.elekto.blocklyrduino.r4.engine.ValidationIssue
import ch.elekto.blocklyrduino.r4.model.BlockCategory
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.defaultBlinkProject
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElektoApp() {
    val context = LocalContext.current
    val store = remember { ProjectStore(context.applicationContext) }
    val blocks = remember {
        mutableStateListOf<ProgramBlock>().apply { addAll(store.loadBlocks()) }
    }

    var showPalette by remember { mutableStateOf(false) }
    var showCode by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ProgramBlock?>(null) }

    val issues by remember { derivedStateOf { ProgramValidator.validate(blocks) } }
    val errorBlockIds by remember {
        derivedStateOf {
            issues.filter { it.level == IssueLevel.ERROR }.mapNotNull { it.blockId }.toSet()
        }
    }

    fun persist() = store.saveBlocks(blocks)

    fun replaceProject(newBlocks: List<ProgramBlock>) {
        blocks.clear()
        blocks.addAll(newBlocks)
        persist()
    }

    fun addBlock(type: BlockType) {
        val nextY = ((blocks.maxOfOrNull { it.yDp } ?: -40f) + 92f).coerceAtMost(1900f)
        blocks += ProgramBlock(type = type, xDp = 40f, yDp = nextY)
        persist()
    }

    fun updateBlock(updated: ProgramBlock) {
        val index = blocks.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            blocks[index] = updated
            persist()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Elekto Blocks", fontWeight = FontWeight.SemiBold)
                        Text(
                            "UNO R4 WiFi • lokal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCode = true }) {
                        Icon(Icons.Default.Code, contentDescription = "Arduino-Code anzeigen")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Blink-Demo laden") },
                                onClick = {
                                    showMenu = false
                                    replaceProject(defaultBlinkProject())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Arbeitsfläche leeren") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    replaceProject(emptyList())
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPalette = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Blöcke") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StatusStrip(blockCount = blocks.size, issues = issues)
            BlockWorkspace(
                blocks = blocks,
                errorBlockIds = errorBlockIds,
                onMove = { id, dx, dy ->
                    val index = blocks.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        val current = blocks[index]
                        blocks[index] = current.copy(
                            xDp = (current.xDp + dx).coerceIn(8f, 1080f),
                            yDp = (current.yDp + dy).coerceIn(8f, 2020f)
                        )
                    }
                },
                onMoveFinished = { id ->
                    val index = blocks.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        val current = blocks[index]
                        blocks[index] = current.copy(
                            xDp = round(current.xDp / 8f) * 8f,
                            yDp = round(current.yDp / 8f) * 8f
                        )
                        persist()
                    }
                },
                onEdit = { editingBlock = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showPalette) {
        BlockPaletteSheet(
            onDismiss = { showPalette = false },
            onAdd = { type ->
                addBlock(type)
                showPalette = false
            }
        )
    }

    editingBlock?.let { block ->
        BlockEditorSheet(
            block = block,
            onDismiss = { editingBlock = null },
            onSave = { updated ->
                updateBlock(updated)
                editingBlock = null
            },
            onDelete = {
                blocks.removeAll { it.id == block.id }
                persist()
                editingBlock = null
            }
        )
    }

    if (showCode) {
        CodeSheet(
            blocks = blocks,
            onDismiss = { showCode = false }
        )
    }
}

@Composable
private fun StatusStrip(blockCount: Int, issues: List<ValidationIssue>) {
    val errors = issues.count { it.level == IssueLevel.ERROR }
    val warnings = issues.count { it.level == IssueLevel.WARNING }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, label = { Text("$blockCount Blöcke") })
            when {
                errors > 0 -> Text(
                    "$errors Fehler",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                warnings > 0 -> Text(
                    "$warnings Hinweise",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge
                )
                else -> Text(
                    "Prüfung OK",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockPaletteSheet(onDismiss: () -> Unit, onAdd: (BlockType) -> Unit) {
    var category by remember { mutableStateOf(BlockCategory.GRUNDLAGEN) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(horizontal = 18.dp)
        ) {
            Text("Blöcke auswählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Kategorien erscheinen nur bei Bedarf und lassen der Arbeitsfläche den Platz.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BlockCategory.entries.toList()) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item.title) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BlockType.entries.filter { it.category == category }) { type ->
                    Surface(
                        onClick = { onAdd(type) },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(type.title, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                type.subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockEditorSheet(
    block: ProgramBlock,
    onDismiss: () -> Unit,
    onSave: (ProgramBlock) -> Unit,
    onDelete: () -> Unit
) {
    var primaryText by remember(block.id) { mutableStateOf(block.primary.toString()) }
    var secondaryText by remember(block.id) { mutableStateOf(block.secondary.toString()) }
    var flag by remember(block.id) { mutableStateOf(block.flag) }
    val primary = primaryText.toIntOrNull()
    val secondary = secondaryText.toIntOrNull()
    val valid = when (block.type) {
        BlockType.DELAY -> primary != null && primary in 0..600_000
        BlockType.DIGITAL_WRITE, BlockType.IF_DIGITAL -> primary != null && primary in 0..13
        BlockType.PWM_WRITE -> primary != null && primary in setOf(3, 5, 6, 9, 10, 11) && secondary != null && secondary in 0..255
        BlockType.ANALOG_READ -> primary != null && primary in 0..5
        BlockType.REPEAT -> primary != null && primary in 1..1000
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(block.type.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(block.type.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (block.type) {
                BlockType.DELAY -> {
                    NumberEditor("Wartezeit (ms)", primaryText, { primaryText = it }, 0, 600_000)
                    QuickValues(listOf(100, 500, 1000, 2000)) { primaryText = it.toString() }
                }
                BlockType.DIGITAL_WRITE -> {
                    NumberEditor("Digital-Pin D0–D13", primaryText, { primaryText = it }, 0, 13)
                    HighLowSelector(flag = flag, onChange = { flag = it })
                }
                BlockType.PWM_WRITE -> {
                    NumberEditor("PWM-Pin", primaryText, { primaryText = it }, 0, 13)
                    Text("UNO R4 PWM: D3, D5, D6, D9, D10, D11", style = MaterialTheme.typography.labelMedium)
                    QuickValues(listOf(3, 5, 6, 9, 10, 11)) { primaryText = it.toString() }
                    NumberEditor("PWM-Wert 0–255", secondaryText, { secondaryText = it }, 0, 255)
                    QuickValues(listOf(0, 64, 128, 192, 255)) { secondaryText = it.toString() }
                }
                BlockType.ANALOG_READ -> {
                    NumberEditor("Analogeingang A0–A5", primaryText, { primaryText = it }, 0, 5)
                    QuickValues((0..5).toList()) { primaryText = it.toString() }
                }
                BlockType.REPEAT -> {
                    NumberEditor("Anzahl Wiederholungen", primaryText, { primaryText = it }, 1, 1000)
                    QuickValues(listOf(2, 5, 10, 20)) { primaryText = it.toString() }
                }
                BlockType.IF_DIGITAL -> {
                    NumberEditor("Eingang D0–D13", primaryText, { primaryText = it }, 0, 13)
                    Text("Bedingung", style = MaterialTheme.typography.labelLarge)
                    HighLowSelector(flag = flag, onChange = { flag = it })
                }
            }

            if (!valid) {
                Text(
                    "Dieser Wert passt nicht zum UNO R4 oder liegt außerhalb des erlaubten Bereichs.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Löschen")
                }
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(
                            block.copy(
                                primary = primary ?: block.primary,
                                secondary = secondary ?: block.secondary,
                                flag = flag
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Übernehmen")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumberEditor(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    min: Int,
    max: Int
) {
    val number = value.toIntOrNull() ?: min
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { text ->
                if (text.isEmpty() || text.all { it.isDigit() }) onValueChange(text)
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalIconButton(onClick = { onValueChange((number - 1).coerceAtLeast(min).toString()) }) {
                Icon(Icons.Default.Remove, contentDescription = "Wert verringern")
            }
            FilledTonalIconButton(onClick = { onValueChange((number + 1).coerceAtMost(max).toString()) }) {
                Icon(Icons.Default.Add, contentDescription = "Wert erhöhen")
            }
        }
    }
}

@Composable
private fun QuickValues(values: List<Int>, onValue: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value ->
            FilledTonalButton(onClick = { onValue(value) }) { Text(value.toString()) }
        }
    }
}

@Composable
private fun HighLowSelector(flag: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(selected = flag, onClick = { onChange(true) }, label = { Text("HIGH") })
        FilterChip(selected = !flag, onClick = { onChange(false) }, label = { Text("LOW") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeSheet(blocks: List<ProgramBlock>, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val code = ArduinoCodeGenerator.generate(blocks)
    val issues = ProgramValidator.validate(blocks)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Arduino-Code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Ziel: Arduino UNO R4 WiFi", color = MaterialTheme.colorScheme.onSurfaceVariant)

            issues.forEach { issue ->
                Surface(
                    color = if (issue.level == IssueLevel.ERROR) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(issue.message, modifier = Modifier.padding(12.dp))
                }
            }

            FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Code kopieren")
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
