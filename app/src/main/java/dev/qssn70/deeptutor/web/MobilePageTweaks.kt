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
          document.getElementById(id)?.remove();
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
            '    transform: none !important;',
            '    bottom: auto !important;',
            '    max-height: calc(100dvh - 24px) !important;',
            '    height: calc(100dvh - 24px) !important;',
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
          ].join(' ');
          document.head.appendChild(style);
          // DeepTutor renders the resource dialog via a portal and computes
          // its top/height inline. CSS alone can be overridden by subsequent
          // React renders; watch for the dialog and re-clamp it.
          if (!window.__deeptutorAndroidResourceFix) {
            let scheduled = false;
            const fix = () => {
              scheduled = false;
              const dialog = document.querySelector('[role="dialog"][aria-label="资源"],[role="dialog"][aria-label="Resources"]');
              if (!dialog || window.innerWidth >= 768) return;
              const viewport = window.visualViewport;
              const height = viewport?.height || window.innerHeight;
              const top = (viewport?.offsetTop || 0) + 12;
              const maxHeight = Math.max(180, height - 24);
              if (dialog.style.getPropertyValue('height') !== maxHeight + 'px')
                dialog.style.setProperty('height', maxHeight + 'px', 'important');
              if (dialog.style.getPropertyValue('transform') !== 'none')
                dialog.style.setProperty('transform', 'none', 'important');
              if (dialog.style.getPropertyValue('top') !== top + 'px')
                dialog.style.setProperty('top', top + 'px', 'important');
              if (dialog.style.getPropertyValue('max-height') !== maxHeight + 'px')
                dialog.style.setProperty('max-height', maxHeight + 'px', 'important');
              if (dialog.style.getPropertyValue('bottom') !== 'auto')
                dialog.style.setProperty('bottom', 'auto', 'important');
              const body = dialog.lastElementChild;
              if (body) {
                body.style.setProperty('min-height', '0', 'important');
                body.style.setProperty('flex', '1 1 auto', 'important');
                body.style.setProperty('overflow-y', 'auto', 'important');
                body.style.setProperty('touch-action', 'pan-y', 'important');
                body.style.setProperty('-webkit-overflow-scrolling', 'touch');
              }
            };
            const schedule = () => {
              if (scheduled) return;
              scheduled = true;
              requestAnimationFrame(fix);
            };
            const observer = new MutationObserver(schedule);
            observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['style'] });
            window.visualViewport?.addEventListener('resize', schedule);
            window.addEventListener('resize', schedule);
            window.__deeptutorAndroidResourceFix = { observer, schedule };
          }
          window.__deeptutorAndroidResourceFix.schedule();
        })();
    """.trimIndent()

    private val removeScript = """
        (function () {
          document.getElementById('deeptutor-android-reading-fixes-v1')?.remove();
          window.__deeptutorAndroidResourceFix?.observer.disconnect();
          delete window.__deeptutorAndroidResourceFix;
        })();
    """.trimIndent()

    fun apply(view: WebView, navigation: NavigationPolicy, enabled: Boolean) {
        val url = view.url?.let(Uri::parse) ?: return
        if (!navigation.inApp(url)) return
        view.evaluateJavascript(if (enabled) installScript else removeScript, null)
    }
}
