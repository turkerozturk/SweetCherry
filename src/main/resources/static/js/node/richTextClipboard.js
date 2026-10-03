(function(root){
    'use strict';
    const allowed={weight:['heavy'],style:['italic'],underline:['single'],strikethrough:['true'],family:['monospace'],
        scale:['h1','h2','h3','h4','h5','h6','small','sub','sup'],justification:['left','right','center','fill']};
    function external(value){
        try{if(/[\u0000-\u001f\u007f]/.test(value))return null;const url=new URL(value);
            return ['http:','https:'].includes(url.protocol)&&url.hostname&&!url.username&&!url.password&&url.href.length<=4091?'webs '+url.href:null;
        }catch(_){return null;}
    }
    /** Keeps only supported CTB text attributes; clipboard content never supplies protected object IDs. */
    function attributes(input){
        const result={};
        for(const [key,values]of Object.entries(allowed))if(values.includes(input[key]))result[key]=input[key];
        for(const key of ['foreground','background'])if(/^#[0-9a-f]{6}(?:[0-9a-f]{6})?$/i.test(input[key]||''))result[key]=input[key];
        if(typeof input.link==='string'&&input.link.startsWith('webs ')){const link=external(input.link.slice(5));if(link)result.link=link;}
        return result;
    }
    function color(value){
        const names={black:'#000000',white:'#ffffff',red:'#ff0000',green:'#008000',blue:'#0000ff',yellow:'#ffff00',gray:'#808080',grey:'#808080',orange:'#ffa500',purple:'#800080'};
        if(names[(value||'').toLowerCase()])return names[value.toLowerCase()];
        if(/^#[0-9a-f]{6}$/i.test(value))return value;
        if(/^#[0-9a-f]{3}$/i.test(value))return '#'+Array.from(value.slice(1)).map(c=>c+c).join('');
        const rgb=/^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i.exec(value||'');
        return rgb&&rgb.slice(1).every(n=>Number(n)<=255)?'#'+rgb.slice(1).map(n=>Number(n).toString(16).padStart(2,'0')).join(''):null;
    }
    function styles(node,inherited){
        const a={...inherited},tag=node.tagName;
        if(['B','STRONG'].includes(tag))a.weight='heavy';if(['I','EM'].includes(tag))a.style='italic';
        if(tag==='U')a.underline='single';if(['S','STRIKE','DEL'].includes(tag))a.strikethrough='true';
        if(['CODE','PRE','TT'].includes(tag))a.family='monospace';
        if(/^H[1-6]$/.test(tag))a.scale=tag.toLowerCase();if(['SMALL','SUB','SUP'].includes(tag))a.scale=tag.toLowerCase();
        const css={};for(const entry of (node.getAttribute('style')||'').split(';')){const at=entry.indexOf(':');if(at>=0)css[entry.slice(0,at).trim().toLowerCase()]=entry.slice(at+1).trim();}
        if(css['font-weight']==='bold'||Number(css['font-weight'])>=600)a.weight='heavy';else if(css['font-weight']==='normal')delete a.weight;
        if(css['font-style']==='italic')a.style='italic';else if(css['font-style']==='normal')delete a.style;
        const decoration=css['text-decoration']||css['text-decoration-line']||'';
        if(decoration.includes('underline'))a.underline='single';if(decoration.includes('line-through'))a.strikethrough='true';
        if(decoration==='none'){delete a.underline;delete a.strikethrough;}
        if((css['font-family']||'').toLowerCase().includes('monospace'))a.family='monospace';
        for(const [from,to]of [['color','foreground'],['background-color','background']]){const value=color(css[from]||'');if(value)a[to]=value;}
        if(css['vertical-align']==='super')a.scale='sup';if(css['vertical-align']==='sub')a.scale='sub';
        if(tag==='FONT'){const value=color(node.getAttribute('color')||'');if(value)a.foreground=value;}
        const align=css['text-align']||node.getAttribute('align');if(['left','right','center','justify'].includes(align))a.justification=align==='justify'?'fill':align;
        if(tag==='A'){const link=external(node.getAttribute('href')||'');if(link)a.link=link;else delete a.link;}
        return a;
    }
    /** Walks inert parsed DOM into text runs and plain tables; scripts/CSS/remote resources never enter the editor DOM. */
    function fromDom(fragment){
        const items=[];let visits=0,length=0,tables=0;
        const add=(text,a,separator=false)=>{text=text.replace(/\uFFFC/g,'');length+=text.length;if(length>200000)throw new Error('Clipboard too large');if(text)items.push({text,attributes:{...a},separator});};
        const tail=()=>items.length?items[items.length-1].text||'\uFFFC':'';
        const newline=(a,force=false)=>{if(force||items.length&&!tail().endsWith('\n'))add('\n',a,true);};
        function cellText(node){if(node.nodeType===3)return node.nodeValue;if(node.nodeType!==1)return '';if(['SCRIPT','STYLE','IFRAME','OBJECT','NOSCRIPT'].includes(node.tagName))return '';if(node.tagName==='BR')return '\n';return Array.from(node.childNodes).map(cellText).join('');}
        function tableRows(node){
            const rows=[];
            for(const child of node.children||[]){if(child.tagName==='TR')rows.push(child);else if(['THEAD','TBODY','TFOOT'].includes(child.tagName))rows.push(...Array.from(child.children).filter(row=>row.tagName==='TR'));}
            const matrix=rows.map(row=>Array.from(row.children).filter(cell=>['TH','TD'].includes(cell.tagName)).map(cell=>cellText(cell)));
            const merged=rows.some(row=>Array.from(row.children).some(cell=>Number(cell.getAttribute('colspan')||1)!==1||Number(cell.getAttribute('rowspan')||1)!==1));
            const nested=Array.from(node.querySelectorAll('table')).length>0;
            return {matrix,valid:!merged&&!nested&&matrix.length>0&&matrix.length<=100&&matrix[0].length>0&&matrix[0].length<=20&&matrix.every(row=>row.length===matrix[0].length&&row.every(cell=>cell.length<=5000))};
        }
        function visit(node,a={},pre=false,depth=0){
            if(++visits>10000||depth>64)throw new Error('Clipboard too complex');
            if(node.nodeType===3){let text=node.nodeValue;if(!pre){text=text.replace(/[\t\r\n ]+/g,' ');if(!items.length||tail().endsWith('\n'))text=text.replace(/^ +/,'');}add(text,a);return;}
            if(node.nodeType!==1){for(const child of node.childNodes||[])visit(child,a,pre,depth+1);return;}
            const tag=node.tagName;if(['SCRIPT','STYLE','HEAD','IFRAME','OBJECT','EMBED','NOSCRIPT','SVG','MATH','TEMPLATE'].includes(tag))return;
            const next=styles(node,a);
            if(tag==='BR'){add('\n',next);return;}
            if(tag==='IMG'){
                const link=external(node.getAttribute('src')||'');const alt=node.getAttribute('alt')||'';
                if(link)add(alt||link.slice(5),{...next,link});else if(alt)add(alt,next);
                return;
            }
            if(tag==='TABLE'){
                newline(next);const {matrix,valid}=tableRows(node);
                length+=matrix.reduce((n,row)=>n+row.reduce((n,cell)=>n+cell.length,0),0);if(length>200000)throw new Error('Clipboard too large');
                if(valid){if(++tables>10)throw new Error('Too many tables');items.push({table:matrix});}
                else for(const row of matrix){add(row.join('\t'),next);add('\n',next,true);}
                newline(next);return;
            }
            const block=['P','DIV','SECTION','ARTICLE','BLOCKQUOTE','PRE','LI','UL','OL','H1','H2','H3','H4','H5','H6'].includes(tag);
            if(block)newline(next);
            if(tag==='LI')add('• ',next);
            const before=items.length;
            for(const child of node.childNodes)visit(child,next,pre||tag==='PRE'||/white-space\s*:\s*(pre|pre-wrap|break-spaces)/i.test(node.getAttribute('style')||''),depth+1);
            if(block)newline(next,items.length===before);
        }
        visit(fragment);
        if(items.length&&items[items.length-1].separator){const run=items[items.length-1];run.text=run.text.slice(0,-1);if(!run.text)items.pop();}
        return items.map(item=>item.table?item:{text:item.text,attributes:item.attributes});
    }
    /** Parses clipboard HTML in a template; the inert source tree is never attached to the document. */
    function fromHtml(html,doc){if(html.length>2000000)throw new Error('Clipboard HTML too large');const template=doc.createElement('template');template.innerHTML=html;return fromDom(template.content);}
    function fromInternal(json){
        if(json.length>2000000)throw new Error('Clipboard too large');const rows=JSON.parse(json);
        if(!Array.isArray(rows)||rows.length>10000)throw new Error('Invalid clipboard');let length=0;
        return rows.map(run=>{if(!run||typeof run.text!=='string'||!run.attributes||typeof run.attributes!=='object'||Array.isArray(run.attributes))throw new Error('Invalid clipboard');length+=run.text.length;if(length>200000)throw new Error('Clipboard too large');return {text:run.text.replace(/\uFFFC/g,''),attributes:attributes(run.attributes)};}).filter(run=>run.text);
    }
    const escape=text=>text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    /** Supplies interoperable safe HTML plus private CTB text metadata for SweetCherry-to-SweetCherry copy. */
    function toHtml(runs){return runs.map(run=>{
        const a=attributes(run.attributes),css=['white-space:pre-wrap'];
        if(a.weight)css.push('font-weight:bold');if(a.style)css.push('font-style:italic');if(a.family)css.push('font-family:monospace');
        const decoration=[a.underline?'underline':'',a.strikethrough?'line-through':''].filter(Boolean).join(' ');if(decoration)css.push('text-decoration:'+decoration);
        const cssColor=value=>value.length===13?'#'+value.slice(1,3)+value.slice(5,7)+value.slice(9,11):value;
        if(a.foreground)css.push('color:'+cssColor(a.foreground));if(a.background)css.push('background-color:'+cssColor(a.background));
        if(a.justification)css.push('text-align:'+(a.justification==='fill'?'justify':a.justification));
        let content='<span style="'+css.join(';')+'">'+escape(run.text).replace(/\n/g,'<br>')+'</span>';
        if(a.scale&&/^h[1-6]$/.test(a.scale))content='<'+a.scale+'>'+content+'</'+a.scale+'>';
        else if(['sub','sup','small'].includes(a.scale))content='<'+a.scale+'>'+content+'</'+a.scale+'>';
        return a.link?'<a href="'+escape(a.link.slice(5))+'">'+content+'</a>':content;
    }).join('');}
    const api={fromDom,fromHtml,fromInternal,toHtml,attributes};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextClipboard=api;
})(typeof window==='undefined'?globalThis:window);
