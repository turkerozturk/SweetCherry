/* Persistent desktop navigation; node content changes independently from the tree. */
(() => {
    'use strict';
    const workspace = document.getElementById('tree-workspace');
    if (!workspace) return;
    const token = workspace.dataset.tenantView;
    const tree = document.getElementById('workspace-tree');
    const pane = document.getElementById('workspace-tree-pane');
    const content = document.getElementById('workspace-content');
    const status = document.getElementById('workspace-status');
    const splitter = document.getElementById('workspace-splitter');
    const storageKey = 'sweetcherry-tree:' + token;
    let state = {expanded: [], selected: 0, width: 300, top: 0, left: 0};
    try { state = {...state, ...JSON.parse(sessionStorage.getItem(storageKey) || '{}')}; } catch (_) { }
    const expanded = new Set(Array.isArray(state.expanded) ? state.expanded : []);
    let controller;
    let stopped = false;

    function save() {
        try { sessionStorage.setItem(storageKey, JSON.stringify({...state, expanded: [...expanded], top: pane.scrollTop, left: pane.scrollLeft})); } catch (_) { }
    }
    function url(path, params = {}) {
        const target = new URL(path, location.origin);
        Object.entries({...params, _tenantView: token}).forEach(([key, value]) => target.searchParams.set(key, value));
        return target.pathname + target.search;
    }
    /** Stops further actions when the session or selected database is no longer valid. */
    function stop(message) {
        stopped = true;
        workspace.replaceChildren();
        const text = document.createElement('p');
        text.textContent = message;
        const home = document.createElement('a');
        home.href = '/';
        home.textContent = 'SweetCherry';
        workspace.append(text, home);
        status.textContent = message;
    }
    async function request(path, signal) {
        const response = await fetch(path, {credentials: 'same-origin', cache: 'no-store', signal});
        if (response.status === 409) { stop(workspace.dataset.stale); throw new Error('stale'); }
        if (response.redirected || response.status === 401) { stop(workspace.dataset.expired); throw new Error('expired'); }
        if (!response.ok) throw new Error('load');
        return response;
    }
    function image(name) {
        const img = document.createElement('img');
        img.src = '/img/icons/' + encodeURIComponent(name) + '.svg';
        img.alt = '';
        return img;
    }
    /** Builds navigation with textContent so titles are never interpreted as HTML. */
    function row(child) {
        const li = document.createElement('li');
        li.dataset.nodeId = String(child.nodeId);
        const line = document.createElement('div');
        line.className = 'workspace-tree-row';
        const fold = document.createElement(child.hasChildren ? 'button' : 'span');
        fold.className = 'workspace-fold';
        if (child.hasChildren) {
            fold.type = 'button';
            fold.textContent = '▸';
            fold.setAttribute('aria-expanded', 'false');
            fold.setAttribute('aria-label', workspace.dataset.expand);
            fold.addEventListener('click', () => toggle(li).catch(() => { if (!stopped) status.textContent = workspace.dataset.error; }));
        } else fold.setAttribute('aria-hidden', 'true');
        const link = document.createElement('a');
        link.href = url('/tree', {nodeId: child.nodeId});
        link.title = child.name;
        if (child.readOnly) link.append(image('ct_locked'));
        link.append(image(child.iconName));
        const name = document.createElement('span');
        name.textContent = child.name;
        if (/^#[0-9a-fA-F]{1,6}$/.test(child.titleColor)) name.style.color = '#' + child.titleColor.slice(1).padStart(6, '0');
        if (child.bold) name.style.fontWeight = 'bold';
        link.append(name);
        line.append(fold, link);
        li.append(line);
        return li;
    }
    /** Loads only one branch, leaving other branches and scroll positions untouched. */
    async function toggle(li, forceOpen = false) {
        if (stopped) return;
        const fold = li.querySelector(':scope > .workspace-tree-row > button');
        if (!fold || fold.disabled) return;
        let branch = li.querySelector(':scope > ul');
        if (!branch) {
            fold.disabled = true;
            try {
                const response = await request(url('/nodes/navigation/children', {fatherId: li.dataset.nodeId}));
                const children = await response.json();
                branch = document.createElement('ul');
                branch.hidden = true;
                children.forEach(child => branch.append(row(child)));
                li.append(branch);
            } finally { fold.disabled = false; }
        }
        branch.hidden = forceOpen ? false : !branch.hidden;
        fold.textContent = branch.hidden ? '▸' : '▾';
        fold.setAttribute('aria-expanded', String(!branch.hidden));
        fold.setAttribute('aria-label', branch.hidden ? workspace.dataset.expand : workspace.dataset.collapse);
        if (branch.hidden) expanded.delete(li.dataset.nodeId); else expanded.add(li.dataset.nodeId);
        save();
    }
    function find(id) { return tree.querySelector('li[data-node-id="' + String(id).replace(/[^0-9]/g, '') + '"]'); }
    function markSelected(id) {
        tree.querySelectorAll('[aria-current]').forEach(link => link.removeAttribute('aria-current'));
        const li = find(id);
        if (li) li.querySelector(':scope > .workspace-tree-row > a').setAttribute('aria-current', 'page');
    }
    /** Fetches server-rendered content with existing role, CSRF and tenant safeguards. */
    async function select(id, push = true, reveal = false) {
        if (stopped) return;
        controller?.abort();
        const active = new AbortController();
        controller = active;
        content.setAttribute('aria-busy', 'true');
        status.textContent = workspace.dataset.loading;
        try {
            const response = await request(url('/tree/content/' + id), active.signal);
            const html = await response.text();
            if (active !== controller || stopped) return;
            const parsed = new DOMParser().parseFromString(html, 'text/html');
            const fragment = parsed.querySelector('.workspace-node');
            if (!fragment) throw new Error('fragment');
            fragment.querySelectorAll('a[href]').forEach(link => {
                const target = new URL(link.getAttribute('href'), location.origin);
                if (target.origin === location.origin && !link.getAttribute('href').startsWith('#')) {
                    target.searchParams.set('_tenantView', token);
                    link.href = target.pathname + target.search + target.hash;
                }
            });
            content.replaceChildren(document.importNode(fragment, true));
            content.scrollTop = 0;
            const template = content.querySelector('template[data-workspace-status]');
            status.replaceChildren(template.content.cloneNode(true));
            state.selected = Number(id);
            document.title = fragment.dataset.nodeTitle + ' — SweetCherry';
            if (push) history.pushState({nodeId: Number(id)}, '', url('/tree', {nodeId: id}));
            document.dispatchEvent(new CustomEvent('sweetcherry:content-loaded', {detail: {root: content}}));
            if (reveal) {
                const path = content.querySelector('[data-path-ids]')?.dataset.pathIds || '';
                for (const ancestor of path.split(',').filter(part => /^\d+$/.test(part) && Number(part) !== Number(id))) {
                    if (active !== controller) return;
                    const li = find(ancestor);
                    if (li) await toggle(li, true);
                }
            }
            if (active === controller) { markSelected(id); save(); }
        } catch (error) {
            if (error.name !== 'AbortError' && !stopped && active === controller) status.textContent = workspace.dataset.error;
        } finally { if (active === controller) content.removeAttribute('aria-busy'); }
    }
    workspace.addEventListener('click', event => {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const link = event.target.closest('a[href]');
        if (!link || link.target === '_blank' || link.hasAttribute('download')) return;
        const target = new URL(link.href, location.href);
        if (target.origin !== location.origin) return;
        const match = target.pathname.match(/^\/nodes\/(\d+)$/);
        const id = match ? match[1] : target.pathname === '/tree' ? target.searchParams.get('nodeId') || '0' : null;
        if (id !== null && /^\d+$/.test(id)) { event.preventDefault(); select(id, true, !pane.contains(link)); }
    });
    window.addEventListener('popstate', () => select(new URL(location.href).searchParams.get('nodeId') || '0', false, true));
    pane.addEventListener('scroll', save, {passive: true});

    function setWidth(value) {
        const maximum = Math.max(160, Math.min(700, workspace.clientWidth - 240));
        state.width = Math.max(160, Math.min(maximum, Number(value) || 300));
        workspace.style.setProperty('--tree-width', state.width + 'px');
        splitter.setAttribute('aria-valuenow', String(state.width));
        splitter.setAttribute('aria-valuemax', String(maximum));
    }
    splitter.addEventListener('pointerdown', event => {
        splitter.setPointerCapture(event.pointerId);
        document.body.classList.add('resizing');
    });
    splitter.addEventListener('pointermove', event => {
        if (splitter.hasPointerCapture(event.pointerId)) setWidth(event.clientX - workspace.getBoundingClientRect().left);
    });
    function endResize() { document.body.classList.remove('resizing'); save(); }
    splitter.addEventListener('pointerup', endResize);
    splitter.addEventListener('pointercancel', endResize);
    splitter.addEventListener('keydown', event => {
        if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
            event.preventDefault(); setWidth(state.width + (event.key === 'ArrowRight' ? 20 : -20)); save();
        }
    });
    window.addEventListener('resize', () => setWidth(state.width));
    /** Restores expanded branches only within this browser tab and selected CTB. */
    async function initialize() {
        setWidth(state.width);
        try {
            const response = await request(url('/nodes/navigation/children', {fatherId: 0}));
            const children = await response.json();
            children.forEach(child => tree.append(row(child)));
            const top = state.top, left = state.left;
            for (const id of [...expanded]) { const li = find(id); if (li) await toggle(li, true); }
            const query = new URL(location.href).searchParams;
            const id = query.has('nodeId') ? workspace.dataset.initialNode : state.selected;
            await select(/^\d+$/.test(String(id)) ? id : 0, false, true);
            pane.scrollTop = top; pane.scrollLeft = left;
            history.replaceState({nodeId: state.selected}, '', url('/tree', {nodeId: state.selected}));
        } catch (_) { if (!stopped) status.textContent = workspace.dataset.error; }
    }
    initialize();
})();
