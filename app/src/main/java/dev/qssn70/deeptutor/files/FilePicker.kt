package dev.qssn70.deeptutor.files

import android.app.Activity
import android.content.Intent
import android.webkit.ValueCallback
import android.net.Uri
import android.webkit.WebChromeClient

/** Exactly one pending chooser; cancelled callbacks are always resolved. */
class FilePicker(private val activity: Activity) {
    private var pending: ValueCallback<Array<Uri>>? = null
    fun open(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        pending?.onReceiveValue(null)
        pending = callback
        return try {
            val intent = params.createIntent().apply { addCategory(Intent.CATEGORY_OPENABLE) }
            activity.startActivityForResult(intent, REQUEST_CODE)
            true
        } catch (e: Exception) {
            pending = null
            callback.onReceiveValue(null)
            false
        }
    }
    fun onResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false
        pending?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
        pending = null
        return true
    }
    fun cancel() { pending?.onReceiveValue(null); pending = null }
    companion object { const val REQUEST_CODE = 4101 }
}
