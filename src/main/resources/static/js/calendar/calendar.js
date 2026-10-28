(() => {
 'use strict';
 const root=document.getElementById('nodeCalendar');if(!root)return;
 const config=window.calendarConfig,dialog=document.getElementById('calendarDialog'),form=document.getElementById('calendarCreatedForm');
 const get=id=>document.getElementById(id),status=get('calendarStatus'),content=get('calendarContent');
 let current=null,version=0,controller,saving=false;
 const url=(path,parameters={})=>{const value=new URL(path,location.href);value.searchParams.set('_tenantView',config.token);for(const [key,data] of Object.entries(parameters))value.searchParams.set(key,data);return value;};
 function close(){if(saving)return;version++;controller?.abort();dialog.close();content.replaceChildren();}
 get('calendarClose').addEventListener('click',close);dialog.addEventListener('cancel',event=>{event.preventDefault();close();});
 root.addEventListener('click',event=>{
  const button=event.target.closest('.cal-entry');if(!button||saving)return;
  version++;controller?.abort();current={...button.dataset};
  get('calendarDialogTitle').textContent=current.name;
  get('calendarEntryId').textContent='['+current.id+']'+(current.shared==='true'?' → ['+current.contentId+']':'');
  get('calendarDates').textContent=config.messages.created+': '+current.local.replace('T',' ')+' · '+config.zone+' | '+config.messages.modified+': '+new Intl.DateTimeFormat(document.documentElement.lang||undefined,{timeZone:config.zone,dateStyle:'medium',timeStyle:'short'}).format(new Date(Number(current.modified)*1000));
  get('calendarShared').hidden=current.shared!=='true';
  form.hidden=root.dataset.editable!=='true'||current.readOnly==='true';get('calendarLocked').hidden=!form.hidden;
  form.elements.expectedCreated.value=current.created;form.elements.expectedModified.value=current.modified;form.elements.local.value=current.local;
  form.action=url(config.base+encodeURIComponent(current.id)+'/created');
  get('calendarOpenNode').href=url(config.node+encodeURIComponent(current.id));
  get('calendarOpenTree').href=url(config.tree,{nodeId:current.id});
  content.replaceChildren();content.hidden=true;status.textContent='';dialog.showModal();
 });
 get('calendarShowContent').addEventListener('click',async()=>{
  if(!current)return;const ticket=version;controller?.abort();controller=new AbortController();status.textContent=config.messages.loading;
  try{const response=await fetch(url(config.base+encodeURIComponent(current.id)+'/content'),{credentials:'same-origin',headers:{Accept:'application/json'},signal:controller.signal});
   const data=(response.headers.get('Content-Type')||'').includes('application/json')?await response.json():{};
   if(ticket!==version)return;if(!response.ok||typeof data.html!=='string')throw new Error(data.error||'failed');
   // The server returns the same escaped rich-text renderer used in the node view, never raw note HTML.
   content.innerHTML=data.html;content.querySelectorAll('a[href]').forEach(link=>{if(!link.getAttribute('href').startsWith('#')){link.target='_blank';link.rel='noopener noreferrer';}});content.hidden=false;status.textContent='';
  }catch(error){if(ticket===version&&error.name!=='AbortError')status.textContent=config.messages[error.message]||config.messages.failed;}
 });
 form.addEventListener('submit',async event=>{
  event.preventDefault();if(saving||!current)return;saving=true;form.inert=true;get('calendarClose').disabled=true;status.textContent=config.messages.saving;
  const abort=new AbortController(),timeout=setTimeout(()=>abort.abort(),30000);
  try{const response=await fetch(form.action,{method:'POST',credentials:'same-origin',headers:{Accept:'application/json'},body:new URLSearchParams(new FormData(form)),signal:abort.signal});
   const data=(response.headers.get('Content-Type')||'').includes('application/json')?await response.json():{};
   if(!response.ok||data.saved!==true)throw new Error(data.error||'failed');status.textContent=config.messages.saved;location.reload();
  }catch(error){status.textContent=config.messages[error.message]||config.messages.failed;}
  finally{clearTimeout(timeout);saving=false;form.inert=false;get('calendarClose').disabled=false;}
 });
})();
