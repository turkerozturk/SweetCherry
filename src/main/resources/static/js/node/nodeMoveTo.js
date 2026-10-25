/* Delegation also handles tree content fragments loaded after navigation. */
(() => {
    'use strict';
    function message(form, text, error = false) {
        const area = form.querySelector('[data-move-to-message]');
        area.textContent = text;
        area.classList.toggle('text-danger', error);
    }
    function controls(form, saving) {
        form.querySelector('[data-move-to-close]').disabled = saving;
        form.elements.parentId.readOnly = saving;
        form.querySelector('[type="submit"]').disabled = form.dataset.phase !== 'ready';
    }
    async function open(group) {
        const dialog = group.querySelector('dialog'), form = dialog.querySelector('form');
        if (dialog.open) return;
        form.reset();
        form.dataset.phase = 'loading';
        const load = String(Number(form.dataset.load || 0) + 1);
        form.dataset.load = load;
        controls(form, false);
        message(form, form.dataset.loading);
        dialog.showModal();
        form.elements.parentId.focus();
        try {
            const url = new URL(form.dataset.stateUrl, window.location.href);
            url.searchParams.set('_tenantView', form.elements._tenantView.value);
            const response = await fetch(url, {credentials:'same-origin', cache:'no-store', headers:{Accept:'application/json'}});
            if (!response.ok || response.redirected) throw new Error('state');
            const state = await response.json();
            if (typeof state.revision !== 'string' || !state.revision) throw new Error('state');
            if (form.dataset.load !== load) return;
            form.elements.revision.value = state.revision;
            form.dataset.phase = 'ready';
            message(form, '');
        } catch (_) {
            if (form.dataset.load !== load) return;
            form.dataset.phase = 'failed';
            message(form, form.dataset.stateError, true);
        }
        controls(form, false);
    }
    document.addEventListener('click', event => {
        const opener = event.target.closest('[data-move-to-open]');
        if (opener) { open(opener.closest('[data-node-move-to]')); return; }
        const closer = event.target.closest('[data-move-to-close]');
        if (closer && !closer.disabled) closer.closest('dialog').close();
    });
    // Escape closes this dialog, not an underlying mobile navigation drawer.
    document.addEventListener('keydown', event => {
        const dialog = event.target.closest('.node-move-to-dialog');
        if (!dialog || event.key !== 'Escape') return;
        event.stopPropagation();
        if (dialog.querySelector('form').dataset.phase === 'saving') event.preventDefault();
    }, true);
    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!form.matches('[data-node-move-to] form')) return;
        event.preventDefault();
        if (form.dataset.phase !== 'ready' || !form.reportValidity()) return;
        form.dataset.phase = 'saving';
        controls(form, true);
        message(form, form.dataset.loading);
        try {
            const response = await fetch(form.action, {method:'POST', credentials:'same-origin', redirect:'error',
                headers:{Accept:'application/json'}, body:new URLSearchParams(new FormData(form))});
            if (response.status === 204) {
                window.location.assign(form.elements.returnUrl.value);
                return;
            }
            const key = ({400:'invalid',404:'missing',422:'cycle',409:'stale',401:'denied',403:'denied'})[response.status] || 'failed';
            // A stale snapshot must be explicitly refreshed by closing and reopening the dialog.
            form.dataset.phase = [400,404,422].includes(response.status) ? 'ready' : 'failed';
            message(form, form.dataset[key], true);
        } catch (_) {
            // A lost response may follow a committed move. Never silently retry a write.
            form.dataset.phase = 'failed';
            message(form, form.dataset.failed, true);
        }
        controls(form, false);
    });
})();
