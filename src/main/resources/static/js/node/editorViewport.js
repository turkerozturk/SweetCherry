(() => {
    'use strict';
    const page=document.querySelector('.plain-editor-page');if(!page)return;
    const desktop=matchMedia('(min-width:900px) and (min-height:500px)');
    let frame;
    function fit(){
        cancelAnimationFrame(frame);frame=requestAnimationFrame(()=>{
            document.body.classList.toggle('plain-editor-viewport',desktop.matches);
            if(!desktop.matches){page.style.removeProperty('--plain-editor-height');return;}
            const footer=document.querySelector('body>footer');
            const reserve=footer?footer.getBoundingClientRect().height+parseFloat(getComputedStyle(footer).marginTop||0):0;
            page.style.setProperty('--plain-editor-height',Math.max(0,innerHeight-page.getBoundingClientRect().top-reserve-2)+'px');
        });
    }
    const observer=new ResizeObserver(fit);
    for(const element of [page.previousElementSibling,document.querySelector('body>footer')])if(element)observer.observe(element);
    window.addEventListener('resize',fit);desktop.addEventListener('change',fit);fit();
})();
