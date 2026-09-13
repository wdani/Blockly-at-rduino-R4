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
import ch.elekto.blocklyrduino.r4.model.CommandWidthDp
import ch.elekto.blocklyrduino.r4.model.ContainerHeaderDp
import ch.elekto.blocklyrduino.r4.model.ContainerWidthDp
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.ValueType
import ch.elekto.blocklyrduino.r4.model.ValueWidthDp
import ch.elekto.blocklyrduino.r4.model.blockHeightDp
import ch.elekto.blocklyrduino.r4.model.connectedValue
import ch.elekto.blocklyrduino.r4.model.directChildren
import ch.elekto.blocklyrduino.r4.model.valueSocketOffset
import kotlin.math.roundToInt

private val WorkspaceWidth = 1400.dp
private val WorkspaceHeight = 2200.dp

/**
 * Statement grammar: an inward previous-connection socket at the top and a
 * matching outward next-connection tab at the bottom. The geometry, not the
 * colour, tells the learner that these blocks form a sequence.
 */
private val StatementShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val corner = h * 0.12f
    val depth = h * 0.14f
    val notchStart = w * 0.135f
    val notchEnd = w * 0.258f
    val bottom = h - depth

    moveTo(corner, 0f)
    lineTo(notchStart, 0f)
    quadraticTo(notchStart + depth * 0.35f, 0f, notchStart + depth * 0.55f, depth)
    lineTo(notchEnd - depth * 0.55f, depth)
    quadraticTo(notchEnd - depth * 0.35f, 0f, notchEnd, 0f)
    lineTo(w - corner, 0f)
    quadraticTo(w, 0f, w, corner)
    lineTo(w, bottom - corner)
    quadraticTo(w, bottom, w - corner, bottom)
    lineTo(notchEnd, bottom)
    quadraticTo(notchEnd - depth * 0.35f, bottom, notchEnd - depth * 0.55f, h)
    lineTo(notchStart + depth * 0.55f, h)
    quadraticTo(notchStart + depth * 0.35f, bottom, notchStart, bottom)
    lineTo(corner, bottom)
    quadraticTo(0f, bottom, 0f, bottom - corner)
    lineTo(0f, corner)
    quadraticTo(0f, 0f, corner, 0f)
    close()
}

/** Number values are capsules. Boolean values will use a hexagon later. */
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

/**
 * C-shaped statement container. The statement-input tab at the header edge is
 * aligned with the previous socket of the first child block.
 */
private val ContainerShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val corner = w * 0.025f
    val depth = w * 0.026f
    val outerNotchStart = w * 0.11f
    val outerNotchEnd = w * 0.21f
    val rail = w * 0.112f
    val header = w * 0.178f
    val footer = w * 0.059f
    val bodyBottom = h - footer
    val outerBottom = h - depth
    val statementTabStart = rail + w * 0.115f
    val statementTabEnd = rail + w * 0.215f

    moveTo(corner, 0f)
    lineTo(outerNotchStart, 0f)
    quadraticTo(outerNotchStart + depth * 0.35f, 0f, outerNotchStart + depth * 0.55f, depth)
    lineTo(outerNotchEnd - depth * 0.55f, depth)
    quadraticTo(outerNotchEnd - depth * 0.35f, 0f, outerNotchEnd, 0f)
    lineTo(w - corner, 0f)
    quadraticTo(w, 0f, w, corner)
    lineTo(w, header - corner)
    quadraticTo(w, header, w - corner, header)

    // Statement-input tab points down into the first child's top socket.
    lineTo(statementTabEnd, header)
    quadraticTo(statementTabEnd - depth * 0.35f, header, statementTabEnd - depth * 0.55f, header + depth)
    lineTo(statementTabStart + depth * 0.55f, header + depth)
    quadraticTo(statementTabStart + depth * 0.35f, header, statementTabStart, header)
    lineTo(rail, header)
    lineTo(rail, bodyBottom)

    lineTo(w - corner, bodyBottom)
    quadraticTo(w, bodyBottom, w, bodyBottom + corner)
    lineTo(w, outerBottom - corner)
    quadraticTo(w, outerBottom, w - corner, outerBottom)
    lineTo(outerNotchEnd, outerBottom)
    quadraticTo(outerNotchEnd - depth * 0.35f, outerBottom, outerNotchEnd - depth * 0.55f, h)
    lineTo(outerNotchStart + depth * 0.55f, h)
    quadraticTo(outerNotchStart + depth * 0.35f, outerBottom, outerNotchStart, outerBottom)
    lineTo(corner, outerBottom)
    quadraticTo(0f, outerBottom, 0f, outerBottom - corner)
    lineTo(0f, corner)
    quadraticTo(0f, 0f, corner, 0f)
    close()
}

@Composable
fun BlockWorkspace(
    blocks: List<ProgramBlock>,
    errorBlockIds: Set<String>,
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

                    blocks.sortedBy { nestingDepth(it, blocks) }.forEach { block ->
                        NativeProgramBlock(
                            block = block,
                            allBlocks = blocks,
                            hasError = block.id in errorBlockIds,
                            zoom = zoom,
                            onMoveStart = onMoveStart,
                            onMove = onMove,
                            onMoveFinished = onMoveFinished,
                            onEdit = { onEdit(block) }
                        )
                    }
                }
            }
        }

        // Bottom-start is intentionally reserved for workspace controls. The
        // "Blöcke" FAB stays bottom-end, so both can always be touched.
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
    zoom: Float,
    onMoveStart: (id: String) -> Unit,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current.density
    val heightDp = blockHeightDp(block, allBlocks).dp
    val widthDp = when (block.type.role) {
        BlockRole.COMMAND -> CommandWidthDp.dp
        BlockRole.VALUE -> ValueWidthDp.dp
        BlockRole.CONTAINER -> ContainerWidthDp.dp
    }
    val shape = when (block.type.role) {
        BlockRole.COMMAND -> StatementShape
        BlockRole.VALUE -> when (block.type.outputType) {
            ValueType.BOOLEAN -> NumberValueShape // reserved until the Boolean hexagon is introduced
            else -> NumberValueShape
        }
        BlockRole.CONTAINER -> ContainerShape
    }
    val childCount = directChildren(allBlocks, block.id).size

    Box(
        modifier = Modifier
            .offset {
                IntOffset((block.xDp * density).roundToInt(), (block.yDp * density).roundToInt())
            }
            .width(widthDp)
            .height(heightDp)
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
            .clickable(onClick = onEdit)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hasError) Modifier.border(3.dp, MaterialTheme.colorScheme.error, shape) else Modifier),
            shape = shape,
            color = blockColor(block.type),
            shadowElevation = 3.dp
        ) {
            when (block.type.role) {
                BlockRole.CONTAINER -> ContainerHeader(block, allBlocks)
                BlockRole.VALUE -> ValueBlockContent(block)
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
        Box(Modifier.fillMaxSize()) {
            Text(
                "Warten",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.offset(x = 16.dp, y = 17.dp)
            )
            ValueSocket(
                fallback = "${block.primary} ms",
                connected = connectedValue(blocks, block.id, "duration") != null,
                modifier = Modifier.offset(x = 128.dp, y = 7.dp)
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
private fun ValueBlockContent(block: ProgramBlock) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (block.type) {
                BlockType.ANALOG_READ -> "Analog A${block.primary}"
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
    Box(modifier = Modifier.width(ContainerWidthDp.dp).height(ContainerHeaderDp.dp)) {
        when (block.type) {
            BlockType.REPEAT -> {
                Text("Wiederhole", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.offset(x = 16.dp, y = 17.dp))
                ValueSocket(
                    fallback = block.primary.toString(),
                    connected = connectedValue(blocks, block.id, "count") != null,
                    modifier = Modifier.offset(x = 150.dp, y = 6.dp)
                )
                Text("mal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.offset(x = 267.dp, y = 18.dp))
            }
            else -> {
                Text(block.type.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.offset(x = 16.dp, y = 17.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.offset(x = 154.dp, y = 8.dp)
                ) {
                    Text(
                        blockValueLabel(block),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueSocket(fallback: String, connected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .width(ValueWidthDp.dp)
            .height(42.dp)
            .border(1.dp, Color.White.copy(alpha = 0.38f), NumberValueShape),
        shape = NumberValueShape,
        color = Color.Black.copy(alpha = 0.18f)
    ) {
        if (!connected) {
            Box(contentAlignment = Alignment.Center) {
                Text(fallback, color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
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

fun blockValueLabel(block: ProgramBlock): String = when (block.type) {
    BlockType.DELAY -> "${block.primary} ms"
    BlockType.DIGITAL_WRITE -> "D${block.primary} • ${if (block.flag) "HIGH" else "LOW"}"
    BlockType.PWM_WRITE -> "D${block.primary} • ${block.secondary}"
    BlockType.ANALOG_READ -> "A${block.primary}"
    BlockType.REPEAT -> "${block.primary}×"
    BlockType.IF_DIGITAL -> "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}"
}

fun blockColor(type: BlockType): Color = when (type) {
    BlockType.DELAY -> Color(0xFF6C55C7)
    BlockType.DIGITAL_WRITE -> Color(0xFF16865C)
    BlockType.PWM_WRITE -> Color(0xFF007A8A)
    BlockType.ANALOG_READ -> Color(0xFF2D6BC4)
    BlockType.REPEAT -> Color(0xFFD89A00)
    BlockType.IF_DIGITAL -> Color(0xFFB85B16)
}
