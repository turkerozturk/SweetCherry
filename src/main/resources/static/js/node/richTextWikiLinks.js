(() => {
 'use strict';
 window.RichTextWikiLinks={install(form){
  const dialog=document.getElementById('richWikiDialog');if(!dialog)return;
  const status=document.getElementById('richWikiStatus'),list=document.getElementById('richWikiIssues'),yes=document.getElementById('richWikiSave'),no=document.getElementById('richWikiCancel');
  let pending=false,approved=false,answer;
  const finish=value=>{dialog.close();if(answer){const resolve=answer;answer=null;resolve(value);}};
  yes.addEventListener('click',()=>finish(true));no.addEventListener('click',()=>finish(false));dialog.addEventListener('cancel',event=>{event.preventDefault();finish(false);});
  form.addEventListener('submit',async event=>{
   if(event.defaultPrevented)return;if(approved){approved=false;return;}
   form.elements.wikiChoices.value='{}';form.elements.keepUnresolvedWikiLinks.value='false';
   const xml=form.elements.xml.value;
   const parsed=new DOMParser().parseFromString(xml,'application/xml');
   if(!(parsed.documentElement?.textContent||'').includes('[[')){status.textContent='';return;}
   event.preventDefault();if(pending)return;pending=true;form.inert=true;
   form.elements.wikiChoices.value='{}';form.elements.keepUnresolvedWikiLinks.value='false';status.textContent=dialog.dataset.loading;
   const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
   try{
    const body=new URLSearchParams({xml,_tenantView:form.elements._tenantView.value});
    const csrf=form.querySelector('input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
    const response=await fetch(dialog.dataset.endpoint,{method:'POST',body,credentials:'same-origin',cache:'no-store',redirect:'error',signal:controller.signal,headers:{Accept:'application/json'}});
    if(!response.ok||response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('failed');
    const preview=await response.json();if(!preview.choices||!Array.isArray(preview.issues))throw Error('failed');clearTimeout(timer);
    const choices={...preview.choices};list.replaceChildren();
    for(const issue of preview.issues){
     const row=document.createElement('section'),title=document.createElement('h3'),label=document.createElement('p');title.className='h6';title.textContent='[['+issue.name+']]';label.textContent=issue.kind==='missing'?dialog.dataset.missing:dialog.dataset.ambiguous;row.append(title,label);
     const select=document.createElement('select');select.className='form-select';select.setAttribute('aria-label',issue.name);
     const literal=document.createElement('option');literal.value='0';literal.textContent=dialog.dataset.literal;select.append(literal);
     for(const candidate of issue.candidates){const option=document.createElement('option');option.value=candidate.id;option.textContent=candidate.breadcrumb+(candidate.shared?' · '+dialog.dataset.shared:'');select.append(option);}
     select.addEventListener('change',()=>{choices[issue.name]=select.value;});row.append(select);
     if(issue.truncated){const hint=document.createElement('p');hint.textContent=dialog.dataset.limit;row.append(hint);}list.append(row);
    }
    if(preview.issues.length){const confirmed=await new Promise(resolve=>{answer=resolve;dialog.showModal();no.focus();});if(!confirmed){status.textContent='';return;}}
    form.elements.wikiChoices.value=JSON.stringify(choices);form.elements.keepUnresolvedWikiLinks.value=String(preview.issues.length>0);
    status.textContent='';form.inert=false;approved=true;form.requestSubmit(event.submitter||undefined);
   }catch(error){status.textContent=dialog.dataset.error;}
   finally{clearTimeout(timer);pending=false;form.inert=false;}
  });
 }};
})();
