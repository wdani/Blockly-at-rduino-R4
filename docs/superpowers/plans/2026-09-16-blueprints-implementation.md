# Elekto Blocks Blueprint-System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fully local Blueprint system to Hybrid 14 so users can save one or more disconnected Blockly groups as a named reusable template, browse them under `Blöcke | Blueprints`, reinsert them with preserved relative layout, rename them, and delete them.

**Architecture:** Blockly remains responsible for graph selection, serialization, insertion, and temporary visual highlighting. Android owns the persistent Blueprint collection, naming dialogs, selection toolbar, browser cards, and management actions. A small bridge exchanges only semantic IDs, selection counts, and versioned JSON payloads; Android never manipulates Blockly SVG connections or connection objects directly.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android WebView bridge, Blockly 13.3.0, JavaScript/CommonJS tests with Node 22, Android/JVM unit tests with JUnit 4, `org.json`, internal app file storage.

**Spec:** `docs/superpowers/specs/2026-09-16-blueprints-design.md`

## Global Constraints

- Blueprint data must remain fully local/offline; no account, server, cloud sync, or network dependency.
- A Blueprint may contain one connected Blockly group or multiple disconnected groups.
- Tapping any block in Blueprint-selection mode selects or deselects the entire connected root group, including nested input blocks, statement blocks, next blocks, and shadow blocks.
- Shadow blocks are never independent Blueprint groups.
- Relative positions between selected disconnected groups must be preserved; absolute workspace coordinates must not be stored.
- Blueprints are copied templates, not live links. Editing an inserted copy must never mutate the stored Blueprint.
- Stored block IDs must not be reused when a Blueprint is inserted; inserted blocks receive fresh Blockly IDs.
- The native Android side must not manipulate Blockly SVG paths, Blockly connection objects, or block graph internals.
- Blockly version stays pinned to `13.3.0` for this implementation.
- Android remains `minSdk 26`, `targetSdk 35`, Java/Kotlin JVM target `17`.
- Blueprint display names are trimmed and must not be blank; duplicate display names are allowed because records use unique IDs.
- The existing Hybrid 14 block context actions, preview system, drag-to-delete behavior, undo/redo, theme handling, and current block catalogue must continue to work outside Blueprint-selection mode.
- The future category-browser redesign is explicitly out of scope for this plan; only the top-level `Blöcke | Blueprints` split is added now.
- No Blueprint cloud library, file import/export, sharing, folders, favorites, search, or preview image generation in this version.

---

## File Structure

### New files

- `android-app/app/src/main/assets/blockly/blueprint-engine.js` — pure/testable Blockly Blueprint graph logic: group resolution, selection state, serialization, normalization, insertion, rollback.
- `android-app/app/src/main/assets/blockly/blueprint-controller.js` — browser/WebView interaction layer: pointer interception, highlight classes, selection-mode lifecycle, Android bridge callbacks.
- `android-app/tests/blueprint-engine.test.cjs` — Node tests for graph selection, serialization, ID stripping, normalized coordinates, insertion, rollback.
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/model/Blueprint.kt` — Blueprint record and constants.
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/data/BlueprintStore.kt` — versioned JSON-file persistence with temp-file replacement and corrupt-file backup.
- `android-app/app/src/test/java/ch/elekto/blocklyrduino/r4/data/BlueprintStoreTest.kt` — JVM tests for save/load, rename, delete, validation, corruption handling.
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt` — Compose UI for selection toolbar, naming dialog, Blueprint cards, empty state, rename/delete controls.

### Modified files

- `android-app/app/src/main/assets/blockly/index.html` — load Blueprint scripts before block-context script.
- `android-app/app/src/main/assets/blockly/block-context.js` — expose `Als Blueprint speichern` through existing native block menu flow and avoid long-press behavior while selection mode is active.
- `android-app/app/src/main/assets/blockly/styles.css` — temporary selection highlight style only.
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt` — bridge callbacks, store lifecycle, selection state, naming, browser integration, insert/rename/delete wiring.
- `android-app/app/build.gradle` — JVM test dependencies and Hybrid 15 version bump.
- `.github/workflows/build-android.yml` — run both JavaScript test files and Android unit tests before assembling Hybrid 15; publish artifact as `Elekto-Blocks-Hybrid-15`.

---

### Task 1: Build the pure Blockly Blueprint engine

**Files:**
- Create: `android-app/tests/blueprint-engine.test.cjs`
- Create: `android-app/app/src/main/assets/blockly/blueprint-engine.js`

**Interfaces:**
- Consumes: global `Blockly` 13.3.0.
- Produces:
  - `ElektoBlueprintEngine.createSelection(workspace, initialBlockId)` → selection object.
  - `selection.toggleByBlockId(blockId)` → `{ selectedRootIds: string[], groupCount: number }`.
  - `selection.getSelectedRootIds()` → `string[]`.
  - `selection.serialize()` → payload object `{ schemaVersion, groupCount, blockCount, groups }`.
  - `ElektoBlueprintEngine.insert(workspace, payload, anchorX, anchorY)` → `{ rootIds: string[] }`.
  - `ElektoBlueprintEngine.resolveRoot(workspace, blockId)` → root block or `null`.

- [ ] **Step 1: Write the failing graph-selection tests**

Create `android-app/tests/blueprint-engine.test.cjs` with Blockly test blocks that cover a statement chain, nested statement input, value input, and shadow value. Start with these tests:

```js
const test = require('node:test');
const assert = require('node:assert/strict');
const Blockly = require('../blockly-package/node_modules/blockly');

globalThis.Blockly = Blockly;

let blueprints = null;
try {
  blueprints = require('../app/src/main/assets/blockly/blueprint-engine.js');
} catch (_) {}

Blockly.defineBlocksWithJsonArray([
  { type: 'bp_stmt', message0: 'stmt', previousStatement: null, nextStatement: null, colour: 210 },
  {
    type: 'bp_container', message0: 'container',
    message1: '%1', args1: [{ type: 'input_statement', name: 'DO' }],
    previousStatement: null, nextStatement: null, colour: 160
  },
  {
    type: 'bp_value_owner', message0: 'owner %1',
    args0: [{ type: 'input_value', name: 'VALUE', check: 'Number' }],
    previousStatement: null, nextStatement: null, colour: 120
  }
]);

function number(workspace, value, shadow = false) {
  const block = workspace.newBlock('math_number');
  block.setFieldValue(String(value), 'NUM');
  block.setShadow(shadow);
  return block;
}

function makeConnectedTree(workspace) {
  const root = workspace.newBlock('bp_container');
  const child = workspace.newBlock('bp_value_owner');
  const tail = workspace.newBlock('bp_stmt');
  const shadow = number(workspace, 42, true);
  root.getInput('DO').connection.connect(child.previousConnection);
  child.getInput('VALUE').connection.connect(shadow.outputConnection);
  child.nextConnection.connect(tail.previousConnection);
  return { root, child, tail, shadow };
}

test('module exists', () => {
  assert.ok(blueprints, 'blueprint-engine.js muss implementiert sein');
});

test('any block in a connected tree resolves to the same root group', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const { root, child, tail, shadow } = makeConnectedTree(ws);
  for (const block of [root, child, tail, shadow]) {
    assert.equal(blueprints.resolveRoot(ws, block.id).id, root.id);
  }
  ws.dispose();
});

test('selection starts with the initial connected group and toggles a second group', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const first = makeConnectedTree(ws);
  const second = ws.newBlock('bp_stmt');
  const selection = blueprints.createSelection(ws, first.child.id);

  assert.deepEqual(selection.getSelectedRootIds(), [first.root.id]);
  selection.toggleByBlockId(second.id);
  assert.deepEqual(new Set(selection.getSelectedRootIds()), new Set([first.root.id, second.id]));
  selection.toggleByBlockId(first.tail.id);
  assert.deepEqual(selection.getSelectedRootIds(), [second.id]);
  ws.dispose();
});
```

- [ ] **Step 2: Run the new tests and verify RED**

Run:

```bash
npm install --prefix android-app/blockly-package --no-save --ignore-scripts blockly@13.3.0
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: FAIL because `blueprint-engine.js` does not exist.

- [ ] **Step 3: Implement minimal root resolution and selection state**

Create a UMD/CommonJS-compatible module following the existing `block-actions.js` pattern. The core implementation must use Blockly graph APIs only:

```js
function resolveRoot(workspace, blockId) {
  let block = workspace?.getBlockById?.(blockId) || null;
  if (!block) return null;
  if (block.isShadow?.()) block = block.getParent?.() || block;
  return block.getRootBlock?.() || block;
}

function createSelection(workspace, initialBlockId) {
  const selected = new Set();
  const first = resolveRoot(workspace, initialBlockId);
  if (first) selected.add(first.id);

  return {
    toggleByBlockId(blockId) {
      const root = resolveRoot(workspace, blockId);
      if (!root) return { selectedRootIds: [...selected], groupCount: selected.size };
      if (selected.has(root.id)) selected.delete(root.id);
      else selected.add(root.id);
      return { selectedRootIds: [...selected], groupCount: selected.size };
    },
    getSelectedRootIds() { return [...selected]; }
  };
}
```

- [ ] **Step 4: Run tests and verify GREEN for selection**

Run:

```bash
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: selection tests PASS.

- [ ] **Step 5: Add failing serialization/normalization/ID tests**

Extend the same test file with:

```js
test('serialize keeps relative group layout, counts all descendants, and strips block ids', () => {
  const ws = new Blockly.Workspace();
  const first = makeConnectedTree(ws);
  const second = ws.newBlock('bp_stmt');
  first.root.moveBy(100, 80);
  second.moveBy(340, 200);
  const originalIds = new Set(ws.getAllBlocks(false).map(b => b.id));

  const selection = blueprints.createSelection(ws, first.root.id);
  selection.toggleByBlockId(second.id);
  const payload = selection.serialize();

  assert.equal(payload.schemaVersion, 1);
  assert.equal(payload.groupCount, 2);
  assert.equal(payload.blockCount, 5);
  assert.deepEqual(payload.groups.map(g => [g.x, g.y]), [[0, 0], [240, 120]]);
  const raw = JSON.stringify(payload);
  for (const id of originalIds) assert.equal(raw.includes(id), false);
  ws.dispose();
});

test('insert recreates all groups with fresh ids and preserved relative positions', () => {
  const source = new Blockly.Workspace();
  const first = makeConnectedTree(source);
  const second = source.newBlock('bp_stmt');
  first.root.moveBy(20, 30);
  second.moveBy(220, 130);
  const oldIds = new Set(source.getAllBlocks(false).map(b => b.id));
  const selection = blueprints.createSelection(source, first.root.id);
  selection.toggleByBlockId(second.id);
  const payload = selection.serialize();
  source.dispose();

  const target = new Blockly.Workspace();
  const result = blueprints.insert(target, payload, 500, 300);
  assert.equal(result.rootIds.length, 2);
  const roots = result.rootIds.map(id => target.getBlockById(id));
  const points = roots.map(b => b.getRelativeToSurfaceXY());
  assert.deepEqual(points.map(p => [p.x, p.y]), [[500, 300], [700, 400]]);
  for (const b of target.getAllBlocks(false)) assert.equal(oldIds.has(b.id), false);
  target.dispose();
});
```

Also add a rollback test using a deliberately invalid second group state and assert that the target workspace contains zero newly created blocks after the thrown error.

- [ ] **Step 6: Run serialization tests and verify RED**

Run:

```bash
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: FAIL because `serialize()` and `insert()` are not implemented.

- [ ] **Step 7: Implement serialization, ID stripping, coordinate normalization, insertion, and rollback**

Implementation rules:

```js
function deepClone(value) {
  return JSON.parse(JSON.stringify(value));
}

function stripIds(value) {
  if (Array.isArray(value)) return value.map(stripIds);
  if (!value || typeof value !== 'object') return value;
  const out = {};
  for (const [key, child] of Object.entries(value)) {
    if (key !== 'id') out[key] = stripIds(child);
  }
  return out;
}
```

For each selected root:

```js
const point = root.getRelativeToSurfaceXY();
const blockState = stripIds(Blockly.serialization.blocks.save(root));
const blockCount = root.getDescendants(false).length;
```

Normalize all root positions using the minimum X/Y across selected roots. `groups` must preserve selection order and contain `{ x, y, blockState }`.

For insertion, record `beforeIds`, call `Blockly.serialization.blocks.append(deepClone(group.blockState), workspace, { recordUndo: true })`, find the newly created root by comparing workspace IDs before/after, and move that root to `anchorX + group.x`, `anchorY + group.y`. If any group fails, dispose every block whose ID was not in `beforeIds`, then rethrow.

- [ ] **Step 8: Run all Node tests**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
```

Expected: all tests PASS with zero failures.

- [ ] **Step 9: Commit Task 1**

```bash
git add android-app/tests/blueprint-engine.test.cjs android-app/app/src/main/assets/blockly/blueprint-engine.js
git commit -m "feat(blueprints): add tested Blockly blueprint engine"
```

---

### Task 2: Add the WebView Blueprint-selection controller and visual highlighting

**Files:**
- Create: `android-app/app/src/main/assets/blockly/blueprint-controller.js`
- Modify: `android-app/app/src/main/assets/blockly/index.html`
- Modify: `android-app/app/src/main/assets/blockly/block-context.js`
- Modify: `android-app/app/src/main/assets/blockly/styles.css`

**Interfaces:**
- Consumes: `window.ElektoBlueprintEngine`, `window.Elekto`, `window.ElektoAndroid`.
- Produces on `window.Elekto`:
  - `startBlueprintSelection(blockId)`
  - `requestBlueprintPayload()`
  - `cancelBlueprintSelection()`
  - `commitBlueprintSelection()`
  - `insertBlueprint(payloadJson)`
  - `isBlueprintSelectionActive()`
- Calls Android bridge:
  - `setBlueprintSelection(active: Boolean, groupCount: Int)`
  - `blueprintPayloadReady(payloadJson: String)`

- [ ] **Step 1: Write a lightweight controller contract test**

Extend `android-app/tests/blueprint-engine.test.cjs` with a pure helper exported from the engine for selected-root descendant IDs:

```js
test('selectedBlockIds includes the whole connected group including shadows', () => {
  const ws = new Blockly.Workspace();
  const tree = makeConnectedTree(ws);
  const selection = blueprints.createSelection(ws, tree.tail.id);
  const ids = new Set(selection.getSelectedBlockIds());
  for (const block of [tree.root, tree.child, tree.tail, tree.shadow]) {
    assert.equal(ids.has(block.id), true);
  }
  ws.dispose();
});
```

- [ ] **Step 2: Run test and verify RED**

Run:

```bash
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: FAIL because `getSelectedBlockIds()` is missing.

- [ ] **Step 3: Add `getSelectedBlockIds()` to the engine and verify GREEN**

Implement by collecting `root.getDescendants(false)` for every selected root and de-duplicating IDs.

Run:

```bash
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: PASS.

- [ ] **Step 4: Implement `blueprint-controller.js`**

Controller state:

```js
let selection = null;
let pointerDown = null;

function active() { return !!selection; }
```

Required behavior:

- `startBlueprintSelection(blockId)` creates engine selection and immediately reports `setBlueprintSelection(true, 1)` if the block exists.
- While active, capture `pointerdown`, `pointermove`, `pointerup`, and `pointercancel` before normal Blockly gesture handlers.
- `pointerdown` on a block records the block ID and coordinates, then calls `preventDefault()`, `stopPropagation()`, and `stopImmediatePropagation()`.
- `pointerup` toggles the entire connected group only when movement is below 14 px.
- After each toggle, call `refreshHighlight()` and `setBlueprintSelection(true, groupCount)`.
- `requestBlueprintPayload()` serializes current selection and calls `blueprintPayloadReady(JSON.stringify(payload))` without leaving selection mode.
- `cancelBlueprintSelection()` removes all highlight classes, resets state, and calls `setBlueprintSelection(false, 0)`.
- `commitBlueprintSelection()` performs the same cleanup only after Android has successfully stored the Blueprint.
- `insertBlueprint(payloadJson)` parses JSON, finds a visible insertion anchor from workspace metrics, calls engine insertion inside one Blockly event group, selects the first inserted root, and resizes the workspace.
- On insertion error, log it and rethrow so Android can show a controlled error.

- [ ] **Step 5: Add temporary highlight CSS**

Append to `styles.css`:

```css
.elekto-blueprint-selected .blocklyPath {
  filter: drop-shadow(0 0 5px rgba(80, 170, 255, .95));
  stroke: #55aaff !important;
  stroke-width: 3px !important;
}
```

`refreshHighlight()` must first remove `.elekto-blueprint-selected` from every previously highlighted Blockly SVG root, then add it to each block ID returned by `selection.getSelectedBlockIds()`.

- [ ] **Step 6: Load Blueprint scripts in deterministic order**

Modify `index.html` so the relevant order is:

```html
<script-loader order>
blockly_compressed.js
blocks_compressed.js
msg/de.js
block-actions.js
blueprint-engine.js
elekto.js
blueprint-controller.js
block-context.js
preview-fix.js
preview-h13.js
```

Keep the existing dynamic `load()` mechanism; only add the new files in this order.

- [ ] **Step 7: Prevent the normal long-press menu while Blueprint selection is active**

At the start of the `pointerdown` handler in `block-context.js`, add:

```js
if (window.Elekto?.isBlueprintSelectionActive?.()) return;
```

Do not change any existing Hybrid 14 action behavior outside selection mode.

- [ ] **Step 8: Run all Node tests**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
```

Expected: PASS.

- [ ] **Step 9: Commit Task 2**

```bash
git add android-app/app/src/main/assets/blockly/blueprint-controller.js \
  android-app/app/src/main/assets/blockly/index.html \
  android-app/app/src/main/assets/blockly/block-context.js \
  android-app/app/src/main/assets/blockly/styles.css \
  android-app/app/src/main/assets/blockly/blueprint-engine.js \
  android-app/tests/blueprint-engine.test.cjs
git commit -m "feat(blueprints): add mobile blueprint selection mode"
```

---

### Task 3: Add the native Blueprint data model and durable file store

**Files:**
- Create: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/model/Blueprint.kt`
- Create: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/data/BlueprintStore.kt`
- Create: `android-app/app/src/test/java/ch/elekto/blocklyrduino/r4/data/BlueprintStoreTest.kt`
- Modify: `android-app/app/build.gradle`

**Interfaces:**
- Produces:
  - `data class BlueprintRecord(...)`
  - `class BlueprintStore(private val file: File)`
  - `fun loadAll(): List<BlueprintRecord>`
  - `fun create(name: String, payloadJson: String, groupCount: Int, blockCount: Int, now: Long = ..., id: String = ...): BlueprintRecord`
  - `fun rename(id: String, newName: String, now: Long = ...): BlueprintRecord`
  - `fun delete(id: String): Boolean`
- File path in app code: `File(context.filesDir, "blueprints-v1.json")`.

- [ ] **Step 1: Add JVM test dependencies**

In `android-app/app/build.gradle` add:

```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.json:json:20240303'
```

Do not change production dependencies.

- [ ] **Step 2: Write failing store tests**

Create `BlueprintStoreTest.kt` using JUnit `TemporaryFolder`:

```kotlin
@get:Rule
val temp = TemporaryFolder()

private fun payload(groups: Int = 1) = JSONObject()
    .put("schemaVersion", 1)
    .put("groupCount", groups)
    .put("blockCount", groups)
    .put("groups", JSONArray().apply {
        repeat(groups) {
            put(JSONObject().put("x", it * 100).put("y", 0).put("blockState", JSONObject()))
        }
    }).toString()

@Test
fun createTrimsNameAndPersistsRecord() {
    val file = temp.newFile("blueprints-v1.json")
    file.delete()
    val store = BlueprintStore(file)

    val created = store.create("  LED blinken  ", payload(), 1, 4, now = 1000L, id = "bp-1")
    val reloaded = BlueprintStore(file).loadAll()

    assertEquals("LED blinken", created.name)
    assertEquals(listOf(created), reloaded)
}

@Test(expected = IllegalArgumentException::class)
fun blankNameIsRejected() {
    BlueprintStore(temp.newFile()).create("   ", payload(), 1, 1)
}

@Test
fun renameChangesOnlyMetadata() {
    val file = temp.newFile("bp.json").apply { delete() }
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
    val file = temp.newFile("bp.json").apply { delete() }
    val store = BlueprintStore(file)
    store.create("A", payload(), 1, 1, id = "a")
    store.create("B", payload(), 1, 1, id = "b")
    assertTrue(store.delete("a"))
    assertEquals(listOf("b"), store.loadAll().map { it.id })
}
```

Add a corruption test: write invalid text to the data file, call `loadAll()`, assert an empty list is returned and a sibling file named `blueprints-v1.corrupt.json` exists containing the original invalid text.

- [ ] **Step 3: Run store tests and verify RED**

Run:

```bash
gradle -p android-app :app:testDebugUnitTest --tests '*BlueprintStoreTest'
```

Expected: FAIL because `BlueprintRecord` and `BlueprintStore` do not exist.

- [ ] **Step 4: Implement `BlueprintRecord`**

Use:

```kotlin
data class BlueprintRecord(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val groupCount: Int,
    val blockCount: Int,
    val engine: String = "blockly",
    val engineVersion: String = "13.3.0",
    val payloadJson: String
)
```

Also define `const val BLUEPRINT_SCHEMA_VERSION = 1` and `const val BLUEPRINT_ENGINE_VERSION = "13.3.0"` in the same file.

- [ ] **Step 5: Implement `BlueprintStore` with versioned JSON and temp-file replacement**

File structure:

```json
{
  "schemaVersion": 1,
  "blueprints": [
    {
      "schemaVersion": 1,
      "id": "bp-1",
      "name": "LED blinken",
      "createdAt": 1000,
      "updatedAt": 1000,
      "groupCount": 1,
      "blockCount": 4,
      "engine": "blockly",
      "engineVersion": "13.3.0",
      "payload": { "schemaVersion": 1, "groups": [] }
    }
  ]
}
```

Rules:

- `create()` validates a nonblank trimmed name and validates `JSONObject(payloadJson)` before modifying the collection.
- `rename()` validates a nonblank trimmed name; if the ID does not exist, throw `IllegalArgumentException("Blueprint nicht gefunden: $id")`.
- `delete()` returns `false` when the ID does not exist.
- `loadAll()` returns `emptyList()` when the file does not exist or is blank.
- Unsupported root schema versions throw a clear exception rather than silently interpreting future data.
- Invalid/corrupt JSON is copied to `blueprints-v1.corrupt.json` before returning `emptyList()`.
- Every write goes to `<name>.tmp`, flushes/closes it, then replaces the target file. A failed replacement must leave the previous target intact whenever possible.

- [ ] **Step 6: Run store tests and verify GREEN**

Run:

```bash
gradle -p android-app :app:testDebugUnitTest --tests '*BlueprintStoreTest'
```

Expected: PASS.

- [ ] **Step 7: Run the full existing test set**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
gradle -p android-app :app:testDebugUnitTest
```

Expected: all PASS.

- [ ] **Step 8: Commit Task 3**

```bash
git add android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/model/Blueprint.kt \
  android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/data/BlueprintStore.kt \
  android-app/app/src/test/java/ch/elekto/blocklyrduino/r4/data/BlueprintStoreTest.kt \
  android-app/app/build.gradle
git commit -m "feat(blueprints): add local blueprint store"
```

---

### Task 4: Add native selection-mode UI and Blueprint naming flow

**Files:**
- Create: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt`
- Modify: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt`

**Interfaces:**
- `BlueprintSelectionBar(groupCount, onCancel, onCreate)`.
- `BlueprintNameDialog(initialName, title, confirmLabel, onDismiss, onConfirm)`.
- Android bridge additions:
  - `setBlueprintSelection(active: Boolean, groupCount: Int)`.
  - `blueprintPayloadReady(payloadJson: String)`.

- [ ] **Step 1: Introduce explicit native Blueprint UI state in `MainActivity.kt`**

Add state equivalent to:

```kotlin
var blueprintSelectionActive by remember { mutableStateOf(false) }
var blueprintGroupCount by remember { mutableStateOf(0) }
var pendingBlueprintPayload by remember { mutableStateOf<String?>(null) }
var showBlueprintNameDialog by remember { mutableStateOf(false) }
```

Create the store once from application context:

```kotlin
val context = LocalContext.current
val blueprintStore = remember(context) {
    BlueprintStore(File(context.filesDir, "blueprints-v1.json"))
}
var blueprints by remember { mutableStateOf(blueprintStore.loadAll()) }
```

Do not store the Blueprint collection in WebView `localStorage`.

- [ ] **Step 2: Extend `ElektoBridge`**

Add callbacks and `@JavascriptInterface` methods:

```kotlin
@JavascriptInterface
fun setBlueprintSelection(active: Boolean, groupCount: Int) =
    mainHandler.post { onBlueprintSelection(active, groupCount) }

@JavascriptInterface
fun blueprintPayloadReady(payloadJson: String) =
    mainHandler.post { onBlueprintPayload(payloadJson) }
```

When selection starts, force `isDraggingBlock = false` so the normal delete target cannot remain visible.

- [ ] **Step 3: Add `Als Blueprint speichern` to the existing Block Options sheet**

Extend `BlockContextSheet` with a normal, non-danger action:

```kotlin
BlockActionCard(
    title = "Als Blueprint speichern",
    description = "Wählt diesen verbundenen Stapel aus. Danach kannst du weitere Stapel hinzufügen.",
    onClick = onStartBlueprint
)
```

Wiring in `MainActivity.kt`:

```kotlin
val id = JSONObject.quote(state.id)
webView?.evaluateJavascript("window.Elekto?.startBlueprintSelection($id)", null)
blockMenu = null
```

- [ ] **Step 4: Create `BlueprintSelectionBar`**

It must be fixed above the bottom edge and replace/cover normal editing controls enough to make selection mode obvious. Required visible text:

- `Blueprint auswählen`
- `1 Gruppe ausgewählt` or `${groupCount} Gruppen ausgewählt`
- `Abbrechen`
- `Blueprint erstellen`

`Blueprint erstellen` is disabled at `groupCount == 0`.

Callbacks:

```kotlin
onCancel = {
    webView?.evaluateJavascript("window.Elekto?.cancelBlueprintSelection()", null)
}

onCreate = {
    webView?.evaluateJavascript("window.Elekto?.requestBlueprintPayload()", null)
}
```

- [ ] **Step 5: Create the naming dialog and save only after a valid payload arrives**

When `blueprintPayloadReady(payloadJson)` fires:

1. Parse it with `JSONObject`.
2. Validate `schemaVersion == 1`, `groupCount >= 1`, and `blockCount >= 1`.
3. Set `pendingBlueprintPayload`.
4. Open `BlueprintNameDialog`.

On dialog cancel: close only the dialog and keep Blueprint-selection mode active.

On confirm:

```kotlin
val payload = requireNotNull(pendingBlueprintPayload)
val json = JSONObject(payload)
val saved = blueprintStore.create(
    name = name,
    payloadJson = payload,
    groupCount = json.getInt("groupCount"),
    blockCount = json.getInt("blockCount")
)
blueprints = blueprintStore.loadAll()
pendingBlueprintPayload = null
showBlueprintNameDialog = false
webView?.evaluateJavascript("window.Elekto?.commitBlueprintSelection()", null)
```

If persistence throws, keep selection mode active and show an error dialog/snackbar; do not call `commitBlueprintSelection()`.

- [ ] **Step 6: Build to catch Compose/bridge integration errors**

Run:

```bash
gradle -p android-app :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run regression tests**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
gradle -p android-app :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 8: Commit Task 4**

```bash
git add android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt \
  android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt
git commit -m "feat(blueprints): add native selection and save flow"
```

---

### Task 5: Add `Blöcke | Blueprints` browser, insertion, rename, and delete

**Files:**
- Modify: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt`
- Modify: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt`

**Interfaces:**
- `BlueprintBrowserList(blueprints, onInsert, onRename, onDelete)`.
- Existing `BlockCatalogSheet` gains parameters for the Blueprint list and callbacks.

- [ ] **Step 1: Add a top-level browser mode**

Inside `BlockCatalogSheet`, replace the single implicit block view with:

```kotlin
enum class CatalogMode { BLOCKS, BLUEPRINTS }
var mode by rememberSaveable { mutableStateOf(CatalogMode.BLOCKS) }
```

Show two large top-level choices labeled exactly:

- `Blöcke`
- `Blueprints`

When `mode == BLOCKS`, keep the existing Hybrid 14 category chips and cards unchanged.

When `mode == BLUEPRINTS`, do not render category chips.

- [ ] **Step 2: Implement Blueprint empty state and cards**

For an empty list show:

```text
Noch keine Blueprints
Halte einen Block auf der Arbeitsfläche gedrückt und wähle „Als Blueprint speichern“.
```

Each Blueprint card shows:

- name;
- `${groupCount} Gruppe(n)`;
- `${blockCount} Blöcke`;
- primary card tap inserts;
- separate small menu/action area for `Umbenennen` and `Löschen` so management cannot be triggered accidentally by a normal card tap.

- [ ] **Step 3: Wire insertion through the adapter**

On card tap:

```kotlin
val payload = JSONObject.quote(record.payloadJson)
webView?.evaluateJavascript("window.Elekto?.insertBlueprint($payload)", null)
showBlocks = false
```

Do not delete or mutate the stored record after insertion.

- [ ] **Step 4: Wire rename with the shared name dialog**

Open `BlueprintNameDialog(initialName = record.name, title = "Blueprint umbenennen", confirmLabel = "Speichern")`.

On confirm:

```kotlin
blueprintStore.rename(record.id, newName)
blueprints = blueprintStore.loadAll()
```

Payload JSON must remain byte-for-byte unchanged by rename; the store unit test from Task 3 enforces this.

- [ ] **Step 5: Wire delete with confirmation**

Before deletion show a confirmation dialog:

```text
Blueprint löschen?
„<Name>“ wird dauerhaft von diesem Gerät gelöscht.
```

Confirm calls:

```kotlin
blueprintStore.delete(record.id)
blueprints = blueprintStore.loadAll()
```

Cancel performs no mutation.

- [ ] **Step 6: Add a JS regression test proving stored payload independence**

Extend `blueprint-engine.test.cjs`:

```js
test('serialized payload remains usable after original blocks are deleted', () => {
  const ws = new Blockly.Workspace();
  const tree = makeConnectedTree(ws);
  const originalIds = new Set(ws.getAllBlocks(false).map(b => b.id));
  const payload = blueprints.createSelection(ws, tree.root.id).serialize();
  tree.root.dispose(false);
  assert.equal(ws.getAllBlocks(false).length, 0);

  const result = blueprints.insert(ws, payload, 100, 100);
  assert.equal(result.rootIds.length, 1);
  assert.ok(ws.getAllBlocks(false).length > 0);
  for (const block of ws.getAllBlocks(false)) assert.equal(originalIds.has(block.id), false);
  ws.dispose();
});
```

- [ ] **Step 7: Run tests and build**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
gradle -p android-app :app:testDebugUnitTest
gradle -p android-app :app:assembleDebug
```

Expected: all tests PASS and build succeeds.

- [ ] **Step 8: Commit Task 5**

```bash
git add android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt \
  android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt \
  android-app/tests/blueprint-engine.test.cjs
git commit -m "feat(blueprints): add blueprint browser and management"
```

---

### Task 6: Harden selection/insertion failure paths and preserve normal editor behavior

**Files:**
- Modify: `android-app/app/src/main/assets/blockly/blueprint-engine.js`
- Modify: `android-app/app/src/main/assets/blockly/blueprint-controller.js`
- Modify: `android-app/tests/blueprint-engine.test.cjs`
- Modify: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt`

**Interfaces:**
- Keeps all previous public APIs unchanged.

- [ ] **Step 1: Add failing tests for removed selected roots and rollback**

Add tests that:

1. create a selection, dispose the selected root externally, then call `serialize()` and expect the vanished group to be pruned rather than crash;
2. insert a two-group payload whose second group is invalid and assert that no blocks from the first group remain after failure;
3. call `createSelection()` with an unknown block ID and assert selection count is zero rather than an exception.

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
node --test android-app/tests/blueprint-engine.test.cjs
```

Expected: at least one of the new failure-path tests FAIL.

- [ ] **Step 3: Implement pruning and transactional rollback**

Before `getSelectedRootIds()`, `getSelectedBlockIds()`, and `serialize()`, prune IDs for which `workspace.getBlockById(id)` returns `null`.

Insertion rollback must compare against the original `beforeIds` and dispose every newly created top/root block so nested descendants are removed once, then rethrow the original error.

- [ ] **Step 4: Add controlled Android error feedback**

`MainActivity.kt` should maintain `var errorMessage by remember { mutableStateOf<String?>(null) }` and display a simple `AlertDialog` for:

- invalid payload received from WebView;
- store failure;
- unsupported Blueprint schema;
- insertion JavaScript error if surfaced.

Do not clear the Blueprint collection on a load/store error.

- [ ] **Step 5: Run the complete regression suite**

Run:

```bash
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
gradle -p android-app :app:testDebugUnitTest
gradle -p android-app :app:assembleDebug
```

Expected: PASS / BUILD SUCCESSFUL.

- [ ] **Step 6: Manual behavior checklist on an Android test device**

Verify all of these before versioning:

```text
[ ] Long press outside Blueprint mode still opens Hybrid 14 block menu.
[ ] Duplicate / collapse / disable / delete still work.
[ ] "Als Blueprint speichern" closes block menu and marks the whole connected stack.
[ ] Tapping a block in another stack selects that whole stack.
[ ] Tapping it again deselects it.
[ ] The final remaining group can be deselected; Create becomes disabled at zero.
[ ] Blocks cannot be dragged or edited while Blueprint selection is active.
[ ] Cancel returns to normal editing with no workspace changes.
[ ] Cancel in the name dialog returns to the existing Blueprint selection.
[ ] Saving persists after app restart.
[ ] Browser has Blöcke | Blueprints; normal block catalogue still works.
[ ] Inserting preserves disconnected-group spacing.
[ ] Inserted copies are editable independently.
[ ] Rename preserves Blueprint content.
[ ] Delete removes only the selected Blueprint.
[ ] Existing block previews still render.
[ ] Drag-to-delete still works outside selection mode.
[ ] Undo/redo still work after Blueprint insertion.
```

- [ ] **Step 7: Commit Task 6**

```bash
git add android-app/app/src/main/assets/blockly/blueprint-engine.js \
  android-app/app/src/main/assets/blockly/blueprint-controller.js \
  android-app/tests/blueprint-engine.test.cjs \
  android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt
git commit -m "fix(blueprints): harden selection and insertion failures"
```

---

### Task 7: Version as Hybrid 15, make CI enforce all tests, and publish the APK

**Files:**
- Modify: `android-app/app/build.gradle`
- Modify: `.github/workflows/build-android.yml`
- Modify: `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt` only if the source version label has not already been changed to Hybrid 15.

**Interfaces:**
- No new runtime interface.

- [ ] **Step 1: Bump Android version**

Set:

```gradle
versionCode 21
versionName '0.16.0-hybrid.15'
```

Set the visible source label to:

```text
Hybrid 15 • Blockly-Engine
```

- [ ] **Step 2: Update CI test gates**

Replace the single Node test step with:

```yaml
- name: Run Blockly tests
  run: node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs

- name: Run Android unit tests
  run: gradle -p android-app :app:testDebugUnitTest
```

Update the stamp step to Hybrid 15 and artifact name to:

```yaml
name: Elekto-Blocks-Hybrid-15
```

Keep Blockly pinned to 13.3.0 and keep the existing stable alpha signing flow unchanged.

- [ ] **Step 3: Run fresh local-equivalent verification before final commit**

Run exactly:

```bash
npm install --prefix android-app/blockly-package --no-save --ignore-scripts blockly@13.3.0
node --test android-app/tests/block-actions.test.cjs android-app/tests/blueprint-engine.test.cjs
gradle -p android-app :app:testDebugUnitTest
gradle -p android-app :app:assembleDebug
```

Expected: zero Node test failures, zero Android unit-test failures, BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Hybrid 15 release metadata**

```bash
git add android-app/app/build.gradle \
  .github/workflows/build-android.yml \
  android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/MainActivity.kt
git commit -m "chore(ci): publish Hybrid 15 blueprint system"
```

- [ ] **Step 5: Verify GitHub Actions from the final commit**

Poll the `Build Android APK` run for the final commit until it is `completed`.

Required evidence before claiming success:

```text
Run Blockly tests: success
Run Android unit tests: success
Build Blockly POC APK: success
Upload APK artifact: success
Overall job conclusion: success
```

- [ ] **Step 6: Download and verify the direct APK**

Download artifact `Elekto-Blocks-Hybrid-15`, extract `app-debug.apk`, copy it to:

```text
/mnt/data/Elekto-Blocks-Hybrid-15.apk
```

Verify the file exists and has non-zero size before presenting a sandbox link.

- [ ] **Step 7: Deliver Hybrid 15 for device acceptance testing**

The user-facing test focus should be:

```text
1. Long press → Als Blueprint speichern.
2. Whole connected stack is selected automatically.
3. Add/remove disconnected stacks by tapping any block in them.
4. Save with a name.
5. Find it under Blöcke → Blueprints.
6. Reinsert and check relative layout.
7. Rename and delete.
8. Confirm normal Hybrid 14 context actions still work.
```
