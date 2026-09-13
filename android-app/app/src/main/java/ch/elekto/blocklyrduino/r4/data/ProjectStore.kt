package ch.elekto.blocklyrduino.r4.data

import android.content.Context
import ch.elekto.blocklyrduino.r4.model.BlockType
import ch.elekto.blocklyrduino.r4.model.ProgramBlock
import ch.elekto.blocklyrduino.r4.model.defaultBlinkProject
import ch.elekto.blocklyrduino.r4.model.normalizeProjectLayout
import org.json.JSONArray
import org.json.JSONObject

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("elekto_native_editor", Context.MODE_PRIVATE)

    fun loadBlocks(): List<ProgramBlock> {
        if (!prefs.contains(KEY_BLOCKS)) return normalizeProjectLayout(defaultBlinkProject())
        val raw = prefs.getString(KEY_BLOCKS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val type = BlockType.valueOf(item.getString("type"))
                    add(
                        ProgramBlock(
                            id = item.getString("id"),
                            type = type,
                            xDp = item.optDouble("xDp", 40.0).toFloat(),
                            yDp = item.optDouble("yDp", 40.0).toFloat(),
                            primary = item.optInt("primary", type.defaultPrimary),
                            secondary = item.optInt("secondary", type.defaultSecondary),
                            flag = item.optBoolean("flag", type.defaultFlag),
                            parentId = item.optString("parentId", "").takeIf { it.isNotBlank() },
                            childOrder = item.optInt("childOrder", 0),
                            previousId = item.optString("previousId", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }.let(::normalizeProjectLayout)
        }.getOrElse { normalizeProjectLayout(defaultBlinkProject()) }
    }

    fun saveBlocks(blocks: List<ProgramBlock>) {
        val array = JSONArray()
        normalizeProjectLayout(blocks).forEach { block ->
            array.put(
                JSONObject()
                    .put("id", block.id)
                    .put("type", block.type.name)
                    .put("xDp", block.xDp.toDouble())
                    .put("yDp", block.yDp.toDouble())
                    .put("primary", block.primary)
                    .put("secondary", block.secondary)
                    .put("flag", block.flag)
                    .put("parentId", block.parentId ?: "")
                    .put("childOrder", block.childOrder)
                    .put("previousId", block.previousId ?: "")
            )
        }
        prefs.edit().putString(KEY_BLOCKS, array.toString()).apply()
    }

    companion object {
        // Keep the same key so Alpha 5 projects migrate in-place. New relation fields are optional.
        private const val KEY_BLOCKS = "program_blocks_v1"
    }
}
