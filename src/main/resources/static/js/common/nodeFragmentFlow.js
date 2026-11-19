(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryNodeFragmentFlow=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    const lines=new Set(['DIV','P','H1','H2','H3','H4','H5','H6','PRE']);
    const blocks=new Set(['TABLE','THEAD','TBODY','TFOOT','TR','TD','TH','UL','OL','LI','BLOCKQUOTE','HR']);
    /** Retain actual line boundaries as BR nodes, rather than forcing the first line into a block. */
    function inline(body,doc){
        function visit(parent){
            const children=[...parent.childNodes];let occupied=false,previousBlock=false;
            const hasLines=children.some(child=>lines.has(child.tagName));
            for(let index=0;index<children.length;index++){
                const child=children[index];
                if(child.nodeType===3){
                    if(hasLines&&!child.textContent.trim())child.remove();
                    else if(child.textContent){if(previousBlock)child.before(doc.createElement('br'));occupied=true;previousBlock=false;}
                    continue;
                }
                if(!child.tagName)continue;
                // Embedded codebox scroll containers remain structured objects.
                if(child.tagName==='DIV'&&/(?:^|;)\s*overflow-x\s*:\s*auto(?:;|$)/i.test(child.getAttribute('style')||'')){occupied=false;previousBlock=false;continue;}
                const isLine=lines.has(child.tagName);
                if(!isLine){
                    visit(child);
                    if(child.tagName==='BR'||blocks.has(child.tagName)){occupied=false;previousBlock=false;}
                    else if(child.textContent||child.tagName==='IMG'){if(previousBlock)child.before(doc.createElement('br'));occupied=true;previousBlock=false;}
                    continue;
                }
                const tag=child.tagName;
                const onlyBreak=child.childNodes.length===1&&child.firstChild.tagName==='BR';
                // Empty intermediate rendered paragraphs already get a separator before the next line.
                if(onlyBreak&&children.slice(index+1).some(item=>item.tagName||item.nodeType===3&&item.textContent.trim()))child.firstChild.remove();
                visit(child);
                const span=doc.createElement('span');
                for(const attr of [...child.attributes])span.setAttribute(attr.name,attr.value);
                if(tag==='PRE')span.setAttribute('class',(span.getAttribute('class')||'')+' node-fragment-pre');
                if(/^H[1-6]$/.test(tag)){span.setAttribute('class',(span.getAttribute('class')||'')+' node-fragment-'+tag.toLowerCase());span.setAttribute('role','heading');span.setAttribute('aria-level',tag.slice(1));}
                while(child.firstChild)span.append(child.firstChild);
                child.replaceWith(span);
                if(occupied)span.before(doc.createElement('br'));
                occupied=true;previousBlock=true;
            }
        }
        visit(body);return body;
    }
    return {inline};
});
