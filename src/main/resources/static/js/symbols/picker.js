(() => {
    'use strict';
    const dialog=document.getElementById('symbol-picker'),core=window.SweetCherryInlineSymbols,catalog=window.SweetCherrySymbolCatalog;if(!dialog||!core||!catalog)return;
    const find=selector=>dialog.querySelector(selector),query=find('[data-symbol-query]'),kind=find('[data-symbol-kind]'),results=find('[data-symbol-results]'),status=find('[data-symbol-status]'),value=find('[data-symbol-value]'),preview=find('[data-symbol-preview]'),copy=find('[data-symbol-copy]'),insert=find('[data-symbol-insert]'),previous=find('[data-symbol-prev]'),next=find('[data-symbol-next]');
    const normalize=text=>text.normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase().replace(/ı/g,'i');
    const entries=catalog.items.map(item=>({item,key:normalize(item.label+' '+item.value+' '+item.terms)}));
    let page=0,selected=null,adapter=null,committed=false,trigger=null,generation=0;
    function choose(item){selected=item;value.value=item.value;preview.replaceChildren(core.create(item));copy.disabled=false;insert.disabled=!adapter;status.textContent='';for(const button of results.querySelectorAll('button'))button.setAttribute('aria-pressed',String(button.dataset.value===item.value));}
    function refresh(){
        const needle=normalize(query.value),matches=entries.filter(entry=>(kind.value==='all'||entry.item.kind===kind.value)&&entry.key.includes(needle));
        page=Math.max(0,Math.min(page,Math.max(0,Math.ceil(matches.length/96)-1)));results.replaceChildren();
        for(const {item} of matches.slice(page*96,page*96+96)){
            const button=document.createElement('button');button.type='button';button.className='sc-symbol-item';button.dataset.value=item.value;button.title=item.value;button.setAttribute('aria-pressed',String(selected?.value===item.value));
            const label=document.createElement('small');label.textContent=item.label;const glyph=core.create(item);glyph.setAttribute('aria-hidden','true');button.append(glyph,label);button.addEventListener('click',()=>choose(item));results.append(button);
        }
        find('[data-symbol-count]').textContent=matches.length?(dialog.dataset.count+' '+(page*96+1)+'–'+Math.min(matches.length,page*96+96)+' / '+matches.length):dialog.dataset.empty;
        previous.disabled=page===0;next.disabled=(page+1)*96>=matches.length;
    }
    function inputAdapter(input){
        if(!input||input.disabled||input.readOnly||!['INPUT','TEXTAREA'].includes(input.tagName)||typeof input.selectionStart!=='number')return null;
        const ticket={text:input.value,start:input.selectionStart,end:input.selectionEnd};
        return {restore(){if(input.isConnected){input.focus();if(input.value===ticket.text)input.setSelectionRange(ticket.start,ticket.end);}},insert(text){
            if(!input.isConnected||input.disabled||input.readOnly||input.value!==ticket.text)throw Error('changed');
            const updated=input.value.slice(0,ticket.start)+text+input.value.slice(ticket.end);if(input.maxLength>=0&&updated.length>input.maxLength)throw Error('long');
            input.setRangeText(text,ticket.start,ticket.end,'end');input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));
            return ()=>{input.focus();input.setSelectionRange(ticket.start+text.length,ticket.start+text.length);};
        }};
    }
    function open(button){
        trigger=button;adapter=null;committed=false;generation++;status.textContent='';
        try{
            if(button.hasAttribute('data-symbol-rich')){
                const bridge=window.RichTextSymbolBridge;if(bridge){const ticket=bridge.capture();adapter={restore:()=>bridge.restore(ticket.selection),insert:text=>{const range=bridge.insert(ticket,text);return()=>bridge.restore(range);}};}
            }else if(button.dataset.symbolTarget)adapter=inputAdapter(document.getElementById(button.dataset.symbolTarget));
        }catch(_){status.textContent=dialog.dataset.changed;}
        insert.disabled=!selected||!adapter;copy.disabled=!selected;refresh();dialog.showModal();query.focus();query.select();
    }
    document.addEventListener('click',event=>{const button=event.target.closest('[data-symbol-picker-open]');if(button){event.preventDefault();if(!dialog.open)open(button);}});
    query.addEventListener('input',()=>{page=0;refresh();});kind.addEventListener('change',()=>{page=0;refresh();});previous.addEventListener('click',()=>{page--;refresh();});next.addEventListener('click',()=>{page++;refresh();});
    for(const selector of ['[data-symbol-close]','[data-symbol-cancel]'])find(selector).addEventListener('click',()=>dialog.close());
    dialog.addEventListener('close',()=>{generation++;if(!committed&&adapter)adapter.restore();else if(!committed&&trigger?.isConnected)trigger.focus();adapter=null;});
    insert.addEventListener('click',()=>{
        if(!selected||!adapter)return;
        try{const after=adapter.insert(selected.value);committed=true;dialog.close();after?.();}
        catch(error){status.textContent=error.message==='long'?dialog.dataset.long:dialog.dataset.changed;}
    });
    copy.addEventListener('click',async()=>{
        if(!selected)return;const current=generation,text=selected.value;
        try{if(navigator.clipboard?.writeText)await navigator.clipboard.writeText(text);else{value.focus();value.select();if(!document.execCommand('copy'))throw Error('clipboard');}if(current===generation&&dialog.open)status.textContent=dialog.dataset.copied;}
        catch(_){if(current===generation&&dialog.open){value.focus();value.select();status.textContent=dialog.dataset.copyFailed;}}
    });
})();
