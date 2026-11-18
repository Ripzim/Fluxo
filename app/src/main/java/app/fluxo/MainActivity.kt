package app.fluxo

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var webView: WebView
    private var coreJs: String = ""
    private var scriptHandler: ScriptHandler? = null

    private var theme by mutableStateOf(FxTheme.presets.first())
    private var savedThemes by mutableStateOf(listOf<FxTheme>())
    private var blockShorts by mutableStateOf(true)

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        theme = prefs.loadTheme()
        savedThemes = prefs.loadSaved()
        blockShorts = prefs.blockShorts
        coreJs = assets.open("fluxo.js").bufferedReader().use { it.readText() }

        webView = buildWebView()
        pushConfig()
        webView.loadUrl(urlFrom(intent) ?: Redirects.HOME)

        setContent {
            var showSettings by remember { mutableStateOf(false) }
            val scheme = remember(theme) { fxColorScheme(theme) }

            SideEffect { applyBars(theme) }
            LaunchedEffect(theme, blockShorts) { pushConfig() }

            BackHandler {
                when {
                    customView != null -> hideCustomView()
                    webView.canGoBack() -> webView.goBack()
                    else -> finish()
                }
            }

            MaterialTheme(colorScheme = scheme) {
                Box(Modifier.fillMaxSize().background(scheme.background)) {
                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.surface.copy(alpha = 0.6f))
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = scheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (showSettings) {
                    ModalBottomSheet(
                        onDismissRequest = { showSettings = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = scheme.surface,
                        contentColor = scheme.onSurface,
                    ) {
                        SettingsContent(
                            theme = theme,
                            saved = savedThemes,
                            blockShorts = blockShorts,
                            onBlockShorts = { blockShorts = it; prefs.blockShorts = it },
                            onTheme = { updateTheme(it) },
                            onSave = { saveCurrent(it) },
                            onDeleteSaved = { deleteSaved(it) },
                            onExport = { exportTheme() },
                            onImport = { importTheme() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        urlFrom(intent)?.let { webView.loadUrl(it) }
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    // ---------- WebView ----------

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(): WebView {
        val wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            builtInZoomControls = false
            // Sem "; wv" e "Version/x.y" o login do Google costuma ser aceito
            userAgentString = userAgentString
                .replace("; wv", "")
                .replace(Regex("Version/\\d+\\.\\d+\\s"), "")
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
        wv.webViewClient = FxClient()
        wv.webChromeClient = FxChrome()
        return wv
    }

    private fun buildScript(): String {
        val cfg = JSONObject()
            .put("css", Injector.themeCss(theme))
            .put("blockShorts", blockShorts)
        return coreJs + "\n;window.__fxSet(" + cfg.toString() + ");"
    }

    private fun pushConfig() {
        if (!::webView.isInitialized) return
        val script = buildScript()
        webView.settings.textZoom = (theme.textScale * 100).roundToInt()
        webView.setBackgroundColor(theme.bg.toColorInt())
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler?.remove()
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(
                webView, script, setOf("https://*.youtube.com"),
            )
        }
        webView.evaluateJavascript(script, null)
    }

    private fun urlFrom(i: Intent?): String? {
        val uri = i?.data ?: return null
        if (!Redirects.isInternal(uri)) return null
        return (if (blockShorts) Redirects.rewriteShorts(uri) else null) ?: uri.toString()
    }

    private inner class FxClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (blockShorts) {
                Redirects.rewriteShorts(uri)?.let { view.loadUrl(it); return true }
            }
            if (Redirects.isInternal(uri)) return false
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                // sem app para abrir: ignora
            }
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            if (!blockShorts || url == null) return
            Redirects.rewriteShorts(android.net.Uri.parse(url))?.let { target ->
                // replace evita que o botão voltar caia de novo no Short
                view.post { view.evaluateJavascript("location.replace(" + JSONObject.quote(target) + ")", null) }
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            view.evaluateJavascript(buildScript(), null)
        }
    }

    private inner class FxChrome : WebChromeClient() {
        override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            (window.decorView as ViewGroup).addView(
                view,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            WindowInsetsControllerCompat(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        override fun onHideCustomView() {
            hideCustomView()
        }
    }

    private fun hideCustomView() {
        val v = customView ?: return
        val cb = customViewCallback
        customView = null
        customViewCallback = null
        (window.decorView as ViewGroup).removeView(v)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        cb?.onCustomViewHidden()
    }

    // ---------- Tema ----------

    private fun updateTheme(t: FxTheme) {
        theme = t
        prefs.saveTheme(t)
    }

    private fun saveCurrent(name: String) {
        val n = name.trim().ifEmpty { "Meu tema" }
        val t = theme.copy(name = n)
        savedThemes = savedThemes.filter { it.name != n } + t
        prefs.saveSaved(savedThemes)
        updateTheme(t)
    }

    private fun deleteSaved(t: FxTheme) {
        savedThemes = savedThemes.filter { it.name != t.name }
        prefs.saveSaved(savedThemes)
    }

    private fun exportTheme() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("tema", theme.toJson().toString()))
        toast("Tema copiado")
    }

    private fun importTheme() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        val t = runCatching { FxTheme.fromJson(JSONObject(text ?: "")) }.getOrNull()
        if (t == null) {
            toast("Nenhum tema válido na área de transferência")
        } else {
            updateTheme(t)
            toast("Tema importado")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun applyBars(t: FxTheme) {
        val c = t.bg.toColorInt()
        window.statusBarColor = c
        window.navigationBarColor = c
        val light = ColorUtils.calculateLuminance(c) > 0.5
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    private fun fxColorScheme(t: FxTheme): ColorScheme {
        val bg = Color(t.bg.toColorInt())
        val surface = Color(t.surface.toColorInt())
        val text = Color(t.text.toColorInt())
        val text2 = Color(t.textSecondary.toColorInt())
        val accent = Color(t.accent.toColorInt())
        val dark = ColorUtils.calculateLuminance(t.bg.toColorInt()) < 0.5
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = accent,
            onPrimary = bg,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surface,
            onSurfaceVariant = text2,
            outline = text2.copy(alpha = 0.5f),
            secondaryContainer = accent.copy(alpha = 0.25f),
            onSecondaryContainer = text,
        )
    }
}
