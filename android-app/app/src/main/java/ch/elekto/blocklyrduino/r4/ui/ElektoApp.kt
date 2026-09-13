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
import ch.elekto.blocklyrduino.r4.model.BlockRole
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.CommandWidthDp
import ch.elekto.blocklyrduino.r4.model.ConnectorOverlapDp
import ch.elekto.blocklyrduino.r4.model.ContainerHeaderDp
import ch.elekto.blocklyrduino.r4.model.ContainerWidthDp
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.ValueWidthDp
import ch.elekto.blocklyrduino.r4.model.blockHeightDp
import ch.elekto.blocklyrduino.r4.model.chainFrom
import ch.elekto.blocklyrduino.r4.model.defaultBlinkProject
import ch.elekto.blocklyrduino.r4.model.linkedDescendantIds
import ch.elekto.blocklyrduino.r4.model.normalizeProjectLayout
import ch.elekto.blocklyrduino.r4.model.valueSocketOffset
import kotlin.math.abs
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

    fun applyNormalized(save: Boolean = false) {
        val normalized = normalizeProjectLayout(blocks.toList())
        blocks.clear()
        blocks.addAll(normalized)
        if (save) persist()
    }

    fun replaceProject(newBlocks: List<ProgramBlock>) {
        blocks.clear()
        blocks.addAll(normalizeProjectLayout(newBlocks))
        persist()
    }

    fun addBlock(type: BlockType) {
        val loose = blocks.filter { it.parentId == null && it.valueOwnerId == null }
        val nextY = ((loose.maxOfOrNull { it.yDp + blockHeightDp(it, blocks) } ?: -20f) + 26f).coerceAtMost(1900f)
        blocks += ProgramBlock(type = type, xDp = 32f, yDp = nextY)
        persist()
    }

    fun updateBlock(updated: ProgramBlock) {
        val index = blocks.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            blocks[index] = updated
            applyNormalized(save = true)
        }
    }

    fun detachForDrag(id: String) {
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = blocks[index]
        if (current.parentId != null || current.previousId != null || current.valueOwnerId != null) {
            blocks[index] = current.copy(
                parentId = null,
                previousId = null,
                childOrder = 0,
                valueOwnerId = null,
                valueInputKey = null
            )
            applyNormalized(save = false)
        }
    }

    fun moveWithConnections(id: String, dx: Float, dy: Float) {
        val linked = linkedDescendantIds(blocks.toList(), id) + id
        blocks.indices.forEach { index ->
            val block = blocks[index]
            if (block.id in linked) {
                blocks[index] = block.copy(
                    xDp = (block.xDp + dx).coerceIn(8f, 1100f),
                    yDp = (block.yDp + dy).coerceIn(8f, 2070f)
                )
            }
        }
    }

    fun finishMove(id: String) {
        var index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return

        var moving = blocks[index]
        moving = moving.copy(
            xDp = round(moving.xDp / 8f) * 8f,
            yDp = round(moving.yDp / 8f) * 8f,
            parentId = null,
            previousId = null,
            valueOwnerId = null,
            valueInputKey = null
        )
        blocks[index] = moving

        val excluded = linkedDescendantIds(blocks.toList(), id) + id

        // Value blocks use typed sockets. A NUMBER output can only enter a
        // NUMBER input. Shape and compatibility therefore describe the same rule.
        if (moving.type.role == BlockRole.VALUE) {
            val target = blocks
                .filter { it.id !in excluded && it.type.valueInputs.isNotEmpty() }
                .flatMap { owner ->
                    owner.type.valueInputs.mapNotNull { spec ->
                        if (moving.type.outputType != spec.acceptedType) return@mapNotNull null
                        val (offsetX, offsetY) = valueSocketOffset(owner, spec.key)
                        val snapX = owner.xDp + offsetX
                        val snapY = owner.yDp + offsetY
                        val dx = abs(moving.xDp - snapX)
                        val dy = abs(moving.yDp - snapY)
                        if (dx <= 86f && dy <= 44f) Triple(owner, spec, dx + dy) else null
                    }
                }
                .minByOrNull { it.third }

            if (target != null) {
                val owner = target.first
                val spec = target.second

                // One value per input. If a socket is already occupied, move
                // the old value next to its owner instead of silently deleting it.
                val occupiedIndex = blocks.indexOfFirst {
                    it.id != moving.id && it.valueOwnerId == owner.id && it.valueInputKey == spec.key
                }
                if (occupiedIndex >= 0) {
                    val old = blocks[occupiedIndex]
                    val outsideX = owner.xDp + if (owner.type.role == BlockRole.CONTAINER) ContainerWidthDp + 24f else CommandWidthDp + 24f
                    blocks[occupiedIndex] = old.copy(
                        valueOwnerId = null,
                        valueInputKey = null,
                        xDp = outsideX,
                        yDp = owner.yDp
                    )
                }

                index = blocks.indexOfFirst { it.id == moving.id }
                if (index >= 0) {
                    blocks[index] = blocks[index].copy(
                        valueOwnerId = owner.id,
                        valueInputKey = spec.key,
                        parentId = null,
                        previousId = null,
                        childOrder = 0
                    )
                }
                applyNormalized(save = true)
                return
            }

            applyNormalized(save = true)
            return
        }

        val movingCenterX = moving.xDp + when (moving.type.role) {
            BlockRole.CONTAINER -> ContainerWidthDp / 2f
            BlockRole.VALUE -> ValueWidthDp / 2f
            BlockRole.COMMAND -> CommandWidthDp / 2f
        }
        val movingCenterY = moving.yDp + blockHeightDp(moving, blocks) / 2f

        // First preference for statement/control blocks: a statement input.
        val containerTarget = blocks.filter {
            it.id !in excluded && it.type.role == BlockRole.CONTAINER
        }.filter { target ->
            val targetHeight = blockHeightDp(target, blocks)
            val zoneBottom = target.yDp + maxOf(targetHeight - 10f, 130f)
            movingCenterX in (target.xDp + 18f)..(target.xDp + ContainerWidthDp) &&
                movingCenterY in (target.yDp + ContainerHeaderDp - 8f)..zoneBottom
        }.minByOrNull { abs(moving.yDp - (it.yDp + ContainerHeaderDp)) }

        if (containerTarget != null) {
            val chain = chainFrom(blocks.toList(), moving.id)
            val existing = blocks.count { it.parentId == containerTarget.id }
            chain.forEachIndexed { orderOffset, item ->
                val itemIndex = blocks.indexOfFirst { it.id == item.id }
                if (itemIndex >= 0) {
                    blocks[itemIndex] = blocks[itemIndex].copy(
                        parentId = containerTarget.id,
                        previousId = null,
                        valueOwnerId = null,
                        valueInputKey = null,
                        childOrder = existing + orderOffset
                    )
                }
            }
            applyNormalized(save = true)
            return
        }

        // Otherwise snap compatible statements underneath another top-level statement.
        val statementTarget = blocks.filter {
            it.id !in excluded && it.parentId == null && it.valueOwnerId == null && it.type.role != BlockRole.VALUE
        }.map { candidate ->
            val snapY = candidate.yDp + blockHeightDp(candidate, blocks) - ConnectorOverlapDp
            Triple(candidate, abs(moving.xDp - candidate.xDp), abs(moving.yDp - snapY))
        }.filter { (_, dx, dy) -> dx <= 74f && dy <= 34f }
            .minByOrNull { (_, dx, dy) -> dx + dy }
            ?.first

        if (statementTarget != null) {
            val chain = chainFrom(blocks.toList(), moving.id)
            val tailId = chain.lastOrNull()?.id ?: moving.id
            val oldFollowerIndex = blocks.indexOfFirst {
                it.parentId == null && it.valueOwnerId == null && it.previousId == statementTarget.id && it.id !in excluded
            }
            if (oldFollowerIndex >= 0) {
                blocks[oldFollowerIndex] = blocks[oldFollowerIndex].copy(previousId = tailId)
            }
            index = blocks.indexOfFirst { it.id == id }
            if (index >= 0) {
                blocks[index] = blocks[index].copy(
                    previousId = statementTarget.id,
                    parentId = null,
                    valueOwnerId = null,
                    valueInputKey = null,
                    xDp = statementTarget.xDp,
                    yDp = statementTarget.yDp + blockHeightDp(statementTarget, blocks) - ConnectorOverlapDp
                )
            }
        }

        applyNormalized(save = true)
    }

    fun deleteBlock(id: String) {
        val block = blocks.firstOrNull { it.id == id } ?: return
        val previous = block.previousId
        blocks.indices.forEach { index ->
            val item = blocks[index]
            when {
                item.parentId == id -> blocks[index] = item.copy(parentId = null, childOrder = 0)
                item.previousId == id -> blocks[index] = item.copy(previousId = previous)
                item.valueOwnerId == id -> {
                    val outsideX = block.xDp + if (block.type.role == BlockRole.CONTAINER) ContainerWidthDp + 24f else CommandWidthDp + 24f
                    blocks[index] = item.copy(
                        valueOwnerId = null,
                        valueInputKey = null,
                        xDp = outsideX,
                        yDp = block.yDp
                    )
                }
            }
        }
        blocks.removeAll { it.id == id }
        applyNormalized(save = true)
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
                            "UNO R4 WiFi • Alpha 7 • lokal",
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
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                onMoveStart = ::detachForDrag,
                onMove = ::moveWithConnections,
                onMoveFinished = ::finishMove,
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
                deleteBlock(block.id)
                editingBlock = null
            }
        )
    }

    if (showCode) {
        CodeSheet(blocks = blocks, onDismiss = { showCode = false })
    }
}

@Composable
private fun StatusStrip(blockCount: Int, issues: List<ValidationIssue>) {
    val errors = issues.count { it.level == IssueLevel.ERROR }
    val warnings = issues.count { it.level == IssueLevel.WARNING }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, label = { Text("$blockCount Blöcke") })
            when {
                errors > 0 -> Text("$errors Fehler", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                warnings > 0 -> Text("$warnings Hinweise", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
                else -> Text("Prüfung OK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
                .fillMaxHeight(0.84f)
                .padding(horizontal = 18.dp)
        ) {
            Text("Blöcke auswählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "Form = grammatische Rolle. Farbe = Funktionsgruppe. Nur passende Anschlüsse rasten zusammen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BlockRole.entries.toList()) { role ->
                    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
                        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                            Text(role.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                when (role) {
                                    BlockRole.COMMAND -> "oben/unten stapelbar"
                                    BlockRole.VALUE -> "passt in Werteingänge"
                                    BlockRole.CONTAINER -> "nimmt Befehle auf"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BlockCategory.entries.toList()) { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.title) })
                }
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                items(BlockType.entries.filter { it.category == category }) { type ->
                    Surface(
                        onClick = { onAdd(type) },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = blockColor(type),
                                modifier = Modifier.width(54.dp).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        when (type.role) {
                                            BlockRole.COMMAND -> "↕"
                                            BlockRole.VALUE -> "()"
                                            BlockRole.CONTAINER -> "C"
                                        },
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(type.title, fontWeight = FontWeight.SemiBold)
                                    AssistChip(onClick = {}, label = { Text(type.role.title) })
                                }
                                Text(type.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(block.type.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(block.type.role.title) })
            }
            Text(block.type.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(block.type.role.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (block.type) {
                BlockType.DELAY -> {
                    NumberEditor("Standard-Wartezeit (ms)", primaryText, { primaryText = it }, 0, 600_000)
                    QuickValues(listOf(100, 500, 1000, 2000)) { primaryText = it.toString() }
                    Text("Der Standardwert wird benutzt, solange kein Zahlenwert in der runden Öffnung steckt.", style = MaterialTheme.typography.bodySmall)
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
                    Text("Dieser runde Zahlenblock kann in den runden Werteingang von „Warten“ oder „Wiederholen“ gezogen werden.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.REPEAT -> {
                    NumberEditor("Standard-Anzahl", primaryText, { primaryText = it }, 1, 1000)
                    QuickValues(listOf(2, 5, 10, 20)) { primaryText = it.toString() }
                    Text("Die runde Öffnung nimmt Zahlenwerte auf. Befehle gehören in die C-förmige Mitte.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.IF_DIGITAL -> {
                    NumberEditor("Eingang D0–D13", primaryText, { primaryText = it }, 0, 13)
                    Text("Bedingung", style = MaterialTheme.typography.labelLarge)
                    HighLowSelector(flag = flag, onChange = { flag = it })
                    Text("Befehle in der C-förmigen Mitte werden nur ausgeführt, wenn die Bedingung stimmt.", style = MaterialTheme.typography.bodySmall)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            onValueChange = { text -> if (text.isEmpty() || text.all { it.isDigit() }) onValueChange(text) },
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
        items(values) { value -> FilledTonalButton(onClick = { onValue(value) }) { Text(value.toString()) } }
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
                    color = when (issue.level) {
                        IssueLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
                        IssueLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                        IssueLevel.INFO -> MaterialTheme.colorScheme.secondaryContainer
                    },
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

            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
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
