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
    /** Code-point offsets; formatting splits must not terminate a logical hyperlink. */
    function linkRange(runs, caret) {
        let offset=0;
        const segments=runs.map(run=>{const start=offset;offset+=Array.from(run.text).length;return {run,start,end:offset};});
        const linked=item=>item.end>item.start && !item.run.attributes.__sweet_object && !!item.run.attributes.link;
        let index=segments.findIndex(item=>linked(item) && item.start<=caret && caret<item.end);
        if(index<0)index=segments.findIndex(item=>linked(item) && item.end===caret);
        if(index<0)return null;
        const link=segments[index].run.attributes.link;
        let first=index,last=index;
        while(first>0 && !segments[first-1].run.attributes.__sweet_object && segments[first-1].run.attributes.link===link)first--;
        while(last+1<segments.length && !segments[last+1].run.attributes.__sweet_object && segments[last+1].run.attributes.link===link)last++;
        return [segments[first].start,segments[last].end];
    }
    function insertionAttributes(runs,start,end) {
        let offset=0,attributes={},leftLink='',rightLink='';
        for(const run of runs){
            const finish=offset+Array.from(run.text).length;
            if(offset<=start && start<=finish)attributes={...run.attributes};
            if(!run.attributes.__sweet_object){
                if(offset<start && start<=finish)leftLink=run.attributes.link||'';
                if(offset<=start && start<finish)rightLink=run.attributes.link||'';
            }
            offset=finish;
        }
        if(start===end && leftLink && leftLink!==rightLink)delete attributes.link;
        delete attributes.__sweet_object;
        return attributes;
    }
    const api={objectId,imageMime,linkRange,insertionAttributes};
    if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RichTextEditorSupport=api;
})(typeof window==='undefined'?globalThis:window);
