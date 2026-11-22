const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/node/nodeTitle.js'),'utf8');
const record={id:'1',name:'Old title',icon:'CT_CIRCLE_GREEN',revision:'a'.repeat(64)};
function setup(responses=[record,{...record,name:'New title',revision:'b'.repeat(64)}]){
 const requests=[],events=[];
 function control(value={}){return Object.assign({hidden:false,disabled:false,handlers:{},addEventListener(k,fn){this.handlers[k]=fn;},focus(){},select(){}},value);}
 const search=control({value:''}),closeIcon=control(),preview=control({hidden:true,removeAttribute(){delete this.src;}}),fallback=control();
 const choice=(value,name)=>{
  const image=value==='OTHER'?null:control({getAttribute:()=>'/app/img/ctbicons/'+name+'.png',hasAttribute(){return Boolean(this.src);}});
  const button=control({dataset:{titleIconChoice:value},title:name,querySelector:()=>image,setAttribute(name,value){this[name]=value;}});
  button.closest=()=>button;return button;
 };
 const choices=[choice('CT_CIRCLE_GREEN','Circle Green'),choice('CT_ADD','Add'),choice('OTHER','No icon')];
 const iconDialog=control({open:false,querySelector:s=>s==='[data-title-icon-search]'?search:closeIcon,querySelectorAll:()=>choices,contains:b=>choices.includes(b),showModal(){this.open=true;},close(){this.open=false;this.handlers.close?.();}});
 const iconValue=control({value:''}),iconButton=control({querySelector:s=>s==='[data-title-icon-preview]'?preview:fallback});
 const open=control(),heading=control(),input=control({value:''}),save=control(),cancel=control(),status=control({textContent:''}),text=control({textContent:'Old title'});
 const form=control({hidden:true,elements:{_tenantView:{value:'tenant'}},querySelector:s=>({'[data-title-value]':input,'[data-title-icon-open]':iconButton,'[data-title-icon-value]':iconValue,'[data-title-save]':save,'[data-title-cancel]':cancel,'[data-title-status]':status,'input[name="_csrf"]':{name:'_csrf',value:'csrf'}})[s]});
 const root={isConnected:true,dataset:{titleId:'1',titleEndpoint:'/app/nodes/properties/1/title',titleLoading:'Loading',titleSaving:'Saving',titleChanged:'Changed',titleReadonly:'Read-only',titleError:'Error'},querySelector:s=>({'[data-title-open]':open,'[data-title-form]':form,'[data-title-heading]':heading,'[data-title-text]':text})[s]};
 vm.runInNewContext(source,{WeakMap,AbortController,URL,URLSearchParams,setTimeout,clearTimeout,location:{href:'https://example.test/app/tree'},MutationObserver:class{observe(){}},CustomEvent:class{constructor(type,options){this.type=type;this.detail=options.detail;}},document:{getElementById:()=>iconDialog,body:{},querySelectorAll:()=>[root],dispatchEvent:e=>events.push(e)},fetch:async(url,options)=>{requests.push({url,options});const answer=responses.shift();if(typeof answer==='function')return answer();if(answer instanceof Error)throw answer;return answer?.status?{ok:false,status:answer.status}:{ok:true,headers:{get:()=> 'application/json'},json:async()=>answer};}});
 return{iconButton,iconValue,iconDialog,choices,search,preview,fallback,choose(index){iconButton.handlers.click();iconDialog.handlers.click({target:choices[index]});},root,form,open,heading,input,save,cancel,status,text,requests,events,async start(){await open.handlers.click();},async submit(){await form.handlers.submit({preventDefault(){}});}};
}
test('title loads current revision and saves only name with tenant and CSRF before refreshing workspace',async()=>{
 const c=setup();await c.start();assert.equal(c.input.value,'Old title');assert.equal(c.heading.hidden,true);assert.equal(c.form.hidden,false);
 assert.equal(c.requests[0].url.searchParams.get('_tenantView'),'tenant');assert.equal(c.requests[0].options.cache,'no-store');
 c.input.value='New title';await c.submit();const body=c.requests[1].options.body;
 assert.equal(body.get('name'),'New title');assert.equal(body.get('revision'),record.revision);assert.equal(body.get('_csrf'),'csrf');assert.equal(body.get('_tenantView'),'tenant');
 assert.equal(c.text.textContent,'New title');assert.equal(c.form.hidden,true);assert.equal(c.heading.hidden,false);assert.equal(c.events[0].type,'sweetcherry:node-renamed');assert.equal(c.events[0].detail.id,'1');
});
test('Escape cancels without posting and stale saves cannot silently overwrite the title',async()=>{
 const c=setup();await c.start();c.input.value='Cancelled';c.form.handlers.keydown({key:'Escape',preventDefault(){}});assert.equal(c.form.hidden,true);assert.equal(c.requests.length,1);assert.equal(c.text.textContent,'Old title');
 const stale=setup([record,{status:409}]);await stale.start();stale.input.value='Overwrite';await stale.submit();assert.equal(stale.status.textContent,'Changed');assert.equal(stale.save.disabled,true);assert.equal(stale.events.length,0);assert.equal(stale.text.textContent,'Old title');
});
test('late reads after cancel and malformed or denied responses leave stored presentation unchanged',async()=>{
 let finish;const c=setup([()=>new Promise(resolve=>finish=resolve)]);const pending=c.start();c.cancel.handlers.click();finish({ok:true,headers:{get:()=> 'application/json'},json:async()=>record});await pending;
 assert.equal(c.form.hidden,true);assert.equal(c.text.textContent,'Old title');assert.equal(c.save.disabled,true);
 for(const response of [{...record,id:'9'},{status:403},new Error('offline')]){const denied=setup([response]);await denied.start();assert.equal(denied.save.disabled,true);assert.equal(denied.text.textContent,'Old title');assert.equal(denied.events.length,0);}
});
test('a pending save cannot be submitted twice or cancelled while its result is uncertain',async()=>{
 let finish;const c=setup([record,()=>new Promise(resolve=>finish=resolve)]);await c.start();c.input.value='New title';const pending=c.submit();await c.submit();c.cancel.handlers.click();assert.equal(c.form.hidden,false);assert.equal(c.requests.length,2);
 finish({ok:true,headers:{get:()=> 'application/json'},json:async()=>({...record,name:'New title',revision:'b'.repeat(64)})});await pending;assert.equal(c.form.hidden,true);assert.equal(c.events.length,1);
});

test('icon chooser filters local choices and submits the selected node icon with name and CSRF',async()=>{
 const c=setup([record,{...record,name:'Name and icon',icon:'CT_ADD',revision:'c'.repeat(64)}]);await c.start();
 assert.equal(c.iconValue.value,'CT_CIRCLE_GREEN');assert.equal(c.preview.src,'/app/img/ctbicons/Circle Green.png');
 c.iconButton.handlers.click();assert.equal(c.iconDialog.open,true);
 c.search.value='add';c.search.handlers.input();assert.equal(c.choices[0].hidden,true);assert.equal(c.choices[1].hidden,false);
 c.iconDialog.handlers.click({target:c.choices[1]});assert.equal(c.iconValue.value,'CT_ADD');assert.equal(c.iconDialog.open,false);
 c.input.value='Name and icon';await c.submit();assert.equal(c.requests[1].options.body.get('icon'),'CT_ADD');
 assert.equal(c.requests[1].options.body.get('_csrf'),'csrf');assert.equal(c.events.length,1);
});
test('unchanged and unknown node icons are omitted while explicit no-icon selection is saved',async()=>{
 const same=setup();await same.start();await same.submit();assert.equal(same.requests[1].options.body.has('icon'),false);
 const unknown=setup([{...record,icon:null},record]);await unknown.start();await unknown.submit();assert.equal(unknown.requests[1].options.body.has('icon'),false);
 const none=setup();await none.start();none.choose(2);assert.equal(none.preview.hidden,true);assert.equal(none.fallback.hidden,false);
 await none.submit();assert.equal(none.requests[1].options.body.get('icon'),'OTHER');
});
test('cancelling name and icon changes never writes; an icon dialog cannot update a detached node',async()=>{
 const c=setup();await c.start();c.choose(1);c.cancel.handlers.click();assert.equal(c.requests.length,1);assert.equal(c.events.length,0);
 const gone=setup();await gone.start();gone.iconButton.handlers.click();gone.root.isConnected=false;
 gone.iconDialog.handlers.click({target:gone.choices[1]});assert.equal(gone.iconValue.value,'CT_CIRCLE_GREEN');
});
