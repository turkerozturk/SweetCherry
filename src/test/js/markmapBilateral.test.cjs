const test=require('node:test'),assert=require('node:assert/strict');
const {partition,path,createClass}=require('../../main/resources/static/js/markmap/bilateral.js');
const node=(name,children=[])=>({name,children,payload:{},state:{rect:{x:0,y:0,width:40,height:20}}});
test('full subtree weights balance sides deterministically and preserve order within each side',()=>{
 const a=node('a',[node('a1'),node('a2')]),b=node('b',[node('b1')]),c=node('c'),d=node('d');
 const result=partition([a,b,c,d]);assert.deepEqual(result.right,[a,d]);assert.deepEqual(result.left,[b,c]);
 a.payload.fold=1;assert.deepEqual(partition([a,b,c,d]),result);
 assert.deepEqual(partition([]),{left:[],right:[]});assert.deepEqual(partition([a]).right,[a]);
});
test('measured layout mirrors coordinates only and preserves the complete source tree',()=>{
 class Base {
  constructor(root){this.state={data:root};}
  _relayout(){const r=this.state.data;r.state.rect={x:0,y:-10,width:60,height:20};
   const visit=(n,x,y)=>{n.state.rect={x,y,width:40,height:20};if(!n.payload.fold)n.children.forEach((c,i)=>visit(c,x+100,y+i*35));};
   if(!r.payload.fold)r.children.forEach((c,i)=>visit(c,160,i*35));
  }
 }
 const a=node('a',[node('a1')]),b=node('b',[node('b1')]),root=node('root',[a,b]);const children=root.children;
 const mm=new (createClass(Base))(root);mm._relayout();
 assert.equal(root.children,children);assert.equal(a.state.rect.x,160);assert.equal(b.state.rect.x,-140);
 assert.equal(b.children[0].state.rect.x,-240);assert.equal(b.state.rect.width,40);assert(mm.state.rect.x1<0);assert(mm.state.rect.x2>0);
 b.payload.fold=1;mm._relayout();assert.equal(b.state.rect.x,-140);assert.equal(mm.state.rect.x1,-140);
 root.payload.fold=1;mm._relayout();assert.deepEqual(mm.state.rect,{x1:0,y1:-10,x2:60,y2:10});assert.equal(root.children,children);
});
test('left and right connectors attach to the outward edge without mirroring text',()=>{
 const root=node('root'),child=node('child');root.state.rect={x:0,y:0,width:60,height:20};child.state.rect={x:-140,y:40,width:40,height:20};
 assert.equal(path(root,child,true,()=>2),'M0,21C-50,21 -50,61 -100,61');
 child.state.rect.x=160;assert.equal(path(root,child,false,()=>2),'M60,21C110,21 110,61 160,61');
});
