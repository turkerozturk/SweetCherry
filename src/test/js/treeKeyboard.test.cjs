const test=require('node:test'),assert=require('node:assert/strict');
const {install}=require('../../main/resources/static/js/node/treeKeyboard.js');
function setup(){
 let listener,modal=false,selectedText=false,stopped=false,focused=null;const selections=[],toggles=[];
 const doc={querySelector:()=>modal?{}:null,defaultView:{getSelection:()=>({isCollapsed:!selectedText})}};
 const nodes=[];
 function node(id,parent=null,hasChildren=false,open=false){
  const li={dataset:{nodeId:String(id)},parent,children:[],open,fold:hasChildren?{getAttribute:()=>li.open?'true':'false'}:null};if(parent)parent.children.push(li);
  li.parentElement={closest:()=>parent};
  const link={closest:s=>s==='.workspace-tree-row > a'?link:s==='li[data-node-id]'?li:s==='[hidden]'?hidden(li)?{}:null:null,focus(){focused=id;},scrollIntoView(){}};
  li.link=link;li.querySelector=s=>s.endsWith('> button')?li.fold:s===':scope > .workspace-tree-row > a'?link:li.open?li.children[0]?.link:null;
  nodes.push(li);return li;
 }
 function hidden(li){return li.parent?(!li.parent.open||hidden(li.parent)):false;}
 const a=node(1,null,true,true),a1=node(2,a),a2=node(3,a),b=node(4,null,true,false),b1=node(5,b),c=node(6);
 const tree={ownerDocument:doc,contains:link=>nodes.some(n=>n.link===link),querySelectorAll:()=>nodes.map(n=>n.link),addEventListener:(type,fn)=>listener=fn};
 install({tree,select:id=>selections.push(id),toggle:async li=>{li.open=!li.open;toggles.push(li.dataset.nodeId);},stopped:()=>stopped});
 return{a,a1,a2,b,b1,c,selections,toggles,get focused(){return focused;},set modal(v){modal=v;},set selectedText(v){selectedText=v;},set stopped(v){stopped=v;},async key(node,key,extra={}){let prevented=false;await listener({target:node.link,key,preventDefault(){prevented=true;},...extra});return prevented;}};
}
test('Up/Down and Home/End visit visible nodes, preserve tree focus and skip collapsed descendants',async()=>{
 const c=setup();assert.equal(await c.key(c.a,'ArrowDown'),true);assert.equal(c.focused,2);assert.deepEqual(c.selections,['2']);
 await c.key(c.b,'ArrowDown');assert.equal(c.focused,6);await c.key(c.c,'ArrowUp');assert.equal(c.focused,4);
 await c.key(c.a1,'Home');assert.equal(c.focused,1);await c.key(c.a,'End');assert.equal(c.focused,6);
});
test('Left/Right collapse and expand before navigating to parents and children',async()=>{
 const c=setup();await c.key(c.b,'ArrowRight');assert.equal(c.b.open,true);assert.deepEqual(c.selections,[]);
 await c.key(c.b,'ArrowRight');assert.equal(c.focused,5);await c.key(c.b1,'ArrowLeft');assert.equal(c.focused,4);
 await c.key(c.a,'ArrowLeft');assert.equal(c.a.open,false);assert.deepEqual(c.toggles,['4','1']);
});
test('modifiers including existing move shortcut, selections, editing and dialogs keep their native behavior',async()=>{
 const c=setup();
 for(const extra of [{altKey:true,shiftKey:true},{ctrlKey:true},{metaKey:true},{shiftKey:true},{isComposing:true}])assert.equal(await c.key(c.a,'ArrowDown',extra),false);
 c.modal=true;assert.equal(await c.key(c.a,'ArrowDown'),false);c.modal=false;
 c.selectedText=true;assert.equal(await c.key(c.a,'ArrowDown'),false);c.selectedText=false;
 c.stopped=true;assert.equal(await c.key(c.a,'ArrowDown'),false);c.stopped=false;
 assert.equal(await c.key({link:{closest:()=>null}},'ArrowDown'),false);
 assert.equal(await c.key(c.a,'Tab'),false);assert.equal(await c.key(c.a,'Enter'),false);assert.deepEqual(c.selections,[]);
});
