const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function calendar(fetcher,editable=true) {
 const elements={},calls=[],reloads=[];
 for(const id of ['nodeCalendar','calendarDialog','calendarCreatedForm','calendarStatus','calendarContent','calendarClose','calendarDialogTitle','calendarEntryId','calendarDates','calendarShared','calendarLocked','calendarOpenNode','calendarOpenTree','calendarShowContent','calendarCreated'])elements[id]={dataset:{editable:String(editable)},handlers:{},hidden:true,value:'',textContent:'',addEventListener(type,handler){this.handlers[type]=handler;},replaceChildren(){this.innerHTML='';},querySelectorAll(){return[];},showModal(){this.open=true;},close(){this.open=false;}};
 const form=elements.calendarCreatedForm;form.elements={expectedCreated:{},expectedModified:{},local:elements.calendarCreated};
 const config={base:'/app/nodes/calendar/',node:'/app/nodes/',tree:'/app/tree',zone:'UTC',token:'v',messages:Object.fromEntries(['loading','failed','saved','saving','conflict','created','modified'].map(key=>[key,key]))};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/calendar/calendar.js'),'utf8'),{window:{calendarConfig:config},document:{getElementById:id=>elements[id],documentElement:{lang:'en'}},location:{href:'http://localhost/app/nodes/calendar/month',reload:()=>reloads.push(true)},URL,URLSearchParams,FormData:class {constructor(){return [['local',form.elements.local.value],['_csrf','csrf'],['_tenantView','v'],['expectedCreated',form.elements.expectedCreated.value],['expectedModified',form.elements.expectedModified.value]];}},AbortController,setTimeout,clearTimeout,fetch:async(url,options)=>{calls.push({url:String(url),options});return fetcher(url,options);}});
 function open(extra={}) {elements.nodeCalendar.handlers.click({target:{closest:()=>({dataset:{id:'9223372036854775807',contentId:'1',name:'Long <title>',local:'2026-10-28T14:15',created:'1793193300',modified:'123',shared:'true',readOnly:'false',...extra}})}});}
 return {elements,calls,reloads,open,content:()=>elements.calendarShowContent.handlers.click(),save:()=>form.handlers.submit({preventDefault(){}}),close:()=>elements.calendarClose.handlers.click()};
}
test('dialog retains full names and string IDs, and chooses new-tab occurrence URLs',()=>{
 const c=calendar(()=>{});c.open();assert.equal(c.elements.calendarDialogTitle.textContent,'Long <title>');assert.equal(c.elements.calendarShared.hidden,false);assert(c.elements.calendarOpenTree.href.searchParams.get('nodeId')==='9223372036854775807');assert(c.elements.calendarCreatedForm.action.pathname.includes('9223372036854775807'));c.close();assert.equal(c.elements.calendarDialog.open,false);
 const read=calendar(()=>{},false);read.open();assert.equal(read.elements.calendarCreatedForm.hidden,true);c.open({readOnly:'true'});assert.equal(c.elements.calendarCreatedForm.hidden,true);
});
test('late content after closing the dialog cannot repopulate it',async()=>{
 let finish;const c=calendar(()=>new Promise(resolve=>finish=resolve));c.open();const pending=c.content();c.close();finish({ok:true,headers:{get:()=> 'application/json'},json:async()=>({html:'<pre>late</pre>'})});await pending;assert.equal(c.elements.calendarContent.innerHTML,'');assert.equal(c.elements.calendarContent.hidden,true);
});
test('a stale or offline save keeps the entered date and dialog; only an explicit receipt reloads',async()=>{
 for(const fetcher of [async()=>{throw new Error('offline');},async()=>({ok:false,headers:{get:()=> 'application/json'},json:async()=>({error:'conflict'})})]) {
  const c=calendar(fetcher);c.open();c.elements.calendarCreated.value='2030-01-01T00:00';await c.save();assert.equal(c.reloads.length,0);assert.equal(c.elements.calendarDialog.open,true);assert.equal(c.elements.calendarCreated.value,'2030-01-01T00:00');assert.equal(c.elements.calendarCreatedForm.inert,false);assert.equal(c.calls[0].options.body.get('_tenantView'),'v');assert.equal(c.calls[0].options.body.get('_csrf'),'csrf');
 }
 const c=calendar(async()=>({ok:true,headers:{get:()=> 'application/json'},json:async()=>({saved:true})}));c.open();await c.save();assert.equal(c.reloads.length,1);
});
