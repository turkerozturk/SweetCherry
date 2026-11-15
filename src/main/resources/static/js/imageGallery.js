(() => {
    'use strict';
    const root = document.getElementById('image-gallery');
    if (!root) return;
    const dialog = document.getElementById('gallery-viewer');
    const cards = [...root.querySelectorAll('.gallery-card')];
    const image = dialog.querySelector('[data-gallery-image]');
    const stage = dialog.querySelector('.gallery-stage');
    const status = dialog.querySelector('[data-gallery-status]');
    const zoom = dialog.querySelector('[data-gallery-zoom]');
    const details = document.getElementById('gallery-details');
    const urls = new Set(), requests = new AbortController();
    let current = -1, generation = 0, previewUrl = null, opener = null;
    function settings() {
        root.classList.toggle('hide-details', !details.checked);
        dialog.querySelector('[data-gallery-details]').setAttribute('aria-pressed', String(details.checked));
        try { localStorage.setItem('sc.gallery.details', String(details.checked)); } catch (_) { }
    }
    try { details.checked = localStorage.getItem('sc.gallery.details') !== 'false'; } catch (_) { }
    details.addEventListener('change', settings); settings();
    async function raster(url) {
        for (let attempt = 0; attempt < 3; attempt++) {
            const response = await fetch(url, {cache:'no-store', credentials:'same-origin', signal:requests.signal});
            if (response.status === 429 && attempt < 2) {
                await new Promise(resolve => setTimeout(resolve, 700)); continue;
            }
            if (response.redirected || !response.ok || !response.headers.get('Content-Type')?.startsWith('image/png'))
                throw new Error(response.status === 409 ? root.dataset.stale : root.dataset.error);
            const urlObject = URL.createObjectURL(await response.blob()); urls.add(urlObject); return urlObject;
        }
    }
    function revoke(url) { if (url) { URL.revokeObjectURL(url); urls.delete(url); } }
    // Limit simultaneous decodes, even when an entire row enters the viewport together.
    const queue = []; let running = 0;
    function pump() {
        while (running < 2 && queue.length && !requests.signal.aborted && !dialog.open) {
            const thumbnail = queue.shift(); running++;
            raster(thumbnail.dataset.thumbnail).then(url => {
                thumbnail.src = url; thumbnail.classList.add('ready');
                thumbnail.nextElementSibling.hidden = true;
            }).catch(error => {
                if (error.name !== 'AbortError') thumbnail.nextElementSibling.textContent = error.message;
            }).finally(() => { running--; pump(); });
        }
    }
    function enqueue(thumbnail) { queue.push(thumbnail); pump(); }
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver(entries => {
            entries.filter(entry => entry.isIntersecting).forEach(entry => {
                observer.unobserve(entry.target); enqueue(entry.target);
            });
        }, {rootMargin:'300px'});
        root.querySelectorAll('[data-thumbnail]').forEach(img => observer.observe(img));
    } else root.querySelectorAll('[data-thumbnail]').forEach(enqueue);

    async function show(index) {
        if (index < 0 || index >= cards.length) return;
        current = index; const version = ++generation;
        image.hidden = true; image.removeAttribute('src'); revoke(previewUrl); previewUrl = null;
        stage.classList.remove('zoomed'); zoom.setAttribute('aria-pressed', 'false');
        status.hidden = false; status.textContent = root.dataset.loading;
        const card = cards[index];
        dialog.querySelector('[data-gallery-info]').replaceChildren(...[...card.querySelector('.gallery-info').childNodes].map(n => n.cloneNode(true)));
        dialog.querySelector('[data-gallery-position]').textContent = root.dataset.position.replace('{0}', String(index + 1)).replace('{1}', String(cards.length));
        dialog.querySelector('[data-gallery-prev]').disabled = index === 0;
        dialog.querySelector('[data-gallery-next]').disabled = index === cards.length - 1;
        if (!dialog.open) dialog.showModal();
        try {
            const url = await raster(card.querySelector('.gallery-open').dataset.large);
            if (version !== generation || !dialog.open) { revoke(url); return; }
            previewUrl = url; image.src = url; image.hidden = false; status.hidden = true;
        } catch (error) {
            if (version === generation && dialog.open && error.name !== 'AbortError') status.textContent = error.message;
        }
    }
    cards.forEach((card, index) => card.querySelector('.gallery-open').addEventListener('click', event => {
        opener = event.currentTarget; show(index);
    }));
    dialog.querySelector('[data-gallery-prev]').addEventListener('click', () => show(current - 1));
    dialog.querySelector('[data-gallery-next]').addEventListener('click', () => show(current + 1));
    dialog.querySelector('[data-gallery-close]').addEventListener('click', () => dialog.close());
    zoom.addEventListener('click', () => zoom.setAttribute('aria-pressed', String(stage.classList.toggle('zoomed'))));
    dialog.querySelector('[data-gallery-details]').addEventListener('click', () => { details.checked = !details.checked; settings(); });
    dialog.addEventListener('keydown', event => {
        if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
        if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
            event.preventDefault(); show(current + (event.key === 'ArrowLeft' ? -1 : 1));
        }
    });
    dialog.addEventListener('close', () => {
        generation++; image.hidden = true; image.removeAttribute('src'); revoke(previewUrl); previewUrl = null;
        opener?.focus();
        pump();
    });
    window.addEventListener('pagehide', () => { requests.abort(); urls.forEach(url => URL.revokeObjectURL(url)); urls.clear(); });
    window.addEventListener('pageshow', event => { if (event.persisted) location.reload(); });
})();
