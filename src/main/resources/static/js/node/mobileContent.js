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
    function prepare(root){root.querySelectorAll('img:not(.sc-inline-icon)').forEach(image=>{image.tabIndex=0;image.title=image.title||document.body.dataset.imageTitle;});}
    content.addEventListener('click',event=>{
        const image=event.target.closest('img:not(.sc-inline-icon)');if(!image||!content.contains(image))return;
        event.preventDefault();openImage(image);
    });
    content.addEventListener('keydown',event=>{
        if((event.key==='Enter'||event.key===' ')&&event.target.matches('img:not(.sc-inline-icon)')){event.preventDefault();openImage(event.target);}
    });
    document.addEventListener('sweetcherry:content-loaded',event=>{if(content.contains(event.detail.root))prepare(event.detail.root);});
    prepare(content);
})();
