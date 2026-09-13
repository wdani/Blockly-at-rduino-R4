package ch.elekto.blocklyrduino.r4;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class MainActivity extends Activity {
    private WebView webView;

    private static final String START_URL =
            "https://appassets.androidplatform.net/assets/www/index.html?board=arduino_uno&lang=de";

    private static final String R4_BOOTSTRAP =
            "(function(){" +
            "if(document.getElementById('r4-android-profile')){return;}" +
            "function load(src,id,done){" +
            " var s=document.createElement('script');s.src=src;s.id=id;" +
            " s.onload=function(){if(done){done();}};document.head.appendChild(s);" +
            "}" +
            "load('r4/uno-r4-profiles.js','r4-android-profile',function(){" +
            " try{" +
            "  history.replaceState(null,'','?board=arduino_uno_r4_wifi&lang=de');" +
            "  if(window.BlocklyDuino){" +
            "   BlocklyDuino.selectedBoard='arduino_uno_r4_wifi';" +
            "   if(typeof BlocklyDuino.setArduinoBoard==='function'){BlocklyDuino.setArduinoBoard();}" +
            "  }" +
            " }catch(e){console.error('R4 Android board bootstrap failed',e);}" +
            " load('r4/legacy-blockly-pointer-compat.js','r4-android-pointer');" +
            " load('r4/android-mobile.js','r4-android-mobile');" +
            "});" +
            "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setTextZoom(100);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        installDocumentStartScripts();

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(R4_BOOTSTRAP, null);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void installDocumentStartScripts() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            return;
        }

        try {
            String pointerCompat = readAssetText("www/r4/legacy-blockly-pointer-compat.js");
            String mobileLayout = readAssetText("www/r4/android-mobile.js");
            WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    pointerCompat + "\n" + mobileLayout,
                    Collections.singleton("https://appassets.androidplatform.net")
            );
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load Android Blockly bootstrap scripts", error);
        }
    }

    private String readAssetText(String path) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(path), StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                text.append(buffer, 0, count);
            }
        }
        return text.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
