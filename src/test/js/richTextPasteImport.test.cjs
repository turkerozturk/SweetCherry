const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function setup({confirm=true,remote=true,embedded=false,fetch,changed=false}={}){
 const ids={richWebDialog:{dataset:{clipboardEndpoint:'/nodes/richtext/import/7/clipboard',clipboardConfirm:'Import?',clipboardFailed:'Failed',clipboardDone:'Skipped {skipped}',loading:'Loading'}},richEditForm:{querySelector:()=>({name:'_csrf',value:'csrf'})},richPasteStatus:{},richWebImport:{}};
 let busy=false,inserted=0,calls=0;const events={};
 const window={confirm:()=>confirm,richEditorTenant:'tenant',DOMPurify:{sanitize:()=>({querySelectorAll:()=>remote?[{getAttribute:()=> 'https://example.org/image.png'}]:embedded?[{getAttribute:()=> 'data:image/gif;base64,AQID'}]:[]})},RichTextImportBridge:{capture(){if(busy)throw Error();return {selection:[2,2]};},begin(){busy=true;return ()=>busy=false;},insert(){assert.equal(busy,false);if(changed)throw Error('changed');inserted++;return true;}},RichTextClipboard:{fromHtml:()=>[{image:'data:image/png;base64,AQID'}]},addEventListener:(name,fn)=>events[name]=fn};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/richTextPasteImport.js'),'utf8'),{window,document:{getElementById:id=>ids[id]},URLSearchParams,AbortController,setTimeout,clearTimeout,fetch:async(url,options)=>{calls++;return fetch(url,options);}});
 return {handle:window.RichTextPasteImport.handle,ids,events,get busy(){return busy;},get inserted(){return inserted;},get calls(){return calls;}};
}
const response=()=>({ok:true,headers:{get:()=> 'application/json'},json:async()=>({html:'<p>Text</p>',images:{},skippedImages:2})});
test('ordinary HTML and declined downloads use the existing synchronous paste with no network request',()=>{
 for(const args of [{remote:false},{confirm:false}]){const c=setup(args);assert.equal(c.handle('<p>Text</p>'),false);assert.equal(c.calls,0);assert.equal(c.busy,false);}
});
test('approved image paste sends CSRF and tenant, locks saves while pending, then inserts one model change',async()=>{
 let request;const c=setup({fetch:async(url,options)=>{request={url,options};return response();}});
 assert.equal(c.handle('<img>'),true);assert.equal(c.busy,true);
 await new Promise(r=>setImmediate(r));assert.equal(c.inserted,1);assert.equal(c.busy,false);
 assert.equal(request.url,'/nodes/richtext/import/7/clipboard');assert.equal(request.options.body.get('_csrf'),'csrf');assert.equal(request.options.body.get('_tenantView'),'tenant');assert.equal(request.options.redirect,'error');assert.equal(c.ids.richPasteStatus.textContent,'Skipped 2');
});
test('failed image import and a changed editor never replace the selection',async()=>{
 for(const args of [{fetch:async()=>{throw Error();}},{changed:true,fetch:async()=>response()}]){
  const c=setup(args);c.handle('<img>');await new Promise(r=>setImmediate(r));assert.equal(c.inserted,0);assert.equal(c.busy,false);assert.equal(c.ids.richPasteStatus.textContent,'Failed');
 }
});
test('leaving the editor cancels a late response instead of inserting in a detached frame',async()=>{
 let resolve;const c=setup({fetch:()=>new Promise(r=>resolve=r)});c.handle('<img>');c.events.pagehide();resolve(response());await new Promise(r=>setImmediate(r));assert.equal(c.inserted,0);assert.equal(c.busy,false);
});

test('embedded GIF raster data is normalized locally without requesting external-image consent',async()=>{
 const c=setup({remote:false,embedded:true,confirm:false,fetch:async()=>response()});assert.equal(c.handle('<img>'),true);await new Promise(r=>setImmediate(r));assert.equal(c.inserted,1);
});
