(() => {
    'use strict';
    const root = document.querySelector('[data-markmap-children]');
    const map = document.getElementById('markmap');
    if (!root || !map) return;
    const dialog = root.querySelector('dialog');
    const form = root.querySelector('form');
    const title = form.elements.namedItem('title');
    const parent = form.elements.namedItem('parentId');
    const cancel = root.querySelector('[data-markmap-child-cancel]');
    const submit = form.querySelector('[type=submit]');
    const status = root.querySelector('[data-markmap-child-status]');
    const parentLabel = root.querySelector('#markmap-child-parent');
    let saving = false, blocked = false, opener = null, revealLevel = 0;

    function showStatus(message) { status.textContent = message; }
    function close() { if (!saving) dialog.close(); }
    cancel.addEventListener('click', close);
    dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    dialog.addEventListener('close', () => { if (opener?.isConnected) opener.focus(); });
    // Capture before Markmap's zoom handlers; clicking + must not navigate or fold a branch.
    for (const type of ['pointerdown', 'mousedown', 'touchstart', 'dblclick']) {
        map.addEventListener(type, event => {
            if (event.target.closest('.markmap-add-child')) event.stopPropagation();
        }, true);
    }
    map.addEventListener('click', event => {
        const button = event.target.closest('.markmap-add-child');
        if (!button || !map.contains(button)) return;
        event.preventDefault();
        event.stopPropagation();
        if (saving || dialog.open) return;
        const id = button.dataset.parentId;
        if (!/^[1-9][0-9]{0,18}$/.test(id || '')) return;
        opener = button;
        parent.value = id; // An occurrence ID, never its shared-content master or rounded JS number.
        parentLabel.textContent = button.closest('.markmap-node-label')
            ?.querySelector('.markmap-node-title')?.textContent || '';
        revealLevel = Number(button.dataset.revealLevel) || 0;
        title.value = '';
        submit.disabled = blocked;
        showStatus(blocked ? root.dataset.reload : '');
        dialog.showModal();
        title.focus();
    }, true);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || blocked || !form.reportValidity()) return;
        if (!title.value.trim()) { title.focus(); return; }
        const body = new URLSearchParams(new FormData(form));
        saving = true;
        submit.disabled = cancel.disabled = title.disabled = true;
        showStatus(root.dataset.saving);
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 20000);
        try {
            const response = await fetch(form.action, {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {Accept: 'application/json'}, body, signal: controller.signal
            });
            if (response.redirected) throw new Error('Login expired');
            if (!response.ok) {
                const message = response.status === 409 ? root.dataset.stale
                    : response.status === 403 ? root.dataset.denied
                    : response.status === 400 || response.status === 404 ? root.dataset.invalid
                    : root.dataset.uncertain;
                // Only validation rejection is safe to correct and resubmit immediately.
                blocked = response.status !== 400;
                showStatus(message);
                return;
            }
            if (!(response.headers.get('Content-Type') || '').includes('application/json')) throw new Error('Invalid response');
            const receipt = await response.json();
            if (typeof receipt.id !== 'string' || !/^[1-9][0-9]{0,18}$/.test(receipt.id)) throw new Error('Missing receipt');
            blocked = true; // Never send a second creation while the refreshed page is loading.
            const url = new URL(window.location.href);
            const level = Number(url.searchParams.get('level') ?? 3);
            url.searchParams.set('level', String(Math.max(Number.isFinite(level) ? level : 3, revealLevel)));
            url.searchParams.set('childOf', parent.value);
            url.searchParams.set('_tenantView', form.elements.namedItem('_tenantView').value);
            window.location.assign(url.href);
        } catch (_) {
            // A lost response can follow a committed write: do not retry automatically or offer a second save.
            blocked = true;
            showStatus(root.dataset.uncertain);
        } finally {
            clearTimeout(timer);
            saving = false;
            title.disabled = cancel.disabled = false;
            submit.disabled = blocked;
        }
    });
})();
