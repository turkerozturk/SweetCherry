(() => {
    'use strict';
    const form=document.getElementById('nodePropertiesForm'),dialog=document.getElementById('nodeConversionDialog');
    if(!form||!dialog||form.dataset.richText!=='true')return;
    const status=document.getElementById('nodeConversionStatus'),yes=document.getElementById('nodeConversionYes'),no=document.getElementById('nodeConversionNo');
    let pending=false,approved=false,answer;
    function finish(value){dialog.close();if(answer){const resolve=answer;answer=null;resolve(value);}}
    yes.addEventListener('click',()=>finish(true));no.addEventListener('click',()=>finish(false));
    dialog.addEventListener('cancel',event=>{event.preventDefault();finish(false);});
    form.addEventListener('submit',async event=>{
        if(approved){approved=false;return;}
        const type=form.querySelector('input[name="nodeType"]:checked');
        if(!type||type.value==='RICH_TEXT')return;
        event.preventDefault();if(pending)return;pending=true;
        form.elements.confirmRichTextConversion.value='false';form.elements.conversionRevision.value='';
        status.hidden=false;status.textContent=form.dataset.conversionLoading;form.inert=true;
        const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
        try{
            const url=new URL(form.dataset.conversionUrl,location.href);url.searchParams.set('_tenantView',form.elements._tenantView.value);
            const response=await fetch(url,{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});
            const data=(response.headers.get('Content-Type')||'').includes('application/json')?await response.json():{};
            if(!response.ok||response.redirected||typeof data.revision!=='string'||!/^[a-f0-9]{64}$/.test(data.revision))throw Error('failed');
            clearTimeout(timer);status.textContent='';status.hidden=true;
            const confirmed=await new Promise(resolve=>{answer=resolve;dialog.showModal();no.focus();});
            if(!confirmed)return;
            form.elements.confirmRichTextConversion.value='true';form.elements.conversionRevision.value=data.revision;
            form.inert=false;approved=true;form.requestSubmit(event.submitter||undefined);
        }catch(_){status.textContent=form.dataset.conversionFailed;status.hidden=false;}
        finally{clearTimeout(timer);form.inert=false;pending=false;}
    });
})();
