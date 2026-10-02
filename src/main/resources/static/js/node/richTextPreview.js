(() => {
    'use strict';
    const loading = new Map();
    /** Highlights detached preview DOM; CTB scripts never execute inside the sandbox. */
    async function highlightFrame(frame) {
        const doc = new DOMParser().parseFromString(frame.getAttribute('srcdoc'), 'text/html');
        for (const code of doc.querySelectorAll('code.cherry-highlight[data-language]')) {
            const name = code.dataset.language;
            if (!/^[a-z0-9-]+$/.test(name)) continue;
            try {
                if (!hljs.getLanguage(name)) {
                    if (!loading.has(name)) loading.set(name, new Promise((resolve, reject) => {
                        const script = document.createElement('script');
                        script.src = `/js/thirdparty/highlightjs/languages/${name}.min.js`;
                        script.onload = resolve;
                        script.onerror = reject;
                        document.head.appendChild(script);
                    }));
                    await loading.get(name);
                }
                code.innerHTML = hljs.highlight(code.textContent, {language: name}).value;
                code.classList.add('hljs');
            } catch (_) { /* Keep escaped plain code if the grammar is unavailable. */ }
        }
        frame.srcdoc = doc.documentElement.outerHTML;
    }
    if (window.hljs) document.querySelectorAll('iframe[srcdoc]').forEach(highlightFrame);
})();
