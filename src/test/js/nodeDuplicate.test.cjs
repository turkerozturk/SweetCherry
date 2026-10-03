const test=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');const path=require('node:path');
async function controls(state,confirm=true,ok=true) {
    const handlers={};let requested,question;
    const buttons=['single','subtree'].map(kind=>({dataset:{duplicate:kind},disabled:true}));
    const error={textContent:''};
    const form={dataset:{stateUrl:'/nodes/duplicate/1/state',error:'Reload',confirmSingle:'Copy?',confirmSubtree:'Copy %COUNT% entries?'},
        elements:{_tenantView:{value:'tenant'},revision:{value:''},withSubnodes:{value:'false'}},
        matches:()=>true,querySelectorAll:()=>buttons,querySelector:()=>error};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/nodeDuplicate.js'),'utf8'),{
        URL,window:{location:{href:'https://example.test/tree'},confirm(message){question=message;return confirm;}},
        document:{body:{},querySelectorAll:()=>[form],addEventListener:(name,fn)=>handlers[name]=fn},
        MutationObserver:class{observe(){}},fetch:async url=>{requested=url;return{ok,redirected:false,json:async()=>state};}});
    await new Promise(resolve=>setImmediate(resolve));
    function submit(kind) {let blocked=false;handlers.submit({target:form,submitter:buttons.find(button=>button.dataset.duplicate===kind),preventDefault(){blocked=true;}});return blocked;}
    return{form,buttons,error,submit,get requested(){return requested;},get question(){return question;}};
}
const state={revision:'snapshot',shared:false,subtreeCount:4};
test('duplication carries tenant/revision and asks for subtree confirmation before posting',async()=>{
    const c=await controls(state);assert.equal(c.requested.searchParams.get('_tenantView'),'tenant');
    assert.equal(c.form.elements.revision.value,'snapshot');assert.equal(c.submit('subtree'),false);
    assert.equal(c.question,'Copy 4 entries?');assert.equal(c.form.elements.withSubnodes.value,'true');
    assert.ok(c.buttons.every(button=>button.disabled));assert.equal(c.submit('single'),true);
});
test('cancelled copy leaves controls and payload unchanged',async()=>{
    const c=await controls(state,false);assert.equal(c.submit('single'),true);assert.equal(c.form.dataset.duplicateReady,'ready');
    assert.equal(c.form.elements.withSubnodes.value,'false');assert.ok(c.buttons.every(button=>!button.disabled));
});
test('alias or unsupported subtree cannot be copied with descendants',async()=>{
    for(const data of [{...state,shared:true},{...state,subtreeCount:0}]) {
        const c=await controls(data);assert.equal(c.buttons[0].disabled,false);assert.equal(c.buttons[1].disabled,true);
        assert.equal(c.submit('subtree'),true);
    }
});
test('failed state leaves duplicate operations disabled',async()=>{
    const c=await controls(state,true,false);assert.equal(c.error.textContent,'Reload');assert.ok(c.buttons.every(button=>button.disabled));
    assert.equal(c.submit('single'),true);
});
