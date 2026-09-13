package ch.elekto.blocklyrduino.r4.model

import java.util.UUID

enum class BlockCategory(val title: String) {
    GRUNDLAGEN("Grundlagen"),
    EIN_AUSGAENGE("Ein-/Ausgänge"),
    LOGIK("Logik")
}

enum class BlockType(
    val title: String,
    val subtitle: String,
    val category: BlockCategory,
    val defaultPrimary: Int,
    val defaultSecondary: Int = 0,
    val defaultFlag: Boolean = true
) {
    DELAY(
        title = "Warten",
        subtitle = "Programm für eine Zeit pausieren",
        category = BlockCategory.GRUNDLAGEN,
        defaultPrimary = 1000
    ),
    DIGITAL_WRITE(
        title = "Digitalausgang",
        subtitle = "Einen digitalen Pin HIGH oder LOW schalten",
        category = BlockCategory.EIN_AUSGAENGE,
        defaultPrimary = 13,
        defaultFlag = true
    ),
    PWM_WRITE(
        title = "PWM-Ausgang",
        subtitle = "Helligkeit oder Leistung mit PWM steuern",
        category = BlockCategory.EIN_AUSGAENGE,
        defaultPrimary = 3,
        defaultSecondary = 128
    ),
    ANALOG_READ(
        title = "Analogwert lesen",
        subtitle = "A0 bis A5 auslesen",
        category = BlockCategory.EIN_AUSGAENGE,
        defaultPrimary = 0
    ),
    REPEAT(
        title = "Wiederholen",
        subtitle = "Einen Abschnitt mehrfach ausführen",
        category = BlockCategory.LOGIK,
        defaultPrimary = 10
    ),
    IF_DIGITAL(
        title = "Wenn Eingang",
        subtitle = "Auf HIGH oder LOW an einem Pin reagieren",
        category = BlockCategory.LOGIK,
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
    val flag: Boolean = type.defaultFlag
)

fun defaultBlinkProject(): List<ProgramBlock> = listOf(
    ProgramBlock(type = BlockType.DIGITAL_WRITE, xDp = 40f, yDp = 48f, primary = 13, flag = true),
    ProgramBlock(type = BlockType.DELAY, xDp = 40f, yDp = 132f, primary = 1000),
    ProgramBlock(type = BlockType.DIGITAL_WRITE, xDp = 40f, yDp = 216f, primary = 13, flag = false),
    ProgramBlock(type = BlockType.DELAY, xDp = 40f, yDp = 300f, primary = 1000)
)
