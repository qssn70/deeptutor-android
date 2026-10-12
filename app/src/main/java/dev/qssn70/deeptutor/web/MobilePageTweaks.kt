package dev.qssn70.deeptutor.web

import android.net.Uri
import android.webkit.WebView

/**
 * WebView-only compatibility adjustments for the official DeepTutor website.
 * No JavaScript bridge and no changes to the user's server are required.
 */
object MobilePageTweaks {
    private val installScript = """
        (() => {
          const id = 'deeptutor-android-mobile-fixes-v2';
          document.getElementById(id)?.remove();
          const style = document.createElement('style');
          style.id = id;
          style.textContent = [
            '@media (max-width: 767px) {',
            ' main pre, main .katex-display { max-width:100%; overflow-x:auto !important; }',
            ' [role="dialog"][aria-label="资源"],[role="dialog"][aria-label="Resources"] {',
            '   position:fixed !important; left:12px !important; right:12px !important;',
            '   top:12px !important; bottom:12px !important; width:auto !important;',
            '   height:min(72dvh, 620px) !important; max-height:min(72dvh, 620px) !important;',
            '   transform:none !important; display:flex !important;',
            '   flex-direction:column !important; overflow:hidden !important;',
            '   touch-action:auto !important;',
            ' }',
            ' [role="dialog"][aria-label="资源"] > :first-child,',
            ' [role="dialog"][aria-label="Resources"] > :first-child { flex:0 0 auto !important; }',
            ' [role="dialog"][aria-label="资源"] > :last-child,',
            ' [role="dialog"][aria-label="Resources"] > :last-child {',
            '   flex:1 1 0% !important; height:0 !important; min-height:0 !important;',
            '   max-height:none !important; overflow-y:auto !important;',
            '   overscroll-behavior:contain; touch-action:pan-y !important;',
            '   -webkit-overflow-scrolling:touch;',
            ' }',
            '}'
          ].join(' ');
          document.head.appendChild(style);
          // Upstream ComposerResources calls searchRef.focus() when the dialog opens.\n          // Suppress only that programmatic focus; a real tap still focuses the input.\n          if (!window.__deeptutorAndroidOriginalFocus) {\n            const originalFocus = HTMLElement.prototype.focus;\n            window.__deeptutorAndroidOriginalFocus = originalFocus;\n            HTMLElement.prototype.focus = function(...args) {\n              const dialog = this.closest?.('[role=\"dialog\"]');\n              const label = dialog?.getAttribute('aria-label');\n              if (this.matches?.('input') && (label === '资源' || label === 'Resources')) return;\n              return originalFocus.apply(this, args);\n            };\n          }\n          // SPA navigation does not reload WebView. CSS stays installed and
          // automatically matches dialogs created later by React portals.
          return 'resource-sheet-v2-installed';
        })();
    """.trimIndent()

    private val removeScript = """
        (() => {
          document.getElementById('deeptutor-android-mobile-fixes-v2')?.remove();
          document.getElementById('deeptutor-android-reading-fixes-v1')?.remove();
          window.__deeptutorAndroidResourceFix?.observer?.disconnect();
          delete window.__deeptutorAndroidResourceFix;\n          if (window.__deeptutorAndroidOriginalFocus) {\n            HTMLElement.prototype.focus = window.__deeptutorAndroidOriginalFocus;\n            delete window.__deeptutorAndroidOriginalFocus;\n          }
        })();
    """.trimIndent()

    fun apply(view: WebView, navigation: NavigationPolicy, enabled: Boolean) {
        val url = view.url?.let(Uri::parse) ?: return
        if (!navigation.inApp(url)) return
        view.evaluateJavascript(if (enabled) installScript else removeScript, null)
    }
}
