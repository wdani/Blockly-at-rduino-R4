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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import kotlin.math.roundToInt

private val WorkspaceWidth = 1400.dp
private val WorkspaceHeight = 2200.dp

@Composable
fun BlockWorkspace(
    blocks: List<ProgramBlock>,
    errorBlockIds: Set<String>,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: (ProgramBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontal)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(vertical)
            ) {
                Box(
                    modifier = Modifier
                        .size(WorkspaceWidth, WorkspaceHeight)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    WorkspaceGrid()

                    if (blocks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Noch keine Blöcke",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tippe auf „Blöcke“, um den ersten Baustein einzufügen.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    blocks.forEach { block ->
                        NativeProgramBlock(
                            block = block,
                            hasError = block.id in errorBlockIds,
                            onMove = onMove,
                            onMoveFinished = onMoveFinished,
                            onEdit = { onEdit(block) }
                        )
                    }
                }
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
    hasError: Boolean,
    onMove: (id: String, deltaXDp: Float, deltaYDp: Float) -> Unit,
    onMoveFinished: (id: String) -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current.density
    val blockColor = blockColor(block.type)
    val shape = RoundedCornerShape(16.dp)
    val isContainer = block.type == BlockType.REPEAT || block.type == BlockType.IF_DIGITAL

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (block.xDp * density).roundToInt(),
                    (block.yDp * density).roundToInt()
                )
            }
            .width(if (isContainer) 292.dp else 252.dp)
            .height(if (isContainer) 104.dp else 72.dp)
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragEnd = { onMoveFinished(block.id) },
                    onDragCancel = { onMoveFinished(block.id) }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(block.id, dragAmount.x / density, dragAmount.y / density)
                }
            }
            .clickable(onClick = onEdit)
            .then(
                if (hasError) Modifier.border(3.dp, MaterialTheme.colorScheme.error, shape)
                else Modifier
            ),
        shape = shape,
        color = blockColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.type.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = blockValueLabel(block),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (isContainer) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Verschachtelung folgt im nächsten Editor-Schritt",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private fun blockValueLabel(block: ProgramBlock): String = when (block.type) {
    BlockType.DELAY -> "${block.primary} ms"
    BlockType.DIGITAL_WRITE -> "D${block.primary} • ${if (block.flag) "HIGH" else "LOW"}"
    BlockType.PWM_WRITE -> "D${block.primary} • ${block.secondary}/255"
    BlockType.ANALOG_READ -> "A${block.primary}"
    BlockType.REPEAT -> "${block.primary}×"
    BlockType.IF_DIGITAL -> "D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}"
}

private fun blockColor(type: BlockType): Color = when (type) {
    BlockType.DELAY -> Color(0xFF7B61D1)
    BlockType.DIGITAL_WRITE -> Color(0xFF16865C)
    BlockType.PWM_WRITE -> Color(0xFF007A8A)
    BlockType.ANALOG_READ -> Color(0xFF2D6BC4)
    BlockType.REPEAT -> Color(0xFFE0A400)
    BlockType.IF_DIGITAL -> Color(0xFFC06A00)
}
