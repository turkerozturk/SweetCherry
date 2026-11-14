(() => {
 'use strict';
 const dialog=document.getElementById('quickTagsDialog');if(!dialog)return;
 const get=id=>document.getElementById(id),form=get('quickTagsForm'),input=get('quickTagsValue'),search=get('quickTagsSearch'),choices=get('quickTagsChoices'),summary=get('quickTagsSummary'),status=get('quickTagsStatus'),save=get('quickTagsSave'),cancel=get('quickTagsCancel');
 let trigger,record,controller,generation=0,saving=false,token='',catalogue=[];
 const split=value=>Array.from(new Set(value.split(/ +/).filter(Boolean)));
 const fold=value=>value.normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase().replace(/ı/g,'i');
 function render(){const selected=new Set(split(input.value)),term=fold(search.value);choices.replaceChildren();summary.replaceChildren();
  for(const value of Array.from(new Set([...catalogue,...selected])).sort((a,b)=>a.localeCompare(b))){
   const label=document.createElement('label'),box=document.createElement('input'),text=document.createElement('span');box.type='checkbox';box.value=value;box.checked=selected.has(value);box.disabled=saving||!record;text.textContent=value;label.hidden=!fold(value).includes(term);
   box.addEventListener('change',()=>{const set=new Set(split(input.value));box.checked?set.add(value):set.delete(value);input.value=Array.from(set).join(' ');render();});label.append(box,text);choices.append(label);
  }
  for(const tag of selected){const badge=document.createElement('span');badge.className='badge text-bg-secondary';badge.textContent=tag;summary.append(badge);}
 }
 function close(){if(saving)return;generation++;controller?.abort();dialog.close();if(trigger?.isConnected)trigger.focus();}
 async function request(method,body){const url=new URL(dialog.dataset.endpoint+encodeURIComponent(trigger.dataset.quickTags),location.href);url.searchParams.set('_tenantView',token);
  const local=new AbortController();controller=local;const timer=setTimeout(()=>local.abort(),30000);
  try{const response=await fetch(url,{method,body,credentials:'same-origin',cache:'no-store',redirect:'error',headers:{Accept:'application/json'},signal:local.signal});
   if(!response.ok)throw Error(response.status===409?'changed':response.status===403?'readonly':'error');
   if(response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('error');return await response.json();
  }finally{clearTimeout(timer);}
 }
 document.addEventListener('click',async event=>{const button=event.target.closest('[data-quick-tags]');if(!button||saving)return;event.preventDefault();
  if(!/^[1-9][0-9]{0,18}$/.test(button.dataset.quickTags))return;
  generation++;controller?.abort();const current=generation;trigger=button;token=button.dataset.quickTagsToken||dialog.dataset.token;record=null;catalogue=[];input.value='';search.value='';input.disabled=true;search.disabled=true;save.disabled=true;render();get('quickTagsNode').textContent='';get('quickTagsMaster').hidden=true;status.textContent=dialog.dataset.loading;dialog.showModal();cancel.focus();
  try{const data=await request('GET');if(current!==generation)return;
   if(data.id!==button.dataset.quickTags||typeof data.tags!=='string'||typeof data.name!=='string'||!Array.isArray(data.catalogue)||data.catalogue.some(v=>typeof v!=='string')||!/^[a-f0-9]{64}$/.test(data.revision))throw Error('error');
   record=data;catalogue=data.catalogue;input.value=data.tags;input.disabled=false;search.disabled=false;save.disabled=false;get('quickTagsNode').textContent=data.name+' ['+data.id+']';
   const master=get('quickTagsMaster');master.hidden=data.id===data.contentId;master.textContent=dialog.dataset.master+' ['+data.contentId+']';render();status.textContent='';input.focus();
  }catch(error){if(current===generation&&error.name!=='AbortError')status.textContent=dialog.dataset[error.message]||dialog.dataset.error;}
 });
 input.addEventListener('input',render);search.addEventListener('input',render);
 form.addEventListener('submit',async event=>{event.preventDefault();if(saving||!record)return;if(input.value.length>20000){status.textContent=dialog.dataset.tooLong;return;}
  const current=generation;saving=true;input.disabled=true;search.disabled=true;save.disabled=true;cancel.disabled=true;render();status.textContent=dialog.dataset.saving;
  const body=new URLSearchParams({tags:input.value,revision:record.revision,_tenantView:token});const csrf=form.querySelector('input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
  try{const result=await request('POST',body);if(current!==generation)return;
   if(result.id!==record.id||typeof result.tags!=='string'||!/^[a-f0-9]{64}$/.test(result.revision))throw Error('error');
   record=result;saving=false;close();if(trigger.dataset.quickTagsReload==='true'){location.reload();return;}
   const notice=get('quickTagsNotice');notice.textContent=dialog.dataset.saved;notice.hidden=false;setTimeout(()=>{notice.hidden=true;},4000);
  }catch(error){if(current===generation){status.textContent=dialog.dataset[error.message]||dialog.dataset.error;if(error.message==='changed'||error.message==='readonly')record=null;}}
  finally{saving=false;input.disabled=!record;search.disabled=!record;save.disabled=!record;cancel.disabled=false;render();}
 });
 cancel.addEventListener('click',close);dialog.addEventListener('cancel',event=>{event.preventDefault();close();});
})();
