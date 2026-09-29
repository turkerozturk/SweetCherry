(() => {
    'use strict';

    if (!window.hljs) return;
    const grammarRoot = new URL('languages/', document.currentScript.src);
    const loading = new Map();

    /** Loads only locally bundled grammars requested by the server's language allowlist. */
    function loadLanguage(name) {
        if (window.hljs.getLanguage(name)) return Promise.resolve();
        if (!loading.has(name)) {
            loading.set(name, new Promise((resolve, reject) => {
                const script = document.createElement('script');
                script.src = new URL(`${name}.min.js`, grammarRoot).href;
                script.onload = resolve;
                script.onerror = reject;
                document.head.appendChild(script);
            }));
        }
        return loading.get(name);
    }

    /** Highlights text nodes; if a grammar cannot load, the escaped code stays readable. */
    function highlightCode(root = document) {
        root.querySelectorAll('code.cherry-highlight[data-language]:not([data-highlighted])')
            .forEach(async code => {
                const name = code.dataset.language;
                if (!/^[a-z0-9-]+$/.test(name)) return;
                try {
                    await loadLanguage(name);
                    window.hljs.highlightElement(code);
                } catch (error) {
                    // Keep the original text when a language module is unavailable.
                }
            });
    }

    highlightCode();
    document.addEventListener('htmx:afterSwap', event => highlightCode(event.detail.target));
})();
