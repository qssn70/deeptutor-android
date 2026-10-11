package dev.qssn70.deeptutor

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
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
import dev.qssn70.deeptutor.web.DeepTutorWebClient
import dev.qssn70.deeptutor.web.NavigationPolicy

class MainActivity : Activity() {
    private lateinit var config: ServerConfig
    private lateinit var picker: FilePicker
    private var webView: WebView? = null
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ServerConfig(this)
        picker = FilePicker(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root)
        config.baseUrl?.let(::showWeb) ?: showSetup()
    }

    private fun showSetup(message: String = "") {
        webView?.let { root.removeView(it); it.destroy() }; webView = null
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
        val toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val home = Button(this).apply { text = "主页"; setOnClickListener { webView?.loadUrl(url) } }
        val reload = Button(this).apply { text = "刷新"; setOnClickListener { webView?.reload() } }
        val settings = Button(this).apply { text = "服务器"; setOnClickListener { showSetup() } }
        toolbar.addView(home); toolbar.addView(reload); toolbar.addView(settings)
        root.addView(toolbar)
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
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView, filePathCallback: android.webkit.ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean =
                picker.open(filePathCallback, fileChooserParams)
        }
        view.webViewClient = DeepTutorWebClient(this, NavigationPolicy(url),
            { error -> Toast.makeText(this, "页面加载失败：$error", Toast.LENGTH_LONG).show() },
            { if (webView === view) { webView = null; root.removeView(view); view.destroy(); showSetup("网页渲染进程已退出，请重新连接") } }
        )
        view.setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
            val source = Uri.parse(downloadUrl)
            if (source.scheme != "https" || !NavigationPolicy(url).inApp(source)) {
                Toast.makeText(this, "暂不支持非同源下载", Toast.LENGTH_SHORT).show()
                return@DownloadListener
            }
            try {
                val name = android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType).replace(Regex("[\\\\/]"), "_")
                val request = DownloadManager.Request(source)
                    .setTitle(name)
                    .setMimeType(mimeType)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(downloadUrl) ?: "")
                request.addRequestHeader("User-Agent", userAgent ?: "")
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "已开始下载", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "下载失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        view.loadUrl(url)
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
        else if (view != null) showSetup()
        else super.onBackPressed()
    }
    override fun onPause() { super.onPause(); CookieManager.getInstance().flush(); webView?.onPause() }
    override fun onResume() { super.onResume(); webView?.onResume() }
    override fun onDestroy() {
        picker.cancel()
        webView?.let { root.removeView(it); it.destroy() }; webView = null
        super.onDestroy()
    }
}
