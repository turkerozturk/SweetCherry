(function(root){
    'use strict';
    const image=type=>/^image\/(png|jpeg)$/i.test(type||'');
    function fromTransfer(transfer) {
        for(const item of Array.from(transfer?.items||[])) {
            if(item.kind!=='file'||!image(item.type))continue;
            const file=item.getAsFile();
            if(file)return file.type?file:new Blob([file],{type:item.type});
        }
        return Array.from(transfer?.files||[]).find(file=>image(file.type))||null;
    }
    async function read(clipboard) {
        if(!clipboard?.read)throw new Error('unsupported');
        const items=await clipboard.read();
        for(const item of items)for(const type of ['image/png','image/jpeg'])if(item.types.includes(type))return item.getType(type);
        throw new Error('no-image');
    }
    async function readContent(clipboard) {
        if(!clipboard?.read)throw new Error('unsupported');
        const items=await clipboard.read();
        const result={};
        // Prefer structured representations across all clipboard items before plain text.
        for(const [type,key] of [['application/x-sweetcherry-richtext+json','own'],['text/html','html'],['text/plain','text']]) {
            for(const item of items)if(item.types.includes(type)) {
                try{result[key]=await (await item.getType(type)).text();break;}catch(_){ }
            }
        }
        for(const item of items) {
            const type=['image/png','image/jpeg'].find(type=>item.types.includes(type));
            if(type){try{result.image=await item.getType(type);break;}catch(_){ }}
        }
        if(!result.own&&!result.html&&!result.text&&!result.image)throw new Error('no-content');
        return result;
    }
    function formats(transfer){return [...new Set([...Array.from(transfer?.types||[]),...Array.from(transfer?.items||[],item=>item.type)])].filter(Boolean).join(', ');}
    const api={fromTransfer,read,readContent,formats};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextImageClipboard=api;
})(typeof window==='undefined'?globalThis:window);
