package ch.elekto.blocklyrduino.r4.model

import java.util.UUID

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
    REPEAT(
        title = "Wiederholen",
        subtitle = "Führt die eingerasteten Befehle mehrfach aus.",
        category = BlockCategory.LOGIK,
        role = BlockRole.CONTAINER,
        defaultPrimary = 10,
        valueInputs = listOf(ValueInputSpec("count", "Anzahl", ValueType.NUMBER))
    ),
    IF_DIGITAL(
        title = "Wenn Eingang",
        subtitle = "Führt eingerastete Befehle nur bei HIGH oder LOW aus.",
        category = BlockCategory.LOGIK,
        role = BlockRole.CONTAINER,
        defaultPrimary = 2,
        defaultFlag = true
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
const val CommandWidthDp = 248f
const val ValueWidthDp = 112f
const val ContainerWidthDp = 304f

fun directChildren(blocks: List<ProgramBlock>, parentId: String): List<ProgramBlock> =
    blocks.filter { it.parentId == parentId }
        .sortedWith(compareBy<ProgramBlock> { it.childOrder }.thenBy { it.yDp })

fun connectedValue(blocks: List<ProgramBlock>, ownerId: String, inputKey: String): ProgramBlock? =
    blocks.firstOrNull { it.valueOwnerId == ownerId && it.valueInputKey == inputKey }

fun valueInputSpec(owner: ProgramBlock, inputKey: String): ValueInputSpec? =
    owner.type.valueInputs.firstOrNull { it.key == inputKey }

fun valueSocketOffset(owner: ProgramBlock, inputKey: String): Pair<Float, Float> = when (owner.type) {
    BlockType.DELAY -> 128f to 7f
    BlockType.REPEAT -> 150f to 6f
    else -> 128f to 7f
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

    currentList().filter { it.valueOwnerId != null }.forEach { value ->
        val owner = value.valueOwnerId?.let(map::get)
        val key = value.valueInputKey
        if (owner != null && key != null) {
            val (dx, dy) = valueSocketOffset(owner, key)
            map[value.id] = value.copy(
                xDp = owner.xDp + dx,
                yDp = owner.yDp + dy,
                parentId = null,
                previousId = null,
                childOrder = 0
            )
        }
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
