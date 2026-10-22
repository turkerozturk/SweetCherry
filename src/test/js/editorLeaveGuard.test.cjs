const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function guard() {
 const events={},formEvents={};let dirty=false,answer=false,confirmed=0;
 const window={location:{href:'https://example.org/edit',origin:'https://example.org',pathname:'/edit',search:''},addEventListener:(type,fn)=>events[type]=fn,confirm:()=>{confirmed++;return answer;}};
 const form={dataset:{leaveMessage:'Discard?'},addEventListener:(type,fn)=>formEvents[type]=fn};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/editorLeaveGuard.js'),'utf8'),{URL,window,document:{addEventListener:(type,fn)=>events[type]=fn}});
 window.EditorLeaveGuard.install(form,()=>dirty);
 function unload(){const e={preventDefault(){this.blocked=true;}};events.beforeunload(e);return !!e.blocked;}
 function link(href,target='',options={}) {const link={href,target,hasAttribute:()=>false};const e={...options,target:{closest:()=>link},preventDefault(){this.defaultPrevented=true;},stopImmediatePropagation(){this.stopped=true;}};events.click(e);return e;}
 return {events,formEvents,unload,link,setDirty:value=>dirty=value,answer:value=>answer=value,confirmed:()=>confirmed};
}
test('cancel and same-tab links prompt only for changes; cancel blocks navigation',()=>{
 const g=guard();assert.equal(g.unload(),false);g.setDirty(true);assert.equal(g.unload(),true);
 const e=g.link('/nodes/1');assert.equal(e.defaultPrevented,true);assert.equal(e.stopped,true);assert.equal(g.confirmed(),1);
 g.setDirty(false);assert.equal(g.link('/nodes/1').defaultPrevented,undefined);
});
test('confirmed leave avoids double prompt; BFCache restores protection',()=>{
 const g=guard();g.setDirty(true);g.answer(true);g.link('/nodes/1');assert.equal(g.unload(),false);
 g.events.pageshow();assert.equal(g.unload(),true);
});
test('save suppresses prompt only if submit succeeds, not when editor blocks save',()=>{
 const g=guard();g.setDirty(true);g.formEvents.submit({defaultPrevented:true});assert.equal(g.unload(),true);
 g.formEvents.submit({defaultPrevented:false});assert.equal(g.unload(),false);
});
test('new tabs and same-page anchors do not discard or clear dirty protection',()=>{
 const g=guard();g.setDirty(true);g.link('/nodes/1','_blank');g.link('/nodes/1','',{ctrlKey:true});g.link('/edit#preview');
 assert.equal(g.confirmed(),0);assert.equal(g.unload(),true);
});
