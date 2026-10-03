const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
async function controls(response,desktop=true) {
    const handlers={};let request;
    const buttons=['UP','DOWN','LEFT','RIGHT'].map(direction=>({dataset:{move:direction},disabled:true}));
    const error={textContent:''};
    const form={dataset:{stateUrl:'/nodes/move/2/state',error:'Reload'},elements:{_tenantView:{value:'tenant-token'},revision:{value:''},direction:{value:''}},
        querySelectorAll:()=>buttons,querySelector:selector=>selector==='[data-move-error]'?error:buttons.find(button=>selector.includes('"'+button.dataset.move+'"')),
        matches:()=>true,requestSubmit(button){handlers.submit({target:form,submitter:button,preventDefault(){}});request=button.dataset.move;}};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/nodeMove.js'),'utf8'),{
        URL,window:{location:{href:'https://example.test/tree'}},MutationObserver:class{observe(){}},
        fetch:async url=>{request=url;return response;},document:{body:{},querySelectorAll:()=>[form],querySelector:()=>form,
            getElementById:()=>desktop?{}:null,addEventListener:(name,fn)=>handlers[name]=fn}});
    await new Promise(resolve=>setImmediate(resolve));
    return {form,buttons,error,handlers,get request(){return request;}};
}
const response=()=>({ok:true,redirected:false,json:async()=>({revision:'snapshot',up:true,down:false,left:false,right:true})});
test('controls use tenant token and enable only valid directions',async()=>{
    const c=await controls(response());assert.equal(c.request.searchParams.get('_tenantView'),'tenant-token');
    assert.equal(c.form.elements.revision.value,'snapshot');assert.deepEqual(c.buttons.map(button=>button.disabled),[false,true,true,false]);
});
test('failed state request leaves all moves disabled',async()=>{
    const c=await controls({ok:false});assert.equal(c.error.textContent,'Reload');assert.ok(c.buttons.every(button=>button.disabled));
});
test('submission preserves direction and freezes repeated actions',async()=>{
    const c=await controls(response());c.form.requestSubmit(c.buttons[0]);assert.equal(c.form.elements.direction.value,'UP');
    assert.ok(c.buttons.every(button=>button.disabled));let blocked=false;
    c.handlers.submit({target:c.form,submitter:c.buttons[0],preventDefault(){blocked=true;}});assert.equal(blocked,true);
});
test('desktop shortcuts move selected node but leave text inputs and mobile alone',async()=>{
    const c=await controls(response());
    const key=(editing)=>({key:'ArrowRight',altKey:true,shiftKey:true,target:{closest:()=>editing?{}:null},preventDefault(){}});
    c.handlers.keydown(key(true));assert.equal(c.form.elements.direction.value,'');
    c.handlers.keydown(key(false));assert.equal(c.form.elements.direction.value,'RIGHT');
    const mobile=await controls(response(),false);mobile.handlers.keydown(key(false));assert.equal(mobile.form.elements.direction.value,'');
});
