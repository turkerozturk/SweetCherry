(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryTreeSavedSearches=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    const uuid=value=>typeof value==='string'&&/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(value);
    function valid(data,id){return data?.id===id&&Number.isInteger(data.page)&&data.page>=0&&Number.isSafeInteger(data.total)&&data.total>=0&&typeof data.hasNext==='boolean'&&Array.isArray(data.results)&&data.results.length<=50&&data.results.every(n=>Number.isSafeInteger(n.nodeId)&&n.nodeId>0&&typeof n.name==='string'&&typeof n.path==='string'&&n.hasChildren===false);}
    function install({box,request,url,row,keyboard,select}){
        const doc=box.ownerDocument,tree=box.querySelector('[data-saved-search-tree]'),status=box.querySelector('[data-saved-search-status]');
        const branches=new Map();let controller,generation=0,stopped=false,selected=0;
        function text(value){const item=doc.createElement('li');item.className='workspace-saved-search-message';item.textContent=value;return item;}
        function markSelected(id){selected=Number(id);tree.querySelectorAll('[aria-current]').forEach(link=>link.removeAttribute('aria-current'));tree.querySelectorAll('li[data-node-id]').forEach(li=>{if(Number(li.dataset.nodeId)===selected)li.querySelector(':scope > .workspace-tree-row > a')?.setAttribute('aria-current','page');});}
        function button(label,action){const item=doc.createElement('button');item.type='button';item.className='workspace-saved-search-button';item.textContent=label;item.addEventListener('click',()=>{action().catch(()=>{});});return item;}
        async function load(branch,page=0){
            branch.controller?.abort();branch.controller=new AbortController();const active=branch.controller;
            branch.list.replaceChildren(text(box.dataset.loading));branch.list.setAttribute('aria-busy','true');
            try{
                const response=await request(url(box.dataset.endpoint+'/'+branch.id,{page}),active.signal),data=await response.json();
                if(stopped||active!==branch.controller||active.signal.aborted)return;
                if(!valid(data,branch.id))throw Error('Invalid search results');
                branch.count.textContent=String(data.total);branch.list.replaceChildren();
                for(const node of data.results){const item=row(node);item.querySelector(':scope > .workspace-tree-row > a').title=node.path||node.name;branch.list.append(item);}
                if(!data.results.length)branch.list.append(text(box.dataset.empty));
                if(data.page>0||data.hasNext){const paging=doc.createElement('li');paging.className='workspace-saved-search-pages';
                    if(data.page>0)paging.append(button(box.dataset.previous,()=>load(branch,data.page-1)));
                    if(data.hasNext)paging.append(button(box.dataset.next,()=>load(branch,data.page+1)));
                    branch.list.append(paging);
                }
                markSelected(selected);
            }catch(error){if(!stopped&&!active.signal.aborted&&active===branch.controller)branch.list.replaceChildren(text(box.dataset.error));}
            finally{if(active===branch.controller)branch.list.removeAttribute('aria-busy');}
        }
        function branch(preset){
            const item=doc.createElement('li'),line=doc.createElement('div'),list=doc.createElement('ul'),count=doc.createElement('small');
            line.className='workspace-saved-search-heading';list.hidden=true;
            const value={id:preset.id,list,count,controller:null};branches.set(preset.id,value);
            const fold=button('▸',async()=>{
                list.hidden=!list.hidden;fold.textContent=list.hidden?'▸':'▾';fold.setAttribute('aria-expanded',String(!list.hidden));
                if(list.hidden)value.controller?.abort();else await load(value);
            });
            fold.setAttribute('aria-expanded','false');fold.setAttribute('aria-label',preset.name);
            const name=doc.createElement('span');name.textContent=preset.name;
            const refresh=button('↻',async()=>{list.hidden=false;fold.textContent='▾';fold.setAttribute('aria-expanded','true');await load(value);});
            refresh.title=box.dataset.refresh;refresh.setAttribute('aria-label',box.dataset.refresh+' '+preset.name);
            line.append(fold,name,count,refresh);item.append(line,list);return item;
        }
        async function reload(){
            if(stopped)return;
            const current=++generation;controller?.abort();controller=new AbortController();
            for(const value of branches.values())value.controller?.abort();branches.clear();tree.replaceChildren();status.textContent=box.dataset.loading;
            try{
                const response=await request(url(box.dataset.endpoint),controller.signal),presets=await response.json();
                if(stopped||current!==generation)return;
                if(!Array.isArray(presets)||presets.length>50||new Set(presets.map(p=>p?.id)).size!==presets.length||!presets.every(p=>uuid(p.id)&&typeof p.name==='string'))throw Error('Invalid saved searches');
                presets.forEach(preset=>tree.append(branch(preset)));status.textContent=presets.length?'':box.dataset.noPresets;
            }catch(error){if(!stopped&&current===generation&&error.name!=='AbortError')status.textContent=box.dataset.error;}
        }
        box.querySelector('[data-saved-search-reload]').addEventListener('click',()=>reload().catch(()=>{}));
        keyboard?.install({tree,select,stopped:()=>stopped,toggle:async()=>{},onError:()=>{status.textContent=box.dataset.error;}});
        return {reload,markSelected,stop(){stopped=true;generation++;controller?.abort();for(const value of branches.values())value.controller?.abort();}};
    }
    return {install,valid};
});
