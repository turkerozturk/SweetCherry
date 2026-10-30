(() => {
 'use strict';
 const root=document.querySelector('[data-quick-nodes]');if(!root)return;
 const switchDialog=root.querySelector('#quick-switch-dialog'),noteDialog=root.querySelector('#quick-note-dialog');
 const switchForm=root.querySelector('[data-quick-switch]'),note=root.querySelector('[data-quick-note]');
 let saving=false,optionsReady=false,scope='',initial='',opener=null;
 function status(area,text,error=false){area.textContent=text;area.classList.toggle('text-danger',error);}
 async function json(url,options={}){
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),20000);
  try{const response=await fetch(url,{credentials:'same-origin',cache:'no-store',...options,signal:controller.signal});
   if(response.redirected||!response.ok||!(response.headers.get('Content-Type')||'').includes('application/json'))
    throw Object.assign(new Error('Request failed'),{status:response.status});
   return await response.json();
  }finally{clearTimeout(timer);}
 }
 const endpoint=base=>{const url=new URL(base,location.href);url.searchParams.set('_tenantView',root.dataset.token);return url;};
 const problem=error=>error.status===409?root.dataset.stale:error.status===403?root.dataset.denied:error.status===400||error.status===404?root.dataset.invalid:root.dataset.failed;
 async function destination(id){const url=endpoint(root.dataset.goUrl);url.searchParams.set('nodeId',id);const result=await json(url);
  const target=new URL(result.url,location.href);if(target.origin!==location.origin)throw new Error('Invalid destination');return target.href;}
 function search(input,container,selected){
  const list=container.querySelector('[data-search-results]'),area=container.querySelector('[data-search-status]');
  let version=0,timer=null,controller=null;
  function reset(){version++;clearTimeout(timer);controller?.abort();list.replaceChildren();status(area,'');}
  input.addEventListener('input',()=>{reset();if(!input.value.trim())return;status(area,root.dataset.loading);const current=version;
   timer=setTimeout(async()=>{controller=new AbortController();const active=controller,timeout=setTimeout(()=>active.abort(),15000);
    try{const url=endpoint(root.dataset.searchUrl);url.searchParams.set('q',input.value);
     const response=await fetch(url,{credentials:'same-origin',cache:'no-store',signal:active.signal,headers:{Accept:'application/json'}});
     if(response.redirected||!response.ok||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Object.assign(new Error('Search failed'),{status:response.status});
     const data=await response.json();if(current!==version||!input.closest('dialog').open)return;
     if(!Array.isArray(data.items))throw new Error('Invalid results');
     for(const item of data.items){if(!/^[1-9][0-9]{0,18}$/.test(item.id))continue;
      const button=document.createElement('button');button.type='button';button.className='quick-node-result';button.title=item.breadcrumb;button.dataset.target=item.id;
      if(/^[a-z0-9_-]+$/.test(item.icon||'')&&item.icon!=='zero'){const image=document.createElement('img');image.src=root.dataset.iconBase+item.icon+'.png';image.alt='';image.width=image.height=20;button.append(image);}
      const label=document.createElement('span');label.className='quick-node-label';label.textContent=item.name;
      if(/^#[0-9a-f]{6}$/i.test(item.color||''))label.style.color=item.color;if(item.bold)label.style.fontWeight='bold';
      const number=document.createElement('small');number.className='quick-node-number';number.textContent='['+item.id+']'+(item.shared?' · '+root.dataset.shared:'');button.append(label,number);
      button.addEventListener('click',()=>selected(item,button));list.append(button);
     }
     status(area,[!data.items.length?root.dataset.empty:'',data.more?root.dataset.more:'',data.scanLimited?root.dataset.scan:''].filter(Boolean).join(' '));
    }catch(error){if(current===version)status(area,problem(error),true);}finally{clearTimeout(timeout);}
   },300);
  });
  container.addEventListener('keydown',event=>{if(event.isComposing)return;const buttons=[...list.querySelectorAll('button')];
   if(event.key==='ArrowDown'||event.key==='ArrowUp'){if(!buttons.length)return;event.preventDefault();const index=buttons.indexOf(document.activeElement);buttons[(index+(event.key==='ArrowDown'?1:-1)+buttons.length)%buttons.length].focus();}
   else if(event.key==='Enter'&&event.target===input){event.preventDefault();buttons[0]?.click();}
  });
  return reset;
 }
 const resetSwitch=search(switchForm.querySelector('input[type=search]'),switchForm,async(item,button)=>{
  if(button.disabled)return;button.disabled=true;
  try{const url=await destination(item.id);if(switchDialog.open&&button.isConnected)location.assign(url);}catch(error){status(switchForm.querySelector('[data-search-status]'),problem(error),true);}finally{button.disabled=false;}
 });
 switchForm.addEventListener('submit',event=>event.preventDefault());
 let resetParent=()=>{};
 function snapshot(){return note?JSON.stringify([note.elements.title.value,note.elements.text.value,note.elements.syntax.value,note.elements.parentId.value]):'';}
 function dirty(){return note&&snapshot()!==initial&&(note.elements.title.value!==''||note.elements.text.value!=='');}
 function setInitial(){initial=snapshot();}
 async function openNote(){
  if(!noteDialog)return;noteDialog.showModal();status(note.querySelector('[data-quick-status]'),root.dataset.loading);optionsReady=false;note.querySelector('[type=submit]').disabled=true;
  try{const options=await json(endpoint(root.dataset.optionsUrl));if(!noteDialog.open)return;
   if(!options.writable)throw Object.assign(new Error('Read only'),{status:403});
   scope=/^[a-f0-9]{64}$/.test(options.scope||'')?options.scope:'';
   if(!dirty()){let parent='0';try{const remembered=scope?localStorage.getItem('sc.quick-parent.'+scope):null;if(/^(0|[1-9][0-9]{0,18})$/.test(remembered||''))parent=remembered;}catch{}
    note.elements.parentId.value=parent;setInitial();}
   optionsReady=true;note.querySelector('[type=submit]').disabled=false;status(note.querySelector('[data-quick-status]'),'');note.elements.title.focus();
  }catch(error){status(note.querySelector('[data-quick-status]'),problem(error),true);}
 }
 if(note){setInitial();resetParent=search(note.querySelector('#quick-parent-query'),note.querySelector('[data-parent-picker]'),item=>{note.elements.parentId.value=item.id;note.querySelector('[data-parent-picker]').open=false;note.elements.title.focus();});
  note.addEventListener('submit',async event=>{event.preventDefault();if(saving||!optionsReady||!note.reportValidity())return;
   saving=true;const area=note.querySelector('[data-quick-status]');status(area,root.dataset.loading);
   const fields=[...note.querySelectorAll('input,textarea,select,button')],data=new URLSearchParams(new FormData(note));fields.forEach(field=>field.disabled=true);
   try{const result=await json(root.dataset.createUrl,{method:'POST',body:data,headers:{Accept:'application/json'}});
    if(!/^[1-9][0-9]{0,18}$/.test(result.id))throw new Error('Invalid receipt');
    try{if(scope)localStorage.setItem('sc.quick-parent.'+scope,data.get('parentId'));}catch{}
    note.elements.title.value='';note.elements.text.value='';setInitial();status(area,root.dataset.saved+' ['+result.id+']');
    const link=note.querySelector('[data-created-link]');link.classList.add('d-none');
    try{link.href=await destination(result.id);link.classList.remove('d-none');}catch{}
   }catch(error){if(error.status===409||error.status===403)optionsReady=false;status(area,problem(error)+(!error.status||error.status>=500?' '+root.dataset.uncertain:''),true);}
   finally{fields.forEach(field=>field.disabled=false);saving=false;note.querySelector('[type=submit]').disabled=!optionsReady;}
  });
  note.addEventListener('input',event=>{if(event.target.name){status(note.querySelector('[data-quick-status]'),'');note.querySelector('[data-created-link]').classList.add('d-none');}});
  window.addEventListener('beforeunload',event=>{if(dirty()||saving){event.preventDefault();event.returnValue='';}});
 }
 root.addEventListener('click',event=>{if(!event.target.closest('[data-quick-close]'))return;const dialog=event.target.closest('dialog');if(dialog===noteDialog&&(saving||(dirty()&&!confirm(root.dataset.discard))))return;dialog.close();});
 for(const dialog of [switchDialog,noteDialog].filter(Boolean)){
  dialog.addEventListener('cancel',event=>{if(dialog===noteDialog&&(saving||(dirty()&&!confirm(root.dataset.discard))))event.preventDefault();});
  dialog.addEventListener('close',()=>{if(dialog===switchDialog){resetSwitch();switchForm.querySelector('input').value='';}else{resetParent();note.querySelector('#quick-parent-query').value='';if(dirty()){note.elements.title.value='';note.elements.text.value='';setInitial();}}opener?.focus();});
 }
 document.addEventListener('click',event=>{const button=event.target.closest('[data-quick-switch-open],[data-quick-note-open]');if(!button||button.disabled)return;opener=button;
  if(button.hasAttribute('data-quick-switch-open')){switchDialog.showModal();switchForm.querySelector('input').focus();}else openNote();
 });
 // Slash opens title search only outside editing controls and other dialogs.
 document.addEventListener('keydown',event=>{if(event.key!=='/'||event.ctrlKey||event.altKey||event.metaKey||event.isComposing||document.querySelector('dialog[open]'))return;
  if(event.target.isContentEditable||event.target.closest('input,textarea,select,[contenteditable],[role="textbox"]'))return;event.preventDefault();opener=document.activeElement;switchDialog.showModal();switchForm.querySelector('input').focus();
 });
})();
