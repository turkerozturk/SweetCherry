/* Shared, read-only title picker for dialogs. IDs remain strings throughout. */
(() => {
    'use strict';
    let sequence=0;
    const validId=value=>typeof value==='string'&&/^[1-9][0-9]{0,18}$/.test(value)&&BigInt(value)<=9223372036854775807n;
    function create(config){
        const {input,list,status}=config,messages=config.messages||{};
        let version=0,timer,controller,composing=false,choices=[],active=-1;
        list.id ||= 'node-target-results-'+(++sequence);list.setAttribute('role','listbox');
        input.setAttribute('role','combobox');input.setAttribute('aria-autocomplete','list');input.setAttribute('aria-controls',list.id);input.setAttribute('aria-expanded','false');
        const available=()=>!input.disabled&&input.isConnected!==false&&(!config.available||config.available());
        function message(key){status.textContent=messages[key]||'';}
        function reset(clear=false){
            version++;clearTimeout(timer);controller?.abort();controller=null;choices=[];active=-1;list.replaceChildren();status.textContent='';
            input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');if(clear)input.value='';
        }
        function highlight(index){
            active=index;choices.forEach((choice,i)=>choice.setAttribute('aria-selected',String(i===active)));
            if(active<0){input.removeAttribute('aria-activedescendant');return;}
            input.setAttribute('aria-activedescendant',choices[active].id);choices[active].scrollIntoView?.({block:'nearest'});
        }
        function select(item){if(!available())return;reset();input.focus();config.onSelect(item);}
        function row(item,index){
            const button=document.createElement('button');button.type='button';button.className='node-target-result';button.id=list.id+'-'+index;
            button.setAttribute('role','option');button.setAttribute('aria-selected','false');button.title=item.breadcrumb||'';
            if(/^[a-zA-Z0-9_-]+$/.test(item.icon||'')&&item.icon!=='zero'){
                const image=document.createElement('img');image.src=config.icons+encodeURIComponent(item.icon)+'.png';image.alt='';image.width=image.height=20;image.loading='lazy';button.append(image);
            }
            const content=document.createElement('span');content.className='node-target-result-text';
            const name=document.createElement('span');name.textContent=item.name;name.dataset.inlineSymbols="true";if(item.bold===true)name.style.fontWeight='bold';if(/^#[a-f0-9]{6}$/i.test(item.color||'')&&item.color.toLowerCase()!=='#000000')name.style.color=item.color;
            const number=document.createElement('span');number.className='node-target-result-id';number.textContent=' ['+item.id+']';
            const path=document.createElement('small');path.className='node-target-result-path';path.dataset.inlineSymbols='true';path.textContent=item.breadcrumb||'';
            content.append(name,number,path);button.append(content);button.addEventListener('click',()=>select(item));return button;
        }
        async function search(current,value){
            if(current!==version||!available())return;
            const request=new AbortController();controller=request;const timeout=setTimeout(()=>request.abort(),15000);
            try{
                const url=new URL(config.url,location.href);url.searchParams.set('_tenantView',config.token());url.searchParams.set('q',value);
                const response=await fetch(url,{credentials:'same-origin',cache:'no-store',redirect:'error',signal:request.signal,headers:{Accept:'application/json'}});
                if(!response.ok||response.redirected||!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error(response.status===409?'stale':'error');
                const data=await response.json();if(current!==version||controller!==request||input.value.trim()!==value||!available())return;
                if(!Array.isArray(data.items)||data.items.length>40||data.items.some(item=>!item||!validId(item.id)||typeof item.name!=='string'||typeof item.breadcrumb!=='string'))throw Error('error');
                choices=data.items.map(row);list.replaceChildren(...choices);input.setAttribute('aria-expanded',String(choices.length>0));
                message(data.more||data.scanLimited?'limit':choices.length?'':'empty');
            }catch(error){if(current===version&&controller===request&&available())message(error.message==='stale'?'stale':'error');}
            finally{clearTimeout(timeout);}
        }
        function refresh(immediate=false){
            reset();const value=input.value.trim();if(composing||!available()||!value)return;
            if(value.length>160){message('long');return;}message('loading');const current=version;
            if(immediate)return search(current,value);
            timer=setTimeout(()=>search(current,value),250);
        }
        input.addEventListener('input',()=>{config.onQueryChange?.();refresh();});
        input.addEventListener('compositionstart',()=>{composing=true;reset();});
        input.addEventListener('compositionend',()=>{composing=false;refresh();});
        input.addEventListener('keydown',event=>{
            if(event.isComposing||composing)return;
            if(event.key==='Enter'){event.preventDefault();event.stopPropagation();if(choices.length)choices[active>=0?active:0].click();else refresh(true);}
            else if(choices.length&&(event.key==='ArrowDown'||event.key==='ArrowUp')){event.preventDefault();highlight(active<0?(event.key==='ArrowDown'?0:choices.length-1):(active+(event.key==='ArrowDown'?1:choices.length-1))%choices.length);}
        });
        config.dialog?.addEventListener('close',()=>reset());config.dialog?.addEventListener('cancel',()=>reset());
        return {reset,refresh};
    }
    window.SweetCherryNodeSearch={create};
})();
