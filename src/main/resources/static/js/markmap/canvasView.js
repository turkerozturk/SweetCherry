(() => {
    'use strict';
    const button = document.getElementById('markmapCanvasToggle');
    const page = document.querySelector('.mind-map-page');
    if (!button || !page) return;
    function setExpanded(expanded) {
        page.classList.toggle('markmap-page-only', expanded);
        button.setAttribute('aria-pressed', String(expanded));
        button.title = expanded ? button.dataset.restore : button.dataset.expand;
        button.setAttribute('aria-label', button.title);
        button.querySelector('path').setAttribute('d', expanded
            ? 'M3 8h5V3m13 5h-5V3M8 21v-5H3m13 5v-5h5'
            : 'M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5');
        // Let the existing SVG resize without fitting/rebuilding the map or changing its zoom.
        window.dispatchEvent(new Event('resize'));
    }
    button.addEventListener('click', () => setExpanded(button.getAttribute('aria-pressed') !== 'true'));
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && !event.defaultPrevented && !document.querySelector('dialog[open]')
            && button.getAttribute('aria-pressed') === 'true') {
            event.preventDefault(); setExpanded(false); button.focus();
        }
    });
})();
