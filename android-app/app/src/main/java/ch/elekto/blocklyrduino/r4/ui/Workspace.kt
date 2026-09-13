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
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.blockHeightDp
import ch.elekto.blocklyrduino.r4.model.directChildren
import kotlin.math.roundToInt

private val WorkspaceWidth = 1400.dp
private val WorkspaceHeight = 2200.dp
private val CommandWidth = 220.dp
private val ValueWidth = 178.dp
private val ContainerWidth = 286.dp

private val StatementShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val corner = h * 0.16f
    val tab = h * 0.14f
    val socketStart = w * 0.20f
    val socketEnd = w * 0.36f

    moveTo(corner, 0f)
    lineTo(socketStart, 0f)
    cubicTo(socketStart + tab, 0f, socketStart + tab, tab, socketStart + tab * 2f, tab)
    cubicTo(socketEnd - tab, tab, socketEnd - tab, 0f, socketEnd, 0f)
    lineTo(w - corner, 0f)
    quadraticBezierTo(w, 0f, w, corner)
    lineTo(w, h - tab - corner)
    quadraticBezierTo(w, h - tab, w - corner, h - tab)
    lineTo(socketEnd, h - tab)
    cubicTo(socketEnd - tab, h - tab, socketEnd - tab, h, socketEnd - tab * 2f, h)
    cubicTo(socketStart + tab, h, socketStart + tab, h - tab, socketStart, h - tab)
    lineTo(corner, h - tab)
    quadraticBezierTo(0f, h - tab, 0f, h - tab - corner)
    lineTo(0f, corner)
    quadraticBezierTo(0f, 0f, corner, 0f)
    close()
}

private val ValueShape = GenericShape { size, _ ->
    val point = size.height * 0.34f
    moveTo(point, 0f)
    lineTo(size.width - point, 0f)
    quadraticBezierTo(size.width, 0f, size.width, size.height / 2f)
    quadraticBezierTo(size.width, size.height, size.width - point, size.height)
    lineTo(point, size.height)
    quadraticBezierTo(0f, size.height, 0f, size.height / 2f)
    quadraticBezierTo(0f, 0f, point, 0f)
    close()
}

private val ContainerShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val tab = w * 0.028f
    val corner = w * 0.035f
    val socketStart = w * 0.20f
    val socketEnd = w * 0.34f
    val header = minOf(h * 0.44f, w * 0.205f)
    val footer = w * 0.08f
    val rail = w * 0.105f

    moveTo(corner, 0f)
    lineTo(socketStart, 0f)
    cubicTo(socketStart + tab, 0f, socketStart + tab, tab, socketStart + tab * 2f, tab)
    cubicTo(socketEnd - tab, tab, socketEnd - tab, 0f, socketEnd, 0f)
    lineTo(w - corner, 0f)
    quadraticBezierTo(w, 0f, w, corner)
    lineTo(w, header - corner)
    quadraticBezierTo(w, header, w - corner, header)
    lineTo(rail, header)
    lineTo(rail, h - footer)
    lineTo(w - corner, h - footer)
    quadraticBezierTo(w, h - footer, w, h - footer + corner)
    lineTo(w, h - tab - corner)
    quadraticBezierTo(w, h - tab, w - corner, h - tab)
    lineTo(socketEnd, h - tab)
    cubicTo(socketEnd - tab, h - tab, socketEnd - tab, h, socketEnd - tab * 2f, h)
    cubicTo(socketStart + tab, h, socketStart + tab, h - tab, socketStart, h - tab)
    lineTo(corner, h - tab)
    quadraticBezierTo(0f, h - tab, 0f, h - tab - corner)
    lineTo(0f, corner)
    quadraticBezierTo(0f, 0f, corner, 0f)
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
                            onMoveStart = onMoveStart,
                            onMove = onMove,
                            onMoveFinished = onMoveFinished,
                            onEdit = { onEdit(block) }
                        )
                    }
                }
            }
        }

        ZoomControls(
            zoom = zoom,
            onZoomOut = { zoom = (zoom - 0.10f).coerceAtLeast(0.50f) },
            onZoomIn = { zoom = (zoom + 0.10f).coerceAtMost(1.30f) },
            onReset = { zoom = 0.78f },
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp)
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
    onMoveStart: (id: String) -> Unit,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current.density
    val heightDp = blockHeightDp(block, allBlocks).dp
    val widthDp = when (block.type.role) {
        BlockRole.COMMAND -> CommandWidth
        BlockRole.VALUE -> ValueWidth
        BlockRole.CONTAINER -> ContainerWidth
    }
    val shape = when (block.type.role) {
        BlockRole.COMMAND -> StatementShape
        BlockRole.VALUE -> ValueShape
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
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = { onMoveStart(block.id) },
                    onDragEnd = { onMoveFinished(block.id) },
                    onDragCancel = { onMoveFinished(block.id) }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(block.id, dragAmount.x / density, dragAmount.y / density)
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
            shadowElevation = 4.dp
        ) {
            when (block.type.role) {
                BlockRole.CONTAINER -> ContainerHeader(block, childCount)
                else -> CompactBlockContent(block)
            }
        }

        if (block.type.role == BlockRole.CONTAINER && childCount == 0) {
            Text(
                text = "Befehle hier einrasten",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .offset(x = 46.dp, y = 72.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun CompactBlockContent(block: ProgramBlock) {
    Row(
        modifier = Modifier.fillMaxSize().padding(start = 13.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (block.type.role == BlockRole.VALUE) "#" else "▶",
            color = Color.White.copy(alpha = 0.82f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 7.dp)
        )
        Text(
            text = block.type.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(7.dp))
        Surface(shape = RoundedCornerShape(9.dp), color = Color.White.copy(alpha = 0.20f)) {
            Text(
                text = blockValueLabel(block),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ContainerHeader(block: ProgramBlock, childCount: Int) {
    Row(
        modifier = Modifier.width(ContainerWidth).height(56.dp).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("↳", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(block.type.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (childCount > 0) {
                Text("$childCount ${if (childCount == 1) "Befehl" else "Befehle"}", color = Color.White.copy(alpha = 0.76f), fontSize = 10.sp)
            }
        }
        Surface(shape = RoundedCornerShape(9.dp), color = Color.White.copy(alpha = 0.20f)) {
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

private fun nestingDepth(block: ProgramBlock, blocks: List<ProgramBlock>): Int {
    val byId = blocks.associateBy { it.id }
    var depth = 0
    var current = block.parentId
    val seen = mutableSetOf<String>()
    while (current != null && seen.add(current)) {
        depth++
        current = byId[current]?.parentId
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
