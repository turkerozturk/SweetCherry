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
    function formats(transfer){return [...new Set([...Array.from(transfer?.types||[]),...Array.from(transfer?.items||[],item=>item.type)])].filter(Boolean).join(', ');}
    const api={fromTransfer,read,formats};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextImageClipboard=api;
})(typeof window==='undefined'?globalThis:window);
