(() => {
    'use strict';
    const dialog=document.getElementById('richWebDialog'),form=document.getElementById('richEditForm'),status=document.getElementById('richPasteStatus');
    if(!dialog||!form||!document.getElementById('richWebImport'))return;
    let controller;
    function handle(html){
        if(typeof html!=='string'||html.length>2000000)return false;
        const fragment=window.DOMPurify.sanitize(html,{RETURN_DOM_FRAGMENT:true,ALLOWED_TAGS:['img'],ALLOWED_ATTR:['src']});
        const sources=Array.from(fragment.querySelectorAll('img')).map(image=>image.getAttribute('src')||'');
        const remote=sources.some(src=>/^https?:\/\//i.test(src));
        if(!remote&&!sources.some(src=>/^data:image\/(jpeg|jpg|gif|webp|bmp|tiff|vnd\.wap\.wbmp);base64,/i.test(src)))return false;
        if(remote&&!window.confirm(dialog.dataset.clipboardConfirm))return false;
        let ticket,release;try{ticket=window.RichTextImportBridge.capture();release=window.RichTextImportBridge.begin();}catch(_){return true;}
        controller?.abort();const request=new AbortController();controller=request;
        status.hidden=false;status.textContent=dialog.dataset.loading;
        const timeout=setTimeout(()=>request.abort(),35000);
        (async()=>{
            try{
                const data=new URLSearchParams({html,_tenantView:window.richEditorTenant||''});
                const csrf=form.querySelector('input[name="_csrf"]');if(csrf)data.set(csrf.name,csrf.value);
                const response=await fetch(dialog.dataset.clipboardEndpoint,{method:'POST',body:data,credentials:'same-origin',redirect:'error',signal:request.signal});
                if(!response.ok||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('import');
                const result=await response.json();if(controller!==request||request.signal.aborted)return;
                const items=window.RichTextClipboard.fromHtml(result.html,document,result.images);
                release();
                if(!window.RichTextImportBridge.insert(ticket,items))throw Error('changed');
                status.textContent=dialog.dataset.clipboardDone.replace('{skipped}',String(result.skippedImages));
            }catch(_){if(controller===request)status.textContent=dialog.dataset.clipboardFailed;}
            finally{release();clearTimeout(timeout);if(controller===request)controller=null;}
        })();
        return true;
    }
    window.addEventListener('pagehide',()=>{controller?.abort();controller=null;});
    window.RichTextPasteImport={handle};
})();
