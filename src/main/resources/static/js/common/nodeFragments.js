(() => {
    'use strict';
    const core=window.SweetCherryNodeFragments;if(!core)return;
    const key='sweetcherry.nodeFragments',states=new Map();let enabled=false,paused=false,sequence=0;
    try{enabled=localStorage.getItem(key)==='true';}catch(_){}
    function labels(){document.querySelectorAll('[data-node-fragments-toggle]').forEach(button=>button.setAttribute('aria-pressed',String(enabled)));}
    function roots(container=document){return [...(container.matches?.('[data-node-fragments]')?[container]:[]),...container.querySelectorAll('[data-node-fragments]')];}
    function release(root,state,restore){
        state.controller.abort();states.delete(root);
        if(restore&&root.isConnected)root.replaceChildren(state.original);
    }
    function scan(container=document){
        for(const [root,state] of states)if(!root.isConnected)release(root,state,false);
        if(!enabled||paused)return;
        for(const root of roots(container)){
            if(states.has(root)||root.dataset.fragmentSyntax!=='custom-colors'||!core.id(root.dataset.fragmentContent)||!root.dataset.fragmentToken)continue;
            const original=document.createDocumentFragment();while(root.firstChild)original.append(root.firstChild);
            root.append(original.cloneNode(true));
            const state={original,controller:new AbortController(),cache:new Map(),budget:core.budget(Math.max(1,Math.min(50,Number(root.dataset.fragmentCount)||20))),stopped:false};
            states.set(root,state);expand(root,root,state,[root.dataset.fragmentContent],0).catch(()=>{});
        }
    }
    function live(root,state){return enabled&&root.isConnected&&states.get(root)===state&&!state.controller.signal.aborted;}
    function note(link,root,state,reason){
        if(!live(root,state))return;
        const hint=document.createElement('small');hint.className='node-fragment-note';hint.textContent=' ('+(root.dataset['fragment'+reason]||root.dataset.fragmentError||'')+')';link.after(hint);
    }
    async function load(root,state,id){
        if(!state.cache.has(id))state.cache.set(id,(async()=>{
            const url=new URL(root.dataset.fragmentEndpoint+encodeURIComponent(id),location.href);url.searchParams.set('_tenantView',root.dataset.fragmentToken);
            const local=new AbortController(),abort=()=>local.abort();state.controller.signal.addEventListener('abort',abort,{once:true});
            const timeout=setTimeout(abort,10000);
            try{
                const response=await fetch(url,{credentials:'same-origin',redirect:'error',cache:'no-store',signal:local.signal,headers:{Accept:'application/json'}});
                if(!response.ok){
                    if(response.status===401||response.status===409){state.stopped=true;throw Error('Stale');}
                    throw Error(response.status===413?'Limit':response.status===404?'Missing':'Error');
                }
                if(!(response.headers.get('Content-Type')||'').includes('application/json'))throw Error('Error');
                const fragment=await response.json();if(!core.valid(fragment))throw Error('Error');return fragment;
            }finally{clearTimeout(timeout);state.controller.signal.removeEventListener('abort',abort);}
        })());
        return state.cache.get(id);
    }
    /** Prefix copied anchors so repeated fragments never steal IDs from their host or each other. */
    function namespace(body,prefix,root,contentId){
        const ids=new Map();let count=0;
        for(const element of body.querySelectorAll('[id],a[name]')){
            const old=element.getAttribute('id')||element.getAttribute('name');let next=prefix+'-'+(++count);while(document.getElementById?.(next))next+='-x';if(!ids.has(old))ids.set(old,next);element.id=next;if(element.hasAttribute('name'))element.setAttribute('name',next);
        }
        for(const link of body.querySelectorAll('a[href]')){
            const href=link.getAttribute('href');let url;try{url=new URL(href,location.href);}catch(_){continue;}
            const own=core.target(href,location.href,new URL(root.dataset.fragmentEndpoint,location.href).pathname.replace(/fragments\/$/,''));
            if(!href.startsWith('#')&&(!own||own.id!==contentId||!url.hash))continue;
            let raw;try{raw=decodeURIComponent(url.hash.slice(1));}catch(_){raw=url.hash.slice(1);}
            if(ids.has(raw)){link.href='#'+ids.get(raw);link.removeAttribute('target');}
            else {const target=new URL(root.dataset.fragmentEndpoint.replace(/fragments\/$/,'')+contentId,location.href);target.searchParams.set('_tenantView',root.dataset.fragmentToken);target.hash=url.hash;link.href=target.href;link.target='_blank';link.rel='noopener noreferrer';link.dataset.fragmentSource='true';}
        }
    }
    /** Split paragraph/inline wrappers around a block inclusion, retaining surrounding formatting. */
    function mount(link,section,scope){
        let host=link.closest('p,h1,h2,h3,h4,h5,h6');
        if(!host||!scope.contains(host)){
            host=link;while(host.parentElement!==scope&&host.parentElement&&/^(SPAN|B|STRONG|I|EM|U|S|DEL|SUP|SUB|SMALL|MARK|FONT)$/.test(host.parentElement.tagName))host=host.parentElement;
        }
        if(host===link){link.replaceWith(section);return;}
        const before=document.createRange();before.setStart(host,0);before.setEndBefore(link);const first=before.extractContents();
        const after=document.createRange();after.setStartAfter(link);after.setEnd(host,host.childNodes.length);const last=after.extractContents();
        const nodes=[];
        if(first.childNodes.length){const part=host.cloneNode(false);part.append(first);nodes.push(part);}
        nodes.push(section);
        if(last.childNodes.length){const part=host.cloneNode(false);part.removeAttribute('id');part.append(last);nodes.push(part);}
        host.replaceWith(...nodes);
    }
    async function expand(scope,root,state,path,depth){
        const nodePath=new URL(root.dataset.fragmentEndpoint,location.href).pathname.replace(/fragments\/$/,'');
        for(const link of [...scope.querySelectorAll('a[href]')]){
            if(!live(root,state))return;
            if(!scope.contains(link)||link.closest('[data-fragment-source]'))continue;
            const target=core.target(link.getAttribute('href'),location.href,nodePath);if(!target)continue;
            if(state.stopped){note(link,root,state,'Stale');continue;}
            if(path.includes(target.id)){if(!new URL(target.href).hash)note(link,root,state,'Cycle');continue;}
            if(depth>=Math.max(1,Math.min(5,Number(root.dataset.fragmentDepth)||3))||!state.budget.reserve()){note(link,root,state,'Limit');continue;}
            try{
                const data=await load(root,state,target.id);if(!live(root,state)||!scope.contains(link))return;
                if(path.includes(data.contentId)){note(link,root,state,'Cycle');continue;}
                if(!state.budget.accept(data.html.length)){note(link,root,state,'Limit');continue;}
                const section=document.createElement('section');section.className='node-fragment'+(data.syntax==='custom-colors'?' theme-note-paper':'');
                const source=document.createElement('a');source.className='node-fragment-source';source.dataset.fragmentSource='true';const sourceUrl=new URL(target.href);sourceUrl.searchParams.set('_tenantView',root.dataset.fragmentToken);source.href=sourceUrl.href;source.target='_blank';source.rel='noopener noreferrer';source.textContent=(root.dataset.fragmentSourceLabel||'')+': '+data.name;
                const body=document.createElement('div');body.className='node-fragment-body';
                const parsed=new DOMParser().parseFromString(data.html,'text/html');
                parsed.querySelectorAll('script,style,iframe,object,embed,link,meta,base,form,input,button,textarea,select').forEach(item=>item.remove());
                for(const element of parsed.body.querySelectorAll('*'))for(const attr of [...element.attributes])if(/^on/i.test(attr.name)||attr.name.toLowerCase()==='srcdoc')element.removeAttribute(attr.name);
                namespace(parsed.body,'sc-inline-'+(++sequence),root,data.contentId);
                parsed.body.querySelectorAll('img').forEach(image=>{image.tabIndex=0;image.title=image.title||root.dataset.fragmentImageTitle||'';});
                body.append(...[...parsed.body.childNodes].map(item=>document.importNode(item,true)));section.append(source,body);
                mount(link,section,scope);
                document.dispatchEvent(new CustomEvent('sweetcherry:content-loaded',{detail:{root:section}}));
                if(data.syntax==='custom-colors')await expand(body,root,state,[...path,data.contentId],depth+1);
            }catch(error){if(live(root,state))note(link,root,state,error.message==='Stale'?'Stale':error.message==='Missing'?'Missing':error.message==='Limit'?'Limit':'Error');}
        }
    }
    function apply(value,persist){
        enabled=value;if(persist)try{localStorage.setItem(key,String(enabled));}catch(_){}
        labels();if(!enabled){for(const [root,state] of [...states])release(root,state,true);}else scan();
    }
    document.addEventListener('click',event=>{
        if(!event.target.closest('[data-node-fragments-toggle]'))return;
        event.preventDefault();event.stopImmediatePropagation();apply(!enabled,true);
    },true);
    document.addEventListener('sweetcherry:content-loaded',event=>scan(event.detail.root));
    window.addEventListener('storage',event=>{if(event.key===key||event.key===null)apply(event.newValue==='true',false);});
    window.addEventListener('pagehide',()=>{paused=true;for(const [root,state] of [...states])release(root,state,true);});
    window.addEventListener('pageshow',()=>{paused=false;labels();scan();});
    labels();scan();
})();
