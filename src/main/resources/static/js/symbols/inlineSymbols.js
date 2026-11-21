/* Display-only, allowlisted icon markers. The source text and database are never edited here. */
(() => {
    'use strict';
    const catalog=window.SweetCherrySymbolCatalog;if(!catalog)return;
    const definitions=new Map(),families=new Set(['solid','regular','brands']);
    for(const item of catalog.items){
        if(item.kind==='unicode')continue;
        if(item.kind==='ctb'&&/^[A-Za-z0-9_-]{1,80}$/.test(item.name))definitions.set('ctb:'+item.name.toLowerCase(),item);
        else if(families.has(item.kind)&&/^[a-z0-9-]+$/.test(item.name)){
            for(const name of [item.name,...(item.aliases||[])])if(/^[a-z0-9-]+$/.test(name)){
                definitions.set('fa-'+item.kind+' fa-'+name,item);
                definitions.set(({solid:'fas',regular:'far',brands:'fab'})[item.kind]+' fa-'+name,item);
            }
        }
    }
    function resolve(body){return definitions.get(body.trim().replace(/\s+/g,' ').replace(/^ctb:(.+)$/i,(_,name)=>'ctb:'+name.toLowerCase()))||null;}
    function matches(text){
        const result=[],regex=/\{\{([^{}\r\n]{1,100})\}\}/g;let match;
        while((match=regex.exec(text))){const item=resolve(match[1]);if(item&&text[match.index-1]!=='{'&&text[regex.lastIndex]!=='}')result.push({item,source:match[0],start:match.index,end:regex.lastIndex});}
        return result;
    }
    function tokens(text){
        const result=[];let at=0;for(const match of matches(text)){if(match.start>at)result.push(text.slice(at,match.start));result.push({item:match.item,source:match.source});at=match.end;}
        if(at<text.length)result.push(text.slice(at));return result;
    }
    const base=document.currentScript?.dataset.ctbBase||'/img/ctbicons/';
    function create(item,source=item.value){
        if(item.kind==='unicode'){const span=document.createElement('span');span.className='sc-native-symbol';span.textContent=item.value;return span;}
        const span=document.createElement('span');span.className='sc-inline-symbol';span.dataset.scSymbol=source;span.setAttribute('role','img');span.setAttribute('aria-label',item.label);span.title=source;
        if(item.kind==='ctb'){const img=document.createElement('img');img.src=base+encodeURIComponent(item.name)+'.png';img.className='sc-inline-icon';img.alt='';img.setAttribute('aria-hidden','true');span.append(img);}
        else{const icon=document.createElement('i');icon.className='fa-'+item.kind+' fa-'+item.name;icon.setAttribute('aria-hidden','true');span.append(icon);}
        return span;
    }
    const scope='[data-inline-symbols],[data-title-text],[data-node-fragments][data-fragment-syntax="custom-colors"],.rich-text-preview';
    const skip='pre,code,table,script,style,textarea,input,select,[contenteditable],[data-symbols-skip],[data-symbol-picker],.sc-inline-symbol,[style*="monospace"]';
    function adjacent(a,b){
        if(a.parentElement===b.parentElement)return a.nextSibling===b;
        const x=a.parentElement,y=b.parentElement;
        return x?.tagName==='SPAN'&&y?.tagName==='SPAN'&&x.parentElement===y.parentElement&&x.nextSibling===y
            &&x.childNodes.length===1&&y.childNodes.length===1
            &&[...x.attributes,...y.attributes].every(attr=>attr.name==='style')&&x.getAttribute('style')===y.getAttribute('style');
    }
    function render(root){
        if(root.closest?.(skip))return;
        const groups=[],walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);let node,previous=null;
        while((node=walker.nextNode())){
            if(node.parentElement?.closest(skip)){previous=null;continue;}
            if(!previous||!adjacent(previous,node))groups.push([]);groups[groups.length-1].push(node);previous=node;
        }
        for(const nodes of groups){
            let text='';const segments=[];for(const part of nodes){const start=text.length;text+=part.nodeValue;if(text.length>start)segments.push({node:part,start,end:text.length});}
            if(!text.includes('{{'))continue;
            const locate=offset=>{let low=0,high=segments.length-1;while(low<high){const mid=(low+high)>>1;if(segments[mid].end<=offset)low=mid+1;else high=mid;}return segments[low];};
            // Work backwards so earlier text-node offsets remain valid, including character-by-character editor runs.
            for(const match of matches(text).reverse()){
                const first=locate(match.start),last=locate(match.end-1),prefix=first.node.nodeValue.slice(0,match.start-first.start),suffix=last.node.nodeValue.slice(match.end-last.start),icon=create(match.item,match.source);
                if(first===last){first.node.nodeValue=prefix;first.node.after(icon,document.createTextNode(suffix));}
                else{for(const part of segments)if(part.start>=first.end&&part.end<=last.start)part.node.nodeValue='';first.node.nodeValue=prefix;last.node.nodeValue=suffix;first.node.after(icon);}
            }
        }
    }
    const pending=new Set();let scheduled=false;
    function scan(node){
        const element=node.nodeType===3?node.parentElement:node;if(!element||element.closest?.(skip))return;
        if(element.matches?.(scope)||element.closest?.(scope))pending.add(element);
        element.querySelectorAll?.(scope).forEach(root=>{if(!root.closest(skip))pending.add(root);});
        if(!scheduled&&pending.size){scheduled=true;Promise.resolve().then(()=>{scheduled=false;const roots=[...pending];pending.clear();roots.filter(root=>root.isConnected).forEach(render);});}
    }
    window.SweetCherryInlineSymbols={resolve,tokens,create,render,scan};
    if(document.body){new MutationObserver(records=>{for(const record of records)if(record.type==='characterData')scan(record.target);else record.addedNodes.forEach(scan);}).observe(document.body,{subtree:true,childList:true,characterData:true});scan(document.body);}
})();
