const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/editorRecovery.js'),'utf8');
function setup(response) {
 const elements={},events=[],calls=[],navigations=[];
 for(const id of ['editorRecovery','recoveryStatus','recoveryEnabled','recoveryInterval','recoveryDrafts','recoveryRestore','recoveryDiscard','recoveryDownload','recoveryLogin','recoveryRescue'])elements[id]={dataset:{scope:'opaque-source'},value:'',checked:true,handlers:{},addEventListener(type,handler){this.handlers[type]=handler;}};
 const form={action:'http://localhost/app/nodes/content/1',handlers:{},addEventListener(type,handler){this.handlers[type]=handler;},dispatchEvent(event){events.push(event.type);}};
 const location={href:'http://localhost/app/nodes/content/1',pathname:'/app/nodes/content/1',origin:'http://localhost',assign:url=>navigations.push(url)};
 const window={addEventListener(){},confirm:()=>true};
 vm.runInNewContext(source,{window,document:{getElementById:id=>elements[id],addEventListener(){}},location,crypto:require('node:crypto').webcrypto,indexedDB:{open(){throw new Error('storage denied');}},localStorage:{getItem:()=>null},URL,URLSearchParams,FormData:class {constructor(){return [['text','unsaved'],['_csrf','token'],['_tenantView','old']];}},AbortController,Event:class {constructor(type){this.type=type;}},setTimeout,clearTimeout,setInterval:()=>0,clearInterval(){},fetch:async(url,options)=>{calls.push({url,options});if(response instanceof Error)throw response;return response;}});
 let text='unsaved';window.EditorRecovery.install(form,{base:'initial',snapshot:()=>({text}),dirty:()=>true,restore(){}});
 return {elements,form,events,calls,navigations,submit:async(submitter)=>{const event={defaultPrevented:false,submitter,preventDefault(){this.defaultPrevented=true;}};await form.handlers.submit(event);assert.equal(event.defaultPrevented,true);}};
}
test('unavailable browser storage does not remove failed-save protection; offline and redirected login preserve editor',async()=>{
 for(const response of [new Error('offline'),{ok:false,headers:{get:()=> 'application/json'}},{ok:true,headers:{get:()=> 'text/html'}}]) {
  const e=setup(response);await new Promise(r=>setImmediate(r));assert.equal(e.elements.recoveryEnabled.disabled,true);
  await e.submit();assert.equal(e.navigations.length,0);assert.equal(e.events.length,0);assert.equal(e.elements.recoveryStatus.textContent,'saveFailed');assert.equal(e.form.inert,false);
  assert.equal(e.calls[0].options.body.get('_csrf'),'token');assert.equal(e.calls[0].options.body.get('_tenantView'),'old');
 }
});
test('only a matching explicit successful receipt permits navigation; save-and-continue submits its own button',async()=>{
 for(const redirect of ['http://example.com/app/nodes/1','/app/nodes/99','/login','/app/nodes/1?wrong=1']) {
  const e=setup({ok:true,headers:{get:()=> 'application/json'},json:async()=>({saved:true,redirect})});await e.submit();assert.equal(e.navigations.length,0);
 }
 const e=setup({ok:true,headers:{get:()=> 'application/json'},json:async()=>({saved:true,redirect:'/app/nodes/content/1'})});
 await e.submit({name:'continueEditing',value:'true'});assert.equal(e.calls[0].options.body.get('continueEditing'),'true');assert.deepEqual(e.events,['editor:saved']);assert.deepEqual(e.navigations,['http://localhost/app/nodes/content/1']);
});
