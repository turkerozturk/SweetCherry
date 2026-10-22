const test=require('node:test'),assert=require('node:assert/strict');
const clipboard=require('../../main/resources/static/js/node/richTextImageClipboard.js');
test('image file items work even when the clipboard files list is empty',()=>{
 const file={type:'image/png',size:3};let read=0;
 assert.equal(clipboard.fromTransfer({items:[{kind:'string',type:'text/html'},{kind:'file',type:'image/png',getAsFile(){read++;return file;}}],files:[]}),file);assert.equal(read,1);
});
test('files fallback and alternate representations do not paste nonimages',()=>{
 const file={type:'image/jpeg'};assert.equal(clipboard.fromTransfer({items:[],files:[{type:'application/pdf'},file]}),file);
 assert.equal(clipboard.fromTransfer({items:[{kind:'file',type:'image/svg+xml',getAsFile(){throw Error();}}],files:[]}),null);
 assert.equal(clipboard.fromTransfer(null),null);
 assert.equal(clipboard.formats({types:['text/html'],items:[{type:'image/png'}]}),'text/html, image/png');
});
test('explicit clipboard read searches all items for a supported image and reports absence',async()=>{
 const blob=new Blob(['image'],{type:'image/png'});
 assert.equal(await clipboard.read({read:async()=>[{types:['text/plain']},{types:['image/png'],getType:async()=>blob}]}),blob);
 await assert.rejects(()=>clipboard.read(null));await assert.rejects(()=>clipboard.read({read:async()=>[{types:['text/plain']}]}));
 await assert.rejects(()=>clipboard.read({read:async()=>{throw Error('denied');}}));
});
