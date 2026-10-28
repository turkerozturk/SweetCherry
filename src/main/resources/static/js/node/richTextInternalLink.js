(() => {
    'use strict';
    const button=document.getElementById('richInternalLink'),dialog=document.getElementById('richInternalDialog');
    if(!button||!dialog)return;
    const byId=id=>document.getElementById('richInternal'+id);
    const id=byId('Id'),text=byId('Text'),anchor=byId('Anchor'),targetBox=byId('Target'),matches=byId('Matches'),status=byId('Status');
    const apply=byId('Apply'),remove=byId('Remove'),lookupButton=byId('Lookup'),search=byId('Search');
    let ticket,target,epoch=0,lookupController,searchController,applying=false;
    const bridge=()=>window.RichTextInternalLinkBridge;
    const message=code=>{status.textContent=dialog.dataset[code]||dialog.dataset.error;};
    const valid=value=>/^[1-9][0-9]{0,18}$/.test(value)&&BigInt(value)<=9223372036854775807n;
    function clearTarget(){target=null;targetBox.replaceChildren();while(anchor.options.length>1)anchor.remove(1);anchor.value='';}
    function abort(){lookupController?.abort();searchController?.abort();lookupController=searchController=null;}
    function close(){epoch++;abort();dialog.close();applying=false;apply.disabled=false;if(ticket)bridge().restore(ticket);}
    function row(item,selectable){
        const element=document.createElement(selectable?'button':'div');element.className='rich-internal-target';if(selectable)element.type='button';
        const image=document.createElement('img');image.width=image.height=20;image.alt='';image.loading='lazy';
        image.src=dialog.dataset.icons+(/^[a-z0-9_-]+$/.test(item.iconName)?item.iconName:'ct_code')+'.png';
        const name=document.createElement('span');name.textContent=item.name;if(item.bold)name.style.fontWeight='bold';if(/^#[a-f0-9]{6}$/i.test(item.color))name.style.color=item.color;
        const number=document.createElement('span');number.textContent='('+item.nodeId+')';element.append(image,name,number);
        if(selectable)element.addEventListener('click',()=>{id.value=item.nodeId;idChanged();lookup();});
        return element;
    }
    async function request(url,params,signal){
        const query=new URLSearchParams({...params,_tenantView:window.richEditorTenant||''});
        const response=await fetch(url+'?'+query,{credentials:'same-origin',redirect:'error',signal});
        if(!response.ok)throw new Error(response.status===404?'missing':response.status===400?'invalid':'error');
        if(!(response.headers.get('content-type')||'').includes('application/json'))throw new Error('error');
        return response.json();
    }
    async function lookup(wantedAnchor=''){
        const value=id.value.trim(),generation=epoch;
        lookupController?.abort();const controller=new AbortController();lookupController=controller;clearTarget();
        if(!valid(value)){message('invalid');return null;}
        message('loading');
        try{
            const result=await request(dialog.dataset.target,{nodeId:value},controller.signal);
            if(generation!==epoch||lookupController!==controller||id.value.trim()!==value)return null;
            if(result.nodeId!==value||typeof result.name!=='string'||!Array.isArray(result.anchors))throw new Error('error');
            target=result;targetBox.appendChild(row(result,false));
            for(const name of result.anchors){const option=document.createElement('option');option.value=option.textContent=name;anchor.appendChild(option);}
            if(wantedAnchor){if(!result.anchors.includes(wantedAnchor)){message('anchorMissing');return null;}anchor.value=wantedAnchor;}
            status.textContent='';return result;
        }catch(error){if(error.name!=='AbortError'&&generation===epoch&&lookupController===controller)message(error.message);return null;}
    }
    async function findMatches(){
        const value=text.value,generation=epoch;
        searchController?.abort();const controller=new AbortController();searchController=controller;matches.replaceChildren();
        if(!value.trim()||value.length>1000){message('empty');return;}
        message('loading');
        try{
            const result=await request(dialog.dataset.matches,{name:value},controller.signal);
            if(generation!==epoch||searchController!==controller||text.value!==value)return;
            if(!Array.isArray(result.targets))throw new Error('error');
            for(const item of result.targets)matches.appendChild(row(item,true));
            if(result.truncated)message('limit');else if(!result.targets.length)message('empty');else status.textContent='';
        }catch(error){if(error.name!=='AbortError'&&generation===epoch&&searchController===controller)message('error');}
    }
    function idChanged(){lookupController?.abort();lookupController=null;clearTarget();status.textContent='';}
    button.addEventListener('pointerdown',event=>event.preventDefault());
    button.addEventListener('click',()=>{
        try{ticket=bridge().open();}catch(error){window.alert(dialog.dataset[error.message]||dialog.dataset.error);return;}
        epoch++;abort();clearTarget();matches.replaceChildren();status.textContent='';text.value=ticket.text;id.value='';apply.disabled=false;applying=false;
        const previous=/^node ([1-9][0-9]*)(?: (.+))?$/.exec(ticket.link);remove.disabled=!ticket.link;
        dialog.showModal();
        if(previous){id.value=previous[1];apply.disabled=true;const generation=epoch;lookup(previous[2]||'').finally(()=>{if(generation===epoch&&!applying)apply.disabled=false;});}
        else if(ticket.text.trim())findMatches();
        id.focus();
    });
    id.addEventListener('input',idChanged);
    text.addEventListener('input',()=>{searchController?.abort();searchController=null;matches.replaceChildren();});
    lookupButton.addEventListener('click',()=>lookup());search.addEventListener('click',findMatches);
    byId('Paste').addEventListener('click',async()=>{
        const generation=epoch,old=id.value;
        try{const value=(await navigator.clipboard.readText()).trim();if(generation!==epoch||!dialog.open||id.value!==old)return;
            if(!valid(value)){message('invalid');return;}id.value=value;idChanged();await lookup();
        }catch(_){if(generation===epoch&&dialog.open)message('clipboard');}
    });
    apply.addEventListener('click',async()=>{
        if(applying)return;applying=true;apply.disabled=true;
        const generation=epoch,wantedText=text.value,wantedAnchor=anchor.value,value=id.value;
        const result=await lookup(wantedAnchor);
        try{
            if(generation!==epoch)return;
            if(!result)return;
            if(id.value!==value||text.value!==wantedText){message('changed');return;}
            const label=wantedText.trim()?wantedText:result.name||'['+result.nodeId+']';
            // Native dialogs make the editor inert: close before restoring its text selection.
            dialog.close();
            try{bridge().apply(ticket,label,'node '+result.nodeId+(wantedAnchor?' '+wantedAnchor:''));epoch++;abort();}
            catch(error){dialog.showModal();message(error.message);}
        }finally{if(generation===epoch){applying=false;apply.disabled=false;}}
    });
    remove.addEventListener('click',()=>{
        dialog.close();
        try{bridge().apply(ticket,ticket.text,null);epoch++;abort();}
        catch(error){dialog.showModal();message(error.message);}
    });
    byId('Cancel').addEventListener('click',close);
    dialog.addEventListener('cancel',event=>{event.preventDefault();close();});
    for(const input of [id,text])input.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();apply.click();}});
})();
