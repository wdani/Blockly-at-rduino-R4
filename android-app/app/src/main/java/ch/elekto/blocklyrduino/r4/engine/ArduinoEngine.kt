package ch.elekto.blocklyrduino.r4.engine

import ch.elekto.blocklyrduino.r4.model.BlockRole
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.directChildren

enum class IssueLevel { INFO, WARNING, ERROR }

data class ValidationIssue(
    val level: IssueLevel,
    val message: String,
    val blockId: String? = null
)

object ProgramValidator {
    private val pwmPins = setOf(3, 5, 6, 9, 10, 11)

    fun validate(blocks: List<ProgramBlock>): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val byId = blocks.associateBy { it.id }
        val outputPins = mutableSetOf<Int>()
        val inputPins = mutableSetOf<Int>()

        blocks.forEach { block ->
            block.parentId?.let { parentId ->
                val parent = byId[parentId]
                when {
                    parent == null -> issues += ValidationIssue(IssueLevel.ERROR, "Dieser Block verweist auf einen nicht mehr vorhandenen Steuerblock.", block.id)
                    parent.type.role != BlockRole.CONTAINER -> issues += ValidationIssue(IssueLevel.ERROR, "Dieser Block steckt an einer Stelle, die keine Befehle aufnehmen kann.", block.id)
                    block.type.role == BlockRole.VALUE -> issues += ValidationIssue(IssueLevel.ERROR, "Wertblöcke gehören in Werteingänge und nicht direkt in einen Steuerblock.", block.id)
                }
            }

            block.previousId?.let { previousId ->
                val previous = byId[previousId]
                if (previous == null) {
                    issues += ValidationIssue(IssueLevel.ERROR, "Die Verbindung zum vorherigen Block ist beschädigt.", block.id)
                } else if (block.parentId != null || block.type.role == BlockRole.VALUE || previous.type.role == BlockRole.VALUE) {
                    issues += ValidationIssue(IssueLevel.ERROR, "Diese Blocktypen können nicht als Befehlsfolge zusammengesteckt werden.", block.id)
                }
            }

            when (block.type) {
                BlockType.DIGITAL_WRITE -> {
                    if (block.primary !in 0..13) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Digital-Pin D${block.primary} existiert am UNO R4 Header nicht.", block.id)
                    } else {
                        outputPins += block.primary
                        if (block.primary in 0..1) {
                            issues += ValidationIssue(IssueLevel.WARNING, "D${block.primary} wird auch für die serielle Schnittstelle verwendet.", block.id)
                        }
                    }
                }
                BlockType.PWM_WRITE -> {
                    if (block.primary !in pwmPins) {
                        issues += ValidationIssue(IssueLevel.ERROR, "PWM ist beim UNO R4 an D3, D5, D6, D9, D10 und D11 vorgesehen.", block.id)
                    } else {
                        outputPins += block.primary
                    }
                    if (block.secondary !in 0..255) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Der PWM-Wert muss zwischen 0 und 255 liegen.", block.id)
                    }
                }
                BlockType.ANALOG_READ -> {
                    if (block.primary !in 0..5) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Der UNO R4 hat in dieser Ansicht die Analogeingänge A0 bis A5.", block.id)
                    }
                }
                BlockType.DELAY -> {
                    if (block.primary !in 0..600_000) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Die Wartezeit muss zwischen 0 und 600000 ms liegen.", block.id)
                    }
                }
                BlockType.REPEAT -> {
                    if (block.primary !in 1..1000) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Die Wiederholungszahl muss zwischen 1 und 1000 liegen.", block.id)
                    }
                    if (directChildren(blocks, block.id).isEmpty()) {
                        issues += ValidationIssue(IssueLevel.INFO, "Der Wiederholen-Block ist noch leer. Ziehe Befehle in seine Öffnung.", block.id)
                    }
                }
                BlockType.IF_DIGITAL -> {
                    if (block.primary !in 0..13) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Digital-Pin D${block.primary} existiert am UNO R4 Header nicht.", block.id)
                    } else {
                        inputPins += block.primary
                    }
                    if (directChildren(blocks, block.id).isEmpty()) {
                        issues += ValidationIssue(IssueLevel.INFO, "Der Wenn-Block ist noch leer. Ziehe Befehle in seine Öffnung.", block.id)
                    }
                }
            }
        }

        // Detect parent/sequence cycles defensively so a damaged project cannot recurse forever.
        blocks.forEach { start ->
            val seen = mutableSetOf<String>()
            var current: ProgramBlock? = start
            while (current != null && seen.add(current.id)) {
                val nextId = current.parentId ?: current.previousId
                current = nextId?.let(byId::get)
            }
            if (current != null) {
                issues += ValidationIssue(IssueLevel.ERROR, "In den Blockverbindungen wurde eine Schleife gefunden.", start.id)
            }
        }

        (outputPins intersect inputPins).forEach { pin ->
            issues += ValidationIssue(
                IssueLevel.ERROR,
                "D$pin wird im selben Projekt als Ein- und Ausgang verwendet. Das sollte zuerst geklärt werden."
            )
        }

        return issues.distinctBy { Triple(it.level, it.message, it.blockId) }
    }
}

object ArduinoCodeGenerator {
    fun generate(blocks: List<ProgramBlock>): String {
        val byId = blocks.associateBy { it.id }
        val outputPins = blocks
            .filter { it.type == BlockType.DIGITAL_WRITE || it.type == BlockType.PWM_WRITE }
            .map { it.primary }
            .filter { it in 0..13 }
            .distinct()
        val inputPins = blocks
            .filter { it.type == BlockType.IF_DIGITAL }
            .map { it.primary }
            .filter { it in 0..13 }
            .distinct()

        fun roots(): List<ProgramBlock> = blocks
            .filter { it.parentId == null && (it.previousId == null || byId[it.previousId] == null) }
            .sortedWith(compareBy<ProgramBlock> { it.yDp }.thenBy { it.xDp })

        return buildString {
            appendLine("// Erzeugt mit Elekto Blocks für Arduino UNO R4 WiFi")
            appendLine()
            appendLine("void setup() {")
            outputPins.forEach { appendLine("  pinMode($it, OUTPUT);") }
            inputPins.forEach { appendLine("  pinMode($it, INPUT);") }
            appendLine("}")
            appendLine()
            appendLine("void loop() {")

            val emitted = mutableSetOf<String>()

            fun emitBlock(block: ProgramBlock, indent: String) {
                if (!emitted.add(block.id)) return
                when (block.type) {
                    BlockType.DIGITAL_WRITE ->
                        appendLine("${indent}digitalWrite(${block.primary}, ${if (block.flag) "HIGH" else "LOW"});")
                    BlockType.PWM_WRITE ->
                        appendLine("${indent}analogWrite(${block.primary}, ${block.secondary.coerceIn(0, 255)});")
                    BlockType.ANALOG_READ -> {
                        val suffix = block.id.replace("-", "").take(6)
                        appendLine("${indent}int analog_$suffix = analogRead(A${block.primary.coerceIn(0, 5)});")
                    }
                    BlockType.DELAY ->
                        appendLine("${indent}delay(${block.primary.coerceAtLeast(0)});")
                    BlockType.REPEAT -> {
                        val suffix = block.id.replace("-", "").take(4)
                        appendLine("${indent}for (int i_$suffix = 0; i_$suffix < ${block.primary.coerceAtLeast(1)}; i_$suffix++) {")
                        directChildren(blocks, block.id).forEach { emitBlock(it, "$indent  ") }
                        appendLine("${indent}}")
                    }
                    BlockType.IF_DIGITAL -> {
                        appendLine("${indent}if (digitalRead(${block.primary}) == ${if (block.flag) "HIGH" else "LOW"}) {")
                        directChildren(blocks, block.id).forEach { emitBlock(it, "$indent  ") }
                        appendLine("${indent}}")
                    }
                }
            }

            fun emitChain(first: ProgramBlock) {
                var current: ProgramBlock? = first
                val chainSeen = mutableSetOf<String>()
                while (current != null && chainSeen.add(current.id)) {
                    emitBlock(current, "  ")
                    current = blocks.firstOrNull {
                        it.parentId == null && it.previousId == current!!.id
                    }
                }
            }

            roots().forEach(::emitChain)
            // Damaged/orphaned blocks are still emitted once so code view never silently loses content.
            blocks.filter { it.id !in emitted }.sortedBy { it.yDp }.forEach { emitBlock(it, "  ") }

            appendLine("}")
        }
    }
}
