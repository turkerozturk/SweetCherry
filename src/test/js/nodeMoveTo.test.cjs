const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function setup(postResponse={status:204}) {
    const handlers={},requests=[],assignments=[];
    const submit={disabled:true},close={disabled:false};
    const area={textContent:'',classList:{toggle(){}}};
    const form={dataset:{stateUrl:'/nodes/move/2/state',loading:'Loading',stateError:'State error',invalid:'Invalid',missing:'Missing',cycle:'Cycle',stale:'Stale',denied:'Denied',failed:'Failed'},
        elements:{parentId:{value:'',focus(){},readOnly:false},revision:{value:''},_tenantView:{value:'current-tenant'},_csrf:{value:'csrf-token'},returnUrl:{value:'/tree?nodeId=2&_tenantView=current-tenant'}},
        action:'https://example.test/nodes/move/2/to',reset(){this.elements.parentId.value='';this.elements.revision.value='';},reportValidity(){return true;},
        querySelector(selector){return selector==='[data-move-to-message]'?area:selector==='[type="submit"]'?submit:close;},matches(){return true;}};
    const dialog={open:false,showModal(){this.open=true;},close(){this.open=false;},querySelector(){return form;}};
    const group={querySelector(){return dialog;}};
    const opener={closest(){return group;}},closer={disabled:false,closest(){return dialog;}};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/nodeMoveTo.js'),'utf8'),{
        URL,URLSearchParams,FormData:class {constructor(f){this.entries=Object.entries(f.elements).map(([key,item])=>[key,item.value]);}[Symbol.iterator](){return this.entries[Symbol.iterator]();}},
        window:{location:{href:'https://example.test/tree',assign(url){assignments.push(url);}}},document:{addEventListener:(name,fn)=>handlers[name]=fn},
        fetch:async(url,options)=>{requests.push({url,options});return options.method==='POST'?postResponse:{ok:true,redirected:false,json:async()=>({revision:'fresh-snapshot'})};}});
    return {form,dialog,submit,close,area,requests,assignments,handlers,
        async open(){handlers.click({target:{closest(selector){return selector==='[data-move-to-open]'?opener:null;}}});await tick();},
        cancel(){handlers.click({target:{closest(selector){return selector==='[data-move-to-close]'?closer:null;}}});},
        send(){return handlers.submit({target:form,preventDefault(){}});}};
}
test('open fetches a fresh tenant-bound revision and cancel performs no write',async()=>{
    const c=setup();await c.open();assert.equal(c.dialog.open,true);assert.equal(c.submit.disabled,false);
    assert.equal(c.requests[0].url.searchParams.get('_tenantView'),'current-tenant');
    assert.equal(c.form.elements.revision.value,'fresh-snapshot');c.cancel();assert.equal(c.dialog.open,false);assert.equal(c.requests.length,1);
    await c.open();assert.equal(c.requests.length,2);
});
test('move submits parent, fresh revision, tenant and CSRF then returns to same selected occurrence',async()=>{
    const c=setup();await c.open();c.form.elements.parentId.value='9';await c.send();
    const request=c.requests[1];assert.equal(request.options.method,'POST');
    for(const [key,value] of Object.entries({parentId:'9',revision:'fresh-snapshot',_tenantView:'current-tenant',_csrf:'csrf-token'}))assert.equal(request.options.body.get(key),value);
    assert.equal(request.options.redirect,'error');assert.deepEqual(c.assignments,['/tree?nodeId=2&_tenantView=current-tenant']);
});
test('invalid or cyclic targets keep dialog open and allow a corrected parent',async()=>{
    for(const [status,message] of [[400,'Invalid'],[404,'Missing'],[422,'Cycle']]) {
        const c=setup({status});await c.open();c.form.elements.parentId.value='9';await c.send();
        assert.equal(c.area.textContent,message);assert.equal(c.dialog.open,true);assert.equal(c.submit.disabled,false);assert.equal(c.form.elements.parentId.value,'9');assert.equal(c.assignments.length,0);
    }
});
test('stale or denied writes require a fresh dialog and never repeat a POST automatically',async()=>{
    for(const status of [409,403,500]) {
        const c=setup({status});await c.open();await c.send();await c.send();
        assert.equal(c.submit.disabled,true);assert.equal(c.requests.length,2);assert.equal(c.close.disabled,false);
    }
});
test('while saving, duplicate submit and Escape cannot dismiss a potentially committed move',async()=>{
    const c=setup();await c.open();const pending=c.send();
    assert.equal(c.close.disabled,true);assert.equal(c.form.elements.parentId.readOnly,true);
    let prevented=false,stopped=false;c.handlers.keydown({key:'Escape',target:{closest(){return c.dialog;}},preventDefault(){prevented=true;},stopPropagation(){stopped=true;}});
    assert.equal(prevented,true);assert.equal(stopped,true);await c.send();await pending;assert.equal(c.requests.length,2);
});
