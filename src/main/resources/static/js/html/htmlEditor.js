(() => {
 'use strict';
 const tools=document.getElementById('htmlTools');if(!tools)return;
 const form=document.getElementById('plainEditForm'),input=document.getElementById('nodeText'),pane=document.getElementById('htmlPreviewPane'),preview=document.getElementById('htmlPreview'),status=document.getElementById('htmlPreviewStatus'),auto=document.getElementById('htmlAutoPreview'),toggle=document.getElementById('htmlPreviewToggle');
 let revision=0,controller,timer,composing=false;
 tools.querySelectorAll('[data-html-before],[data-html-text]').forEach(button=>{
  button.addEventListener('pointerdown',event=>event.preventDefault());
  button.addEventListener('click',()=>{
   const selected=input.value.slice(input.selectionStart,input.selectionEnd),d=button.dataset;
   window.PlainTextEditorBridge?.replace(input.selectionStart,input.selectionEnd,d.htmlText??((d.htmlBefore||'')+selected+(d.htmlAfter||'')));
   schedule();
  });
 });
 function invalidate(){revision++;controller?.abort();}
 async function update(){
  clearTimeout(timer);if(pane.hidden||composing)return;
  invalidate();const ticket=revision,source=input.value;
  controller=new AbortController();const active=controller;
  status.textContent=tools.dataset.loading;
  const body=new URLSearchParams();
  for(const name of ['_tenantView','_csrf']){const field=form.querySelector('[name="'+name+'"]');if(field)body.set(name,field.value);}
  body.set('text',source);const timeout=setTimeout(()=>active.abort(),20000);
  try{
   const response=await fetch(tools.dataset.previewUrl,{method:'POST',body,credentials:'same-origin',headers:{Accept:'application/json'},signal:active.signal});
   if(!response.ok||response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw new Error('failed');
   const data=await response.json();
   if(ticket!==revision||source!==input.value||pane.hidden)return;
   if(typeof data.document!=='string')throw new Error('failed');
   // Server-sanitized HTML goes only to a sandboxed iframe, never to the parent DOM.
   preview.srcdoc=data.document;status.textContent='';
  }catch(error){if(ticket===revision&&!pane.hidden)status.textContent=tools.dataset.failed;}
  finally{clearTimeout(timeout);}
 }
 function schedule(){clearTimeout(timer);invalidate();if(auto.checked&&!pane.hidden&&!composing)timer=setTimeout(update,600);}
 toggle.addEventListener('click',()=>{
  pane.hidden=!pane.hidden;toggle.setAttribute('aria-expanded',String(!pane.hidden));document.getElementById('markdownShell').classList.toggle('md-preview-open',!pane.hidden);
  if(pane.hidden){clearTimeout(timer);invalidate();}else update();
 });
 document.getElementById('htmlRefresh').addEventListener('click',update);
 input.addEventListener('compositionstart',()=>{composing=true;clearTimeout(timer);invalidate();});
 input.addEventListener('compositionend',()=>{composing=false;schedule();});
 auto.addEventListener('change',schedule);input.addEventListener('input',schedule);form.addEventListener('editor:changed',schedule);
 toggle.click();
})();
