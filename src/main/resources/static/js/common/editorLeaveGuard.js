(() => {
    'use strict';
    window.EditorLeaveGuard = {
        install(form, isDirty) {
            let leaving = false;
            const message = form.dataset.leaveMessage;
            window.addEventListener('beforeunload', event => {
                if (!leaving && isDirty()) { event.preventDefault(); event.returnValue = ''; }
            });
            window.addEventListener('pageshow', () => { leaving = false; });
            document.addEventListener('click', event => {
                if (event.defaultPrevented || event.button > 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
                const link = event.target.closest('a[href]');
                if (!link || link.hasAttribute('download') || (link.target && link.target !== '_self')) return;
                const url = new URL(link.href, window.location.href);
                if (url.origin === window.location.origin && url.pathname === window.location.pathname && url.search === window.location.search && url.hash) return;
                if (!leaving && isDirty()) {
                    if (!window.confirm(message)) { event.preventDefault(); event.stopImmediatePropagation(); }
                    else leaving = true;
                }
            }, true);
            // Install after the editor's own submit handler: blocked saves keep protection enabled.
            form.addEventListener('submit', event => { if (!event.defaultPrevented) leaving = true; });
        }
    };
})();
