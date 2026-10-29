(() => {
 'use strict';
 const tools=document.getElementById('markdownTools');if(!tools)return;
 const form=document.getElementById('plainEditForm'),input=document.getElementById('nodeText'),pane=document.getElementById('markdownPreviewPane'),preview=document.getElementById('markdownPreview'),status=document.getElementById('markdownPreviewStatus'),auto=document.getElementById('markdownAutoPreview'),toggle=document.getElementById('markdownPreviewToggle');
 let version=0,controller,timer;
 tools.querySelectorAll('[data-md-before],[data-md-prefix],[data-md-text]').forEach(button=>{
  button.addEventListener('pointerdown',event=>event.preventDefault());
  button.addEventListener('click',()=>{
   const selected=input.value.slice(input.selectionStart,input.selectionEnd),d=button.dataset;
   let text=d.mdText??((d.mdBefore||'')+selected+(d.mdAfter||'')),start=input.selectionStart,end=input.selectionEnd;
   if(d.mdPrefix!==undefined){start=input.value.lastIndexOf('\n',Math.max(0,start-1))+1;text=input.value.slice(start,end).split('\n').map(line=>d.mdPrefix+line).join('\n');}
   window.PlainTextEditorBridge?.replace(start,end,text);schedule();
  });
 });
 async function update(){
  if(pane.hidden)return;controller?.abort();controller=new AbortController();const active=controller;const ticket=++version,source=input.value;
  status.textContent=tools.dataset.loading;
  const body=new URLSearchParams();for(const name of ['_tenantView','_csrf']){const field=form.querySelector('[name="'+name+'"]');if(field)body.set(name,field.value);}body.set('text',source);
  const timeout=setTimeout(()=>active.abort(),20000);
  try{
   const response=await fetch(tools.dataset.previewUrl,{method:'POST',body,credentials:'same-origin',headers:{Accept:'application/json'},signal:controller.signal});
   if(!response.ok||!(response.headers.get('Content-Type')||'').includes('application/json'))throw new Error('failed');
   const data=await response.json();if(ticket!==version||source!==input.value||pane.hidden)return;
   if(typeof data.html!=='string')throw new Error('failed');preview.innerHTML=data.html;status.textContent='';
  }catch(error){if(ticket===version&&!pane.hidden)status.textContent=tools.dataset.failed;}
  finally{clearTimeout(timeout);}
 }
 function schedule(){clearTimeout(timer);if(auto.checked&&!pane.hidden)timer=setTimeout(update,600);}
 toggle.addEventListener('click',()=>{pane.hidden=!pane.hidden;toggle.setAttribute('aria-expanded',String(!pane.hidden));document.getElementById('markdownShell').classList.toggle('md-preview-open',!pane.hidden);if(pane.hidden){version++;controller?.abort();}else update();});
 document.getElementById('markdownRefresh').addEventListener('click',update);
 auto.addEventListener('change',schedule);input.addEventListener('input',schedule);form.addEventListener('editor:changed',schedule);
 toggle.click();
})();
