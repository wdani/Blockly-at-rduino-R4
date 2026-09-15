package ch.elekto.blocklyrduino.r4

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONTokener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.CenterFocusStrong
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import ch.elekto.blocklyrduino.r4.ui.ElektoTheme

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

private class ElektoBridge(
    private val onCode: (String) -> Unit,
    private val onDraggingChanged: (Boolean) -> Unit,
    private val onPreview: (String, String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun showCode(code: String) = mainHandler.post { onCode(code) }

    @JavascriptInterface
    fun setDragging(active: Boolean) = mainHandler.post { onDraggingChanged(active) }

    @JavascriptInterface
    fun setPreview(id: String, dataUrl: String) =
        mainHandler.post { onPreview(id, dataUrl) }
}

private enum class BlockCategory(val title: String) {
    BASICS("Grundlagen"),
    IO("Ein-/Ausgänge"),
    VALUES("Werte & Sensoren"),
    LOGIC("Logik"),
    LOOPS("Schleifen")
}

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
    var webView by remember { mutableStateOf<WebView?>(null) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var showBlocks by remember { mutableStateOf(false) }
    var isDraggingBlock by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun applyEditorTheme(view: WebView?) {
        val name = if (darkMode) "dark" else "light"
        view?.evaluateJavascript("window.Elekto?.setTheme('$name')", null)
    }

    LaunchedEffect(darkMode, webView) {
        applyEditorTheme(webView)
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
                            "Hybrid 8 • Blockly-Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.undo()", null) }) {
                        Icon(Icons.Default.Undo, contentDescription = "Rückgängig")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.redo()", null) }) {
                        Icon(Icons.Default.Redo, contentDescription = "Wiederholen")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto?.requestCode()", null) }) {
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val loader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                        .build()

                    WebView(context).apply {
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
                                onDraggingChanged = { isDraggingBlock = it },
                                onPreview = { id, data ->
                                    previewImages = previewImages + (id to data)
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
        }
    }

    if (showBlocks) {
        BlockCatalogSheet(
            darkMode = darkMode,
            editorWebView = webView,
            previewImages = previewImages,
            onDismiss = { showBlocks = false },
            onAdd = { id ->
                webView?.evaluateJavascript("window.Elekto?.addBlock('$id')", null)
                showBlocks = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCatalogSheet(
    darkMode: Boolean,
    editorWebView: WebView?,
    previewImages: Map<String, String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var category by remember { mutableStateOf(BlockCategory.BASICS) }
    val visible = catalog.filter { it.category == category }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Blöcke auswählen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth(),
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
                            if (block.id == "compare") {
                                ComparisonLegend()
                            }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BlocklyBlockPreview(
    id: String,
    darkMode: Boolean,
    editorWebView: WebView?,
    dataUrl: String?
) {
    LaunchedEffect(id, darkMode, editorWebView) {
        editorWebView?.evaluateJavascript(
            "window.Elekto?.requestPreview('$id')",
            null
        )
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
            key(dataUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportZoom(false)
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            val html = """
                                <!doctype html>
                                <html>
                                  <head>
                                    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                                    <style>
                                      html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}
                                      body{display:flex;align-items:center;justify-content:center}
                                      img{display:block;max-width:92%;max-height:82%;width:auto;height:auto;object-fit:contain}
                                    </style>
                                  </head>
                                  <body><img src="$dataUrl"></body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            tag = dataUrl
                        }
                    },
                    update = { view ->
                        if (view.tag != dataUrl) {
                            val html = """
                                <!doctype html>
                                <html>
                                  <head>
                                    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                                    <style>
                                      html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}
                                      body{display:flex;align-items:center;justify-content:center}
                                      img{display:block;max-width:92%;max-height:82%;width:auto;height:auto;object-fit:contain}
                                    </style>
                                  </head>
                                  <body><img src="$dataUrl"></body>
                                </html>
                            """.trimIndent()
                            view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            view.tag = dataUrl
                        }
                    }
                )
            }
        }
    }
}

