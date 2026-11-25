(function(root){
    'use strict';
    const image=type=>/^image\/(png|jpe?g|gif|webp|bmp|x-bmp|x-ms-bmp|tiff|x-tiff|vnd\.wap\.wbmp)$/i.test(type||'');
    function fromTransferAll(transfer) {
        const found=[];
        for(const item of Array.from(transfer?.items||[])) {
            if(item.kind!=='file'||!image(item.type))continue;
            const file=item.getAsFile();
            if(file)found.push(file.type?file:new Blob([file],{type:item.type}));
        }
        return found.length?found:Array.from(transfer?.files||[]).filter(file=>image(file.type));
    }
    const fromTransfer=transfer=>fromTransferAll(transfer)[0]||null;
    async function read(clipboard) {
        if(!clipboard?.read)throw new Error('unsupported');
        const items=await clipboard.read();
        for(const item of items)for(const type of ['image/png','image/jpeg','image/jpg','image/webp','image/gif','image/bmp','image/tiff','image/vnd.wap.wbmp'])if(item.types.includes(type))return item.getType(type);
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
            const type=['image/png','image/jpeg','image/jpg','image/webp','image/gif','image/bmp','image/tiff','image/vnd.wap.wbmp'].find(type=>item.types.includes(type));
            if(type){try{(result.images??=[]).push(await item.getType(type));}catch(_){ }}
        }
        result.image=result.images?.[0];
        if(!result.own&&!result.html&&!result.text&&!result.image)throw new Error('no-content');
        return result;
    }
    function formats(transfer){return [...new Set([...Array.from(transfer?.types||[]),...Array.from(transfer?.items||[],item=>item.type)])].filter(Boolean).join(', ');}
    const api={fromTransfer,fromTransferAll,read,readContent,formats};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextImageClipboard=api;
})(typeof window==='undefined'?globalThis:window);
