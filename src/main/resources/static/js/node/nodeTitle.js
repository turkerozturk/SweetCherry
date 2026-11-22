(() => {
    'use strict';
    const states=new WeakMap();
    const iconDialog=document.getElementById?.('node-title-icons');
    let iconSelection=null;
    const choices=()=>Array.from(iconDialog?.querySelectorAll('[data-title-icon-choice]')||[]);
    if(iconDialog){
        const search=iconDialog.querySelector('[data-title-icon-search]');
        const normalize=text=>text.normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase();
        search.addEventListener('input',()=>{
            const term=normalize(search.value.trim());
            choices().forEach(button=>{button.hidden=!normalize(button.title+' '+button.dataset.titleIconChoice).includes(term);});
        });
        iconDialog.addEventListener('click',event=>{
            const button=event.target.closest('[data-title-icon-choice]');
            if(button&&iconDialog.contains(button)&&iconSelection){iconSelection.choose(button.dataset.titleIconChoice);iconDialog.close();}
        });
        iconDialog.querySelector('[data-title-icon-close]').addEventListener('click',()=>iconDialog.close());
        iconDialog.addEventListener('close',()=>{iconSelection=null;});
    }

    function setup(root){
        if(states.has(root))return;
        const open=root.querySelector('[data-title-open]'),form=root.querySelector('[data-title-form]');if(!open||!form)return;
        const heading=root.querySelector('[data-title-heading]'),input=form.querySelector('[data-title-value]'),save=form.querySelector('[data-title-save]'),cancel=form.querySelector('[data-title-cancel]'),status=form.querySelector('[data-title-status]');
        const iconButton=form.querySelector('[data-title-icon-open]'),iconValue=form.querySelector('[data-title-icon-value]');
        function displayIcon(value){
            if(!iconValue||!iconButton)return;
            iconValue.value=value||'';
            const choice=choices().find(button=>button.dataset.titleIconChoice===value);
            const source=choice?.querySelector('[data-title-icon-src]')?.getAttribute('data-title-icon-src');
            const image=iconButton.querySelector('[data-title-icon-preview]'),fallback=iconButton.querySelector('[data-title-icon-fallback]');
            image.hidden=!source;fallback.hidden=Boolean(source);
            if(source)image.src=source;else image.removeAttribute('src');
        }
        const state={generation:0,saving:false,record:null,controller:null};states.set(root,state);
        const valid=data=>data?.id===root.dataset.titleId&&typeof data.name==='string'&&/^[a-f0-9]{64}$/.test(data.revision);
        iconButton?.addEventListener('click',()=>{
            if(!iconDialog||!state.record||state.saving)return;
            const generation=state.generation,search=iconDialog.querySelector('[data-title-icon-search]');
            iconSelection={root,choose(value){if(root.isConnected&&generation===state.generation&&!state.saving)displayIcon(value);}};
            search.value='';choices().forEach(button=>{
                button.hidden=false;button.setAttribute('aria-pressed',String(button.dataset.titleIconChoice===iconValue.value));
                const image=button.querySelector('[data-title-icon-src]');
                if(image&&!image.hasAttribute('src'))image.src=image.getAttribute('data-title-icon-src');
            });
            iconDialog.showModal();search.focus();
        });
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
            form.hidden=false;heading.hidden=true;open.hidden=true;input.disabled=true;save.disabled=true;if(iconButton)iconButton.disabled=true;status.textContent=root.dataset.titleLoading;cancel.focus();
            try{const data=await request('GET');if(generation!==state.generation||!root.isConnected)return;state.record=data;input.value=data.name;displayIcon(data.icon);input.disabled=false;save.disabled=false;if(iconButton)iconButton.disabled=false;status.textContent='';input.focus();input.select();}
            catch(error){if(generation===state.generation&&root.isConnected)status.textContent=root.dataset['title'+error.message[0].toUpperCase()+error.message.slice(1)]||root.dataset.titleError;}
        });
        cancel.addEventListener('click',close);
        form.addEventListener('keydown',event=>{if(event.key==='Escape'){event.preventDefault();close();}});
        form.addEventListener('submit',async event=>{
            event.preventDefault();if(state.saving||!state.record)return;
            state.saving=true;input.disabled=true;save.disabled=true;cancel.disabled=true;if(iconButton)iconButton.disabled=true;status.textContent=root.dataset.titleSaving;
            const generation=state.generation,body=new URLSearchParams({name:input.value,revision:state.record.revision,_tenantView:form.elements._tenantView.value});
            if(iconValue?.value&&iconValue.value!==state.record.icon)body.set('icon',iconValue.value);
            const csrf=form.querySelector('input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
            try{const result=await request('POST',body);if(generation!==state.generation||!root.isConnected)return;
                root.querySelector('[data-title-text]').textContent=result.name;state.saving=false;close();
                document.dispatchEvent(new CustomEvent('sweetcherry:node-renamed',{detail:{id:result.id,name:result.name}}));
            }catch(error){if(root.isConnected){status.textContent=root.dataset['title'+error.message[0].toUpperCase()+error.message.slice(1)]||root.dataset.titleError;state.record=null;}}
            finally{state.saving=false;input.disabled=!state.record;save.disabled=!state.record;cancel.disabled=false;if(iconButton)iconButton.disabled=!state.record;}
        });
    }
    function scan(){if(iconSelection&&!iconSelection.root.isConnected)iconDialog.close();document.querySelectorAll('[data-node-title-editor]').forEach(setup);}
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan();
})();
