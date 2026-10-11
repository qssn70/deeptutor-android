package dev.qssn70.deeptutor

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.qssn70.deeptutor.config.ServerConfig
import dev.qssn70.deeptutor.files.FilePicker
import dev.qssn70.deeptutor.files.DownloadHandler
import dev.qssn70.deeptutor.config.MobilePreferences
import dev.qssn70.deeptutor.ui.MobileChrome
import dev.qssn70.deeptutor.ui.ChromeAction
import dev.qssn70.deeptutor.ui.MobileInsets
import dev.qssn70.deeptutor.web.MobilePageTweaks
import dev.qssn70.deeptutor.web.DeepTutorWebClient
import dev.qssn70.deeptutor.web.NavigationPolicy

class MainActivity : Activity() {
    private lateinit var config: ServerConfig
    private lateinit var picker: FilePicker
    private lateinit var mobilePrefs: MobilePreferences
    private var chrome: MobileChrome? = null
    private var activeUrl: String? = null
    private var webView: WebView? = null
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ServerConfig(this)
        picker = FilePicker(this)
        mobilePrefs = MobilePreferences(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root)
        MobileInsets.install(this, root)
        config.baseUrl?.let(::showWeb) ?: showSetup()
    }

    private fun showSetup(message: String = "") {
        webView?.let { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }; webView = null
        chrome = null
        root.removeAllViews()
        val label = TextView(this).apply {
            text = "DeepTutor 服务器地址（HTTPS）\n$message"
            textSize = 18f
            setPadding(24, 48, 24, 24)
        }
        val input = EditText(this).apply {
            hint = "https://deeptutor.example.com"
            setSingleLine(true)
            setText(config.baseUrl ?: "")
        }
        val button = Button(this).apply {
            text = "连接服务器"
            setOnClickListener {
                try {
                    val url = ServerConfig.normalize(input.text.toString())
                    config.baseUrl = url
                    showWeb(url)
                } catch (e: IllegalArgumentException) {
                    input.error = e.message
                }
            }
        }
        root.addView(label)
        root.addView(input)
        root.addView(button)
    }

    private fun showWeb(url: String) {
        root.removeAllViews()
        activeUrl = url
        val view = WebView(this)
        webView = view
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            textZoom = mobilePrefs.textZoomPercent
            useWideViewPort = true
            loadWithOverviewMode = false
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        val navigation = NavigationPolicy(url)
        val overlay = MobileChrome(this, mobilePrefs, ::handleChromeAction)
        chrome = overlay
        overlay.attach(view)
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, newProgress: Int) {
                if (this@MainActivity.webView === webView) overlay.loading(newProgress)
            }
            override fun onShowFileChooser(webView: WebView, filePathCallback: android.webkit.ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean =
                picker.open(filePathCallback, fileChooserParams)
        }
        view.webViewClient = DeepTutorWebClient(
            this, navigation,
            { error -> if (webView === view) overlay.showError(error) },
            {
                if (webView === view) {
                    webView = null
                    root.removeView(overlay)
                    overlay.removeView(view)
                    view.destroy()
                    chrome = null
                    showSetup("网页渲染进程已退出，请重新连接")
                }
            },
            onPageStarted = { if (webView === view) overlay.hideError() },
            onPageFinished = {
                if (webView === view) MobilePageTweaks.apply(view, navigation, mobilePrefs.enableReadingFixes)
            }
        )
        view.setDownloadListener(DownloadHandler(this, navigation))
        root.addView(overlay, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        view.loadUrl(url)
    }

    private fun handleChromeAction(action: ChromeAction) {
        val view = webView ?: return
        when (action) {
            ChromeAction.BACK -> if (view.canGoBack()) view.goBack()
            ChromeAction.FORWARD -> if (view.canGoForward()) view.goForward()
            ChromeAction.HOME -> activeUrl?.let(view::loadUrl)
            ChromeAction.RELOAD -> { chrome?.hideError(); view.reload() }
            ChromeAction.TEXT_SIZE -> {
                view.settings.textZoom = mobilePrefs.nextTextZoom()
                Toast.makeText(this, "文字大小：${mobilePrefs.textZoomPercent}%", Toast.LENGTH_SHORT).show()
            }
            ChromeAction.READING_FIXES -> {
                mobilePrefs.enableReadingFixes = !mobilePrefs.enableReadingFixes
                activeUrl?.let { MobilePageTweaks.apply(view, NavigationPolicy(it), mobilePrefs.enableReadingFixes) }
            }
            ChromeAction.COPY_LINK -> {
                val current = view.url ?: return
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("DeepTutor URL", current))
                Toast.makeText(this, "已复制链接", Toast.LENGTH_SHORT).show()
            }
            ChromeAction.SERVER -> showSetup()
        }
    }

    @Deprecated("Handled for compatibility with Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        picker.onResult(requestCode, resultCode, data)
    }
    @Deprecated("Handled for compatibility with Activity API")
    override fun onBackPressed() {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack()
        else if (view != null) super.onBackPressed()
        else super.onBackPressed()
    }
    override fun onPause() { super.onPause(); CookieManager.getInstance().flush(); webView?.onPause() }
    override fun onResume() { super.onResume(); webView?.onResume() }
    override fun onDestroy() {
        picker.cancel()
        webView?.let { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }; webView = null
        super.onDestroy()
    }
}
