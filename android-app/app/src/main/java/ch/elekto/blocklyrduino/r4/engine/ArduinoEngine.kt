package ch.elekto.blocklyrduino.r4.engine

import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ProgramBlock

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
        val outputPins = mutableSetOf<Int>()
        val inputPins = mutableSetOf<Int>()

        blocks.forEach { block ->
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
                }
                BlockType.IF_DIGITAL -> {
                    if (block.primary !in 0..13) {
                        issues += ValidationIssue(IssueLevel.ERROR, "Digital-Pin D${block.primary} existiert am UNO R4 Header nicht.", block.id)
                    } else {
                        inputPins += block.primary
                    }
                }
            }
        }

        (outputPins intersect inputPins).forEach { pin ->
            issues += ValidationIssue(
                IssueLevel.ERROR,
                "D$pin wird im selben Projekt als Ein- und Ausgang verwendet. Das sollte zuerst geklärt werden."
            )
        }

        return issues
    }
}

object ArduinoCodeGenerator {
    fun generate(blocks: List<ProgramBlock>): String {
        val ordered = blocks.sortedWith(compareBy<ProgramBlock> { it.yDp }.thenBy { it.xDp })
        val outputPins = ordered.filter { it.type == BlockType.DIGITAL_WRITE || it.type == BlockType.PWM_WRITE }
            .map { it.primary }
            .filter { it in 0..13 }
            .distinct()
        val inputPins = ordered.filter { it.type == BlockType.IF_DIGITAL }
            .map { it.primary }
            .filter { it in 0..13 }
            .distinct()

        return buildString {
            appendLine("// Erzeugt mit Elekto Blocks für Arduino UNO R4 WiFi")
            appendLine()
            appendLine("void setup() {")
            outputPins.forEach { appendLine("  pinMode($it, OUTPUT);") }
            inputPins.forEach { appendLine("  pinMode($it, INPUT);") }
            appendLine("}")
            appendLine()
            appendLine("void loop() {")

            ordered.forEach { block ->
                when (block.type) {
                    BlockType.DIGITAL_WRITE ->
                        appendLine("  digitalWrite(${block.primary}, ${if (block.flag) "HIGH" else "LOW"});")
                    BlockType.PWM_WRITE ->
                        appendLine("  analogWrite(${block.primary}, ${block.secondary.coerceIn(0, 255)});")
                    BlockType.ANALOG_READ -> {
                        val suffix = block.id.replace("-", "").take(6)
                        appendLine("  int analog_$suffix = analogRead(A${block.primary.coerceIn(0, 5)});")
                    }
                    BlockType.DELAY ->
                        appendLine("  delay(${block.primary.coerceAtLeast(0)});")
                    BlockType.REPEAT -> {
                        val suffix = block.id.replace("-", "").take(4)
                        appendLine("  for (int i_$suffix = 0; i_$suffix < ${block.primary.coerceAtLeast(1)}; i_$suffix++) {")
                        appendLine("    // In Alpha 5 folgt die Verschachtelung von Blöcken als nächster Editor-Schritt.")
                        appendLine("  }")
                    }
                    BlockType.IF_DIGITAL -> {
                        appendLine("  if (digitalRead(${block.primary}) == ${if (block.flag) "HIGH" else "LOW"}) {")
                        appendLine("    // In Alpha 5 folgt die Verschachtelung von Blöcken als nächster Editor-Schritt.")
                        appendLine("  }")
                    }
                }
            }

            appendLine("}")
        }
    }
}
