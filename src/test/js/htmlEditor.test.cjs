const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function editor(fetcher){
 const handlers={},elements={},changes=[],requests=[],timers=new Map();let next=0;
 for(const id of ['htmlTools','plainEditForm','nodeText','htmlPreviewPane','htmlPreview','htmlPreviewStatus','htmlAutoPreview','htmlPreviewToggle','htmlRefresh','markdownShell'])elements[id]={value:'<p>hello</p>',hidden:id==='htmlPreviewPane',checked:true,dataset:{previewUrl:'/nodes/html/1/preview',loading:'loading',failed:'failed'},selectionStart:3,selectionEnd:8,addEventListener(type,handler){handlers[id+':'+type]=handler;},setAttribute(){},classList:{toggle(){}}};
 const button={dataset:{htmlBefore:'<strong>',htmlAfter:'</strong>'},addEventListener(type,handler){handlers['bold:'+type]=handler;}};elements.htmlTools.querySelectorAll=()=>[button];elements.plainEditForm.querySelector=selector=>({value:selector.includes('_csrf')?'csrf':'token'});elements.htmlPreviewToggle.click=()=>handlers['htmlPreviewToggle:click']();
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/html/htmlEditor.js'),'utf8'),{document:{getElementById:id=>elements[id]},window:{PlainTextEditorBridge:{replace:(start,end,text)=>changes.push({start,end,text})}},AbortController,URLSearchParams,setTimeout:(callback,delay)=>{const id=++next;timers.set(id,{callback,delay});return id;},clearTimeout:id=>timers.delete(id),fetch:async(url,options)=>{requests.push({url,options});return fetcher(url,options);}});
 return {elements,handlers,changes,requests,debounce(){for(const [id,t] of timers)if(t.delay===600){timers.delete(id);t.callback();}},flush:()=>new Promise(r=>setImmediate(r))};
}
const response=document=>({ok:true,headers:{get:()=> 'application/json'},json:async()=>({document})});
test('HTML tools use history bridge; preview goes only to srcdoc without rewriting source',async()=>{
 const e=editor(async()=>response('<p>safe</p>'));await e.flush();assert.equal(e.elements.htmlPreview.srcdoc,'<p>safe</p>');assert.equal(e.elements.htmlPreview.innerHTML,undefined);assert.equal(e.elements.nodeText.value,'<p>hello</p>');e.handlers['bold:click']();assert.equal(e.changes[0].text,'<strong>hello</strong>');assert.equal(e.requests[0].options.body.get('_csrf'),'csrf');assert.equal(e.requests[0].options.body.get('_tenantView'),'token');
});
test('stale previews are rejected even after undo restores identical text',async()=>{
 let finish;const e=editor(()=>new Promise(resolve=>finish=resolve));e.handlers['nodeText:input']();finish(response('stale'));await e.flush();assert.equal(e.elements.htmlPreview.srcdoc,undefined);
});
test('manual preview works with auto off and composition defers automatic requests',async()=>{
 const e=editor(async()=>response('current'));await e.flush();e.elements.htmlAutoPreview.checked=false;e.handlers['nodeText:input']();e.debounce();assert.equal(e.requests.length,1);await e.handlers['htmlRefresh:click']();assert.equal(e.requests.length,2);
 e.elements.htmlAutoPreview.checked=true;e.handlers['nodeText:compositionstart']();e.handlers['nodeText:input']();e.debounce();assert.equal(e.requests.length,2);e.handlers['nodeText:compositionend']();e.debounce();await e.flush();assert.equal(e.requests.length,3);
});
test('hiding cancels pending previews and login redirects or offline errors preserve source',async()=>{
 let finish;const e=editor(()=>new Promise(resolve=>finish=resolve));e.handlers['htmlPreviewToggle:click']();assert.equal(e.requests[0].options.signal.aborted,true);finish(response('hidden'));await e.flush();assert.equal(e.elements.htmlPreview.srcdoc,undefined);
 for(const fetcher of [async()=>{throw Error('offline');},async()=>({...response('login'),redirected:true})]){const failed=editor(fetcher);await failed.flush();assert.equal(failed.elements.htmlPreviewStatus.textContent,'failed');assert.equal(failed.elements.nodeText.value,'<p>hello</p>');}
});
