package ch.elekto.blocklyrduino.r4.model

const val BLUEPRINT_SCHEMA_VERSION = 1
const val BLUEPRINT_ENGINE_VERSION = "13.3.0"

data class BlueprintRecord(
    val schemaVersion: Int = BLUEPRINT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val groupCount: Int,
    val blockCount: Int,
    val engine: String = "blockly",
    val engineVersion: String = BLUEPRINT_ENGINE_VERSION,
    val payloadJson: String
)
