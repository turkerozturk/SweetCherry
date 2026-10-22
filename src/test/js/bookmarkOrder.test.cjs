const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function setup(readonly=false){
 const handlers={},documentHandlers={};let submitted=0,order='',target;
 const body={children:[],addEventListener:(name,fn)=>handlers[name]=fn,querySelectorAll:selector=>selector.includes('bookmark-drag')?body.children.map(row=>row.handle):[...body.children],
  insertBefore(row,before){if(row===before)return;body.children=body.children.filter(n=>n!==row);const at=before?body.children.indexOf(before):body.children.length;body.children.splice(at,0,row);},appendChild(row){body.insertBefore(row,null);}};
 for(let id=1;id<=3;id++){
  const row={dataset:{bookmarkId:String(id)},parentElement:body,classList:{add(){},remove(){}},closest:()=>row,getBoundingClientRect:()=>({top:body.children.indexOf(row)*100,height:100})};
  Object.defineProperty(row,'nextSibling',{get:()=>body.children[body.children.indexOf(row)+1]||null});
  row.handle={closest:selector=>selector==='.bookmark-drag'?row.handle:row,focus(){},setPointerCapture(){}};body.children.push(row);
 }
 const form={elements:{namedItem:()=>({set value(value){order=value;}})},requestSubmit(){submitted++;}};
 const document={getElementById:()=>readonly?null:form,querySelector:()=>body,addEventListener:(name,fn)=>documentHandlers[name]=fn,elementFromPoint:()=>target};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/bookmarks/bookmarkOrder.js'),'utf8'),{document,window:{innerHeight:800,scrollBy(){}}});
 const event=extra=>({button:0,pointerId:1,clientX:0,clientY:250,preventDefault(){},...extra});
 return{body,handlers,documentHandlers,event,set target(row){target=row;},get submitted(){return submitted;},get order(){return order;}};
}
test('touch/mouse drag saves the complete visible order once on release',()=>{
 const x=setup(),first=x.body.children[0];x.handlers.pointerdown(x.event({target:first.handle,pointerType:'touch'}));
 x.target=x.body.children[2];x.documentHandlers.pointermove(x.event({clientY:290}));assert.equal(x.submitted,0);
 x.documentHandlers.pointerup(x.event());assert.equal(x.order,'2,3,1');assert.equal(x.submitted,1);
 x.documentHandlers.pointerup(x.event());assert.equal(x.submitted,1);
});
test('cancelled drag restores rows without submitting',()=>{
 const x=setup();x.handlers.pointerdown(x.event({target:x.body.children[0].handle}));x.target=x.body.children[2];
 x.documentHandlers.pointermove(x.event({clientY:290}));x.documentHandlers.pointercancel(x.event());
 assert.equal(x.body.children.map(r=>r.dataset.bookmarkId).join(','),'1,2,3');assert.equal(x.submitted,0);
});
test('click without moving does not write; keyboard handles support reorder and boundaries',()=>{
 const x=setup(),handle=x.body.children[0].handle;x.handlers.pointerdown(x.event({target:handle}));x.documentHandlers.pointerup(x.event());assert.equal(x.submitted,0);
 x.handlers.keydown(x.event({target:handle,key:'ArrowUp'}));assert.equal(x.submitted,0);
 x.handlers.keydown(x.event({target:handle,key:'ArrowDown'}));assert.equal(x.order,'2,1,3');assert.equal(x.submitted,1);
});
test('read-only page has no reorder handlers',()=>{const x=setup(true);assert.deepEqual(x.handlers,{});assert.equal(x.submitted,0);});
