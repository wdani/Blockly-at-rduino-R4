package ch.elekto.blocklyrduino.r4.data

import ch.elekto.blocklyrduino.r4.model.BLUEPRINT_ENGINE_VERSION
import ch.elekto.blocklyrduino.r4.model.BLUEPRINT_SCHEMA_VERSION
import ch.elekto.blocklyrduino.r4.model.BlueprintRecord
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class BlueprintStore(private val file: File) {
    fun loadAll(): List<BlueprintRecord> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        val raw = file.readText()
        if (raw.isBlank()) return emptyList()

        return try {
            val root = JSONObject(raw)
            val rootVersion = root.optInt("schemaVersion", -1)
            if (rootVersion != BLUEPRINT_SCHEMA_VERSION) {
                throw IllegalStateException("Nicht unterstützte Blueprint-Version: $rootVersion")
            }
            val array = root.optJSONArray("blueprints") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    add(decodeRecord(array.getJSONObject(index)))
                }
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (_: Exception) {
            backupCorrupt(raw)
            emptyList()
        }
    }

    fun create(
        name: String,
        payloadJson: String,
        groupCount: Int,
        blockCount: Int,
        now: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString()
    ): BlueprintRecord {
        val cleanName = normalizeName(name)
        require(groupCount >= 1) { "Ein Blueprint braucht mindestens eine Gruppe." }
        require(blockCount >= 1) { "Ein Blueprint braucht mindestens einen Block." }
        val payload = validatePayload(payloadJson)

        val record = BlueprintRecord(
            id = id,
            name = cleanName,
            createdAt = now,
            updatedAt = now,
            groupCount = groupCount,
            blockCount = blockCount,
            payloadJson = payload.toString()
        )
        val records = loadAll().toMutableList()
        records.add(record)
        writeAll(records)
        return record
    }

    fun rename(
        id: String,
        newName: String,
        now: Long = System.currentTimeMillis()
    ): BlueprintRecord {
        val cleanName = normalizeName(newName)
        val records = loadAll().toMutableList()
        val index = records.indexOfFirst { it.id == id }
        require(index >= 0) { "Blueprint nicht gefunden: $id" }
        val renamed = records[index].copy(name = cleanName, updatedAt = now)
        records[index] = renamed
        writeAll(records)
        return renamed
    }

    fun delete(id: String): Boolean {
        val records = loadAll().toMutableList()
        val changed = records.removeAll { it.id == id }
        if (!changed) return false
        writeAll(records)
        return true
    }

    private fun normalizeName(name: String): String {
        val clean = name.trim()
        require(clean.isNotBlank()) { "Blueprint-Name darf nicht leer sein." }
        return clean
    }

    private fun validatePayload(payloadJson: String): JSONObject {
        val payload = JSONObject(payloadJson)
        val version = payload.optInt("schemaVersion", -1)
        require(version == BLUEPRINT_SCHEMA_VERSION) {
            "Nicht unterstützte Blueprint-Payload-Version: $version"
        }
        return payload
    }

    private fun decodeRecord(json: JSONObject): BlueprintRecord {
        val version = json.optInt("schemaVersion", -1)
        if (version != BLUEPRINT_SCHEMA_VERSION) {
            throw IllegalStateException("Nicht unterstützte Blueprint-Version: $version")
        }
        val payload = json.getJSONObject("payload")
        validatePayload(payload.toString())
        return BlueprintRecord(
            schemaVersion = version,
            id = json.getString("id"),
            name = normalizeName(json.getString("name")),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            groupCount = json.getInt("groupCount"),
            blockCount = json.getInt("blockCount"),
            engine = json.optString("engine", "blockly"),
            engineVersion = json.optString("engineVersion", BLUEPRINT_ENGINE_VERSION),
            payloadJson = payload.toString()
        )
    }

    private fun writeAll(records: List<BlueprintRecord>) {
        file.parentFile?.mkdirs()
        val root = JSONObject()
            .put("schemaVersion", BLUEPRINT_SCHEMA_VERSION)
            .put("blueprints", JSONArray().apply {
                records.forEach { put(encodeRecord(it)) }
            })
        val temp = File(file.parentFile ?: File("."), file.name + ".tmp")
        temp.writeText(root.toString())

        try {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun encodeRecord(record: BlueprintRecord): JSONObject = JSONObject()
        .put("schemaVersion", record.schemaVersion)
        .put("id", record.id)
        .put("name", record.name)
        .put("createdAt", record.createdAt)
        .put("updatedAt", record.updatedAt)
        .put("groupCount", record.groupCount)
        .put("blockCount", record.blockCount)
        .put("engine", record.engine)
        .put("engineVersion", record.engineVersion)
        .put("payload", JSONObject(record.payloadJson))

    private fun backupCorrupt(raw: String) {
        val stem = file.name.substringBeforeLast('.', file.name)
        val backup = File(file.parentFile ?: File("."), "$stem.corrupt.json")
        runCatching { backup.writeText(raw) }
    }
}
