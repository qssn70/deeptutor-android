package dev.qssn70.deeptutor.web

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import android.content.Context

class DeepTutorWebClient(
    private val context: Context,
    private val policy: NavigationPolicy,
    private val onMainFrameError: (String) -> Unit,
    private val onRendererGone: () -> Unit,
    private val onPageStarted: () -> Unit = {},
    private val onPageFinished: () -> Unit = {}
) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        onPageStarted()
    }
    override fun onPageFinished(view: WebView, url: String?) {
        onPageFinished()
    }
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (policy.inApp(request.url)) return false
        val uri = request.url
        if (uri.scheme == "https" || uri.scheme == "http" || uri.scheme == "mailto" || uri.scheme == "tel") {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (_: ActivityNotFoundException) { }
        }
        return true
    }
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onMainFrameError(error.description.toString())
    }
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRendererGone()
        return true
    }
}
