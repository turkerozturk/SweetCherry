const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function ui(fetch) {
 const ids={},handlers={};let inserted,capture=0;
 for(const id of ['richWebImport','richWebDialog','richWebUrl','richWebLoad','richWebApply','richWebStatus','richWebPreview','richEditForm','richWebCancel'])ids[id]={dataset:{},value:'',addEventListener:(type,fn)=>handlers[id+':'+type]=fn,focus(){},showModal(){this.open=true;},close(){this.open=false;},querySelector:()=>({name:'_csrf',value:'csrf-token'})};
 ids.richWebDialog.dataset={endpoint:'/nodes/richtext/import/1',loading:'Loading',summary:'{images}/{tables}/{skipped}',error:'Error',errorBlocked:'Blocked',errorChanged:'Changed',errorNetwork:'Network'};
 ids.richWebUrl.value='https://example.org/';
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/richTextWebImport.js'),'utf8'),{document:{getElementById:id=>ids[id]},URL,URLSearchParams,AbortController,fetch,window:{richEditorTenant:'tenant-token',RichTextImportBridge:{capture:()=>({at:++capture}),insert:(ticket,items)=>{assert.equal(ids.richWebDialog.open,false);inserted={ticket,items};return true;}},RichTextClipboard:{fromHtml:()=>[{text:'<img onerror=bad()>',attributes:{}},{image:'data:image/png;base64,AQID'},{table:[['H']]}]}}});
 return {ids,fire:(id,type,event={})=>handlers[id+':'+type](event),inserted:()=>inserted};
}
const result={title:'Page',sourceUrl:'https://example.org/',html:'<p>Text</p>',images:{},skippedImages:2};
test('URL import sends CSRF and tenant tokens, previews text safely and inserts only on confirmation',async()=>{
 let request;const u=ui(async(url,options)=>{request={url,options};return {ok:true,json:async()=>result};});
 u.fire('richWebImport','click');await u.fire('richWebLoad','click');
 assert.equal(request.options.body.get('_csrf'),'csrf-token');assert.equal(request.options.body.get('_tenantView'),'tenant-token');assert.equal(request.options.redirect,'error');
 assert.equal(u.inserted(),undefined);assert.equal(u.ids.richWebPreview.innerHTML,undefined);assert.match(u.ids.richWebPreview.textContent,/<img/);assert.equal(u.ids.richWebApply.disabled,false);
 u.fire('richWebApply','click');assert.equal(u.inserted().ticket.at,1);assert.equal(u.ids.richWebDialog.open,false);
});
test('blocked fetch and redirect failure leave insertion disabled and never overwrite editor state',async()=>{
 for(const fetch of [async()=>({ok:false,json:async()=>({error:'blocked'})}),async()=>{throw Error('redirect');}]) {
  const u=ui(fetch);u.fire('richWebImport','click');await u.fire('richWebLoad','click');
  assert.equal(u.ids.richWebApply.disabled,true);assert.equal(u.inserted(),undefined);assert.ok(['Blocked','Network'].includes(u.ids.richWebStatus.textContent));
 }
});
test('cancelling an import discards a late fetch response',async()=>{
 let resolve;const u=ui(()=>new Promise(r=>resolve=r));u.fire('richWebImport','click');const pending=u.fire('richWebLoad','click');
 u.fire('richWebCancel','click');resolve({ok:true,json:async()=>result});await pending;
 assert.equal(u.ids.richWebApply.disabled,true);assert.equal(u.ids.richWebDialog.open,false);assert.equal(u.inserted(),undefined);
});
