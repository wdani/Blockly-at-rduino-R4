package ch.elekto.blocklyrduino.r4

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import ch.elekto.blocklyrduino.r4.ui.ElektoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElektoTheme {
                ElektoHybridApp()
            }
        }
    }
}

private class ElektoBridge(private val onCode: (String) -> Unit) {
    @JavascriptInterface
    fun showCode(code: String) = onCode(code)
}

private enum class BlockCategory(val title: String) {
    BASICS("Grundlagen"),
    IO("Ein-/Ausgänge"),
    VALUES("Werte & Sensoren"),
    LOGIC("Logik"),
    LOOPS("Schleifen")
}

private enum class PreviewKind { COMMAND, VALUE, BOOLEAN, CONTAINER }

private data class CatalogBlock(
    val id: String,
    val title: String,
    val category: BlockCategory,
    val description: String,
    val example: String,
    val kind: PreviewKind,
    val colour: Color,
    val previewText: String
)

private val catalog = listOf(
    CatalogBlock(
        "delay", "Warten", BlockCategory.BASICS,
        "Pausiert den Programmablauf für eine bestimmte Zeit.",
        "Beispiel: LED an → 1000 ms warten → LED aus.",
        PreviewKind.COMMAND, Color(0xFF6C55C7), "warte 1000 ms"
    ),
    CatalogBlock(
        "digital_write", "Digitaler Ausgang", BlockCategory.IO,
        "Schaltet einen digitalen Pin auf HIGH oder LOW.",
        "Beispiel: D13 auf HIGH schaltet die eingebaute LED ein.",
        PreviewKind.COMMAND, Color(0xFF16865C), "setze D13 HIGH"
    ),
    CatalogBlock(
        "analog_read", "Analogwert", BlockCategory.VALUES,
        "Liest einen analogen Eingang und liefert einen Zahlenwert.",
        "Beispiel: A0 kann den Wert eines Potentiometers liefern.",
        PreviewKind.VALUE, Color(0xFF2D6BC4), "Analog A0"
    ),
    CatalogBlock(
        "number", "Zahl", BlockCategory.VALUES,
        "Ein fester Zahlenwert für einen passenden Eingang.",
        "Beispiel: 500 als Vergleichswert oder 1000 ms Wartezeit.",
        PreviewKind.VALUE, Color(0xFF2D6BC4), "1000"
    ),
    CatalogBlock(
        "compare", "Zahlen vergleichen", BlockCategory.LOGIC,
        "Vergleicht zwei Zahlen. Das Ergebnis ist wahr oder falsch.",
        "Beispiel: Analog A0 > 500.",
        PreviewKind.BOOLEAN, Color(0xFFC25235), "0 > 500"
    ),
    CatalogBlock(
        "if", "Wenn", BlockCategory.LOGIC,
        "Führt die enthaltenen Befehle nur aus, wenn die Bedingung wahr ist.",
        "Beispiel: Wenn A0 > 500, dann LED einschalten.",
        PreviewKind.CONTAINER, Color(0xFFC25235), "wenn …"
    ),
    CatalogBlock(
        "repeat", "Wiederholen", BlockCategory.LOOPS,
        "Führt die enthaltenen Befehle mehrmals hintereinander aus.",
        "Beispiel: Eine LED 10-mal blinken lassen.",
        PreviewKind.CONTAINER, Color(0xFFD89A00), "wiederhole 10×"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ElektoHybridApp() {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var showBlocks by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Elekto Blocks", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Hybrid 1 • Blockly-Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.undo()", null) }) {
                        Icon(Icons.Default.Undo, contentDescription = "Rückgängig")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.redo()", null) }) {
                        Icon(Icons.Default.Redo, contentDescription = "Wiederholen")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.requestCode()", null) }) {
                        Icon(Icons.Default.Code, contentDescription = "Arduino-Code anzeigen")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.loadDemo()", null) }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Blink-Demo laden")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBlocks = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Blöcke") }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                    addJavascriptInterface(ElektoBridge { generatedCode = it }, "ElektoAndroid")
                    webViewClient = object : WebViewClientCompat() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: android.webkit.WebResourceRequest
                        ) = loader.shouldInterceptRequest(request.url)
                    }
                    loadUrl("https://appassets.androidplatform.net/assets/blockly/index.html")
                    webView = this
                }
            },
            update = { webView = it }
        )
    }

    if (showBlocks) {
        BlockCatalogSheet(
            onDismiss = { showBlocks = false },
            onAdd = { id ->
                webView?.evaluateJavascript("window.Elekto.addBlock('$id')", null)
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
                "Form zeigt, was zusammenpasst. Farbe zeigt die Funktionsgruppe.",
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                BlockPreview(block)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(3.dp))
                                    Text(block.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    block.example,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun BlockPreview(block: CatalogBlock) {
    when (block.kind) {
        PreviewKind.VALUE -> Surface(
            modifier = Modifier.width(118.dp).height(44.dp),
            shape = CircleShape,
            color = block.colour
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(block.previewText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        PreviewKind.BOOLEAN -> Surface(
            modifier = Modifier.width(128.dp).height(46.dp),
            shape = RoundedCornerShape(22.dp),
            color = block.colour
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(block.previewText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        PreviewKind.COMMAND -> Box(
            modifier = Modifier.width(142.dp).height(52.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = block.colour
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(block.previewText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Box(
                Modifier.align(Alignment.TopCenter).width(26.dp).height(7.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
            )
            Box(
                Modifier.align(Alignment.BottomCenter).width(26.dp).height(7.dp)
                    .background(block.colour, RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
            )
        }

        PreviewKind.CONTAINER -> Box(
            modifier = Modifier.width(148.dp).height(78.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = block.colour
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(block.previewText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}
