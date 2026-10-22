(() => {
    'use strict';
    const panel=document.getElementById('timelineInfo');
    if (!panel) return;
    const body=panel.querySelector('.offcanvas-body');
    let pending;
    document.addEventListener('click',async event=>{
        const link=event.target.closest('[data-node-info-link]');
        if (!link || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button>0 || !window.bootstrap?.Offcanvas) return;
        event.preventDefault();
        if (pending) pending.abort();
        const request=new AbortController();pending=request;
        body.textContent=panel.dataset.loading;
        bootstrap.Offcanvas.getOrCreateInstance(panel).show();
        try {
            const response=await fetch(link.href,{credentials:'same-origin',cache:'no-store',signal:request.signal});
            if (!response.ok || response.redirected) throw new Error('Node info unavailable');
            const html=await response.text();
            if (request.signal.aborted) return;
            const parsed=new DOMParser().parseFromString(html,'text/html');
            const info=parsed.querySelector('[data-node-info]');
            if (!info) throw new Error('Unexpected response');
            const source=new URL(link.href,window.location.href);
            const token=source.searchParams.get('_tenantView');
            info.querySelectorAll('a[href]').forEach(anchor=>{
                const target=new URL(anchor.getAttribute('href'),source);
                if(token && target.origin===source.origin && /^\/(nodes|images|grids|codeboxes)(\/|$)/.test(target.pathname)) {
                    target.searchParams.set('_tenantView',token);anchor.href=target.pathname+target.search+target.hash;
                }
            });
            body.replaceChildren(document.importNode(info,true));
        } catch(error) {if (error.name!=='AbortError' && !request.signal.aborted) body.textContent=panel.dataset.error;}
    });
    panel.addEventListener('hidden.bs.offcanvas',()=>{if(pending)pending.abort();});
})();
