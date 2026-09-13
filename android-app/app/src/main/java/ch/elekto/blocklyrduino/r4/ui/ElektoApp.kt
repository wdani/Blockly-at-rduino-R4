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
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.SnapTarget
import ch.elekto.blocklyrduino.r4.model.ValueType
import ch.elekto.blocklyrduino.r4.model.blockHeightDp
import ch.elekto.blocklyrduino.r4.model.blockWidthDp
import ch.elekto.blocklyrduino.r4.model.chainFrom
import ch.elekto.blocklyrduino.r4.model.defaultBlinkProject
import ch.elekto.blocklyrduino.r4.model.directChildren
import ch.elekto.blocklyrduino.r4.model.findSnapTarget
import ch.elekto.blocklyrduino.r4.model.linkedDescendantIds
import ch.elekto.blocklyrduino.r4.model.normalizeProjectLayout
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
    var selectionMode by remember { mutableStateOf(false) }
    var selectedBlockIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draggingBlockId by remember { mutableStateOf<String?>(null) }

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

    fun toggleSelection(id: String) {
        selectedBlockIds = if (id in selectedBlockIds) selectedBlockIds - id else selectedBlockIds + id
    }

    fun selectionMoveRoots(ids: Set<String>): Set<String> {
        val byId = blocks.associateBy { it.id }
        fun hasSelectedAncestor(block: ProgramBlock): Boolean {
            var current = block.valueOwnerId ?: block.parentId ?: block.previousId
            val seen = mutableSetOf<String>()
            while (current != null && seen.add(current)) {
                if (current in ids) return true
                val parent = byId[current]
                current = parent?.valueOwnerId ?: parent?.parentId ?: parent?.previousId
            }
            return false
        }
        return ids.filterTo(linkedSetOf()) { id ->
            blocks.firstOrNull { it.id == id }?.let { !hasSelectedAncestor(it) } == true
        }
    }

    fun detachSelectionForDrag(ids: Set<String>) {
        val roots = selectionMoveRoots(ids)
        blocks.indices.forEach { index ->
            val block = blocks[index]
            if (block.id !in roots) return@forEach
            blocks[index] = block.copy(
                parentId = block.parentId?.takeIf { it in ids },
                previousId = block.previousId?.takeIf { it in ids },
                valueOwnerId = block.valueOwnerId?.takeIf { it in ids },
                valueInputKey = block.valueInputKey?.takeIf { block.valueOwnerId in ids },
                childOrder = if (block.parentId in ids) block.childOrder else 0
            )
        }
    }

    fun detachForDrag(id: String) {
        if (selectionMode && id in selectedBlockIds && selectedBlockIds.size > 1) {
            detachSelectionForDrag(selectedBlockIds)
            return
        }
        val index = blocks.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = blocks[index]
        if (current.parentId != null || current.previousId != null || current.valueOwnerId != null) {
            blocks[index] = current.copy(parentId = null, previousId = null, childOrder = 0, valueOwnerId = null, valueInputKey = null)
        }
    }

    fun moveSelection(ids: Set<String>, dx: Float, dy: Float) {
        val roots = selectionMoveRoots(ids)
        val movingIds = linkedSetOf<String>()
        roots.forEach { root ->
            movingIds += root
            movingIds += linkedDescendantIds(blocks.toList(), root)
        }
        blocks.indices.forEach { index ->
            val block = blocks[index]
            if (block.id in movingIds) {
                blocks[index] = block.copy(
                    xDp = (block.xDp + dx).coerceIn(8f, 1100f),
                    yDp = (block.yDp + dy).coerceIn(8f, 2070f)
                )
            }
        }
    }

    fun finishSelectionMove(ids: Set<String>) {
        val roots = selectionMoveRoots(ids)
        val movingIds = linkedSetOf<String>()
        roots.forEach { root ->
            movingIds += root
            movingIds += linkedDescendantIds(blocks.toList(), root)
        }
        blocks.indices.forEach { index ->
            val block = blocks[index]
            if (block.id in movingIds) {
                blocks[index] = block.copy(
                    xDp = round(block.xDp / 8f) * 8f,
                    yDp = round(block.yDp / 8f) * 8f
                )
            }
        }
        persist()
    }

    fun moveWithConnections(id: String, dx: Float, dy: Float) {
        if (selectionMode && id in selectedBlockIds && selectedBlockIds.size > 1) {
            moveSelection(selectedBlockIds, dx, dy)
            return
        }
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
        if (selectionMode && id in selectedBlockIds && selectedBlockIds.size > 1) {
            draggingBlockId = null
            finishSelectionMove(selectedBlockIds)
            return
        }
        var index = blocks.indexOfFirst { it.id == id }
        if (index < 0) {
            draggingBlockId = null
            return
        }

        val current = blocks[index]
        blocks[index] = current.copy(
            xDp = round(current.xDp / 8f) * 8f,
            yDp = round(current.yDp / 8f) * 8f,
            parentId = null,
            previousId = null,
            childOrder = 0,
            valueOwnerId = null,
            valueInputKey = null
        )

        val target = findSnapTarget(blocks.toList(), id)
        val moving = blocks.firstOrNull { it.id == id }
        if (moving == null) {
            draggingBlockId = null
            return
        }

        when (target) {
            is SnapTarget.Value -> {
                val owner = blocks.firstOrNull { it.id == target.ownerId }
                if (owner != null) {
                    val occupiedIndex = blocks.indexOfFirst {
                        it.id != moving.id && it.valueOwnerId == owner.id && it.valueInputKey == target.inputKey
                    }
                    if (occupiedIndex >= 0) {
                        val old = blocks[occupiedIndex]
                        blocks[occupiedIndex] = old.copy(
                            valueOwnerId = null,
                            valueInputKey = null,
                            xDp = owner.xDp + blockWidthDp(owner, blocks) + 24f,
                            yDp = owner.yDp
                        )
                    }
                    index = blocks.indexOfFirst { it.id == moving.id }
                    if (index >= 0) {
                        blocks[index] = blocks[index].copy(
                            valueOwnerId = owner.id,
                            valueInputKey = target.inputKey,
                            parentId = null,
                            previousId = null,
                            childOrder = 0
                        )
                    }
                }
            }
            is SnapTarget.Container -> {
                val chain = chainFrom(blocks.toList(), moving.id)
                val chainIds = chain.map { it.id }.toSet()
                blocks.indices.forEach { childIndex ->
                    val child = blocks[childIndex]
                    if (child.parentId == target.parentId && child.id !in chainIds && child.childOrder >= target.insertIndex) {
                        blocks[childIndex] = child.copy(childOrder = child.childOrder + chain.size)
                    }
                }
                chain.forEachIndexed { offset, item ->
                    val itemIndex = blocks.indexOfFirst { it.id == item.id }
                    if (itemIndex >= 0) {
                        blocks[itemIndex] = blocks[itemIndex].copy(
                            parentId = target.parentId,
                            previousId = null,
                            valueOwnerId = null,
                            valueInputKey = null,
                            childOrder = target.insertIndex + offset
                        )
                    }
                }
            }
            is SnapTarget.Statement -> {
                val chain = chainFrom(blocks.toList(), moving.id)
                val tailId = chain.lastOrNull()?.id ?: moving.id
                val oldFollowerIndex = blocks.indexOfFirst {
                    it.parentId == null && it.valueOwnerId == null && it.previousId == target.previousId && it.id != moving.id
                }
                if (oldFollowerIndex >= 0) {
                    blocks[oldFollowerIndex] = blocks[oldFollowerIndex].copy(previousId = tailId)
                }
                index = blocks.indexOfFirst { it.id == id }
                if (index >= 0) {
                    blocks[index] = blocks[index].copy(
                        previousId = target.previousId,
                        parentId = null,
                        valueOwnerId = null,
                        valueInputKey = null,
                        xDp = target.xDp,
                        yDp = target.yDp
                    )
                }
            }
            null -> Unit
        }

        applyNormalized(save = true)
        draggingBlockId = null
    }

    fun deleteBlock(id: String) {
        val block = blocks.firstOrNull { it.id == id } ?: return
        val previous = block.previousId
        blocks.indices.forEach { index ->
            val item = blocks[index]
            when {
                item.parentId == id -> blocks[index] = item.copy(parentId = null, childOrder = 0)
                item.previousId == id -> blocks[index] = item.copy(previousId = previous)
                item.valueOwnerId == id -> blocks[index] = item.copy(
                    valueOwnerId = null,
                    valueInputKey = null,
                    xDp = block.xDp + blockWidthDp(block, blocks) + 24f,
                    yDp = block.yDp
                )
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
                            "UNO R4 WiFi • Alpha 9 • lokal",
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
                                text = { Text(if (selectionMode) "Mehrfachauswahl beenden" else "Mehrfachauswahl") },
                                onClick = {
                                    showMenu = false
                                    selectionMode = !selectionMode
                                    if (!selectionMode) selectedBlockIds = emptySet()
                                }
                            )
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
            StatusStrip(blockCount = blocks.size, issues = issues, selectionMode = selectionMode, selectionCount = selectedBlockIds.size, onFinishSelection = { selectionMode = false; selectedBlockIds = emptySet() })
            BlockWorkspace(
                blocks = blocks,
                errorBlockIds = errorBlockIds,
                draggingBlockId = draggingBlockId,
                selectionMode = selectionMode,
                selectedBlockIds = selectedBlockIds,
                onToggleSelection = ::toggleSelection,
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
private fun StatusStrip(blockCount: Int, issues: List<ValidationIssue>, selectionMode: Boolean, selectionCount: Int, onFinishSelection: () -> Unit) {
    val errors = issues.count { it.level == IssueLevel.ERROR }
    val warnings = issues.count { it.level == IssueLevel.WARNING }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, label = { Text("$blockCount Blöcke") })
            if (selectionMode) {
                Text("$selectionCount ausgewählt", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = onFinishSelection) { Text("Fertig") }
            } else when {
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
                "Form = Grammatik und Datentyp. Farbe = Funktionsgruppe. Nur passende Anschlüsse rasten zusammen.",
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
                                    BlockRole.VALUE -> "rund = Zahl, sechseckig = Wahr/Falsch"
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
                                            BlockRole.CONTAINER -> "C"
                                            BlockRole.VALUE -> if (type.outputType == ValueType.BOOLEAN) "◇" else "()"
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
    var option by remember(block.id) { mutableStateOf(block.option) }
    val primary = primaryText.toIntOrNull()
    val secondary = secondaryText.toIntOrNull()
    val valid = when (block.type) {
        BlockType.DELAY -> primary != null && primary in 0..600_000
        BlockType.DIGITAL_WRITE, BlockType.DIGITAL_READ_BOOL, BlockType.IF_DIGITAL -> primary != null && primary in 0..13
        BlockType.PWM_WRITE -> primary != null && primary in setOf(3, 5, 6, 9, 10, 11) && secondary != null && secondary in 0..255
        BlockType.ANALOG_READ -> primary != null && primary in 0..5
        BlockType.NUMBER_LITERAL -> primary != null && primary in -1_000_000..1_000_000
        BlockType.COMPARE_NUMBER -> primary != null && secondary != null && primary in -1_000_000..1_000_000 && secondary in -1_000_000..1_000_000 && option in setOf("EQ", "NE", "LT", "LTE", "GT", "GTE")
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
                    Text("Der Standardwert wird benutzt, solange kein Zahlenblock in der runden Öffnung steckt.", style = MaterialTheme.typography.bodySmall)
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
                    Text("Runde Form: Dieser Block liefert eine Zahl.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.NUMBER_LITERAL -> {
                    NumberEditor("Zahl", primaryText, { primaryText = it }, -1_000_000, 1_000_000)
                    QuickValues(listOf(0, 1, 10, 100, 500, 1000)) { primaryText = it.toString() }
                    Text("Runde Form: Die Zahl passt in jeden Zahlen-Eingang.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.DIGITAL_READ_BOOL -> {
                    NumberEditor("Digital-Pin D0–D13", primaryText, { primaryText = it }, 0, 13)
                    Text("Wann soll die Bedingung wahr sein?", style = MaterialTheme.typography.labelLarge)
                    HighLowSelector(flag = flag, onChange = { flag = it })
                    Text("Sechseckige Form: Dieser Block liefert Wahr oder Falsch.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.COMPARE_NUMBER -> {
                    NumberEditor("Standardwert links", primaryText, { primaryText = it }, -1_000_000, 1_000_000)
                    ComparatorSelector(option = option, onChange = { option = it })
                    NumberEditor("Standardwert rechts", secondaryText, { secondaryText = it }, -1_000_000, 1_000_000)
                    Text("In beide runden Öffnungen können Zahlenblöcke gesteckt werden. Das Ergebnis ist Wahr oder Falsch.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.REPEAT -> {
                    NumberEditor("Standard-Anzahl", primaryText, { primaryText = it }, 1, 1000)
                    QuickValues(listOf(2, 5, 10, 20)) { primaryText = it.toString() }
                    Text("Die runde Öffnung nimmt Zahlen auf. Befehle gehören in die C-förmige Mitte.", style = MaterialTheme.typography.bodySmall)
                }
                BlockType.IF_DIGITAL -> {
                    NumberEditor("Fallback-Eingang D0–D13", primaryText, { primaryText = it }, 0, 13)
                    Text("Fallback-Bedingung", style = MaterialTheme.typography.labelLarge)
                    HighLowSelector(flag = flag, onChange = { flag = it })
                    Text("Die sechseckige Öffnung erwartet einen Wahr/Falsch-Block. Solange dort nichts steckt, gilt diese Fallback-Bedingung.", style = MaterialTheme.typography.bodySmall)
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
                                flag = flag,
                                option = option
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
    val number = value.toIntOrNull() ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { text ->
                val accepted = text.isEmpty() || (min < 0 && text == "-") || text.toIntOrNull() != null
                if (accepted) onValueChange(text)
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (min < 0) KeyboardType.NumberPassword else KeyboardType.Number),
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

@Composable
private fun ComparatorSelector(option: String, onChange: (String) -> Unit) {
    val options = listOf(
        "EQ" to "=",
        "NE" to "≠",
        "LT" to "<",
        "LTE" to "≤",
        "GT" to ">",
        "GTE" to "≥"
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Vergleich", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { item ->
                FilterChip(
                    selected = option == item.first,
                    onClick = { onChange(item.first) },
                    label = { Text(item.second) }
                )
            }
        }
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
