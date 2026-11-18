const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/richTextInternalLink.js'),'utf8');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function element(){return {value:'',children:[],listeners:{},options:[{}],style:{},dataset:{},append(...nodes){this.children.push(...nodes);},appendChild(node){this.children.push(node);this.options.push(node);},replaceChildren(){this.children=[];},remove(index){this.options.splice(index,1);},addEventListener(key,fn){this.listeners[key]=fn;},focus(){},click(){return this.listeners.click?.();}};}
function setup(label='Click here'){
 const names=['Link','Dialog','Id','Text','Anchor','Target','Matches','Status','Apply','Remove','Lookup','Query','SearchStatus','Paste','Cancel'];const nodes=Object.fromEntries(names.map(name=>[name,element()]));
 const dialog=nodes.Dialog;dialog.dataset={target:'/nodes/richtext/links/target',searchUrl:'/nodes/quick/search',icons:'/img/ctbicons/'};dialog.showModal=()=>dialog.open=true;dialog.close=()=>dialog.open=false;
 const applied=[],requests=[];let picker;
 vm.runInNewContext(source,{document:{getElementById:id=>nodes[id.replace('richInternal','')],createElement:element},window:{richEditorTenant:'current',SweetCherryNodeSearch:{create(config){picker={config,resets:0,refreshes:0,reset(){this.resets++;},refresh(){this.refreshes++;}};return picker;}},RichTextInternalLinkBridge:{open:()=>({text:label,link:''}),apply:(...args)=>applied.push(args),restore(){}},alert(){}},URLSearchParams,AbortController,navigator:{},fetch:async(url,options)=>{
  requests.push({url,options});const id=new URL(url,'https://example.test').searchParams.get('nodeId');return {ok:true,headers:{get:()=> 'application/json'},json:async()=>({nodeId:id,name:'Target name',anchors:['anchor'],iconName:'ct_code',bold:false,color:'#000000'})};
 }});
 return {nodes,picker,applied,requests};
}
test('contains picker selection resolves target/anchors while keeping the authored link label',async()=>{
 const s=setup();s.nodes.Link.click();assert.equal(s.nodes.Query.value,'Click here');assert.equal(s.picker.config.url,'/nodes/quick/search');assert.equal(s.picker.config.token(),'current');
 s.picker.config.onSelect({id:'9007199254740993'});await tick();assert.equal(s.nodes.Id.value,'9007199254740993');assert.equal(s.nodes.Text.value,'Click here');assert.equal(s.applied.length,0);
 assert.equal(s.nodes.Anchor.options[1].value,'anchor');s.nodes.Anchor.value='anchor';await s.nodes.Apply.click();
 assert.equal(s.applied[0][1],'Click here');assert.equal(s.applied[0][2],'node 9007199254740993 anchor');assert.match(s.requests[0].url,/_tenantView=current/);
});
test('empty link labels use the selected target name and cancellation never changes the editor',async()=>{
 const s=setup('');s.nodes.Link.click();s.picker.config.onSelect({id:'2'});await tick();await s.nodes.Apply.click();assert.equal(s.applied[0][1],'Target name');
 const cancelled=setup();cancelled.nodes.Link.click();cancelled.nodes.Cancel.click();assert.equal(cancelled.applied.length,0);assert.equal(cancelled.nodes.Dialog.open,false);
});
