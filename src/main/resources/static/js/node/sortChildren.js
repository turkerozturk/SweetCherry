(() => {
    'use strict';
    const menu=document.querySelector('[data-sort-children]');if(!menu)return;
    let busy=false;
    function context(){
        let win=window;
        const readers=document.getElementById('workspace-readers');
        if(readers?.dataset.splitOpen==='true'&&readers.dataset.splitActive==='right')win=document.getElementById('workspace-right-frame')?.contentWindow;
        if(win?.document.querySelector('#richEditForm, #plainEditForm, [data-title-form]:not([hidden])'))return {win,id:''};
        const id=win?.SweetCherryWorkspace?.selected?.() || win?.document.querySelector('[data-node-move]')?.dataset.sortNode;
        return {win,id:String(id||'')};
    }
    async function request(url,options={}){
        const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
        try{const response=await fetch(url,{...options,signal:controller.signal,credentials:'same-origin',cache:'no-store',redirect:'error',headers:{Accept:'application/json'}});
            if(!response.ok)throw Error(response.status===409?'conflict':'error');return response;
        }finally{clearTimeout(timer);}
    }
    menu.addEventListener('click',async event=>{
        const button=event.target.closest('[data-sort-key]');if(!button||busy)return;
        event.preventDefault();const selected=context();if(!/^[1-9][0-9]*$/.test(selected.id)){window.alert(menu.dataset.select);return;}
        busy=true;const buttons=menu.querySelectorAll('[data-sort-key]');buttons.forEach(item=>item.disabled=true);
        try{
            const url=new URL(menu.dataset.base+selected.id+'/sort',location.href);url.searchParams.set('_tenantView',menu.dataset.token);
            const response=await request(url);if(!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('error');
            const state=await response.json();if(!/^[a-f0-9]{64}$/.test(state.revision)||!Number.isSafeInteger(state.count)||state.count<0||typeof state.name!=='string')throw Error('error');
            if(state.count<2){window.alert(menu.dataset.empty);return;}
            if(!window.confirm(menu.dataset.confirm.replace('{0}',state.name).replace('{1}',String(state.count)).replace('{2}',button.textContent.trim())))return;
            // Switching the selected node while loading must not sort a different branch.
            const current=context();if(current.win!==selected.win||current.id!==selected.id)throw Error('conflict');
            const body=new URLSearchParams({_tenantView:menu.dataset.token,key:button.dataset.sortKey,order:button.dataset.sortOrder,revision:state.revision,confirmed:'true'});
            const csrf=menu.querySelector('[data-sort-csrf] input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
            await request(url,{method:'POST',body});
            if(selected.win.SweetCherryWorkspace)await selected.win.SweetCherryWorkspace.select(selected.id);
            else selected.win.location.reload();
        }catch(error){window.alert(menu.dataset[error.message]||menu.dataset.error);}
        finally{busy=false;buttons.forEach(item=>item.disabled=false);}
    });
})();
