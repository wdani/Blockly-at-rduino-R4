package ch.elekto.blocklyrduino.r4.model

import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

enum class BlockCategory(val title: String) {
    GRUNDLAGEN("Grundlagen"),
    EIN_AUSGAENGE("Ein-/Ausgänge"),
    LOGIK("Logik"),
    WERTE("Werte")
}

enum class BlockRole(val title: String, val explanation: String) {
    COMMAND("Befehl", "Wird von oben nach unten ausgeführt und kann mit anderen Befehlen verbunden werden."),
    VALUE("Wert", "Liefert einen Wert und passt nur in einen Werteingang mit passendem Datentyp."),
    CONTAINER("Steuerung", "Umschließt andere Befehle und bestimmt, wann oder wie oft sie ausgeführt werden.")
}

enum class ValueType(val title: String) {
    NUMBER("Zahl"),
    BOOLEAN("Wahr/Falsch"),
    TEXT("Text")
}

data class ValueInputSpec(
    val key: String,
    val label: String,
    val acceptedType: ValueType
)

enum class BlockType(
    val title: String,
    val subtitle: String,
    val category: BlockCategory,
    val role: BlockRole,
    val defaultPrimary: Int,
    val defaultSecondary: Int = 0,
    val defaultFlag: Boolean = true,
    val defaultOption: String = "",
    val outputType: ValueType? = null,
    val valueInputs: List<ValueInputSpec> = emptyList()
) {
    DELAY(
        title = "Warten",
        subtitle = "Pausiert den Ablauf für eine bestimmte Zeit.",
        category = BlockCategory.GRUNDLAGEN,
        role = BlockRole.COMMAND,
        defaultPrimary = 1000,
        valueInputs = listOf(ValueInputSpec("duration", "Wartezeit", ValueType.NUMBER))
    ),
    DIGITAL_WRITE(
        title = "Digitaler Ausgang",
        subtitle = "Schaltet einen digitalen Pin auf HIGH oder LOW.",
        category = BlockCategory.EIN_AUSGAENGE,
        role = BlockRole.COMMAND,
        defaultPrimary = 13,
        defaultFlag = true
    ),
    PWM_WRITE(
        title = "PWM-Ausgang",
        subtitle = "Steuert Helligkeit oder Leistung mit einem Wert von 0 bis 255.",
        category = BlockCategory.EIN_AUSGAENGE,
        role = BlockRole.COMMAND,
        defaultPrimary = 3,
        defaultSecondary = 128
    ),
    ANALOG_READ(
        title = "Analogwert",
        subtitle = "Liest A0 bis A5 und liefert daraus einen Zahlenwert.",
        category = BlockCategory.WERTE,
        role = BlockRole.VALUE,
        defaultPrimary = 0,
        outputType = ValueType.NUMBER
    ),
    NUMBER_LITERAL(
        title = "Zahl",
        subtitle = "Ein fester Zahlenwert für einen Zahlen-Eingang.",
        category = BlockCategory.WERTE,
        role = BlockRole.VALUE,
        defaultPrimary = 1000,
        outputType = ValueType.NUMBER
    ),
    DIGITAL_READ_BOOL(
        title = "Digitaler Zustand",
        subtitle = "Prüft, ob ein digitaler Eingang HIGH oder LOW ist.",
        category = BlockCategory.EIN_AUSGAENGE,
        role = BlockRole.VALUE,
        defaultPrimary = 2,
        defaultFlag = true,
        outputType = ValueType.BOOLEAN
    ),
    COMPARE_NUMBER(
        title = "Zahlen vergleichen",
        subtitle = "Vergleicht zwei Zahlen und liefert Wahr oder Falsch.",
        category = BlockCategory.LOGIK,
        role = BlockRole.VALUE,
        defaultPrimary = 0,
        defaultSecondary = 0,
        defaultOption = "GT",
        outputType = ValueType.BOOLEAN,
        valueInputs = listOf(
            ValueInputSpec("left", "Links", ValueType.NUMBER),
            ValueInputSpec("right", "Rechts", ValueType.NUMBER)
        )
    ),
    REPEAT(
        title = "Wiederholen",
        subtitle = "Führt die eingerasteten Befehle mehrfach aus.",
        category = BlockCategory.LOGIK,
        role = BlockRole.CONTAINER,
        defaultPrimary = 10,
        valueInputs = listOf(ValueInputSpec("count", "Anzahl", ValueType.NUMBER))
    ),
    IF_DIGITAL(
        title = "Wenn",
        subtitle = "Führt eingerastete Befehle nur aus, wenn die Bedingung wahr ist.",
        category = BlockCategory.LOGIK,
        role = BlockRole.CONTAINER,
        defaultPrimary = 2,
        defaultFlag = true,
        valueInputs = listOf(ValueInputSpec("condition", "Bedingung", ValueType.BOOLEAN))
    )
}

data class ProgramBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val xDp: Float,
    val yDp: Float,
    val primary: Int = type.defaultPrimary,
    val secondary: Int = type.defaultSecondary,
    val flag: Boolean = type.defaultFlag,
    val option: String = type.defaultOption,
    val parentId: String? = null,
    val childOrder: Int = 0,
    val previousId: String? = null,
    val valueOwnerId: String? = null,
    val valueInputKey: String? = null
)

const val StatementHeightDp = 56f
const val ValueHeightDp = 42f
const val ConnectorOverlapDp = 8f
const val ContainerHeaderDp = 54f
const val ContainerFooterDp = 18f
const val ContainerEmptyBodyDp = 54f
const val ContainerIndentDp = 34f
const val MinCommandWidthDp = 208f
const val MinContainerWidthDp = 286f
const val NumberSocketMinWidthDp = 92f
const val BooleanSocketMinWidthDp = 152f

private const val HorizontalPaddingDp = 16f
private const val InlineGapDp = 10f
private const val OperatorWidthDp = 48f

private fun approximateTextWidthDp(text: String, fontSp: Float = 14f): Float =
    max(18f, text.length * fontSp * 0.52f)

fun directChildren(blocks: List<ProgramBlock>, parentId: String): List<ProgramBlock> =
    blocks.filter { it.parentId == parentId }
        .sortedWith(compareBy<ProgramBlock> { it.childOrder }.thenBy { it.yDp })

fun connectedValue(blocks: List<ProgramBlock>, ownerId: String, inputKey: String): ProgramBlock? =
    blocks.firstOrNull { it.valueOwnerId == ownerId && it.valueInputKey == inputKey }

fun valueInputSpec(owner: ProgramBlock, inputKey: String): ValueInputSpec? =
    owner.type.valueInputs.firstOrNull { it.key == inputKey }

fun defaultSocketWidthDp(type: ValueType): Float = when (type) {
    ValueType.NUMBER -> NumberSocketMinWidthDp
    ValueType.BOOLEAN -> BooleanSocketMinWidthDp
    ValueType.TEXT -> 150f
}

fun valueSocketWidthDp(
    owner: ProgramBlock,
    inputKey: String,
    blocks: List<ProgramBlock>,
    visited: Set<String> = emptySet()
): Float {
    val spec = valueInputSpec(owner, inputKey) ?: return NumberSocketMinWidthDp
    val connected = connectedValue(blocks, owner.id, inputKey)
    return max(
        defaultSocketWidthDp(spec.acceptedType),
        connected?.let { blockWidthDpInternal(it, blocks, visited + owner.id) } ?: 0f
    )
}

fun blockWidthDp(block: ProgramBlock, blocks: List<ProgramBlock>): Float =
    blockWidthDpInternal(block, blocks, emptySet())

private fun blockWidthDpInternal(block: ProgramBlock, blocks: List<ProgramBlock>, visited: Set<String>): Float {
    if (block.id in visited) return when (block.type.role) {
        BlockRole.COMMAND -> MinCommandWidthDp
        BlockRole.VALUE -> 120f
        BlockRole.CONTAINER -> MinContainerWidthDp
    }

    val nextVisited = visited + block.id
    return when (block.type) {
        BlockType.DELAY -> max(
            MinCommandWidthDp,
            HorizontalPaddingDp + approximateTextWidthDp("Warten") + InlineGapDp +
                valueSocketWidthDp(block, "duration", blocks, nextVisited) + HorizontalPaddingDp
        )
        BlockType.DIGITAL_WRITE -> max(
            MinCommandWidthDp,
            HorizontalPaddingDp + approximateTextWidthDp(block.type.title) + InlineGapDp +
                approximateTextWidthDp("D${block.primary} • ${if (block.flag) "HIGH" else "LOW"}", 12f) + 34f
        )
        BlockType.PWM_WRITE -> max(
            MinCommandWidthDp,
            HorizontalPaddingDp + approximateTextWidthDp(block.type.title) + InlineGapDp +
                approximateTextWidthDp("D${block.primary} • ${block.secondary}", 12f) + 34f
        )
        BlockType.ANALOG_READ -> max(112f, approximateTextWidthDp("Analog A${block.primary}", 12f) + 28f)
        BlockType.NUMBER_LITERAL -> max(72f, approximateTextWidthDp(block.primary.toString(), 13f) + 30f)
        BlockType.DIGITAL_READ_BOOL -> max(
            154f,
            approximateTextWidthDp("D${block.primary} = ${if (block.flag) "HIGH" else "LOW"}", 12f) + 40f
        )
        BlockType.COMPARE_NUMBER -> {
            val left = valueSocketWidthDp(block, "left", blocks, nextVisited)
            val right = valueSocketWidthDp(block, "right", blocks, nextVisited)
            max(262f, HorizontalPaddingDp + left + InlineGapDp + OperatorWidthDp + InlineGapDp + right + HorizontalPaddingDp)
        }
        BlockType.REPEAT -> max(
            MinContainerWidthDp,
            HorizontalPaddingDp + approximateTextWidthDp("Wiederhole") + InlineGapDp +
                valueSocketWidthDp(block, "count", blocks, nextVisited) + InlineGapDp + approximateTextWidthDp("mal", 13f) + HorizontalPaddingDp
        )
        BlockType.IF_DIGITAL -> max(
            MinContainerWidthDp,
            HorizontalPaddingDp + approximateTextWidthDp("Wenn") + InlineGapDp +
                valueSocketWidthDp(block, "condition", blocks, nextVisited) + HorizontalPaddingDp
        )
    }
}

fun valueSocketOffset(owner: ProgramBlock, inputKey: String, blocks: List<ProgramBlock>): Pair<Float, Float> = when (owner.type) {
    BlockType.DELAY ->
        HorizontalPaddingDp + approximateTextWidthDp("Warten") + InlineGapDp to (StatementHeightDp - ValueHeightDp) / 2f
    BlockType.REPEAT ->
        HorizontalPaddingDp + approximateTextWidthDp("Wiederhole") + InlineGapDp to (ContainerHeaderDp - ValueHeightDp) / 2f
    BlockType.IF_DIGITAL ->
        HorizontalPaddingDp + approximateTextWidthDp("Wenn") + InlineGapDp to (ContainerHeaderDp - ValueHeightDp) / 2f
    BlockType.COMPARE_NUMBER -> when (inputKey) {
        "left" -> HorizontalPaddingDp to 0f
        "right" -> {
            val leftWidth = valueSocketWidthDp(owner, "left", blocks)
            HorizontalPaddingDp + leftWidth + InlineGapDp + OperatorWidthDp + InlineGapDp to 0f
        }
        else -> HorizontalPaddingDp to 0f
    }
    else -> HorizontalPaddingDp to (StatementHeightDp - ValueHeightDp) / 2f
}

fun blockHeightDp(block: ProgramBlock, blocks: List<ProgramBlock>, visited: Set<String> = emptySet()): Float {
    if (block.id in visited) return StatementHeightDp
    return when (block.type.role) {
        BlockRole.COMMAND -> StatementHeightDp
        BlockRole.VALUE -> ValueHeightDp
        BlockRole.CONTAINER -> {
            val children = directChildren(blocks, block.id)
            val bodyHeight = if (children.isEmpty()) {
                ContainerEmptyBodyDp
            } else {
                children.mapIndexed { index, child ->
                    val height = blockHeightDp(child, blocks, visited + block.id)
                    if (index == children.lastIndex) height else height - ConnectorOverlapDp
                }.sum()
            }
            ContainerHeaderDp + bodyHeight + ContainerFooterDp
        }
    }
}

fun linkedDescendantIds(blocks: List<ProgramBlock>, blockId: String): Set<String> {
    val result = linkedSetOf<String>()
    fun collect(id: String) {
        val nestedStatements = blocks.filter { it.parentId == id }
        val followers = blocks.filter { it.previousId == id && it.parentId == null && it.valueOwnerId == null }
        val valueChildren = blocks.filter { it.valueOwnerId == id }
        (nestedStatements + followers + valueChildren).forEach { child ->
            if (result.add(child.id)) collect(child.id)
        }
    }
    collect(blockId)
    return result
}

fun chainFrom(blocks: List<ProgramBlock>, firstId: String): List<ProgramBlock> {
    val result = mutableListOf<ProgramBlock>()
    val seen = mutableSetOf<String>()
    var current = blocks.firstOrNull { it.id == firstId }
    while (current != null && seen.add(current.id)) {
        result += current
        current = blocks.firstOrNull {
            it.previousId == current.id && it.parentId == null && it.valueOwnerId == null
        }
    }
    return result
}

sealed interface SnapTarget {
    val xDp: Float
    val yDp: Float

    data class Value(
        val ownerId: String,
        val inputKey: String,
        val acceptedType: ValueType,
        override val xDp: Float,
        override val yDp: Float
    ) : SnapTarget

    data class Container(
        val parentId: String,
        val insertIndex: Int,
        override val xDp: Float,
        override val yDp: Float
    ) : SnapTarget

    data class Statement(
        val previousId: String,
        override val xDp: Float,
        override val yDp: Float
    ) : SnapTarget
}

fun findSnapTarget(blocks: List<ProgramBlock>, movingId: String): SnapTarget? {
    val moving = blocks.firstOrNull { it.id == movingId } ?: return null
    val excluded = linkedDescendantIds(blocks, movingId) + movingId

    if (moving.type.role == BlockRole.VALUE) {
        return blocks
            .filter { it.id !in excluded && it.type.valueInputs.isNotEmpty() }
            .flatMap { owner ->
                owner.type.valueInputs.mapNotNull { spec ->
                    if (moving.type.outputType != spec.acceptedType) return@mapNotNull null
                    val (dx, dy) = valueSocketOffset(owner, spec.key, blocks)
                    val snapX = owner.xDp + dx
                    val snapY = owner.yDp + dy
                    val distanceX = abs(moving.xDp - snapX)
                    val distanceY = abs(moving.yDp - snapY)
                    if (distanceX <= 88f && distanceY <= 46f) {
                        SnapTarget.Value(owner.id, spec.key, spec.acceptedType, snapX, snapY) to (distanceX + distanceY)
                    } else null
                }
            }
            .minByOrNull { it.second }
            ?.first
    }

    val movingCenterX = moving.xDp + blockWidthDp(moving, blocks) / 2f
    val movingCenterY = moving.yDp + blockHeightDp(moving, blocks) / 2f

    val containerTarget = blocks
        .filter { it.id !in excluded && it.type.role == BlockRole.CONTAINER }
        .filter { target ->
            val targetHeight = blockHeightDp(target, blocks)
            val targetWidth = blockWidthDp(target, blocks)
            val zoneBottom = target.yDp + max(targetHeight - 10f, 130f)
            movingCenterX in (target.xDp + 18f)..(target.xDp + targetWidth) &&
                movingCenterY in (target.yDp + ContainerHeaderDp - 8f)..zoneBottom
        }
        .minByOrNull { abs(moving.yDp - (it.yDp + ContainerHeaderDp)) }

    if (containerTarget != null) {
        val children = directChildren(blocks, containerTarget.id)
        val insertIndex = children.indexOfFirst { child ->
            movingCenterY < child.yDp + blockHeightDp(child, blocks) / 2f
        }.let { if (it < 0) children.size else it }

        val snapY = if (insertIndex == 0) {
            containerTarget.yDp + ContainerHeaderDp
        } else {
            val previous = children[insertIndex - 1]
            previous.yDp + blockHeightDp(previous, blocks) - ConnectorOverlapDp
        }
        return SnapTarget.Container(
            parentId = containerTarget.id,
            insertIndex = insertIndex,
            xDp = containerTarget.xDp + ContainerIndentDp,
            yDp = snapY
        )
    }

    return blocks
        .filter {
            it.id !in excluded && it.parentId == null && it.valueOwnerId == null && it.type.role != BlockRole.VALUE
        }
        .map { candidate ->
            val snapY = candidate.yDp + blockHeightDp(candidate, blocks) - ConnectorOverlapDp
            Triple(candidate, abs(moving.xDp - candidate.xDp), abs(moving.yDp - snapY))
        }
        .filter { (_, dx, dy) -> dx <= 76f && dy <= 36f }
        .minByOrNull { (_, dx, dy) -> dx + dy }
        ?.first
        ?.let { SnapTarget.Statement(it.id, it.xDp, it.yDp + blockHeightDp(it, blocks) - ConnectorOverlapDp) }
}

fun normalizeProjectLayout(source: List<ProgramBlock>): List<ProgramBlock> {
    val map = source.associateBy { it.id }.toMutableMap()

    fun currentList(): List<ProgramBlock> = source.mapNotNull { map[it.id] }

    val heads = source.filter {
        it.parentId == null && it.valueOwnerId == null && it.previousId == null && it.type.role != BlockRole.VALUE
    }.sortedWith(compareBy<ProgramBlock> { it.yDp }.thenBy { it.xDp })

    heads.forEach { head ->
        var current = map[head.id] ?: return@forEach
        val seen = mutableSetOf<String>()
        while (seen.add(current.id)) {
            val follower = currentList().firstOrNull {
                it.parentId == null && it.valueOwnerId == null && it.previousId == current.id
            } ?: break
            val updated = follower.copy(
                xDp = current.xDp,
                yDp = current.yDp + blockHeightDp(current, currentList()) - ConnectorOverlapDp
            )
            map[follower.id] = updated
            current = updated
        }
    }

    fun layoutChildren(parentId: String, ancestry: Set<String>) {
        if (parentId in ancestry) return
        val parent = map[parentId] ?: return
        var nextY = parent.yDp + ContainerHeaderDp
        val children = currentList().filter { it.parentId == parentId }
            .sortedWith(compareBy<ProgramBlock> { it.childOrder }.thenBy { it.yDp })

        children.forEachIndexed { index, child ->
            val updated = child.copy(
                xDp = parent.xDp + ContainerIndentDp,
                yDp = nextY,
                childOrder = index,
                previousId = null,
                valueOwnerId = null,
                valueInputKey = null
            )
            map[child.id] = updated
            layoutChildren(child.id, ancestry + parentId)
            nextY += blockHeightDp(updated, currentList()) - ConnectorOverlapDp
        }
    }

    currentList().filter { it.type.role == BlockRole.CONTAINER }.forEach { container ->
        layoutChildren(container.id, emptySet())
    }

    fun layoutValueChildren(ownerId: String, ancestry: Set<String>) {
        if (ownerId in ancestry) return
        val owner = map[ownerId] ?: return
        currentList().filter { it.valueOwnerId == ownerId }.forEach { value ->
            val key = value.valueInputKey ?: return@forEach
            val (dx, dy) = valueSocketOffset(owner, key, currentList())
            val updated = value.copy(
                xDp = owner.xDp + dx,
                yDp = owner.yDp + dy,
                parentId = null,
                previousId = null,
                childOrder = 0
            )
            map[value.id] = updated
            layoutValueChildren(value.id, ancestry + ownerId)
        }
    }

    currentList().filter { it.valueOwnerId == null }.forEach { root ->
        layoutValueChildren(root.id, emptySet())
    }

    return source.mapNotNull { map[it.id] }
}

fun defaultBlinkProject(): List<ProgramBlock> {
    val high = ProgramBlock(type = BlockType.DIGITAL_WRITE, xDp = 32f, yDp = 40f, primary = 13, flag = true)
    val waitHigh = ProgramBlock(type = BlockType.DELAY, xDp = 32f, yDp = 88f, primary = 1000, previousId = high.id)
    val low = ProgramBlock(type = BlockType.DIGITAL_WRITE, xDp = 32f, yDp = 136f, primary = 13, flag = false, previousId = waitHigh.id)
    val waitLow = ProgramBlock(type = BlockType.DELAY, xDp = 32f, yDp = 184f, primary = 1000, previousId = low.id)
    return normalizeProjectLayout(listOf(high, waitHigh, low, waitLow))
}
