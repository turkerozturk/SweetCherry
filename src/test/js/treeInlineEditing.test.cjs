const test=require('node:test'),assert=require('node:assert/strict');
const {install,editorUrl}=require('../../main/resources/static/js/node/treeInlineEditing.js');
class Element {
 constructor(tag='div'){this.tag=tag;this.dataset={};this.attributes={};this.children=[];this.events={};this.hidden=false;this.inert=false;this.classList={add(){},remove(){}};}
 append(...nodes){for(const node of nodes){node.parent=this;this.children.push(node);}}
 remove(){if(this.parent)this.parent.children=this.parent.children.filter(item=>item!==this);}
 setAttribute(k,v){this.attributes[k]=v;}
 getAttribute(k){return this.attributes[k]??null;}
 addEventListener(k,fn){(this.events[k]??=[]).push(fn);}
 dispatchEvent(e){for(const fn of this.events[e.type]||[])fn(e);}
 focus(){this.focused=true;}
}
function setup(){
 const events={},timers=new Map();let nextTimer=0,dirty=false,busy=false,answer=false,approved=0,confirmations=0;
 const frames=[],refreshes=[],replacements=[],html=new Element(),childHtml=new Element();
 const scroller=new Element();scroller.scrollTop=230;scroller.scrollLeft=12;
 const reader=new Element();reader.querySelector=()=>scroller;
 const content=new Element();content.append(reader);content.querySelector=()=>reader;
 const childDoc=new Element();childDoc.documentElement=childHtml;
 const child={ready:()=>true,dirty:()=>dirty,busy:()=>busy,approveLeave(){approved++;dirty=false;},invalidate(){this.invalid=true;busy=false;}};
 const doc={documentElement:html,createElement(tag){const el=new Element(tag);if(tag==='iframe'){
  const location={get href(){return el.src;},replace(url){replacements.push(url);}};
  el.contentWindow={SweetCherryInlineEditor:child,location};el.contentDocument=childDoc;frames.push(el);
 }return el;}};
 const win={document:doc,location:{href:'https://notes.test/app/tree?nodeId=7'},Event:class{constructor(type){this.type=type;}},
 setTimeout(fn){const id=++nextTimer;timers.set(id,fn);return id;},clearTimeout(id){timers.delete(id);},
 addEventListener(name,fn){events[name]=fn;},confirm(){confirmations++;return answer;}};
 const api=install({win,workspace:{dataset:{editorLoading:'Loading',editorBusy:'Wait',editorError:'Error',editorLeave:'Discard?',editorReturn:'Read',editorSeparate:'Separate',editorTitle:'Edit'}},content,token:'current',getSelected:()=>7,onSaved:async(id,scroll)=>refreshes.push({id,scroll})});
 return {api,frames,reader,scroller,content,html,childHtml,child,refreshes,replacements,timers,
 open(){return api.open('https://notes.test/app/nodes/content/7?_tenantView=old');},
 load(){frames.at(-1).dispatchEvent({type:'load'});},layout(){events['sweetcherry:split-layout']();},
 set dirty(value){dirty=value;},set busy(value){busy=value;},set answer(value){answer=value;},get approved(){return approved;},get confirmations(){return confirmations;}};
}
test('only the current node editor under the same application origin/context can be embedded',()=>{
 for(const route of ['content','richtext/edit']){
  const url=editorUrl('/app/nodes/'+route+'/7?_tenantView=old&extra=1',7,'https://notes.test/app/tree','fresh');
  assert.equal(url.search,'?_tenantView=fresh');
 }
 for(const url of ['https://evil.test/app/nodes/content/7','/app/nodes/content/8','/app/nodes/delete/7','/wrong/nodes/content/7','/app/nodes/content/7#x'])
  assert.throws(()=>editorUrl(url,7,'https://notes.test/app/tree','current'));
});
test('Cancel restores the exact reader DOM and scroll position without a fetch',async()=>{
 const c=setup();assert.equal(c.open(),true);assert.equal(c.reader.hidden,true);assert.equal(c.frames[0].dataset.treeEditor,'true');
 assert.equal(c.open(),false);c.load();assert.equal(await c.api.finish(),true);
 assert.equal(c.reader.hidden,false);assert.equal(c.scroller.scrollTop,230);assert.equal(c.scroller.scrollLeft,12);assert.equal(c.refreshes.length,0);assert.equal(c.api.active(),false);
});
test('unsaved changes require one confirmation before navigation or cancellation',async()=>{
 const c=setup();c.open();c.load();c.dirty=true;
 assert.equal(c.api.beforeNavigate(),false);assert.equal(c.api.active(),true);assert.equal(c.approved,0);
 c.answer=true;assert.equal(await c.api.finish(),true);assert.equal(c.approved,1);assert.equal(c.confirmations,2);
});
test('loading and saving prevent removal even when discard confirmation would be accepted',()=>{
 const c=setup();c.answer=true;c.open();assert.equal(c.api.beforeNavigate(),false);
 c.load();c.busy=true;assert.equal(c.api.canLeave(),false);assert.equal(c.api.beforeNavigate(),false);assert.equal(c.confirmations,0);assert.equal(c.api.active(),true);
});
test('successful saves refresh reading with its previous scroll position only after a valid receipt',async()=>{
 const c=setup();c.open();c.load();const frame=c.frames[0];
 for(const url of ['https://evil.test/app/nodes/7','/app/nodes/9','/login','/app/nodes/7?x=1'])assert.equal(await c.api.saved(frame,url,false),false);
 assert.equal(c.refreshes.length,0);assert.equal(await c.api.saved(frame,'https://notes.test/app/nodes/7',false),true);
 assert.deepEqual(c.refreshes,[{id:'7',scroll:{top:230,left:12}}]);assert.equal(c.api.active(),false);
});
test('Save and continue replaces the editor document and later Cancel reads the committed version',async()=>{
 const c=setup();c.open();c.load();const frame=c.frames[0];
 assert.equal(await c.api.saved(frame,'https://notes.test/app/nodes/content/7',true),true);
 assert.equal(c.api.active(),true);assert.equal(c.refreshes.length,0);assert.equal(c.replacements[0],'https://notes.test/app/nodes/content/7?_tenantView=current');
 assert.equal(c.api.beforeNavigate(),false);c.load();await c.api.finish();assert.equal(c.refreshes.length,1);
});
test('compact layout changes are applied to the editor without modifying reader preferences',()=>{
 const c=setup();c.html.setAttribute('data-tree-layout-title','false');c.open();c.load();
 c.html.setAttribute('data-tree-layout-compact','true');c.layout();assert.equal(c.childHtml.getAttribute('data-inline-compact'),'true');
 assert.equal(c.html.getAttribute('data-tree-layout-title'),'false');
});
test('stale source invalidation retains the editor for draft rescue and never accepts a save receipt',async()=>{
 const c=setup();c.open();c.load();c.api.invalidate('Stale');assert.equal(c.child.invalid,true);assert.equal(c.api.active(),true);assert.equal(c.reader.hidden,true);
 assert.equal(await c.api.saved(c.frames[0],'/app/nodes/7',false),false);assert.equal(c.refreshes.length,0);
});
test('a delayed old iframe cannot complete a newly opened editing session',async()=>{
 const c=setup();c.open();c.load();const old=c.frames[0];await c.api.finish();c.open();c.load();
 assert.equal(await c.api.saved(old,'/app/nodes/7',false),false);assert.equal(c.api.active(),true);
});
test('opening an editor is blocked while the reading navigation is still loading',()=>{
 const c=setup();c.content.setAttribute('aria-busy','true');assert.equal(c.open(),false);assert.equal(c.frames.length,0);assert.equal(c.reader.hidden,false);
});
