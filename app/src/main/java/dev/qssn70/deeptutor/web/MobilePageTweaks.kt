package dev.qssn70.deeptutor.web

import android.net.Uri
import android.webkit.WebView

/**
 * Conservative, optional fixes for long formulas and code on narrow screens.
 *
 * This must NOT manipulate DeepTutor's React tree, authentication, sidebars,
 * chat input, WebSocket, or viewport meta. Upstream owns responsive layout.
 * A single identifiable <style> element makes these rules easy to remove.
 */
object MobilePageTweaks {
    private val installScript = """
        (function () {
          if (!document.head) return;
          const id = 'deeptutor-android-reading-fixes-v1';
          if (document.getElementById(id)) return;
          const style = document.createElement('style');
          style.id = id;
          style.textContent = [
            '@media (max-width: 767px) {',
            '  main pre, main .katex-display {',
            '    max-width: 100%;',
            '    overflow-x: auto !important;',
            '    -webkit-overflow-scrolling: touch;',
            '  }',
            '  main .katex-display { overflow-y: hidden; }',
            '  /* DeepTutor ComposerResources: inline window.innerHeight positioning can place', 
            '     the dialog partly below the visual viewport in Android WebView. */',
            '  [role="dialog"][aria-label="资源"],',
            '  [role="dialog"][aria-label="Resources"] {',
            '    top: 12px !important;',
            '    bottom: auto !important;',
            '    max-height: calc(100dvh - 24px) !important;',
            '    height: auto !important;',
            '    display: flex !important;',
            '    flex-direction: column !important;',
            '  }',
            '  [role="dialog"][aria-label="资源"] > div:last-child,',
            '  [role="dialog"][aria-label="Resources"] > div:last-child {',
            '    min-height: 0 !important;',
            '    overflow-y: auto !important;',
            '    overscroll-behavior: contain;',
            '    -webkit-overflow-scrolling: touch;',
            '  }',
            '}'
          ].join('\\n');
          document.head.appendChild(style);
        })();
    """.trimIndent()

    private val removeScript = """
        (function () {
          document.getElementById('deeptutor-android-reading-fixes-v1')?.remove();
        })();
    """.trimIndent()

    fun apply(view: WebView, navigation: NavigationPolicy, enabled: Boolean) {
        val url = view.url?.let(Uri::parse) ?: return
        if (!navigation.inApp(url)) return
        view.evaluateJavascript(if (enabled) installScript else removeScript, null)
    }
}
