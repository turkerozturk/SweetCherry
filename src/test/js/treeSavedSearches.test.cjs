const test=require('node:test'),assert=require('node:assert/strict');
const {install,valid}=require('../../main/resources/static/js/node/treeSavedSearches.js');
const ID='12345678-1234-1234-1234-123456789abc';
class Element {
 constructor(tag='div'){this.tag=tag;this.children=[];this.dataset={};this.attrs={};this.listeners={};}
 append(...nodes){this.children.push(...nodes);}
 replaceChildren(...nodes){this.children=[...nodes];}
 setAttribute(k,v){this.attrs[k]=v;}
 removeAttribute(k){delete this.attrs[k];}
 addEventListener(k,v){this.listeners[k]=v;}
 querySelector(s){if(s===':scope > .workspace-tree-row > a')return this.link;return this.parts?.[s];}
 querySelectorAll(s){return this.children.flatMap(n=>[n,...n.querySelectorAll('*')]).filter(n=>s==='*'||s==='[aria-current]'?s==='*'||n.attrs['aria-current']!==undefined:s==='li[data-node-id]'&&n.tag==='li'&&n.dataset.nodeId);}
 set innerHTML(value){throw Error('Never interpret saved names or node titles as HTML');}
}
const flush=()=>new Promise(resolve=>setImmediate(resolve));
function setup(response){
 const tree=new Element('ul'),status=new Element(),reload=new Element('button'),box=new Element();
 box.ownerDocument={createElement:tag=>new Element(tag)};box.dataset={endpoint:'/nodes/saved-filters/tree',loading:'Loading',error:'Error',empty:'Empty',noPresets:'No presets',refresh:'Refresh',previous:'Previous',next:'Next'};
 box.parts={'[data-saved-search-tree]':tree,'[data-saved-search-status]':status,'[data-saved-search-reload]':reload};
 const calls=[],signals=[];let keyboard;
 const api=install({box,request:async(path,signal)=>{calls.push(path);signals.push(signal);return {json:async()=>response(path,signal)};},url:(path,params={})=>path+'?'+new URLSearchParams({...params,_tenantView:'token'}),row:n=>{const li=new Element('li');li.dataset.nodeId=String(n.nodeId);li.link=new Element('a');li.append(li.link);return li;},keyboard:{install:options=>keyboard=options},select:()=>{}});
 return {api,tree,status,reload,calls,signals,get fold(){return tree.children[0].children[0].children[0];},get list(){return tree.children[0].children[1];},get refresh(){return tree.children[0].children[0].children[3];},get keyboard(){return keyboard;}};
}
const data=(page=0)=>({id:ID,page,total:51,hasNext:page===0,results:[{nodeId:page?22:11,name:'<b>Literal title</b>',path:'Parent / Node',hasChildren:false}]});
test('presets are lazy virtual branches and names stay literal text',async()=>{
 const c=setup(path=>path.includes('/'+ID)?data():[{id:ID,name:'<img src=x onerror=alert(1)>'}]);
 await c.api.reload();assert.equal(c.calls.length,1);assert.equal(c.list.hidden,true);
 assert.equal(c.tree.children[0].children[0].children[1].textContent,'<img src=x onerror=alert(1)>');
 c.fold.listeners.click();await flush();assert.equal(c.calls.length,2);assert.match(c.calls[1],/page=0&_tenantView=token/);
 assert.equal(c.list.children[0].link.title,'Parent / Node');assert.equal(c.list.children[0].dataset.nodeId,'11');
});
test('paging is server-backed and selected result markers do not alter real node IDs',async()=>{
 const c=setup(path=>path.includes('/'+ID)?data(Number(new URL(path,'http://app').searchParams.get('page'))):[{id:ID,name:'Tasks'}]);
 await c.api.reload();c.fold.listeners.click();await flush();c.api.markSelected(11);assert.equal(c.list.children[0].link.attrs['aria-current'],'page');
 c.list.children[1].children[0].listeners.click();await flush();assert.equal(c.list.children[0].dataset.nodeId,'22');assert.match(c.calls.at(-1),/page=1/);
 c.list.children[1].children[0].listeners.click();await flush();assert.equal(c.list.children[0].link.attrs['aria-current'],'page');
});
test('each expansion or refresh reruns the query; closing aborts an unfinished request',async()=>{
 const c=setup(path=>path.includes('/'+ID)?data():[{id:ID,name:'Tasks'}]);await c.api.reload();
 c.fold.listeners.click();await flush();c.fold.listeners.click();assert.equal(c.signals.at(-1).aborted,true);
 c.fold.listeners.click();await flush();assert.equal(c.calls.length,3);c.refresh.listeners.click();await flush();assert.equal(c.calls.length,4);
 c.api.stop();assert.equal(c.signals.at(-1).aborted,true);await c.api.reload();assert.equal(c.calls.length,4);
});
test('empty, invalid and unavailable saved searches leave a useful message without loading content',async()=>{
 const none=setup(()=>[]);await none.api.reload();assert.equal(none.status.textContent,'No presets');
 const bad=setup(()=>[{id:'../../other-user',name:'Invalid'}]);await bad.api.reload();assert.equal(bad.status.textContent,'Error');assert.equal(bad.tree.children.length,0);
 const empty=setup(path=>path.includes('/'+ID)?{...data(),results:[],total:0,hasNext:false}:[{id:ID,name:'Empty'}]);await empty.api.reload();empty.fold.listeners.click();await flush();assert.equal(empty.list.children[0].textContent,'Empty');
 const invalid=setup(path=>path.includes('/'+ID)?{...data(),id:'other'}:[{id:ID,name:'Bad'}]);await invalid.api.reload();invalid.fold.listeners.click();await flush();assert.equal(invalid.list.children[0].textContent,'Error');
});
test('result validation rejects unbounded pages, invalid targets and nested real-tree branches',()=>{
 assert.equal(valid(data(),ID),true);
 for(const value of [{...data(),results:Array(51).fill(data().results[0])},{...data(),results:[{...data().results[0],nodeId:-1}]},{...data(),results:[{...data().results[0],hasChildren:true}]}])assert.equal(valid(value,ID),false);
});
test('a superseded response cannot replace the newly opened branch results',async()=>{
 const pending=[];const c=setup(path=>path.includes('/'+ID)?new Promise(resolve=>pending.push(resolve)):[{id:ID,name:'Tasks'}]);
 await c.api.reload();c.fold.listeners.click();await flush();c.fold.listeners.click();c.fold.listeners.click();await flush();
 pending[1]({...data(),results:[{...data().results[0],nodeId:22}]});await flush();
 pending[0](data());await flush();assert.equal(c.list.children[0].dataset.nodeId,'22');
});
