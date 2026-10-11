package dev.qssn70.deeptutor.files

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import dev.qssn70.deeptutor.web.NavigationPolicy

/** Authenticated, same-origin HTTPS downloads. Blob URLs are not supported. */
class DownloadHandler(
    private val context: Context,
    private val navigation: NavigationPolicy
) : DownloadListener {
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        if (url.isNullOrBlank()) return
        val uri = Uri.parse(url)
        if (!navigation.inApp(uri)) {
            notifyUser("只支持当前 DeepTutor 服务器的 HTTPS 下载")
            return
        }
        try {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                .replace('/', '_').replace('\\', '_').take(150)
            val request = DownloadManager.Request(uri)
                .setTitle(filename)
                .setMimeType(mimeType ?: "application/octet-stream")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            CookieManager.getInstance().getCookie(url)?.let {
                request.addRequestHeader("Cookie", it)
            }
            userAgent?.let { request.addRequestHeader("User-Agent", it) }
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            notifyUser("已开始下载：" + filename)
        } catch (error: Exception) {
            notifyUser("下载失败：" + (error.message ?: "未知错误"))
        }
    }

    private fun notifyUser(message: String) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
