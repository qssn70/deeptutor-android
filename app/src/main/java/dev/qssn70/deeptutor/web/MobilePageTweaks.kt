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
            '   height:72dvh !important; min-height:72dvh !important; max-height:72dvh !important;',
            '   transform:none !important; display:flex !important;',
            '   flex-direction:column !important; overflow:hidden !important;',
            '   touch-action:auto !important;',
            ' }',
            ' [role="dialog"][aria-label="资源"] > :first-child,',
            ' [role="dialog"][aria-label="Resources"] > :first-child { flex:0 0 auto !important; }',
            ' [role="dialog"][aria-label="资源"] > :last-child,',
            ' [role="dialog"][aria-label="Resources"] > :last-child {',
            '   flex:1 1 auto !important; height:auto !important; min-height:0 !important;',
            '   max-height:none !important; overflow-y:auto !important;',
            '   overscroll-behavior:contain; touch-action:pan-y !important;',
            '   -webkit-overflow-scrolling:touch;',
            ' }',
            ' [role="dialog"][aria-label="资源"] > :last-child > *,',
            ' [role="dialog"][aria-label="Resources"] > :last-child > * {',
            '   height:auto !important; min-height:0 !important; flex:none !important;',
            ' }'
          ].join(' ');
          document.head.appendChild(style);
          // Preserve intrinsic height of the animated inner content, which can otherwise
          // report the same scrollHeight as clientHeight despite clipped descendants.
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
          // React/Framer Motion may paint descendants outside their measured parent.
          // Give the actual content wrapper an explicit height so WebView can scroll.
          if (!window.__deeptutorResourceScrollRepair) {
            let pending = false;
            let lastInner = null;
            let resizeObserver = null;
            const repair = () => {
              pending = false;
              const dialog = document.querySelector('[role="dialog"][aria-label="资源"],[role="dialog"][aria-label="Resources"]');
              if (!dialog) return;
              const body = dialog.lastElementChild;
              const inner = body?.firstElementChild;
              if (!inner) return;
              const header = dialog.firstElementChild;
              const available = Math.max(100, dialog.clientHeight - (header?.offsetHeight || 40));
              body.style.setProperty('height', available + 'px', 'important');
              body.style.setProperty('max-height', available + 'px', 'important');
              body.style.setProperty('flex', '0 0 auto', 'important');
              body.style.setProperty('overflow-y', 'auto', 'important');
              // Compute in content coordinates, independent of current scrollTop.
              const base = inner.getBoundingClientRect().top;
              let bottom = base;
              for (const el of inner.querySelectorAll('section, section *, button, input')) {
                const rect = el.getBoundingClientRect();
                if (rect.height > 0) bottom = Math.max(bottom, rect.bottom);
              }
              const required = Math.ceil(bottom - base + 2);
              if (required > 0) {
                inner.style.setProperty('height', required + 'px', 'important');
                inner.style.setProperty('min-height', required + 'px', 'important');
              }
              if (inner !== lastInner) {
                resizeObserver?.disconnect();
                lastInner = inner;
                resizeObserver = new ResizeObserver(schedule);
                for (const child of inner.children) resizeObserver.observe(child);
              }
            };
            const schedule = () => {
              if (pending) return;
              pending = true;
              requestAnimationFrame(repair);
            };
            const observer = new MutationObserver(schedule);
            observer.observe(document.body, {childList:true, subtree:true});
            window.addEventListener('resize', schedule);
            window.__deeptutorResourceScrollRepair = {
              observer, schedule, disconnect: () => {observer.disconnect(); resizeObserver?.disconnect();}
            };
            schedule();
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
                  'Inner height/scroll: ' + (body?.firstElementChild?.offsetHeight ?? '-') + '/' + (body?.firstElementChild?.scrollHeight ?? '-'),
                  'Inner computed: ' + (() => { const e=body?.firstElementChild; if(!e)return '-'; const c=getComputedStyle(e); return c.display+'/'+c.position+'/'+c.transform+'/'+c.overflowY; })(),
                  'Inner children: ' + (() => { const e=body?.firstElementChild; return e ? Array.from(e.children).map(n => n.tagName+':'+Math.round(n.getBoundingClientRect().height)+'/'+n.scrollHeight).join(',') : '-'; })(),
                  'Sections: ' + (() => { const e=body?.firstElementChild; return e ? Array.from(e.querySelectorAll('section')).map(n => {const r=n.getBoundingClientRect();return Math.round(r.top)+':'+Math.round(r.bottom)+':'+n.scrollHeight;}).join(',') : '-'; })(),
                  'Last item bottom: ' + (() => {const e=body?.firstElementChild; const nodes=e?.querySelectorAll('button');return nodes?.length ? Math.round(nodes[nodes.length-1].getBoundingClientRect().bottom) : '-';})(),
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
          window.__deeptutorResourceScrollRepair?.disconnect();
          delete window.__deeptutorResourceScrollRepair;
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
