const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function setup(response={status:200,ok:true,json:async()=>({url:'/tree?nodeId=11&_tenantView=current'})}) {
 const handlers={},requests=[],destinations=[];
 const submit={},close={},paste={},bookmarks={setAttribute(){}},panel={},area={classList:{toggle(){}}};
 const form={dataset:{navigation:'true',loading:'Loading',invalid:'Invalid',missing:'Missing',stale:'Stale',denied:'Denied',failed:'Failed'},
   action:'https://example.test/nodes/go-to',elements:{parentId:{value:'',focus(){},dispatchEvent(){}},_tenantView:{value:'current'}},
   reset(){this.elements.parentId.value='';},reportValidity(){return true;},matches(){return true;},closest(){return dialog;},
   querySelector(s){return s==='[data-move-to-message]'?area:s==='[type="submit"]'?submit:s==='[data-move-to-paste]'?paste:s==='[data-move-to-bookmarks]'?bookmarks:s==='[data-move-to-bookmarks-panel]'?panel:close;}};
 const dialog={open:false,querySelector(){return form;},showModal(){this.open=true;},close(){this.open=false;}};
 const group={querySelector(){return dialog;}},button={disabled:false};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/nodeMoveTo.js'),'utf8'),{
   URL,URLSearchParams,window:{location:{href:'https://example.test/bookmarks',assign(url){destinations.push(url);}}},
   document:{querySelector(){return group;},addEventListener:(type,fn)=>handlers[type]=fn},
   fetch:async(url,options)=>{requests.push({url,options});return response;}});
 return {form,dialog,submit,close,area,requests,destinations,
  async open(){handlers.click({target:{closest:s=>s==='[data-node-go-open]'?button:null}});await tick();},
  send(){return handlers.submit({target:form,preventDefault(){}});}};
}
test('Go opens without writable move state and uses a tenant-bound GET',async()=>{
 const c=setup();await c.open();assert.equal(c.requests.length,0);assert.equal(c.submit.disabled,false);
 c.form.elements.parentId.value='11';await c.send();assert.equal(c.requests.length,1);
 assert.equal(c.requests[0].url.searchParams.get('nodeId'),'11');assert.equal(c.requests[0].url.searchParams.get('_tenantView'),'current');
 assert.equal(c.requests[0].options.method,undefined);assert.equal(c.dialog.open,false);
 assert.deepEqual(c.destinations,['https://example.test/tree?nodeId=11&_tenantView=current']);
 assert.equal(c.form.dataset.phase,'ready');assert.equal(c.close.disabled,false);
});
test('invalid or missing Go target stays on the current page and permits correction',async()=>{
 for(const [status,message] of [[400,'Invalid'],[404,'Missing']]) {
  const c=setup({status,ok:false});await c.open();c.form.elements.parentId.value='999';await c.send();
  assert.equal(c.area.textContent,message);assert.equal(c.dialog.open,true);assert.equal(c.submit.disabled,false);assert.deepEqual(c.destinations,[]);
 }
});
test('Go refuses stale tenants, login redirects and external destinations',async()=>{
 for(const response of [{status:409,ok:false},{status:200,ok:true,redirected:true},{status:200,ok:true,json:async()=>({url:'https://external.test/'})}]) {
  const c=setup(response);await c.open();c.form.elements.parentId.value='11';await c.send();
  assert.equal(c.dialog.open,true);assert.equal(c.close.disabled,false);assert.deepEqual(c.destinations,[]);
 }
});
test('delegated confirmations initialize once and cancelling prevents a POST',()=>{
 const handlers=[];let confirms=0,prevented=0;
 const context=vm.createContext({window:{confirm(text){assert.equal(text,'Türkçe & title [11]');confirms++;return false;}},document:{addEventListener:(type,fn)=>handlers.push(fn)}});
 const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/confirmNodeCreation.js'),'utf8');
 vm.runInContext(source,context);vm.runInContext(source,context);assert.equal(handlers.length,1);
 handlers[0]({target:{closest:()=>({dataset:{confirmMessage:'Türkçe & title [11]'}})},preventDefault(){prevented++;}});
 assert.equal(confirms,1);assert.equal(prevented,1);
});
