(() => {
    'use strict';
    const body = document.body, tree = document.getElementById('readerTreeNodes');
    const token = body.dataset.tenantToken;
    const key = 'sweetcherry:reader:' + token;
    const drawer = document.getElementById('readerDrawer');
    const drawerToggle = document.getElementById('readerDrawerToggle');
    const controls = document.querySelector('.reader-controls');
    const drawerKey = key + ':context-open';
    /** Keep the drawer below the bar, including browser zoom and device orientation changes. */
    function sizeDrawer() {
        body.style.setProperty('--reader-bar-height', controls.getBoundingClientRect().height + 'px');
    }
    /** Bring the selected sibling near the top without scrolling the node content or page. */
    function revealContextSelection() {
        const selected = drawer.querySelector('[data-reader-selected="true"]');
        if (selected) drawer.scrollTop += selected.getBoundingClientRect().top - drawer.getBoundingClientRect().top - 8;
    }
    function setDrawerOpen(value) {
        drawer.hidden = !value;
        drawerToggle.setAttribute('aria-expanded', String(value));
        drawerToggle.querySelector('i').className = value ? 'fas fa-chevron-up' : 'fas fa-chevron-down';
        try { sessionStorage.setItem(drawerKey, String(value)); } catch (_) {}
        if (value) requestAnimationFrame(revealContextSelection);
    }
    drawerToggle.addEventListener('click', () => setDrawerOpen(drawer.hidden));
    let drawerOpen = false;
    try { drawerOpen = sessionStorage.getItem(drawerKey) === 'true'; } catch (_) {}
    sizeDrawer();
    if (window.ResizeObserver) new ResizeObserver(sizeDrawer).observe(controls);
    else window.addEventListener('resize', sizeDrawer);
    setDrawerOpen(drawerOpen);
    let state = { open: [], scroll: 0 };
    try { state = { ...state, ...JSON.parse(sessionStorage.getItem(key) || '{}') }; } catch (_) {}
    const open = new Set(Array.isArray(state.open) ? state.open.map(String) : []);
    for (const id of (body.dataset.pathIds || '').split(',')) { if (/^[1-9][0-9]*$/.test(id)) open.add(id); }
    const panel = document.getElementById('readerTree');
    const pane = panel.querySelector('.offcanvas-body');
    /** Keep expanded branches and the tree position for this CTB in this browser tab. */
    function save() {
        try { sessionStorage.setItem(key, JSON.stringify({open: [...open], scroll: pane.scrollTop})); } catch (_) {}
    }
    /** Load children with the session's CTB token; never follow a login redirect as tree data. */
    async function load(parent, list) {
        const url = new URL('/nodes/navigation/children', location.origin);
        url.searchParams.set('fatherId', parent);
        url.searchParams.set('_tenantView', token);
        const response = await fetch(url, { headers: {Accept:'application/json'} });
        if (!response.ok || response.redirected) throw new Error('Tree unavailable');
        const nodes = await response.json();
        list.replaceChildren();
        for (const node of nodes) {
            const id = String(node.nodeId), li = document.createElement('li'), row = document.createElement('div');
            const fold = document.createElement(node.hasChildren ? 'button' : 'span');
            fold.className = 'reader-fold';
            const link = document.createElement('a');
            const target = new URL('/nodes/' + id, location.origin);
            target.searchParams.set('_tenantView', token);
            link.href = target.pathname + target.search;
            link.textContent = node.name;
            if (node.bold) link.style.fontWeight = 'bold';
            if (node.titleColor && /^#[0-9a-f]{6}$/i.test(node.titleColor)) link.style.color = node.titleColor;
            if (id === body.dataset.selectedId) link.setAttribute('aria-current','page');
            const icon = document.createElement('img');
            if (/^[a-zA-Z0-9_-]+$/.test(node.iconName)) icon.src = '/img/icons/' + node.iconName + '.svg';
            icon.alt = ''; row.append(fold, icon, link); li.append(row); list.append(li);
            if (node.hasChildren) {
                fold.type = 'button'; fold.textContent = '+'; fold.setAttribute('aria-expanded','false');
                fold.setAttribute('aria-label', node.name);
                const children = document.createElement('ul'); children.hidden = true; li.append(children);
                let loaded = false, busy = false;
                async function expand() {
                    if (busy) return;
                    busy = true; fold.disabled = true;
                    try {
                        if (!loaded) { await load(id, children); loaded = true; }
                        children.hidden = false; fold.textContent = '−'; fold.setAttribute('aria-expanded','true'); open.add(id); save();
                    } catch (_) { fold.textContent = '!'; fold.title = body.dataset.loadError; }
                    finally { busy = false; fold.disabled = false; }
                }
                fold.addEventListener('click', () => {
                    if (children.hidden) expand();
                    else { children.hidden = true; fold.textContent = '+'; fold.setAttribute('aria-expanded','false'); open.delete(id); save(); }
                });
                if (open.has(id)) await expand();
            }
        }
    }
    let initialized = false;
    const retry = document.getElementById('readerRetry');
    async function initialize() {
        retry.hidden = true;
        try { await load(0, tree); initialized = true; pane.scrollTop = state.scroll; }
        catch (_) { retry.hidden = false; tree.textContent = body.dataset.loadError; }
    }
    panel.addEventListener('shown.bs.offcanvas', () => { if (!initialized) initialize(); });
    retry.addEventListener('click', initialize);
    pane.addEventListener('scroll', save, {passive:true});
    /** Recognize a deliberate single-finger horizontal swipe without intercepting scrolling or pinch zoom. */
    let gesture = null;
    const content = document.getElementById('readerContent');
    function unzoomed() { return !window.visualViewport || Math.abs(window.visualViewport.scale - 1) < .02; }
    content.addEventListener('touchstart', event => {
        gesture = null;
        if (event.touches.length !== 1 || !unzoomed() || event.target.closest('a,button,input,textarea,select,pre,code,table,video,img')) return;
        for (let el = event.target; el; el = el.parentElement) {
            if (el.scrollWidth > el.clientWidth + 2 && /auto|scroll/.test(getComputedStyle(el).overflowX)) return;
            if (el === content) break;
        }
        const touch = event.touches[0]; gesture = {x:touch.clientX,y:touch.clientY,time:Date.now()};
    }, {passive:true});
    content.addEventListener('touchmove', event => { if (event.touches.length !== 1 || !unzoomed()) gesture = null; }, {passive:true});
    content.addEventListener('touchcancel', () => { gesture = null; }, {passive:true});
    content.addEventListener('touchend', event => {
        const start = gesture; gesture = null;
        if (!start || !unzoomed() || event.changedTouches.length !== 1) return;
        const touch = event.changedTouches[0], dx = touch.clientX-start.x, dy = touch.clientY-start.y;
        if (Math.abs(dx) < 85 || Math.abs(dx) < Math.abs(dy)*2.5 || Date.now()-start.time > 650 || window.getSelection()?.toString()) return;
        bootstrap.Offcanvas.getOrCreateInstance(document.getElementById(dx > 0 ? 'readerTree' : 'readerActions')).show();
    }, {passive:true});
})();
