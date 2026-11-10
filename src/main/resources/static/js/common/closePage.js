(() => {
    'use strict';
    document.querySelectorAll('[data-close-page]').forEach(button => {
        button.addEventListener('click', () => {
            try { window.close(); } catch (_) { /* Some browsers refuse to close manually opened tabs. */ }
            window.setTimeout(() => {
                if (!window.closed) window.location.assign(button.dataset.closeFallback);
            }, 150);
        });
    });
})();
