(function(root){
    'use strict';
    function objectId(cryptoApi=root.crypto) {
        if(typeof cryptoApi?.randomUUID==='function')return cryptoApi.randomUUID();
        if(typeof cryptoApi?.getRandomValues!=='function')throw new Error('unsupported');
        const bytes=new Uint8Array(16);cryptoApi.getRandomValues(bytes);
        bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
        const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');
        return [hex.slice(0,8),hex.slice(8,12),hex.slice(12,16),hex.slice(16,20),hex.slice(20)].join('-');
    }
    async function imageMime(file) {
        const declared=(file.type||'').toLowerCase();
        if(['image/png','image/jpeg'].includes(declared))return declared;
        if(declared==='image/jpg')return 'image/jpeg';
        if(declared==='image/x-png')return 'image/png';
        // Android/cloud providers may supply no MIME type or application/octet-stream.
        // Inspect bytes instead of trusting a filename; the server validates and normalizes again.
        const blob=file.slice(0,12);
        const buffer=blob.arrayBuffer ? await blob.arrayBuffer() : await new Promise((resolve,reject)=>{
            const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reader.onabort=()=>reject(new Error('read'));reader.readAsArrayBuffer(blob);
        });
        const bytes=new Uint8Array(buffer);
        if(bytes.length>=8 && [137,80,78,71,13,10,26,10].every((b,i)=>bytes[i]===b))return 'image/png';
        if(bytes.length>=3 && bytes[0]===255 && bytes[1]===216 && bytes[2]===255)return 'image/jpeg';
        throw new Error('type');
    }
    const api={objectId,imageMime};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextEditorSupport=api;
})(typeof window==='undefined'?globalThis:window);
