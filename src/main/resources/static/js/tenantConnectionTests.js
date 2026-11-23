(() => {
    'use strict';
    const form = document.getElementById('tenantWizardForm');
    const messages = document.getElementById('wizardTestMessages')?.dataset;
    if (!form || !messages) return;

    const fields = {
        dtt: ['dttBaseUrl', 'dttTokenEnv'],
        database: ['type', 'path', 'url', 'username', 'password', 'tenant', 'revision', 'createEmpty', 'clearPassword']
    };

    function parameters(kind) {
        const body = new URLSearchParams();
        for (const name of [...fields[kind], '_csrf']) {
            const field = form.elements.namedItem(name);
            if (field) body.set(name, field.type === 'checkbox' ? String(field.checked) : field.value);
        }
        return body;
    }

    for (const kind of Object.keys(fields)) {
        const button = form.querySelector('[data-wizard-test="' + kind + '"]');
        const status = form.querySelector('[data-wizard-test-result="' + kind + '"]');
        button.addEventListener('click', async () => {
            const body = parameters(kind);
            const contents = body.toString();
            const abort = new AbortController();
            const timeout = setTimeout(() => abort.abort(), 7500);
            button.disabled = true;
            status.textContent = messages.loading;
            let outcome = 'failed';
            try {
                const endpoint = form.action.replace(/\/$/, '') + '/test-' + kind;
                const response = await fetch(endpoint, {method: 'POST', body, cache: 'no-store',
                    credentials: 'same-origin', redirect: 'error', signal: abort.signal});
                if (response.ok && response.headers.get('Content-Type')?.includes('application/json')) {
                    outcome = (await response.json()).result;
                }
            } catch (_) {
                if (abort.signal.aborted) outcome = 'timeout';
            } finally {
                clearTimeout(timeout);
                button.disabled = false;
                status.textContent = parameters(kind).toString() === contents
                    ? (Object.hasOwn(messages, outcome) ? messages[outcome] : messages.failed) : '';
            }
        });
    }
})();
