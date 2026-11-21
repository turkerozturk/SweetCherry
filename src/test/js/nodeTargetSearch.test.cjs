const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/nodeTargetSearch.js'),'utf8');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function element(){return {dataset:{},children:[],listeners:{},attributes:{},style:{},value:'',disabled:false,isConnected:true,
 append(...items){this.children.push(...items);},replaceChildren(...items){this.children=items;},setAttribute(key,value){this.attributes[key]=String(value);},removeAttribute(key){delete this.attributes[key];},addEventListener(key,fn){this.listeners[key]=fn;},focus(){this.focused=true;},click(){this.listeners.click?.();},scrollIntoView(){}};}
function setup(){
 const input=element(),list=element(),status=element(),dialog=element(),requests=[],selected=[],timers=new Map();dialog.open=true;let sequence=0,changed=0;
 const context={window:{},document:{createElement:()=>element()},location:{href:'https://example.test/app/tree'},URL,AbortController,
 setTimeout:(fn,ms)=>{timers.set(++sequence,{fn,ms});return sequence;},clearTimeout:id=>timers.delete(id),
 fetch:(url,options)=>new Promise((resolve,reject)=>requests.push({url,options,resolve,reject}))};
 vm.runInNewContext(source,context);
 const picker=context.window.SweetCherryNodeSearch.create({input,list,status,dialog,url:'/app/nodes/quick/search',token:()=> 'current',icons:'/app/img/ctbicons/',available:()=>dialog.open,
 messages:{loading:'loading',empty:'empty',limit:'limit',error:'error',stale:'stale',long:'long'},onSelect:item=>selected.push(item),onQueryChange:()=>changed++});
 return {input,list,status,dialog,requests,selected,picker,changed:()=>changed,type(value){input.value=value;input.listeners.input();},run(){for(const [id,timer]of [...timers])if(timer.ms===250){timers.delete(id);timer.fn();}},resolve(index,items,extra={}){requests[index].resolve({ok:true,redirected:false,status:200,headers:{get:()=> 'application/json'},json:async()=>({items,more:false,scanLimited:false,...extra})});return tick();},key(key){let prevented=false;input.listeners.keydown({key,preventDefault(){prevented=true;},stopPropagation(){}});return prevented;}};
}
const item=(id='9007199254740993',name='<Node>')=>({id,name,icon:'ct_code',bold:true,color:'#123456',breadcrumb:'Parent / '+name});
test('typing uses a debounced contains-search, current token and safe literal rows',async()=>{
 const s=setup();s.type('par');s.type('part');assert.equal(s.requests.length,0);s.run();assert.equal(s.requests.length,1);
 assert.equal(s.requests[0].url.searchParams.get('q'),'part');assert.equal(s.requests[0].url.searchParams.get('_tenantView'),'current');assert.equal(s.requests[0].options.redirect,'error');assert.equal(s.requests[0].options.cache,'no-store');
 await s.resolve(0,[item()]);const button=s.list.children[0];assert.equal(button.children[1].children[0].textContent,'<Node>');assert.equal(button.children[1].children[2].textContent,'Parent / <Node>');
 assert.equal(s.input.attributes['aria-expanded'],'true');button.click();assert.equal(s.selected[0].id,'9007199254740993');assert.equal(s.list.children.length,0);
});
test('late responses after a newer query or closing cannot restore old choices',async()=>{
 const s=setup();s.type('old');s.run();s.type('new');s.run();assert.equal(s.requests[0].options.signal.aborted,true);
 await s.resolve(1,[item('2','New')]);await s.resolve(0,[item('1','Old')]);assert.equal(s.list.children[0].children[1].children[0].textContent,'New');
 s.type('another');s.run();s.dialog.open=false;s.dialog.listeners.close();await s.resolve(2,[item('3','Late')]);assert.equal(s.list.children.length,0);
});
test('arrow keys and Enter select without submitting and preserve separate link text',async()=>{
 const s=setup();s.type('node');s.run();await s.resolve(0,[item('1','One'),item('2','Two')]);
 assert.equal(s.key('ArrowUp'),true);assert.equal(s.list.children[1].attributes['aria-selected'],'true');assert.equal(s.key('Enter'),true);assert.equal(s.selected[0].id,'2');
 assert.equal(s.input.value,'node');
});
test('blank/oversized queries, bounded results and IME composition are handled',async()=>{
 const s=setup();s.type(' ');s.run();assert.equal(s.requests.length,0);s.type('x'.repeat(161));s.run();assert.equal(s.status.textContent,'long');assert.equal(s.requests.length,0);
 s.input.listeners.compositionstart();s.type('İstanbul');s.run();assert.equal(s.requests.length,0);s.input.listeners.compositionend();s.run();await s.resolve(0,[item('1')],{more:true});assert.equal(s.status.textContent,'limit');
 s.type('zero');s.run();await s.resolve(1,[]);assert.equal(s.status.textContent,'empty');
});
test('wrong ID types, redirects and stale tenants never produce selectable rows',async()=>{
 const s=setup();s.type('node');s.run();await s.resolve(0,[item(123)]);assert.equal(s.status.textContent,'error');assert.equal(s.list.children.length,0);
 s.type('next');s.run();s.requests[1].resolve({ok:false,status:409});await tick();assert.equal(s.status.textContent,'stale');
 s.type('login');s.run();s.requests[2].resolve({ok:true,redirected:true,status:200});await tick();assert.equal(s.status.textContent,'error');assert.equal(s.list.children.length,0);
 s.type('timeout');s.run();s.requests[3].reject(Object.assign(new Error('timeout'),{name:'AbortError'}));await tick();assert.equal(s.status.textContent,'error');
});
