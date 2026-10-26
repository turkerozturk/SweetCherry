/* Delegation also handles tree content fragments loaded after navigation. */
(() => {
    'use strict';
    function message(form, text, error = false) {
        const area = form.querySelector('[data-move-to-message]');
        area.textContent = text;
        area.classList.toggle('text-danger', error);
    }
    function closeBookmarks(form) {
        form.querySelector('[data-move-to-bookmarks-panel]').hidden = true;
        form.querySelector('[data-move-to-bookmarks]').setAttribute('aria-expanded', 'false');
        form.dataset.bookmarkRequest = String(Number(form.dataset.bookmarkRequest || 0) + 1);
    }
    function fillParent(form, value) {
        form.elements.parentId.value = value;
        form.elements.parentId.dispatchEvent(new Event('input', {bubbles:true}));
        message(form, '');
        form.elements.parentId.focus();
    }
    // Match title text only (not the displayed ID), ignoring case and accents.
    function filterBookmarks(form) {
        const panel = form.querySelector('[data-move-to-bookmarks-panel]');
        if (panel.dataset.loaded !== 'true') return;
        const normalize = value => value.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/ı/g, 'i');
        const query = normalize(form.querySelector('[data-move-to-bookmarks-filter]').value.trim());
        const choices = [...form.querySelectorAll('[data-bookmark-target]')];
        let visible = 0;
        choices.forEach(choice => {
            const name = choice.querySelector('.node-move-to-bookmark-name').textContent;
            choice.hidden = !normalize(name).includes(query);
            if (!choice.hidden) visible++;
        });
        form.querySelector('[data-move-to-bookmarks-status]').textContent = !choices.length
            ? form.dataset.bookmarksEmpty : visible ? '' : form.dataset.bookmarksNoMatch;
    }
    document.addEventListener('input', event => {
        if (event.target.matches('[data-move-to-bookmarks-filter]')) filterBookmarks(event.target.closest('form'));
    });
    async function bookmarks(button) {
        const form = button.closest('form'), dialog = button.closest('dialog');
        if (button.disabled || form.dataset.phase !== 'ready' || form.dataset.pasting === 'true') return;
        const panel = form.querySelector('[data-move-to-bookmarks-panel]');
        if (!panel.hidden) { closeBookmarks(form); return; }
        const list = form.querySelector('[data-move-to-bookmarks-list]');
        const status = form.querySelector('[data-move-to-bookmarks-status]');
        list.replaceChildren();
        panel.dataset.loaded = 'false';
        form.querySelector('[data-move-to-bookmarks-filter]').value = '';
        status.textContent = form.dataset.bookmarksLoading;
        status.classList.remove('text-danger');
        panel.hidden = false;
        button.setAttribute('aria-expanded', 'true');
        const load = form.dataset.load;
        const request = form.dataset.bookmarkRequest = String(Number(form.dataset.bookmarkRequest || 0) + 1);
        const current = () => dialog.open && !panel.hidden && form.dataset.phase === 'ready'
            && form.dataset.load === load && form.dataset.bookmarkRequest === request;
        try {
            const url = new URL(form.dataset.bookmarksUrl, window.location.href);
            url.searchParams.set('_tenantView', form.elements._tenantView.value);
            const response = await fetch(url, {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if (!response.ok || response.redirected) throw new Error('Bookmarks unavailable');
            const items = await response.json();
            if (!Array.isArray(items)) throw new Error('Invalid bookmark list');
            if (!current()) return;
            for (const item of items) {
                const id = String(item.nodeId);
                if (!/^[1-9][0-9]{0,18}$/.test(id) || BigInt(id) > 9223372036854775807n || typeof item.name !== 'string') continue;
                const choice = document.createElement('button');
                choice.type = 'button';
                choice.className = 'node-move-to-bookmark';
                choice.dataset.bookmarkTarget = id;
                if (item.iconName !== 'zero' && /^[a-zA-Z0-9_-]+$/.test(item.iconName || '')) {
                    const icon = document.createElement('img');
                    icon.src = form.dataset.iconBase + encodeURIComponent(item.iconName) + '.png';
                    icon.alt = ''; icon.width = 20; icon.height = 20;
                    choice.append(icon);
                }
                const name = document.createElement('span');
                name.className = 'node-move-to-bookmark-name';
                name.textContent = item.name;
                if (/^#[0-9a-fA-F]{6}$/.test(item.color || '')) name.style.color = item.color;
                if (item.bold === true) name.style.fontWeight = 'bold';
                const number = document.createElement('span');
                number.className = 'node-move-to-bookmark-id';
                number.textContent = '[' + id + ']';
                choice.append(name, number);
                list.append(choice);
            }
            panel.dataset.loaded = 'true';
            filterBookmarks(form);
        } catch (_) {
            if (current()) {
                status.textContent = form.dataset.bookmarksError;
                status.classList.add('text-danger');
            }
        }
    }
    function controls(form, saving) {
        form.querySelector('[data-move-to-close]').disabled = saving;
        form.elements.parentId.readOnly = saving;
        form.querySelector('[type="submit"]').disabled = form.dataset.phase !== 'ready' || form.dataset.pasting === 'true';
        form.querySelector('[data-move-to-paste]').disabled = saving || form.dataset.phase !== 'ready' || form.dataset.pasting === 'true';
        form.querySelector('[data-move-to-bookmarks]').disabled = saving || form.dataset.phase !== 'ready' || form.dataset.pasting === 'true';
        if (saving) closeBookmarks(form);
    }
    async function open(group) {
        const dialog = group.querySelector('dialog'), form = dialog.querySelector('form');
        if (dialog.open) return;
        form.reset();
        closeBookmarks(form);
        form.dataset.phase = 'loading';
        form.dataset.pasting = 'false';
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
    async function paste(button) {
        const form = button.closest('form'), dialog = button.closest('dialog');
        if (button.disabled || form.dataset.phase !== 'ready' || form.dataset.pasting === 'true') return;
        const load = form.dataset.load, previous = form.elements.parentId.value;
        // Ignore a late clipboard result after closing/reopening or manually changing the field.
        const current = () => dialog.open && form.dataset.load === load && form.dataset.phase === 'ready'
            && form.elements.parentId.value === previous;
        closeBookmarks(form);
        form.dataset.pasting = 'true';
        controls(form, false);
        try {
            if (!window.isSecureContext || !navigator.clipboard?.readText) throw new Error('Clipboard unavailable');
            const value = (await navigator.clipboard.readText()).trim();
            if (!current()) return;
            if (!/^[0-9]{1,19}$/.test(value) || BigInt(value) > 9223372036854775807n) {
                message(form, form.dataset.invalid, true);
            } else {
                fillParent(form, value);
            }
            form.elements.parentId.focus();
        } catch (_) {
            if (current()) {
                message(form, form.dataset.clipboardError, true);
                form.elements.parentId.focus();
            }
        } finally {
            if (form.dataset.load === load) {
                form.dataset.pasting = 'false';
                controls(form, form.dataset.phase === 'saving');
            }
        }
    }
    document.addEventListener('click', event => {
        const opener = event.target.closest('[data-move-to-open]');
        if (opener) { open(opener.closest('[data-node-move-to]')); return; }
        const pasteButton = event.target.closest('[data-move-to-paste]');
        if (pasteButton) { paste(pasteButton); return; }
        const bookmarkButton = event.target.closest('[data-move-to-bookmarks]');
        if (bookmarkButton) { bookmarks(bookmarkButton); return; }
        const choice = event.target.closest('[data-bookmark-target]');
        if (choice) {
            const form = choice.closest('form');
            if (form.dataset.phase === 'ready' && form.dataset.pasting !== 'true') {
                fillParent(form, choice.dataset.bookmarkTarget);
                closeBookmarks(form);
            }
            return;
        }
        const closer = event.target.closest('[data-move-to-close]');
        if (closer && !closer.disabled) closer.closest('dialog').close();
    });
    // Escape closes this dialog, not an underlying mobile navigation drawer.
    document.addEventListener('keydown', event => {
        const dialog = event.target.closest('.node-move-to-dialog');
        if (dialog && event.key === 'Enter' && event.target.matches('[data-move-to-bookmarks-filter]')) {
            event.preventDefault(); return;
        }
        if (!dialog || event.key !== 'Escape') return;
        event.stopPropagation();
        if (dialog.querySelector('form').dataset.phase === 'saving') event.preventDefault();
    }, true);
    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!form.matches('[data-node-move-to] form')) return;
        event.preventDefault();
        if (form.dataset.phase !== 'ready' || form.dataset.pasting === 'true' || !form.reportValidity()) return;
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
