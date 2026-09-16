package ch.elekto.blocklyrduino.r4.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BlueprintStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun payload(groups: Int = 1): String = JSONObject()
        .put("schemaVersion", 1)
        .put("groupCount", groups)
        .put("blockCount", groups)
        .put("groups", JSONArray().apply {
            repeat(groups) { index ->
                put(
                    JSONObject()
                        .put("x", index * 100)
                        .put("y", 0)
                        .put("blockState", JSONObject().put("type", "bp_stmt"))
                )
            }
        })
        .toString()

    @Test
    fun createTrimsNameAndPersistsRecord() {
        val file = temp.newFile("blueprints-v1.json")
        file.delete()
        val store = BlueprintStore(file)

        val created = store.create(
            name = "  LED blinken  ",
            payloadJson = payload(),
            groupCount = 1,
            blockCount = 4,
            now = 1000L,
            id = "bp-1"
        )
        val reloaded = BlueprintStore(file).loadAll()

        assertEquals("LED blinken", created.name)
        assertEquals(listOf(created), reloaded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankNameIsRejected() {
        val file = temp.newFile("blueprints-v1.json").apply { delete() }
        BlueprintStore(file).create("   ", payload(), 1, 1)
    }

    @Test
    fun renameChangesOnlyMetadata() {
        val file = temp.newFile("blueprints-v1.json").apply { delete() }
        val store = BlueprintStore(file)
        val created = store.create("Alt", payload(2), 2, 2, now = 1000L, id = "bp-1")
        val renamed = store.rename("bp-1", " Neu ", now = 2000L)

        assertEquals("Neu", renamed.name)
        assertEquals(created.payloadJson, renamed.payloadJson)
        assertEquals(created.createdAt, renamed.createdAt)
        assertEquals(2000L, renamed.updatedAt)
    }

    @Test
    fun deleteRemovesOnlyRequestedRecord() {
        val file = temp.newFile("blueprints-v1.json").apply { delete() }
        val store = BlueprintStore(file)
        store.create("A", payload(), 1, 1, id = "a")
        store.create("B", payload(), 1, 1, id = "b")

        assertTrue(store.delete("a"))
        assertEquals(listOf("b"), store.loadAll().map { it.id })
        assertFalse(store.delete("missing"))
    }

    @Test
    fun corruptFileIsBackedUpAndReturnsEmptyCollection() {
        val file = temp.newFile("blueprints-v1.json")
        file.writeText("not-json-at-all")
        val store = BlueprintStore(file)

        val loaded = store.loadAll()
        val backup = temp.root.resolve("blueprints-v1.corrupt.json")

        assertTrue(loaded.isEmpty())
        assertTrue(backup.exists())
        assertEquals("not-json-at-all", backup.readText())
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedRootSchemaIsRejected() {
        val file = temp.newFile("blueprints-v1.json")
        file.writeText(JSONObject().put("schemaVersion", 99).put("blueprints", JSONArray()).toString())

        BlueprintStore(file).loadAll()
    }
}
