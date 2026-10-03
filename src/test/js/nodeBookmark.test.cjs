const test=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');const path=require('node:path');
async function controls(state,ok=true) {
    const handlers={};let requested;
    const button={disabled:true},label={textContent:'Add'},error={textContent:''};
    const form={dataset:{stateUrl:'/bookmarks/10/state',existing:'Bookmarked',error:'Reload'},elements:{_tenantView:{value:'tenant'}},
        matches:()=>true,querySelector:selector=>selector.includes('label')?label:selector.includes('error')?error:button};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/nodeBookmark.js'),'utf8'),{
        URL,window:{location:{href:'https://example.test/tree'}},MutationObserver:class{observe(){}},
        document:{body:{},querySelectorAll:()=>[form],addEventListener:(name,fn)=>handlers[name]=fn},
        fetch:async url=>{requested=url;return{ok,redirected:false,json:async()=>state};}});
    await new Promise(resolve=>setImmediate(resolve));
    function submit(){let blocked=false;handlers.submit({target:form,preventDefault(){blocked=true;}});return blocked;}
    return{form,button,label,error,submit,get requested(){return requested;}};
}
test('add uses the occurrence ID and tenant token and prevents repeated submissions',async()=>{
    const c=await controls({bookmarked:false});assert.equal(c.requested.pathname,'/bookmarks/10/state');
    assert.equal(c.requested.searchParams.get('_tenantView'),'tenant');assert.equal(c.button.disabled,false);
    assert.equal(c.submit(),false);assert.equal(c.button.disabled,true);assert.equal(c.submit(),true);
});
test('already-bookmarked occurrence disables Add without enabling another write',async()=>{
    const c=await controls({bookmarked:true});assert.equal(c.label.textContent,'Bookmarked');
    assert.equal(c.button.disabled,true);assert.equal(c.submit(),true);
});
test('failed or invalid state leaves Add disabled',async()=>{
    for(const [data,ok] of [[{},true],[{bookmarked:false},false]]) {
        const c=await controls(data,ok);assert.equal(c.button.disabled,true);assert.equal(c.error.textContent,'Reload');assert.equal(c.submit(),true);
    }
});
