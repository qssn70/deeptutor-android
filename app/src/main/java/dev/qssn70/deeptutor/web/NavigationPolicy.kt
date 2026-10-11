package dev.qssn70.deeptutor.web

import android.net.Uri

/** In-app navigation is limited to the configured server origin. */
class NavigationPolicy(baseUrl: String) {
    private val base = Uri.parse(baseUrl)
    fun inApp(target: Uri): Boolean =
        target.scheme.equals("https", true) &&
        target.host.equals(base.host, true) &&
        target.port == base.port
}
