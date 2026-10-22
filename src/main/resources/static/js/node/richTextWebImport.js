(() => {
    'use strict';
    const button=document.getElementById('richWebImport'),dialog=document.getElementById('richWebDialog');
    if(!button||!dialog)return;
    const url=document.getElementById('richWebUrl'),load=document.getElementById('richWebLoad'),apply=document.getElementById('richWebApply');
    const status=document.getElementById('richWebStatus'),preview=document.getElementById('richWebPreview'),form=document.getElementById('richEditForm');
    let ticket,items,controller;
    function error(code){status.textContent=dialog.dataset['error'+code.charAt(0).toUpperCase()+code.slice(1)]||dialog.dataset.error;}
    function close(){controller?.abort();controller=null;dialog.close();}
    button.addEventListener('pointerdown',event=>event.preventDefault());
    button.addEventListener('click',()=>{
        try{ticket=window.RichTextImportBridge.capture();}catch(_){return;}
        items=null;apply.disabled=true;preview.textContent='';status.textContent='';load.disabled=false;dialog.showModal();url.focus();
    });
    load.addEventListener('click',async()=>{
        controller?.abort();const request=new AbortController();controller=request;
        apply.disabled=true;items=null;preview.textContent='';status.textContent=dialog.dataset.loading;load.disabled=true;
        try {
            let parsed;try{parsed=new URL(url.value.trim());}catch(_){throw new Error('url');}
            if(!['http:','https:'].includes(parsed.protocol)||parsed.username||parsed.password)throw new Error('url');
            const data=new URLSearchParams({url:parsed.href,_tenantView:window.richEditorTenant||''});
            const csrf=form.querySelector('input[name="_csrf"]');if(csrf)data.set(csrf.name,csrf.value);
            const response=await fetch(dialog.dataset.endpoint,{method:'POST',body:data,credentials:'same-origin',redirect:'error',signal:request.signal});
            const result=await response.json();if(!response.ok)throw new Error(result.error||'network');
            if(controller!==request)return;
            try{items=window.RichTextClipboard.fromHtml(result.html,document,result.images);}catch(_){throw new Error('large');}
            if(!items.length)throw new Error('empty');
            const images=items.filter(item=>item.image).length,tables=items.filter(item=>item.table).length;
            preview.textContent=items.map(item=>item.text|| (item.table?item.table.map(row=>row.join(' | ')).join('\n'):'[image]')).join('').slice(0,4000);
            status.textContent=result.title+' — '+dialog.dataset.summary.replace('{images}',images).replace('{tables}',tables).replace('{skipped}',result.skippedImages);
            apply.disabled=false;
        } catch(ex) {if(ex.name!=='AbortError'&&controller===request)error(['url','large','blocked','busy','type','timeout','disabled','empty'].includes(ex.message)?ex.message:'network');}
        finally{if(controller===request)load.disabled=false;}
    });
    apply.addEventListener('click',()=>{
        // A modal makes the editor inert. Close it before restoring the captured caret.
        if(!items)return;
        dialog.close();
        try{if(!window.RichTextImportBridge.insert(ticket,items))throw new Error();controller?.abort();controller=null;}
        catch(_){dialog.showModal();error('changed');}
    });
    url.addEventListener('input',()=>{controller?.abort();controller=null;items=null;apply.disabled=true;load.disabled=false;preview.textContent='';status.textContent='';});
    document.getElementById('richWebCancel').addEventListener('click',close);
    dialog.addEventListener('cancel',event=>{event.preventDefault();close();});
    url.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();load.click();}});
})();
