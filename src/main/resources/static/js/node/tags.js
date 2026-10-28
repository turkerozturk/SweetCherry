(() => {
    'use strict';
    const split=value=>Array.from(new Set((value||'').split(/ +/).filter(Boolean)));
    const filterText=value=>value.normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase().replace(/ı/g,'i');
    function setupBrowser(browser){
        const filter=browser.querySelector('[data-tag-filter]'),list=browser.querySelector('[data-tag-list]'),summary=browser.querySelector('[data-tag-summary]');
        const update=()=>{
            const term=filterText(filter.value);summary.replaceChildren();
            list.querySelectorAll('label').forEach(label=>{
                const input=label.querySelector('input');label.hidden=!filterText(input.value).includes(term);
                if(input.checked){const chip=document.createElement('span');chip.className='badge tag-badge';chip.textContent=input.value;summary.appendChild(chip);}
            });
        };
        filter.addEventListener('input',update);filter.addEventListener('keydown',event=>{if(event.key==='Enter')event.preventDefault();});list.addEventListener('change',update);return update;
    }
    function initialize(){
        document.querySelectorAll('[data-tag-browser]').forEach(browser=>{browser.tagUpdate=setupBrowser(browser);browser.tagUpdate();});
        const button=document.getElementById('nodeTagsChoose'),dialog=document.getElementById('nodeTagsDialog');if(!button||!dialog)return;
        const input=document.getElementById('nodeTags'),list=document.getElementById('nodeTagsList'),status=document.getElementById('nodeTagsStatus');
        const apply=document.getElementById('nodeTagsApply'),search=document.getElementById('nodeTagsSearch'),browser=dialog.querySelector('[data-tag-browser]');
        let controller,generation=0,original='';
        function close(){generation++;controller?.abort();dialog.close();button.focus();}
        button.addEventListener('click',async()=>{
            generation++;controller?.abort();const request=new AbortController();controller=request;const current=generation;original=input.value;
            list.replaceChildren();search.value='';browser.tagUpdate();apply.disabled=true;status.textContent=dialog.dataset.loading;dialog.showModal();search.focus();
            try{
                const token=document.querySelector('#nodePropertiesForm input[name="_tenantView"]').value;
                const response=await fetch(dialog.dataset.endpoint+'?'+new URLSearchParams({_tenantView:token}),{credentials:'same-origin',redirect:'error',signal:request.signal});
                if(!response.ok||!(response.headers.get('content-type')||'').includes('application/json'))throw new Error();
                const values=await response.json();if(current!==generation)return;if(!Array.isArray(values)||values.some(value=>typeof value!=='string'))throw new Error();
                const selected=new Set(split(original)),choices=Array.from(new Set([...values,...selected])).sort((a,b)=>a.localeCompare(b));
                for(const value of choices){const label=document.createElement('label');label.className='tag-choice';const checkbox=document.createElement('input');checkbox.type='checkbox';checkbox.value=value;checkbox.checked=selected.has(value);const text=document.createElement('span');text.textContent=value;label.append(checkbox,text);list.appendChild(label);}
                browser.tagUpdate();status.textContent=choices.length?'':dialog.dataset.empty;apply.disabled=false;
            }catch(error){if(current===generation&&error.name!=='AbortError')status.textContent=dialog.dataset.error;}
        });
        apply.addEventListener('click',()=>{
            if(input.value!==original){status.textContent=dialog.dataset.changed;return;}
            const selected=Array.from(list.querySelectorAll('input:checked')).map(checkbox=>checkbox.value);
            const value=Array.from(new Set(selected)).join(' ');if(value.length>20000){status.textContent=dialog.dataset.tooLong;return;}
            input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));close();input.focus();
        });
        document.getElementById('nodeTagsCancel').addEventListener('click',close);dialog.addEventListener('cancel',event=>{event.preventDefault();close();});
    }
    if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',initialize);else initialize();
})();
