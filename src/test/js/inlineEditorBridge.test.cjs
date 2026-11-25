const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/node/inlineEditorBridge.js'),'utf8');
function setup(embedded=true,accepted=true){
 const attrs={},events={};let finishes=0,saves=0,approved=0,dirty=true;
 const form={inert:false},frame={dataset:{treeEditor:'true'}};
 const host={owns:()=>accepted,finish:async()=>{finishes++;},saved:async()=>{saves++;return accepted;}};
 const window={frameElement:embedded?frame:null,parent:{location:{origin:'https://notes.test'},document:{documentElement:{getAttribute:()=> 'true'}},SweetCherryTreeEditor:host},SweetCherryEditorState:{dirty:()=>dirty,approveLeave(){approved++;dirty=false;}}};
 const doc={readyState:'complete',documentElement:{classList:{add:name=>attrs[name]=true},setAttribute:(k,v)=>attrs[k]=v},querySelector:()=>form,querySelectorAll:()=>[],addEventListener:(name,fn)=>events[name]=fn};
 vm.runInNewContext(source,{window,document:doc,location:{origin:'https://notes.test'}});
 return {window,form,attrs,events,frame,host,get finishes(){return finishes;},get saves(){return saves;},get approved(){return approved;}};
}
test('standalone pages and undesignated frames are untouched',()=>{
 for(const c of [setup(false),setup(true,false)]){assert.equal(c.window.SweetCherryInlineEditor,undefined);assert.deepEqual(c.attrs,{});}
});
test('the embedded editor exposes the existing dirty and save guards',()=>{
 const c=setup();const api=c.window.SweetCherryInlineEditor;
 assert.equal(api.ready(),true);assert.equal(api.dirty(),true);assert.equal(api.busy(),false);c.form.inert=true;assert.equal(api.busy(),true);
 api.approveLeave();assert.equal(c.approved,1);assert.equal(api.dirty(),false);assert.equal(c.attrs['data-inline-compact'],'true');
});
test('Cancel is delegated once to the tree host and keeps the standalone leave handler from double prompting',()=>{
 const c=setup();let prevented=0,stopped=0;
 c.events.click({button:0,target:{closest:()=>({})},preventDefault(){prevented++;},stopImmediatePropagation(){stopped++;}});
 assert.equal(c.finishes,1);assert.equal(prevented,1);assert.equal(stopped,1);
});
test('only receipts accepted by the current host finish editing; invalidated editors retain their model',async()=>{
 const c=setup(),api=c.window.SweetCherryInlineEditor;assert.equal(await api.saved('/nodes/7',false),true);assert.equal(c.saves,1);
 c.host.saved=async()=>false;await assert.rejects(api.saved('/nodes/7',false));api.invalidate();assert.equal(c.form.inert,true);assert.equal(api.busy(),false);await assert.rejects(api.saved('/nodes/7',false));
});
