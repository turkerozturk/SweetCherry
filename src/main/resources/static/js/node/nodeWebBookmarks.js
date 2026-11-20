(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else api.install(root);})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    function install(win){
        const doc=win.document,states=new Map();
        async function load(root,state){
            state.controller?.abort();const controller=new AbortController();state.controller=controller;
            state.status.textContent=root.dataset.loading;state.frame.hidden=true;state.frame.removeAttribute('srcdoc');state.button.disabled=true;
            try{
                const url=new URL(root.dataset.endpoint,win.location.href);url.searchParams.set('_tenantView',root.dataset.tenantView);
                const response=await win.fetch(url,{credentials:'same-origin',cache:'no-store',redirect:'error',signal:controller.signal,headers:{Accept:'application/json'}});
                if(!root.isConnected||controller.signal.aborted||controller!==state.controller)return;
                if(response.status===401||response.status===409){state.stale=true;throw Error('stale');}
                if(!response.ok||response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('load');
                const data=await response.json();
                if(!root.isConnected||controller.signal.aborted||controller!==state.controller)return;
                if(typeof data?.html!=='string'||data.html.length>16_100_000)throw Error('load');
                // Never place remote markup in the application document or relax this sandbox.
                state.frame.setAttribute('sandbox','');state.frame.srcdoc=data.html;state.frame.hidden=false;state.status.textContent='';
            }catch(error){if(root.isConnected&&!controller.signal.aborted&&controller===state.controller)state.status.textContent=state.stale?root.dataset.stale:root.dataset.error;}
            finally{if(controller===state.controller)state.button.disabled=state.stale===true;}
        }
        function scan(){
            for(const [root,state] of states)if(!root.isConnected){state.controller?.abort();states.delete(root);}
            for(const root of doc.querySelectorAll('[data-web-bookmark]')){
                if(states.has(root))continue;
                const state={frame:root.querySelector('[data-web-bookmark-frame]'),status:root.querySelector('[data-web-bookmark-status]'),button:root.querySelector('[data-web-bookmark-reload]')};
                if(!state.frame||!state.status||!state.button)continue;
                states.set(root,state);state.button.addEventListener('click',()=>load(root,state));load(root,state);
            }
        }
        new win.MutationObserver(scan).observe(doc.body,{childList:true,subtree:true});
        doc.addEventListener('sweetcherry:content-loaded',scan);win.addEventListener('pagehide',()=>{for(const state of states.values())state.controller?.abort();});
        win.addEventListener('pageshow',event=>{if(event.persisted)for(const [root,state] of states)if(root.isConnected&&!state.stale)load(root,state);});scan();
    }
    return {install};
});
