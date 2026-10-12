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
            ' main pre, main .katex-display { max-width:100%; overflow-x:auto !important; }',
            ' [role="dialog"][aria-label="资源"],[role="dialog"][aria-label="Resources"] {',
            '   position:fixed !important; left:12px !important; right:12px !important;',
            '   top:12px !important; bottom:12px !important; width:auto !important;',
            '   height:auto !important; max-height:72dvh !important;',
            '   transform:none !important; display:flex !important;',
            '   flex-direction:column !important; overflow:hidden !important;',
            '   touch-action:auto !important;',
            ' }',
            ' [role="dialog"][aria-label="资源"] > :first-child,',
            ' [role="dialog"][aria-label="Resources"] > :first-child { flex:0 0 auto !important; }',
            ' [role="dialog"][aria-label="资源"] > :last-child,',
            ' [role="dialog"][aria-label="Resources"] > :last-child {',
            '   flex:0 1 auto !important; height:auto !important; min-height:0 !important;',
            '   max-height:calc(72dvh - 40px) !important; overflow-y:auto !important;',
            '   overscroll-behavior:contain; touch-action:pan-y !important;',
            '   -webkit-overflow-scrolling:touch;',
            ' }',
            ' [role="dialog"][aria-label="资源"] > :last-child > *,',
            ' [role="dialog"][aria-label="Resources"] > :last-child > * {',
            '   height:auto !important; min-height:max-content !important;',
            ' }'
          ].join(' ');
          document.head.appendChild(style);
          // Upstream ComposerResources calls searchRef.focus() when the dialog opens.
          // Suppress only that programmatic focus; a real tap still focuses the input.
          if (!window.__deeptutorAndroidOriginalFocus) {
            const originalFocus = HTMLElement.prototype.focus;
            window.__deeptutorAndroidOriginalFocus = originalFocus;
            HTMLElement.prototype.focus = function(...args) {
              const dialog = this.closest?.('[role=\"dialog\"]');
              const label = dialog?.getAttribute('aria-label');
              if (this.matches?.('input') && (label === '资源' || label === 'Resources')) return;
              return originalFocus.apply(this, args);
            };
          }
          // Temporary on-device diagnostics: only inspect; do not modify scrolling.
          if (!window.__deeptutorResourceDiagnostics) {
            const installDiagnostics = () => {
              const dialog = document.querySelector('[role="dialog"][aria-label="资源"],[role="dialog"][aria-label="Resources"]');
              if (!dialog || dialog.querySelector('[data-resource-diagnostics]')) return;
              const header = dialog.firstElementChild;
              if (!header) return;
              const button = document.createElement('button');
              button.type = 'button';
              button.textContent = '诊断';
              button.setAttribute('data-resource-diagnostics', 'true');
              button.style.cssText = 'flex:none;font-size:12px;padding:5px 8px;border:1px solid #888;border-radius:6px;margin-right:4px;';
              button.addEventListener('click', () => {
                const body = dialog.lastElementChild;
                const outer = dialog.getBoundingClientRect();
                const inner = body?.getBoundingClientRect();
                const before = body?.scrollTop ?? 0;
                if (body) body.scrollTop = before + 100;
                const after = body?.scrollTop ?? 0;
                if (body) body.scrollTop = before;
                const css = body ? getComputedStyle(body) : null;
                const dcss = getComputedStyle(dialog);
                const info = [
                  'Viewport: ' + innerWidth + 'x' + innerHeight,
                  'Dialog: ' + Math.round(outer.width) + 'x' + Math.round(outer.height),
                  'Dialog CSS: ' + dcss.display + ', overflow=' + dcss.overflowY,
                  'Body: ' + (inner ? Math.round(inner.width) + 'x' + Math.round(inner.height) : 'missing'),
                  'Body client/scroll: ' + (body?.clientHeight ?? '-') + '/' + (body?.scrollHeight ?? '-'),
                  'Body CSS: overflow=' + css?.overflowY + ', flex=' + css?.flex + ', touch=' + css?.touchAction,
                  'ScrollTop test: ' + before + ' -> ' + after,
                  'Children: ' + (body?.children.length ?? 0),
                  'CSS loaded: ' + !!document.getElementById(id)
                ].join('\\n');
                alert(info);
              });
              header.insertBefore(button, header.lastElementChild);
            };
            const observer = new MutationObserver(() => installDiagnostics());
            observer.observe(document.body, {childList:true, subtree:true});
            window.__deeptutorResourceDiagnostics = observer;
            installDiagnostics();
          }
          // SPA navigation does not reload WebView. CSS stays installed and
          // automatically matches dialogs created later by React portals.
          return 'resource-sheet-v2-installed';
        })();
    """.trimIndent()

    private val removeScript = """
        (() => {
          document.getElementById('deeptutor-android-mobile-fixes-v2')?.remove();
          document.getElementById('deeptutor-android-reading-fixes-v1')?.remove();
          window.__deeptutorAndroidResourceFix?.observer?.disconnect();
          delete window.__deeptutorAndroidResourceFix;
          window.__deeptutorResourceDiagnostics?.disconnect();
          delete window.__deeptutorResourceDiagnostics;
          if (window.__deeptutorAndroidOriginalFocus) {
            HTMLElement.prototype.focus = window.__deeptutorAndroidOriginalFocus;
            delete window.__deeptutorAndroidOriginalFocus;
          }
        })();
    """.trimIndent()

    fun apply(view: WebView, navigation: NavigationPolicy, enabled: Boolean) {
        val url = view.url?.let(Uri::parse) ?: return
        if (!navigation.inApp(url)) return
        view.evaluateJavascript(if (enabled) installScript else removeScript, null)
    }
}
