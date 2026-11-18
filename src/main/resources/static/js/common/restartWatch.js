(() => {
    'use strict';
    const root=document.querySelector('[data-restart-watch]');if(!root)return;
    const status=document.querySelector('[data-restart-status]');
    let stopped=false,attempts=0,timer;
    window.addEventListener('pagehide',()=>{stopped=true;clearTimeout(timer);});
    async function check(){
        if(stopped)return;
        const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),4000);
        try{
            // No stale session cookie: the old session is invalid after restart.
            const response=await fetch(root.dataset.endpoint,{credentials:'omit',cache:'no-store',redirect:'error',signal:controller.signal,headers:{Accept:'application/json'}});
            if(response.ok){const state=await response.json();
                if(!stopped && state.ready===true && typeof state.instance==='string' && state.instance && state.instance!==root.dataset.instance){stopped=true;location.replace(root.dataset.home);return;}
            }
        }catch(_){/* The old server is closing or the new server is still starting. */}
        finally{clearTimeout(timeout);}
        if(!stopped){attempts++;status.textContent=attempts<60?status.dataset.waiting:status.dataset.slow;timer=setTimeout(check,2000);}
    }
    check();
})();
