(() => {
    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!form.matches('[data-dtt-form]')) return;
        event.preventDefault();
        if (form.dataset.pending === 'true') return;

        const operation = event.submitter?.value || 'save';
        if (operation === 'delete' && !window.confirm(form.dataset.confirmDelete)) return;

        const body = new URLSearchParams(new FormData(form));
        body.set('operation', operation);
        const feedback = form.querySelector('[data-dtt-feedback]');
        const buttons = [...form.querySelectorAll('button[type="submit"]')];

        form.dataset.pending = 'true';
        buttons.forEach(button => { button.disabled = true; });
        feedback.textContent = '';
        feedback.classList.remove('text-danger');
        try {
            const response = await fetch(form.action, { method: 'POST', body });
            if (!form.isConnected) return;
            if (response.status === 409) {
                feedback.textContent = form.dataset.conflict;
                feedback.classList.add('text-danger');
                return;
            }
            if (!response.ok || response.redirected) throw new Error('DTT request failed');
            const documentFragment = new DOMParser().parseFromString(await response.text(), 'text/html');
            const panel = documentFragment.querySelector('[data-dtt-panel]');
            if (!panel) throw new Error('Missing DTT response');
            form.closest('[data-dtt-panel]')?.replaceWith(panel);
            panel.querySelector('button[value="save"]')?.focus();
        } catch {
            feedback.textContent = form.dataset.error;
            feedback.classList.add('text-danger');
        } finally {
            form.dataset.pending = 'false';
            buttons.forEach(button => { button.disabled = false; });
        }
    });
})();
