const test=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const script=fs.readFileSync('src/main/resources/static/js/node/sortChildren.js','utf8');
function setup({id=12,confirm=true,count=3,postStatus=204,right=false,reader=false}={}){
    const calls=[],alerts=[],confirmations=[],refreshes=[];let click;
    const button={dataset:{sortKey:'NAME',sortOrder:'ASC'},textContent:'Sort by Name Ascending ↑',disabled:false};
    const menu={dataset:{base:'/app/nodes/children/',token:'current',confirm:'Sort {1} children of {0}? {2}',error:'error',conflict:'conflict',select:'select',empty:'empty'},
        addEventListener(name,handler){if(name==='click')click=handler;},querySelectorAll:()=>[button],querySelector:()=>({name:'_csrf',value:'csrf-token'})};
    const pane={SweetCherryWorkspace:{selected:()=>id,select:async node=>refreshes.push(node)},location:{reload:()=>refreshes.push('reload')}};
    pane.document={querySelector:selector=>selector==='[data-node-move]'?{dataset:{sortNode:String(id)}}:null};
    if(reader)delete pane.SweetCherryWorkspace;
    const document={querySelector:selector=>selector==='[data-sort-children]'?menu:pane.document.querySelector(selector),getElementById:selector=>selector==='workspace-readers'?{dataset:{splitOpen:String(right),splitActive:'right'}}:selector==='workspace-right-frame'?{contentWindow:pane}:null};
    const window={document,SweetCherryWorkspace:right?{selected:()=>77,select:()=>assert.fail('Wrong pane refreshed')}:pane.SweetCherryWorkspace,location:pane.location,alert:value=>alerts.push(value),confirm:value=>{confirmations.push(value);return confirm;}};
    const fetch=async (url,options)=>{calls.push({url:new URL(url),options});const isPost=options.method==='POST',status=isPost?postStatus:200;return {ok:status>=200&&status<300,status,headers:{get:()=> 'application/json'},json:async()=>({revision:'a'.repeat(64),count,name:'Example'})};};
    vm.runInNewContext(script,{window,document,location:{href:'https://example.test/app/tree'},fetch,URL,URLSearchParams,AbortController,setTimeout,clearTimeout});
    return {calls,alerts,confirmations,refreshes,button,click:()=>click({target:{closest:()=>button},preventDefault(){}})};
}
test('cancelled confirmation never submits a sort',async()=>{
    const fixture=setup({confirm:false});await fixture.click();assert.equal(fixture.calls.length,1);assert.equal(fixture.confirmations.length,1);assert.equal(fixture.button.disabled,false);
});
test('confirmation submits only selected branch and includes revision tenant and CSRF',async()=>{
    const fixture=setup();await fixture.click();assert.equal(fixture.calls.length,2);const {url,options}=fixture.calls[1];assert.equal(url.pathname,'/app/nodes/children/12/sort');assert.equal(url.searchParams.get('_tenantView'),'current');
    assert.equal(options.method,'POST');for(const [key,value] of Object.entries({_tenantView:'current',_csrf:'csrf-token',revision:'a'.repeat(64),key:'NAME',order:'ASC',confirmed:'true'}))assert.equal(options.body.get(key),value);
    assert.equal(options.redirect,'error');assert.deepEqual(fixture.refreshes,['12']);
});
test('active right pane supplies the target and receives the refresh',async()=>{
    const fixture=setup({right:true,id:24});await fixture.click();assert.equal(fixture.calls[1].url.pathname,'/app/nodes/children/24/sort');assert.deepEqual(fixture.refreshes,['24']);
});
test('standalone reader uses its requested occurrence and reloads after sorting',async()=>{
    const fixture=setup({reader:true,id:31});await fixture.click();assert.equal(fixture.calls[1].url.pathname,'/app/nodes/children/31/sort');assert.deepEqual(fixture.refreshes,['reload']);
});
test('missing selection and branches with fewer than two children never submit',async()=>{
    const missing=setup({id:0});await missing.click();assert.equal(missing.calls.length,0);assert.deepEqual(missing.alerts,['select']);
    const empty=setup({count:1});await empty.click();assert.equal(empty.calls.length,1);assert.equal(empty.confirmations.length,0);assert.deepEqual(empty.alerts,['empty']);
});
test('stale tree responses leave the page intact and enable retry',async()=>{
    const fixture=setup({postStatus:409});await fixture.click();assert.deepEqual(fixture.alerts,['conflict']);assert.deepEqual(fixture.refreshes,[]);assert.equal(fixture.button.disabled,false);
});
