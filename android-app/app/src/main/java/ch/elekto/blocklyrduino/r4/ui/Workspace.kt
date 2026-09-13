package ch.elekto.blocklyrduino.r4.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.elekto.blocklyrduino.r4.model.BlockRole
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ContainerHeaderDp
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.SnapTarget
import ch.elekto.blocklyrduino.r4.model.ValueHeightDp
import ch.elekto.blocklyrduino.r4.model.ValueType
import ch.elekto.blocklyrduino.r4.model.blockHeightDp
import ch.elekto.blocklyrduino.r4.model.blockWidthDp
import ch.elekto.blocklyrduino.r4.model.connectedValue
import ch.elekto.blocklyrduino.r4.model.directChildren
import ch.elekto.blocklyrduino.r4.model.findSnapTarget
import ch.elekto.blocklyrduino.r4.model.valueSocketOffset
import ch.elekto.blocklyrduino.r4.model.valueSocketWidthDp
import kotlin.math.roundToInt

private val WorkspaceWidth = 1400.dp
private val WorkspaceHeight = 2200.dp

private fun statementShape(widthDp: Float): Shape = GenericShape { size, _ ->
    val sx = size.width / widthDp
    val sy = size.height / 56f
    fun x(dp: Float) = dp * sx
    fun y(dp: Float) = dp * sy

    val corner = y(7f)
    val depth = y(8f)
    val notchStart = x(34f)
    val notchEnd = x(64f)
    val bottom = size.height - depth

    moveTo(corner, 0f)
    lineTo(notchStart, 0f)
    quadraticTo(notchStart + x(3f), 0f, notchStart + x(5f), depth)
    lineTo(notchEnd - x(5f), depth)
    quadraticTo(notchEnd - x(3f), 0f, notchEnd, 0f)
    lineTo(size.width - corner, 0f)
    quadraticTo(size.width, 0f, size.width, corner)
    lineTo(size.width, bottom - corner)
    quadraticTo(size.width, bottom, size.width - corner, bottom)
    lineTo(notchEnd, bottom)
    quadraticTo(notchEnd - x(3f), bottom, notchEnd - x(5f), size.height)
    lineTo(notchStart + x(5f), size.height)
    quadraticTo(notchStart + x(3f), bottom, notchStart, bottom)
    lineTo(corner, bottom)
    quadraticTo(0f, bottom, 0f, bottom - corner)
    lineTo(0f, corner)
    quadraticTo(0f, 0f, corner, 0f)
    close()
}

private val NumberValueShape = GenericShape { size, _ ->
    val r = size.height / 2f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width, r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

private val BooleanValueShape = GenericShape { size, _ ->
    val point = (size.height / 2f).coerceAtMost(size.width * 0.16f)
    moveTo(point, 0f)
    lineTo(size.width - point, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width - point, size.height)
    lineTo(point, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

private fun containerShape(widthDp: Float, heightDp: Float): Shape = GenericShape { size, _ ->
    val sx = size.width / widthDp
    val sy = size.height / heightDp
    fun x(dp: Float) = dp * sx
    fun y(dp: Float) = dp * sy

    val cornerX = x(7f)
    val cornerY = y(7f)
    val depthY = y(8f)
    val notchStart = x(34f)
    val notchEnd = x(64f)
    val rail = x(34f)
    val header = y(54f)
    val footer = y(18f)
    val bodyBottom = size.height - footer
    val outerBottom = size.height - depthY
    val statementTabStart = rail + x(34f)
    val statementTabEnd = rail + x(64f)

    moveTo(cornerX, 0f)
    lineTo(notchStart, 0f)
    quadraticTo(notchStart + x(3f), 0f, notchStart + x(5f), depthY)
    lineTo(notchEnd - x(5f), depthY)
    quadraticTo(notchEnd - x(3f), 0f, notchEnd, 0f)
    lineTo(size.width - cornerX, 0f)
    quadraticTo(size.width, 0f, size.width, cornerY)
    lineTo(size.width, header - cornerY)
    quadraticTo(size.width, header, size.width - cornerX, header)

    lineTo(statementTabEnd, header)
    quadraticTo(statementTabEnd - x(3f), header, statementTabEnd - x(5f), header + depthY)
    lineTo(statementTabStart + x(5f), header + depthY)
    quadraticTo(statementTabStart + x(3f), header, statementTabStart, header)
    lineTo(rail, header)
    lineTo(rail, bodyBottom)

    lineTo(size.width - cornerX, bodyBottom)
    quadraticTo(size.width, bodyBottom, size.width, bodyBottom + cornerY)
    lineTo(size.width, outerBottom - cornerY)
    quadraticTo(size.width, outerBottom, size.width - cornerX, outerBottom)
    lineTo(notchEnd, outerBottom)
    quadraticTo(notchEnd - x(3f), outerBottom, notchEnd - x(5f), size.height)
    lineTo(notchStart + x(5f), size.height)
    quadraticTo(notchStart + x(3f), outerBottom, notchStart, outerBottom)
    lineTo(cornerX, outerBottom)
    quadraticTo(0f, outerBottom, 0f, outerBottom - cornerY)
    lineTo(0f, cornerY)
    quadraticTo(0f, 0f, cornerX, 0f)
    close()
}

private fun blockShape(block: ProgramBlock, blocks: List<ProgramBlock>): Shape {
    val width = blockWidthDp(block, blocks)
    val height = blockHeightDp(block, blocks)
    return when (block.type.role) {
        BlockRole.COMMAND -> statementShape(width)
        BlockRole.CONTAINER -> containerShape(width, height)
        BlockRole.VALUE -> when (block.type.outputType) {
            ValueType.BOOLEAN -> BooleanValueShape
            else -> NumberValueShape
        }
    }
}

@Composable
fun BlockWorkspace(
    blocks: List<ProgramBlock>,
    errorBlockIds: Set<String>,
    draggingBlockId: String?,
    selectionMode: Boolean,
    selectedBlockIds: Set<String>,
    onToggleSelection: (id: String) -> Unit,
    onMoveStart: (id: String) -> Unit,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: (ProgramBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    var zoom by rememberSaveable { mutableStateOf(0.78f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontal)
                .verticalScroll(vertical)
        ) {
            Box(modifier = Modifier.size(WorkspaceWidth * zoom, WorkspaceHeight * zoom)) {
                Box(
                    modifier = Modifier
                        .size(WorkspaceWidth, WorkspaceHeight)
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            transformOrigin = TransformOrigin(0f, 0f)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    WorkspaceGrid()

                    if (blocks.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.TopStart).padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Noch keine Blöcke", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tippe auf „Blöcke“, um den ersten Baustein einzufügen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    val activeSnapTarget = draggingBlockId?.let { findSnapTarget(blocks, it) }

                    blocks.sortedBy { nestingDepth(it, blocks) }.forEach { block ->
                        NativeProgramBlock(
                            block = block,
                            allBlocks = blocks,
                            hasError = block.id in errorBlockIds,
                            isDragging = block.id == draggingBlockId,
                            hasActiveSnapTarget = block.id == draggingBlockId && activeSnapTarget != null,
                            isSelected = block.id in selectedBlockIds,
                            selectionMode = selectionMode,
                            zoom = zoom,
                            onToggleSelection = onToggleSelection,
                            onMoveStart = onMoveStart,
                            onMove = onMove,
                            onMoveFinished = onMoveFinished,
                            onEdit = { onEdit(block) }
                        )
                    }

                    SnapPreviewOverlay(blocks = blocks, draggingBlockId = draggingBlockId)
                }
            }
        }

        ZoomControls(
            zoom = zoom,
            onZoomOut = { zoom = (zoom - 0.10f).coerceAtLeast(0.50f) },
            onZoomIn = { zoom = (zoom + 0.10f).coerceAtMost(1.30f) },
            onReset = { zoom = 0.78f },
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
    }
}

@Composable
private fun SnapPreviewOverlay(blocks: List<ProgramBlock>, draggingBlockId: String?) {
    val moving = draggingBlockId?.let { id -> blocks.firstOrNull { it.id == id } } ?: return
    val target = findSnapTarget(blocks, moving.id) ?: return
    val width = blockWidthDp(moving, blocks).dp
    val height = blockHeightDp(moving, blocks).dp
    val shape = blockShape(moving, blocks)
    val density = LocalDensity.current.density

    Surface(
        modifier = Modifier
            .offset {
                IntOffset((target.xDp * density).roundToInt(), (target.yDp * density).roundToInt())
            }
            .width(width)
            .height(height)
            .border(4.dp, Color(0xFF65B5FF), shape),
        shape = shape,
        color = Color(0xFF65B5FF).copy(alpha = 0.22f),
        shadowElevation = 0.dp
    ) {}
}

@Composable
private fun ZoomControls(
    zoom: Float,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), tonalElevation = 5.dp, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onZoomOut, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text("−", fontSize = 20.sp)
            }
            Text(
                "${(zoom * 100).roundToInt()} %",
                modifier = Modifier.clickable(onClick = onReset).padding(horizontal = 5.dp),
                fontWeight = FontWeight.SemiBold
            )
            FilledTonalButton(onClick = onZoomIn, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text("+", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun WorkspaceGrid() {
    val dotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 32.dp.toPx()
        val radius = 1.25.dp.toPx()
        var x = spacing
        while (x < size.width) {
            var y = spacing
            while (y < size.height) {
                drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun NativeProgramBlock(
    block: ProgramBlock,
    allBlocks: List<ProgramBlock>,
    hasError: Boolean,
    isDragging: Boolean,
    hasActiveSnapTarget: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    zoom: Float,
    onToggleSelection: (id: String) -> Unit,
    onMoveStart: (id: String) -> Unit,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current.density
    val widthDp = blockWidthDp(block, allBlocks).dp
    val heightDp = blockHeightDp(block, allBlocks).dp
    val shape = blockShape(block, allBlocks)
    val childCount = directChildren(allBlocks, block.id).size

    Box(
        modifier = Modifier
            .offset {
                IntOffset((block.xDp * density).roundToInt(), (block.yDp * density).roundToInt())
            }
            .width(widthDp)
            .height(heightDp)
            .graphicsLayer { alpha = if (isDragging && hasActiveSnapTarget) 0.52f else 1f }
            .pointerInput(block.id, zoom) {
                detectDragGestures(
                    onDragStart = { onMoveStart(block.id) },
                    onDragEnd = { onMoveFinished(block.id) },
                    onDragCancel = { onMoveFinished(block.id) }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(block.id, dragAmount.x / (density * zoom), dragAmount.y / (density * zoom))
                }
            }
            .clickable { if (selectionMode) onToggleSelection(block.id) else onEdit() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isSelected) Modifier.border(4.dp, Color(0xFF65B5FF), shape) else Modifier)
                .then(if (hasError) Modifier.border(3.dp, MaterialTheme.colorScheme.error, shape) else Modifier),
            shape = shape,
            color = blockColor(block.type),
            shadowElevation = 3.dp
        ) {
            when (block.type.role) {
                BlockRole.CONTAINER -> ContainerHeader(block, allBlocks)
                BlockRole.VALUE -> ValueBlockContent(block, allBlocks)
                BlockRole.COMMAND -> CommandBlockContent(block, allBlocks)
            }
        }

        if (block.type.role == BlockRole.CONTAINER && childCount == 0) {
            Text(
                text = "Befehle hier einrasten",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .offset(x = 54.dp, y = (ContainerHeaderDp + 16f).dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun CommandBlockContent(block: ProgramBlock, blocks: List<ProgramBlock>) {
    if (block.type == BlockType.DELAY) {
        val (socketX, socketY) = valueSocketOffset(block, "duration", blocks)
        val socketWidth = valueSocketWidthDp(block, "duration", blocks)
        Box(Modifier.fillMaxSize()) {
            Text(
                "Warten",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.offset(x = 16.dp, y = 17.dp)
            )
            ValueSocket(
                type = ValueType.NUMBER,
                widthDp = socketWidth,
                fallback = "${block.primary} ms",
                connected = connectedValue(blocks, block.id, "duration") != null,
                modifier = Modifier.offset(x = socketX.dp, y = socketY.dp)
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp, top = 7.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(block.type.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(7.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.20f)) {
            Text(
                blockValueLabel(block),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ValueBlockContent(block: ProgramBlock, blocks: List<ProgramBlock>) {
    if (block.type == BlockType.COMPARE_NUMBER) {
        val (leftX, leftY) = valueSocketOffset(block, "left", blocks)
        val (rightX, rightY) = valueSocketOffset(block, "right", blocks)
        val leftWidth = valueSocketWidthDp(block, "left", blocks)
        val rightWidth = valueSocketWidthDp(block, "right", blocks)
        val operatorX = leftX + leftWidth + 10f
        Box(Modifier.fillMaxSize()) {
            ValueSocket(
                type = ValueType.NUMBER,
                widthDp = leftWidth,
                fallback = block.primary.toString(),
                connected = connectedValue(blocks, block.id, "left") != null,
                modifier = Modifier.offset(x = leftX.dp, y = leftY.dp)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.offset(x = operatorX.dp, y = 5.dp).width(48.dp).height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(operatorLabel(block.option), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
            ValueSocket(
                type = ValueType.NUMBER,
                widthDp = rightWidth,
                fallback = block.secondary.toString(),
                connected = connectedValue(blocks, block.id, "right") != null,
                modifier = Modifier.offset(x = rightX.dp, y = rightY.dp)
            )
        }
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = when (block.type) {
                BlockType.ANALOG_READ -> "Analog A${block.primary}"
                BlockType.NUMBER_LITERAL -> block.primary.toString()
                BlockType.DIGITAL_READ_BOOL -> "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}"
                else -> blockValueLabel(block)
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ContainerHeader(block: ProgramBlock, blocks: List<ProgramBlock>) {
    Box(modifier = Modifier.width(blockWidthDp(block, blocks).dp).height(ContainerHeaderDp.dp)) {
        when (block.type) {
            BlockType.REPEAT -> {
                val (socketX, socketY) = valueSocketOffset(block, "count", blocks)
                Text("Wiederhole", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.offset(x = 16.dp, y = 17.dp))
                ValueSocket(
                    type = ValueType.NUMBER,
                    widthDp = valueSocketWidthDp(block, "count", blocks),
                    fallback = block.primary.toString(),
                    connected = connectedValue(blocks, block.id, "count") != null,
                    modifier = Modifier.offset(x = socketX.dp, y = socketY.dp)
                )
                Text("mal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.offset(x = (blockWidthDp(block, blocks) - 38f).dp, y = 18.dp))
            }
            BlockType.IF_DIGITAL -> {
                val (socketX, socketY) = valueSocketOffset(block, "condition", blocks)
                Text("Wenn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.offset(x = 16.dp, y = 17.dp))
                ValueSocket(
                    type = ValueType.BOOLEAN,
                    widthDp = valueSocketWidthDp(block, "condition", blocks),
                    fallback = "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}",
                    connected = connectedValue(blocks, block.id, "condition") != null,
                    modifier = Modifier.offset(x = socketX.dp, y = socketY.dp)
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun ValueSocket(
    type: ValueType,
    widthDp: Float,
    fallback: String,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = when (type) {
        ValueType.BOOLEAN -> BooleanValueShape
        else -> NumberValueShape
    }
    if (connected) {
        Box(modifier = modifier.width(widthDp.dp).height(ValueHeightDp.dp))
        return
    }
    Surface(
        modifier = modifier.width(widthDp.dp).height(ValueHeightDp.dp)
            .border(1.dp, Color.White.copy(alpha = 0.42f), shape),
        shape = shape,
        color = Color.Black.copy(alpha = 0.18f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(fallback, color = Color.White.copy(alpha = 0.94f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

private fun nestingDepth(block: ProgramBlock, blocks: List<ProgramBlock>): Int {
    val byId = blocks.associateBy { it.id }
    var depth = 0
    var current = block.valueOwnerId ?: block.parentId
    val seen = mutableSetOf<String>()
    while (current != null && seen.add(current)) {
        depth++
        val parent = byId[current]
        current = parent?.valueOwnerId ?: parent?.parentId
    }
    return depth
}

@Composable
fun BlockTypePreview(type: BlockType, modifier: Modifier = Modifier) {
    val sample = ProgramBlock(type = type, xDp = 0f, yDp = 0f)
    val empty = listOf(sample)

    val previewWidth = when (type.role) {
        BlockRole.VALUE -> if (type.outputType == ValueType.BOOLEAN) 132.dp else 112.dp
        BlockRole.COMMAND -> 150.dp
        BlockRole.CONTAINER -> 156.dp
    }
    val previewHeight = when (type.role) {
        BlockRole.CONTAINER -> 88.dp
        else -> 48.dp
    }

    val shape: Shape = when (type.role) {
        BlockRole.COMMAND -> statementShape(150f)
        BlockRole.VALUE -> if (type.outputType == ValueType.BOOLEAN) BooleanValueShape else NumberValueShape
        BlockRole.CONTAINER -> containerShape(156f, 88f)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(previewWidth).height(previewHeight),
            shape = shape,
            color = blockColor(type),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = if (type.role == BlockRole.CONTAINER) Alignment.TopStart else Alignment.Center) {
                if (type.role == BlockRole.CONTAINER) {
                    Box(
                        modifier = Modifier
                            .offset(x = 34.dp, y = 44.dp)
                            .width(108.dp)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(5.dp))
                    )
                }
                Text(
                    text = when (type) {
                        BlockType.DELAY -> "Warten"
                        BlockType.DIGITAL_WRITE -> "Digitaler"
                        BlockType.PWM_WRITE -> "PWM"
                        BlockType.ANALOG_READ -> "Analog A0"
                        BlockType.NUMBER_LITERAL -> "1000"
                        BlockType.DIGITAL_READ_BOOL -> "D2 = HIGH"
                        BlockType.COMPARE_NUMBER -> "100 > 50"
                        BlockType.REPEAT -> "Wiederhole 10×"
                        BlockType.IF_DIGITAL -> "Wenn …"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (type.role == BlockRole.CONTAINER) 10.sp else 11.sp,
                    maxLines = 1,
                    modifier = if (type.role == BlockRole.CONTAINER) Modifier.padding(start = 10.dp, top = 9.dp) else Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

fun operatorLabel(option: String): String = when (option) {
    "EQ" -> "="
    "NE" -> "≠"
    "LT" -> "<"
    "LTE" -> "≤"
    "GT" -> ">"
    "GTE" -> "≥"
    else -> ">"
}

fun blockValueLabel(block: ProgramBlock): String = when (block.type) {
    BlockType.DELAY -> "${block.primary} ms"
    BlockType.DIGITAL_WRITE -> "D${block.primary} • ${if (block.flag) "HIGH" else "LOW"}"
    BlockType.PWM_WRITE -> "D${block.primary} • ${block.secondary}"
    BlockType.ANALOG_READ -> "A${block.primary}"
    BlockType.NUMBER_LITERAL -> block.primary.toString()
    BlockType.DIGITAL_READ_BOOL -> "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}"
    BlockType.COMPARE_NUMBER -> operatorLabel(block.option)
    BlockType.REPEAT -> "${block.primary}×"
    BlockType.IF_DIGITAL -> "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}"
}

fun blockColor(type: BlockType): Color = when (type) {
    BlockType.DELAY -> Color(0xFF6C55C7)
    BlockType.DIGITAL_WRITE -> Color(0xFF16865C)
    BlockType.PWM_WRITE -> Color(0xFF007A8A)
    BlockType.ANALOG_READ, BlockType.NUMBER_LITERAL -> Color(0xFF2D6BC4)
    BlockType.DIGITAL_READ_BOOL, BlockType.COMPARE_NUMBER -> Color(0xFFC25235)
    BlockType.REPEAT -> Color(0xFFD89A00)
    BlockType.IF_DIGITAL -> Color(0xFFB85B16)
}
