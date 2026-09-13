package ch.elekto.blocklyrduino.r4

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                BlocklyPocApp()
            }
        }
    }
}

private class PocBridge(private val onCode: (String) -> Unit) {
    @JavascriptInterface
    fun showCode(code: String) = onCode(code)
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BlocklyPocApp() {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var generatedCode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Elekto Blocks • Blockly POC")
                        Text(
                            "Moderner Blockly-Kern • offline • Vergleichstest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.requestCode()", null) }) {
                        Icon(Icons.Default.Code, contentDescription = "Arduino-Code anzeigen")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("window.Elekto.loadDemo()", null) }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Demo laden")
                    }
                }
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

                    addJavascriptInterface(PocBridge { generatedCode = it }, "ElektoAndroid")
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

    generatedCode?.let { code ->
        AlertDialog(
            onDismissRequest = { generatedCode = null },
            title = { Text("Arduino-Code") },
            text = {
                Text(
                    code,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { generatedCode = null }) { Text("Schließen") }
            }
        )
    }
}
