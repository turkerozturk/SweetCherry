(() => {
    'use strict';
    const key='sweetcherry.nodeContentZoom';
    let percent=100;
    function valid(value) { return Number.isInteger(value) && value>=70 && value<=200 && value%10===0; }
    try { const stored=Number(localStorage.getItem(key)); if(valid(stored))percent=stored; } catch (_) { /* Private/storage-disabled browser. */ }
    function apply() {
        document.documentElement.style.setProperty('--node-content-zoom',String(percent/100));
        document.querySelectorAll('[data-node-zoom-value]').forEach(label=>label.textContent=percent+'%');
        document.querySelectorAll('[data-node-zoom]').forEach(button=>{
            button.disabled=(button.dataset.nodeZoom==='out' && percent===70)||(button.dataset.nodeZoom==='in' && percent===200);
        });
    }
    document.addEventListener('click',event=>{
        const button=event.target.closest('[data-node-zoom]');
        if(!button)return;
        event.preventDefault();event.stopImmediatePropagation();
        percent=button.dataset.nodeZoom==='reset'?100:Math.max(70,Math.min(200,percent+(button.dataset.nodeZoom==='in'?10:-10)));
        try { localStorage.setItem(key,String(percent)); } catch (_) { }
        apply();
    },true);
    window.addEventListener('storage',event=>{if(event.key===key){const value=Number(event.newValue);percent=valid(value)?value:100;apply();}});
    apply();
})();
