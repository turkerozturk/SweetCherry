const test=require('node:test'),assert=require('node:assert/strict');
const support=require('../../main/resources/static/js/node/richTextEditorSupport.js');
test('HTTP without randomUUID uses secure random values and creates a valid version-4 ID',()=>{
 let calls=0;const crypto={getRandomValues(bytes){calls++;bytes.fill(calls);return bytes;}};
 const first=support.objectId(crypto),second=support.objectId(crypto);
 assert.match(first,/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);assert.notEqual(first,second);assert.equal(calls,2);
 assert.equal(support.objectId({randomUUID:()=> 'native'}),'native');assert.throws(()=>support.objectId({}));
});
test('cloud files with missing/generic MIME types are recognized by PNG/JPEG bytes',async()=>{
 const png=new Blob([new Uint8Array([137,80,78,71,13,10,26,10])],{type:'application/octet-stream'});
 const jpeg=new Blob([new Uint8Array([255,216,255,224])]);
 assert.equal(await support.imageMime(png),'image/png');assert.equal(await support.imageMime(jpeg),'image/jpeg');
 await assert.rejects(()=>support.imageMime(new Blob(['not an image'],{type:'application/octet-stream'})),/type/);
});
