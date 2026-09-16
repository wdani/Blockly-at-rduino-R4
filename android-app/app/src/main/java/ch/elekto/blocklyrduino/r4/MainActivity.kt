package ch.elekto.blocklyrduino.r4

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import ch.elekto.blocklyrduino.r4.data.BlueprintStore
import ch.elekto.blocklyrduino.r4.model.BLUEPRINT_SCHEMA_VERSION
import ch.elekto.blocklyrduino.r4.model.BlueprintRecord
import ch.elekto.blocklyrduino.r4.ui.BlueprintBrowserList
import ch.elekto.blocklyrduino.r4.ui.BlueprintDeleteDialog
import ch.elekto.blocklyrduino.r4.ui.BlueprintNameDialog
import ch.elekto.blocklyrduino.r4.ui.BlueprintSelectionBar
import ch.elekto.blocklyrduino.r4.ui.ElektoTheme
import java.io.File
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ElektoRoot() }
    }
}

@Composable
private fun ElektoRoot() {
    val systemDark = isSystemInDarkTheme()
    var darkMode by rememberSaveable { mutableStateOf(systemDark) }

    ElektoTheme(darkTheme = darkMode) {
        ElektoHybridApp(
            darkMode = darkMode,
            onToggleTheme = { darkMode = !darkMode }
        )
    }
}

private data class BlockMenuState(
    val id: String,
    val type: String,
    val collapsed: Boolean,
    val enabled: Boolean
)

private class ElektoBridge(
    private val onCode: (String) -> Unit,
    private val onDraggingChanged: (Boolean) -> Unit,
    private val onPreview: (String, String) -> Unit,
    private val onBlockMenu: (BlockMenuState) -> Unit,
    private val onBlueprintSelectionChanged: (Boolean, Int) -> Unit,
    private val onBlueprintPayload: (String) -> Unit,
    private val onBlueprintError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun showCode(code: String) = mainHandler.post { onCode(code) }

    @JavascriptInterface
    fun setDragging(active: Boolean) = mainHandler.post { onDraggingChanged(active) }

    @JavascriptInterface
    fun setPreview(id: String, dataUrl: String) =
        mainHandler.post { onPreview(id, dataUrl) }

    @JavascriptInterface
    fun showBlockMenu(id: String, type: String, collapsed: Boolean, enabled: Boolean) =
        mainHandler.post {
            onBlockMenu(BlockMenuState(id, type, collapsed, enabled))
        }

    @JavascriptInterface
    fun setBlueprintSelection(active: Boolean, groupCount: Int) =
        mainHandler.post { onBlueprintSelectionChanged(active, groupCount) }

    @JavascriptInterface
    fun blueprintPayloadReady(payloadJson: String) =
        mainHandler.post { onBlueprintPayload(payloadJson) }

    @JavascriptInterface
    fun blueprintError(message: String) =
        mainHandler.post { onBlueprintError(message) }
}

private enum class BlockCategory(val title: String) {
    BASICS("Grundlagen"),
    IO("Ein-/Ausgänge"),
    VALUES("Werte & Sensoren"),
    LOGIC("Logik"),
    LOOPS("Schleifen")
}

private enum class CatalogMode { BLOCKS, BLUEPRINTS }

private data class CatalogBlock(
    val id: String,
    val title: String,
    val category: BlockCategory,
    val description: String,
    val example: String,
    val details: String? = null
)

private val catalog = listOf(
    CatalogBlock(
        "delay", "Warten", BlockCategory.BASICS,
        "Pausiert den Programmablauf für eine bestimmte Zeit.",
        "Beispiel: LED an → 1000 ms warten → LED aus."
    ),
    CatalogBlock(
        "digital_write", "Digitaler Ausgang", BlockCategory.IO,
        "Schaltet einen digitalen Pin auf HIGH oder LOW.",
        "Beispiel: D13 auf HIGH schaltet die eingebaute LED ein."
    ),
    CatalogBlock(
        "analog_read", "Analogwert", BlockCategory.VALUES,
        "Liest einen analogen Eingang und liefert einen Zahlenwert.",
        "Beispiel: A0 kann den Wert eines Potentiometers liefern."
    ),
    CatalogBlock(
        "number", "Zahl", BlockCategory.VALUES,
        "Ein fester Zahlenwert für einen passenden Eingang.",
        "Beispiel: 500 als Vergleichswert oder 1000 ms Wartezeit."
    ),
    CatalogBlock(
        "compare", "Zahlen vergleichen", BlockCategory.LOGIC,
        "Vergleicht zwei Zahlen. Das Ergebnis ist wahr oder falsch.",
        "Beispiel: Analog A0 > 500.",
        "> bedeutet größer als · < bedeutet kleiner als · = bedeutet gleich · ≥ bedeutet größer oder gleich · ≤ bedeutet kleiner oder gleich."
    ),
    CatalogBlock(
        "if", "Wenn", BlockCategory.LOGIC,
        "Führt die enthaltenen Befehle nur aus, wenn die Bedingung wahr ist.",
        "Beispiel: Wenn A0 > 500, dann LED einschalten."
    ),
    CatalogBlock(
        "repeat", "Wiederholen", BlockCategory.LOOPS,
        "Führt die enthaltenen Befehle mehrmals hintereinander aus.",
        "Beispiel: Eine LED 10-mal blinken lassen."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ElektoHybridApp(
    darkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val blueprintStore = remember(context.applicationContext) {
        BlueprintStore(File(context.filesDir, "blueprints-v1.json"))
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var showBlocks by remember { mutableStateOf(false) }
    var isDraggingBlock by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var blockMenu by remember { mutableStateOf<BlockMenuState?>(null) }

    var blueprintSelectionActive by remember { mutableStateOf(false) }
    var blueprintGroupCount by remember { mutableStateOf(0) }
    var pendingBlueprintPayload by remember { mutableStateOf<String?>(null) }
    var blueprints by remember { mutableStateOf<List<BlueprintRecord>>(emptyList()) }
    var renameBlueprint by remember { mutableStateOf<BlueprintRecord?>(null) }
    var deleteBlueprint by remember { mutableStateOf<BlueprintRecord?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun applyEditorTheme(view: WebView?) {
        val name = if (darkMode) "dark" else "light"
        view?.evaluateJavascript("window.Elekto?.setTheme('$name')", null)
    }

    fun reloadBlueprints() {
        runCatching { blueprintStore.loadAll() }
            .onSuccess { blueprints = it }
            .onFailure { errorMessage = it.message ?: "Blueprints konnten nicht geladen werden." }
    }

    fun runBlockAction(state: BlockMenuState, action: String) {
        val blockId = JSONObject.quote(state.id)
        val actionName = JSONObject.quote(action)
        webView?.evaluateJavascript(
            "window.Elekto?.blockAction($blockId,$actionName)",
            null
        )
        blockMenu = null
    }

    fun startBlueprintSelection(state: BlockMenuState) {
        val blockId = JSONObject.quote(state.id)
        blockMenu = null
        webView?.evaluateJavascript(
            "window.Elekto?.startBlueprintSelection($blockId)",
            null
        )
    }

    fun validateAndOpenBlueprintName(payloadJson: String) {
        runCatching {
            val payload = JSONObject(payloadJson)
            require(payload.optInt("schemaVersion", -1) == BLUEPRINT_SCHEMA_VERSION)
            require(payload.optInt("groupCount", 0) > 0)
            require(payload.optInt("blockCount", 0) > 0)
            payloadJson
        }.onSuccess {
            pendingBlueprintPayload = it
        }.onFailure {
            errorMessage = "Der Blueprint konnte nicht vorbereitet werden."
        }
    }

    fun savePendingBlueprint(name: String) {
        val raw = pendingBlueprintPayload ?: return
        runCatching {
            val payload = JSONObject(raw)
            blueprintStore.create(
                name = name,
                payloadJson = raw,
                groupCount = payload.getInt("groupCount"),
                blockCount = payload.getInt("blockCount")
            )
        }.onSuccess {
            reloadBlueprints()
            pendingBlueprintPayload = null
            blueprintSelectionActive = false
            blueprintGroupCount = 0
            webView?.evaluateJavascript("window.Elekto?.commitBlueprintSelection()", null)
        }.onFailure {
            errorMessage = it.message ?: "Blueprint konnte nicht gespeichert werden."
        }
    }

    fun insertBlueprint(blueprint: BlueprintRecord) {
        val payload = JSONObject.quote(blueprint.payloadJson)
        showBlocks = false
        webView?.evaluateJavascript("window.Elekto?.insertBlueprint($payload)", null)
    }

    LaunchedEffect(darkMode, webView) {
        applyEditorTheme(webView)
    }

    LaunchedEffect(blueprintStore) {
        reloadBlueprints()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Elekto Blocks", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Hybrid 15 • Blockly-Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !blueprintSelectionActive,
                        onClick = { webView?.evaluateJavascript("window.Elekto?.undo()", null) }
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Rückgängig")
                    }
                    IconButton(
                        enabled = !blueprintSelectionActive,
                        onClick = { webView?.evaluateJavascript("window.Elekto?.redo()", null) }
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Wiederholen")
                    }
                    IconButton(
                        enabled = !blueprintSelectionActive,
                        onClick = { webView?.evaluateJavascript("window.Elekto?.requestCode()", null) }
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Arduino-Code anzeigen")
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkMode) "Helles Theme" else "Dunkles Theme"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!blueprintSelectionActive) {
                if (isDraggingBlock) {
                    Surface(
                        modifier = Modifier.width(126.dp).height(76.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Block löschen",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Hier löschen",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showBlocks = true },
                        icon = { Text("+", style = MaterialTheme.typography.headlineSmall) },
                        text = { Text("Blöcke") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidContext ->
                    val loader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(androidContext))
                        .build()

                    WebView(androidContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportZoom(false)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        addJavascriptInterface(
                            ElektoBridge(
                                onCode = { generatedCode = it },
                                onDraggingChanged = { active ->
                                    if (!blueprintSelectionActive) isDraggingBlock = active
                                },
                                onPreview = { id, data ->
                                    previewImages = previewImages + (id to data)
                                },
                                onBlockMenu = { state ->
                                    if (!blueprintSelectionActive) {
                                        isDraggingBlock = false
                                        blockMenu = state
                                    }
                                },
                                onBlueprintSelectionChanged = { active, count ->
                                    blueprintSelectionActive = active
                                    blueprintGroupCount = count.coerceAtLeast(0)
                                    if (active) isDraggingBlock = false
                                },
                                onBlueprintPayload = { validateAndOpenBlueprintName(it) },
                                onBlueprintError = {
                                    errorMessage = it.ifBlank { "Blueprint-Aktion fehlgeschlagen." }
                                }
                            ),
                            "ElektoAndroid"
                        )
                        webViewClient = object : WebViewClientCompat() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: android.webkit.WebResourceRequest
                            ) = loader.shouldInterceptRequest(request.url)

                            override fun onPageFinished(view: WebView, url: String) {
                                val name = if (darkMode) "dark" else "light"
                                view.evaluateJavascript("window.Elekto?.setTheme('$name')", null)
                            }
                        }
                        loadUrl("https://appassets.androidplatform.net/assets/blockly/index.html")
                        webView = this
                    }
                },
                update = {
                    webView = it
                    applyEditorTheme(it)
                }
            )

            if (!blueprintSelectionActive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = 14.dp),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 5.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.zoomOut()", null) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Verkleinern")
                        }
                        IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.resetZoom()", null) }) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Ansicht zurücksetzen")
                        }
                        IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.zoomIn()", null) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Vergrößern")
                        }
                    }
                }
            } else {
                BlueprintSelectionBar(
                    groupCount = blueprintGroupCount,
                    onCancel = {
                        pendingBlueprintPayload = null
                        blueprintSelectionActive = false
                        blueprintGroupCount = 0
                        webView?.evaluateJavascript("window.Elekto?.cancelBlueprintSelection()", null)
                    },
                    onCreate = {
                        webView?.evaluateJavascript("window.Elekto?.requestBlueprintPayload()", null)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                )
            }
        }
    }

    if (showBlocks) {
        BlockCatalogSheet(
            darkMode = darkMode,
            editorWebView = webView,
            previewImages = previewImages,
            blueprints = blueprints,
            onDismiss = { showBlocks = false },
            onAdd = { id ->
                webView?.evaluateJavascript("window.Elekto?.addBlock('$id')", null)
                showBlocks = false
            },
            onInsertBlueprint = ::insertBlueprint,
            onRenameBlueprint = { renameBlueprint = it },
            onDeleteBlueprint = { deleteBlueprint = it }
        )
    }

    blockMenu?.let { state ->
        BlockContextSheet(
            state = state,
            onDismiss = { blockMenu = null },
            onAction = { action -> runBlockAction(state, action) },
            onStartBlueprint = { startBlueprintSelection(state) }
        )
    }

    pendingBlueprintPayload?.let {
        BlueprintNameDialog(
            title = "Blueprint speichern",
            confirmLabel = "Speichern",
            onDismiss = { pendingBlueprintPayload = null },
            onConfirm = ::savePendingBlueprint
        )
    }

    renameBlueprint?.let { blueprint ->
        BlueprintNameDialog(
            title = "Blueprint umbenennen",
            confirmLabel = "Umbenennen",
            initialName = blueprint.name,
            onDismiss = { renameBlueprint = null },
            onConfirm = { newName ->
                runCatching { blueprintStore.rename(blueprint.id, newName) }
                    .onSuccess {
                        reloadBlueprints()
                        renameBlueprint = null
                    }
                    .onFailure {
                        errorMessage = it.message ?: "Blueprint konnte nicht umbenannt werden."
                    }
            }
        )
    }

    deleteBlueprint?.let { blueprint ->
        BlueprintDeleteDialog(
            blueprintName = blueprint.name,
            onDismiss = { deleteBlueprint = null },
            onConfirm = {
                runCatching { blueprintStore.delete(blueprint.id) }
                    .onSuccess {
                        reloadBlueprints()
                        deleteBlueprint = null
                    }
                    .onFailure {
                        errorMessage = it.message ?: "Blueprint konnte nicht gelöscht werden."
                    }
            }
        )
    }

    generatedCode?.let { code ->
        AlertDialog(
            onDismissRequest = { generatedCode = null },
            title = { Text("Arduino-Code") },
            text = { Text(code, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = { generatedCode = null }) { Text("Schließen") }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Blueprint-Fehler") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }
}

private fun blockDisplayName(type: String): String = when (type) {
    "elekto_delay" -> "Warten"
    "elekto_digital_write" -> "Digitaler Ausgang"
    "elekto_analog_read" -> "Analogwert"
    "math_number" -> "Zahl"
    "logic_compare" -> "Zahlen vergleichen"
    "elekto_if" -> "Wenn"
    "elekto_repeat" -> "Wiederholen"
    else -> "Block"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockContextSheet(
    state: BlockMenuState,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onStartBlueprint: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Block-Optionen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                blockDisplayName(state.type),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BlockActionCard(
                title = "Duplizieren",
                description = "Erstellt eine Kopie. Bei einem verbundenen Stapel werden die darunterliegenden Blöcke mitkopiert.",
                onClick = { onAction("duplicate") }
            )
            BlockActionCard(
                title = "Als Blueprint speichern",
                description = "Wählt diesen verbundenen Stapel aus. Danach kannst du weitere Stapel hinzufügen.",
                onClick = onStartBlueprint
            )
            BlockActionCard(
                title = if (state.collapsed) "Ausklappen" else "Einklappen",
                description = if (state.collapsed)
                    "Zeigt den Block wieder vollständig an."
                else
                    "Macht den Block kompakter, ohne ihn zu löschen.",
                onClick = { onAction("toggleCollapsed") }
            )
            BlockActionCard(
                title = if (state.enabled) "Deaktivieren" else "Aktivieren",
                description = if (state.enabled)
                    "Der Block bleibt sichtbar, wird aber beim Arduino-Code übersprungen."
                else
                    "Der Block wird wieder beim Arduino-Code berücksichtigt.",
                onClick = { onAction("toggleEnabled") }
            )
            BlockActionCard(
                title = "Löschen",
                description = "Entfernt den Block und die damit verbundenen Unterblöcke.",
                danger = true,
                onClick = { onAction("delete") }
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BlockActionCard(
    title: String,
    description: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val container = if (danger) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val foreground = if (danger) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = foreground)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = foreground.copy(alpha = 0.82f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCatalogSheet(
    darkMode: Boolean,
    editorWebView: WebView?,
    previewImages: Map<String, String>,
    blueprints: List<BlueprintRecord>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onInsertBlueprint: (BlueprintRecord) -> Unit,
    onRenameBlueprint: (BlueprintRecord) -> Unit,
    onDeleteBlueprint: (BlueprintRecord) -> Unit
) {
    var category by remember { mutableStateOf(BlockCategory.BASICS) }
    var mode by rememberSaveable { mutableStateOf(CatalogMode.BLOCKS) }
    val visible = catalog.filter { it.category == category }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Blöcke auswählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == CatalogMode.BLOCKS,
                    onClick = { mode = CatalogMode.BLOCKS },
                    label = { Text("Blöcke") }
                )
                FilterChip(
                    selected = mode == CatalogMode.BLUEPRINTS,
                    onClick = { mode = CatalogMode.BLUEPRINTS },
                    label = { Text("Blueprints") }
                )
            }

            if (mode == CatalogMode.BLOCKS) {
                Text(
                    "Die Vorschau ist derselbe Blockly-Block, der später auf der Arbeitsfläche liegt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BlockCategory.entries) { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.title) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { block ->
                        Card(
                            onClick = { onAdd(block.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text(block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                BlocklyBlockPreview(
                                    id = block.id,
                                    darkMode = darkMode,
                                    editorWebView = editorWebView,
                                    dataUrl = previewImages[block.id]
                                )
                                Text(block.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (block.id == "compare") ComparisonLegend()
                                Text(
                                    block.example,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            } else {
                Text(
                    "Gespeicherte Blockkombinationen kannst du mit einem Tipp wieder einfügen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BlueprintBrowserList(
                    blueprints = blueprints,
                    onInsert = onInsertBlueprint,
                    onRename = onRenameBlueprint,
                    onDelete = onDeleteBlueprint,
                    modifier = Modifier.heightIn(max = 520.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ComparisonMeaning(">", "größer als", Modifier.weight(1f))
            ComparisonMeaning("<", "kleiner als", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ComparisonMeaning("=", "gleich", Modifier.weight(1f))
            ComparisonMeaning("≥", "größer oder gleich", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            ComparisonMeaning("≤", "kleiner oder gleich", Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ComparisonMeaning(
    symbol: String,
    meaning: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    symbol,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                meaning,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BlocklyBlockPreview(
    id: String,
    darkMode: Boolean,
    editorWebView: WebView?,
    dataUrl: String?
) {
    LaunchedEffect(id, darkMode, editorWebView) {
        editorWebView?.evaluateJavascript("window.Elekto?.requestPreview('$id')", null)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(116.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (dataUrl == null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "Vorschau wird erstellt …",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val imageBitmap = remember(dataUrl) {
                try {
                    val encoded = dataUrl.substringAfter("base64,", "")
                    if (encoded.isBlank()) null
                    else {
                        val bytes = Base64.decode(encoded, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (imageBitmap == null) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Vorschau konnte nicht geladen werden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Vorschau: $id",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
