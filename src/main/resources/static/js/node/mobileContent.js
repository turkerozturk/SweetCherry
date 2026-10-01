/* Keep original-image viewing separate from the mobile navigation gestures. */
(() => {
    'use strict';
    const content = document.querySelector('#readerContent #nodeContent');
    if (!content) return;
    /** Open the original source so the browser can zoom it without resizing the reading page. */
    function openImage(image) {
        const source = image.currentSrc || image.src;
        if (!source) return;
        const target = new URL(source, location.origin);
        if (['http:', 'https:', 'blob:'].includes(target.protocol) || /^data:image\//i.test(source)) {
            window.open(source, '_blank', 'noopener,noreferrer');
        }
    }
    content.querySelectorAll('img').forEach(image => {
        image.tabIndex = 0;
        image.title = image.title || document.body.dataset.imageTitle;
        image.addEventListener('click', event => {
            event.preventDefault(); openImage(image);
        });
        image.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault(); openImage(image);
            }
        });
    });
})();
