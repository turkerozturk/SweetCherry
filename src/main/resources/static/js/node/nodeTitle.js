(() => {
    'use strict';
    const states=new WeakMap();
    function setup(root){
        if(states.has(root))return;
        const open=root.querySelector('[data-title-open]'),form=root.querySelector('[data-title-form]');if(!open||!form)return;
        const heading=root.querySelector('[data-title-heading]'),input=form.querySelector('[data-title-value]'),save=form.querySelector('[data-title-save]'),cancel=form.querySelector('[data-title-cancel]'),status=form.querySelector('[data-title-status]');
        const state={generation:0,saving:false,record:null,controller:null};states.set(root,state);
        const valid=data=>data?.id===root.dataset.titleId&&typeof data.name==='string'&&/^[a-f0-9]{64}$/.test(data.revision);
        async function request(method,body){
            const controller=new AbortController();state.controller=controller;const timer=setTimeout(()=>controller.abort(),30000);
            const url=new URL(root.dataset.titleEndpoint,location.href);url.searchParams.set('_tenantView',form.elements._tenantView.value);
            try{
                const response=await fetch(url,{method,body,credentials:'same-origin',cache:'no-store',redirect:'error',signal:controller.signal,headers:{Accept:'application/json'}});
                if(!response.ok)throw Error(response.status===409?'changed':response.status===403?'readonly':'error');
                if(response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('error');
                const data=await response.json();if(!valid(data))throw Error('error');return data;
            }finally{clearTimeout(timer);}
        }
        function close(){if(state.saving)return;state.generation++;state.controller?.abort();form.hidden=true;heading.hidden=false;open.hidden=false;if(root.isConnected)open.focus();}
        open.addEventListener('click',async()=>{
            const generation=++state.generation;state.controller?.abort();state.record=null;
            form.hidden=false;heading.hidden=true;open.hidden=true;input.disabled=true;save.disabled=true;status.textContent=root.dataset.titleLoading;cancel.focus();
            try{const data=await request('GET');if(generation!==state.generation||!root.isConnected)return;state.record=data;input.value=data.name;input.disabled=false;save.disabled=false;status.textContent='';input.focus();input.select();}
            catch(error){if(generation===state.generation&&root.isConnected)status.textContent=root.dataset['title'+error.message[0].toUpperCase()+error.message.slice(1)]||root.dataset.titleError;}
        });
        cancel.addEventListener('click',close);
        form.addEventListener('keydown',event=>{if(event.key==='Escape'){event.preventDefault();close();}});
        form.addEventListener('submit',async event=>{
            event.preventDefault();if(state.saving||!state.record)return;
            state.saving=true;input.disabled=true;save.disabled=true;cancel.disabled=true;status.textContent=root.dataset.titleSaving;
            const generation=state.generation,body=new URLSearchParams({name:input.value,revision:state.record.revision,_tenantView:form.elements._tenantView.value});
            const csrf=form.querySelector('input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
            try{const result=await request('POST',body);if(generation!==state.generation||!root.isConnected)return;
                root.querySelector('[data-title-text]').textContent=result.name;state.saving=false;close();
                document.dispatchEvent(new CustomEvent('sweetcherry:node-renamed',{detail:{id:result.id,name:result.name}}));
            }catch(error){if(root.isConnected){status.textContent=root.dataset['title'+error.message[0].toUpperCase()+error.message.slice(1)]||root.dataset.titleError;state.record=null;}}
            finally{state.saving=false;input.disabled=!state.record;save.disabled=!state.record;cancel.disabled=false;}
        });
    }
    function scan(){document.querySelectorAll('[data-node-title-editor]').forEach(setup);}
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan();
})();
