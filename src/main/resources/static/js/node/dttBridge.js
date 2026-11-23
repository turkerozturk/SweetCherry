(() => {
    async function replacePanel(panel, url, options, errorMessage, conflictMessage, focusSelector) {
        const controls = [...panel.querySelectorAll('input, select, textarea, button')].filter(control => !control.disabled);
        const feedback = panel.querySelector('[data-dtt-feedback]');
        panel.dataset.pending = 'true';
        controls.forEach(control => { control.disabled = true; });
        feedback.textContent = '';
        feedback.classList.remove('text-danger');
        try {
            const response = await fetch(url, options);
            if (!panel.isConnected) return false;
            if (!response.ok || response.redirected) {
                feedback.textContent = response.status === 409 && conflictMessage ? conflictMessage : errorMessage;
                feedback.classList.add('text-danger');
                return false;
            }
            const fragment = new DOMParser().parseFromString(await response.text(), 'text/html');
            const next = fragment.querySelector('[data-dtt-panel]');
            if (!next) throw new Error('Missing DTT response');
            if (!panel.isConnected) return false;
            panel.replaceWith(next);
            next.querySelector(focusSelector)?.focus();
            return true;
        } catch {
            feedback.textContent = errorMessage;
            feedback.classList.add('text-danger');
            return false;
        } finally {
            panel.dataset.pending = 'false';
            controls.forEach(control => { control.disabled = false; });
        }
    }

    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!form.matches('[data-dtt-form]')) return;
        event.preventDefault();
        const panel = form.closest('[data-dtt-panel]');
        if (panel.dataset.pending === 'true') return;

        const operation = event.submitter?.value || 'save';
        if (operation === 'delete' && !window.confirm(form.dataset.confirmDelete)) return;

        const body = new URLSearchParams(new FormData(form));
        body.set('operation', operation);
        await replacePanel(panel, form.action, { method: 'POST', body }, form.dataset.error, form.dataset.conflict,
            'button[value="save"]');
    });

    document.addEventListener('change', async event => {
        const date = event.target;
        if (!date.matches('[data-dtt-date]') || date.value === date.defaultValue) return;
        const panel = date.closest('[data-dtt-panel]');
        const form = panel.querySelector('[data-dtt-form]');
        const note = form?.elements.namedItem('note');
        const status = form?.elements.namedItem('status');
        const defaultStatus = status && ([...status.options].find(option => option.defaultSelected) || status.options[0]);
        const dirty = (note && note.value !== note.defaultValue) || (status && status.value !== defaultStatus.value);
        if (panel.dataset.pending === 'true' || !date.checkValidity() || (dirty && !window.confirm(date.dataset.discard))) {
            date.value = date.defaultValue;
            return;
        }

        const url = new URL(date.dataset.endpoint, location.href);
        url.searchParams.set('_tenantView', panel.dataset.tenantView);
        url.searchParams.set('date', date.value);
        if (!await replacePanel(panel, url, { cache: 'no-store' }, panel.dataset.loadError, null, '[data-dtt-date]')) {
            date.value = date.defaultValue;
        }
    });
})();
