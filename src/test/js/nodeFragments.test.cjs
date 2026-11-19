const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const core=require('../../main/resources/static/js/common/nodeFragmentsState.js');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/nodeFragments.js'),'utf8');
// Small DOM double for lifecycle/network behavior. Browser paragraph layout remains a manual check.
function element(tag='div'){
 const e={tagName:tag.toUpperCase(),children:[],dataset:{},attrs:{},parentElement:null,textContent:'',
  append(...nodes){for(const n of nodes){if(n.tagName==='#FRAGMENT'){this.append(...[...n.children]);continue;}n.remove();n.parentElement=this;this.children.push(n);}},
  remove(){if(this.parentElement){const p=this.parentElement;p.children.splice(p.children.indexOf(this),1);this.parentElement=null;}},
  replaceChildren(...nodes){for(const n of [...this.children])n.remove();this.append(...nodes);},
  replaceWith(...nodes){const p=this.parentElement,index=p.children.indexOf(this);this.remove();for(let i=0;i<nodes.length;i++){nodes[i].remove();nodes[i].parentElement=p;p.children.splice(index+i,0,nodes[i]);}},
  after(node){const p=this.parentElement;node.remove();node.parentElement=p;p.children.splice(p.children.indexOf(this)+1,0,node);},
  contains(node){return this===node||this.children.some(c=>c.contains(node));},
  cloneNode(deep){const copy=element(tag);copy.dataset={...this.dataset};copy.attrs={...this.attrs};copy.textContent=this.textContent;if(deep)copy.append(...this.children.map(c=>c.cloneNode(true)));return copy;},
  matches(selector){return selector.split(',').some(s=>{s=s.trim();if(s==='*')return true;if(s==='[data-node-fragments]')return Object.hasOwn(this.dataset,'nodeFragments');if(s==='[data-node-fragments-source-toggle]')return Object.hasOwn(this.dataset,'nodeFragmentsSourceToggle');if(s.startsWith('.'))return this.className.split(' ').includes(s.slice(1));if(s==='[data-node-fragments-toggle]')return Object.hasOwn(this.dataset,'nodeFragmentsToggle');if(s==='[data-fragment-source]')return Object.hasOwn(this.dataset,'fragmentSource');if(s==='a[href]')return this.tagName==='A'&&this.attrs.href!=null;if(s==='[id]')return this.attrs.id!=null;if(s==='a[name]')return this.tagName==='A'&&this.attrs.name!=null;return s.toUpperCase()===this.tagName;});},
  closest(selector){for(let n=this;n;n=n.parentElement)if(n.matches(selector))return n;return null;},
  querySelectorAll(selector){return this.children.flatMap(c=>[...(c.matches(selector)?[c]:[]),...c.querySelectorAll(selector)]);},
  setAttribute(name,value){this.attrs[name]=String(value);},getAttribute(name){return this.attrs[name]??null;},removeAttribute(name){delete this.attrs[name];},hasAttribute(name){return this.attrs[name]!=null;}
 };
 Object.defineProperties(e,{firstChild:{get(){return this.children[0];}},childNodes:{get(){return this.children;}},isConnected:{get(){let n=this;while(n.parentElement)n=n.parentElement;return n.tagName==='#DOCUMENT';}},attributes:{get(){return Object.entries(this.attrs).map(([name,value])=>({name,value}));}}});
 for(const attr of ['href','id','target','rel','className'])Object.defineProperty(e,attr,{get(){return this.attrs[attr]||'';},set(value){this.attrs[attr]=String(value);}});
 return e;
}
const tick=()=>new Promise(resolve=>setImmediate(resolve));
async function settle(){for(let i=0;i<6;i++)await tick();}
function setup({stored=null,hideStored=null,blocked=false,delay=false,links=['2','3'],maximum='20',reply}={}){
 const document=element('#document'),root=element(),button=element('button'),sourceButton=element('button'),events={},windowEvents={},requests=[],pending=[];
 root.dataset={nodeFragments:'',fragmentSyntax:'custom-colors',fragmentContent:'1',fragmentToken:'current',fragmentEndpoint:'/nodes/fragments/',fragmentDepth:'3',fragmentCount:maximum,fragmentSourceLabel:'source',fragmentCycle:'cycle',fragmentError:'error',fragmentLimit:'limit',fragmentStale:'stale'};
 const original=links.map(id=>{const a=element('a');a.href='/nodes/'+id;a.textContent=id;return a;});root.append(...original);button.dataset.nodeFragmentsToggle='';sourceButton.dataset.nodeFragmentsSourceToggle='';document.append(button,sourceButton,root);
 document.createElement=element;document.createDocumentFragment=()=>element('#fragment');document.importNode=(item,deep)=>item.cloneNode(deep);document.addEventListener=(name,fn)=>events[name]=fn;document.dispatchEvent=event=>events[event.type]?.(event);
 class Parser{parseFromString(html){const doc=element('#document'),body=element('body');doc.body=body;doc.append(body);const content=element('div');content.textContent=html;body.append(content);for(const [,href]of html.matchAll(/<a href="([^"]+)">/g)){const a=element('a');a.href=href;content.append(a);}return doc;}}
 vm.runInNewContext(source,{window:{SweetCherryNodeFragments:core,addEventListener:(name,fn)=>windowEvents[name]=fn},document,location:{href:'https://example.test/tree'},URL,AbortController,DOMParser:Parser,CustomEvent:class{constructor(type,values){this.type=type;Object.assign(this,values);}},setTimeout,clearTimeout,localStorage:{getItem(key){if(blocked)throw Error();return key==='sweetcherry.nodeFragments'?stored:hideStored;},setItem(key,value){if(blocked)throw Error();if(key==='sweetcherry.nodeFragments')stored=value;else hideStored=value;}},fetch:async(url,options)=>{
  requests.push({url,options});if(delay)await new Promise(resolve=>pending.push(resolve));
  const id=url.pathname.split('/').pop(),data=reply?reply(id):{contentId:id==='3'?'1':id,name:'<Source>',syntax:'custom-colors',html:'<div>included <a href="/nodes/1">cycle</a></div>'};
  return {ok:true,status:200,headers:{get:()=> 'application/json'},json:async()=>data};
 }});
 const count=()=>root.querySelectorAll('.node-fragment').length;
 return {root,button,sourceButton,original,requests,events,windowEvents,pending,count,hideStored:()=>hideStored,sourceClick(){events.click({target:sourceButton,preventDefault(){},stopImmediatePropagation(){}});},stored:()=>stored,click(){events.click({target:button,preventDefault(){},stopImmediatePropagation(){}});}};
}
test('default link view, persistent toggle, alias/self cycles and exact original-node restoration',async()=>{
 const p=setup();assert.equal(p.requests.length,0);p.click();await settle();
 assert.equal(p.count(),1);assert.equal(p.stored(),'true');assert.equal(p.requests.length,2);
 assert.equal(p.requests[0].url.searchParams.get('_tenantView'),'current');assert.equal(p.requests[0].options.cache,'no-store');
 assert.equal(p.button.getAttribute('aria-pressed'),'true');p.click();assert.equal(p.count(),0);assert.equal(p.root.children[0],p.original[0]);assert.equal(p.root.children[1],p.original[1]);
 const next=setup({stored:'true'});await settle();assert.equal(next.count(),1);
});
test('late replies after disabling or navigation never insert old-source fragments',async()=>{
 const p=setup({stored:'true',delay:true,links:['2']});assert.equal(p.requests.length,1);p.click();p.pending.splice(0).forEach(resolve=>resolve());await settle();assert.equal(p.count(),0);assert.equal(p.root.children[0],p.original[0]);
 const old=setup({stored:'true',delay:true,links:['2']});old.root.remove();old.pending.splice(0).forEach(resolve=>resolve());await settle();assert.equal(old.count(),0);
});
test('storage failures, cross-tab changes, pagehide and inclusion budgets are handled',async()=>{
 const p=setup({blocked:true,links:['2','2'],maximum:'1'});p.click();await settle();assert.equal(p.count(),1);assert.equal(p.requests.length,1);
 p.windowEvents.storage({key:'sweetcherry.nodeFragments',newValue:'false'});assert.equal(p.count(),0);assert.equal(p.root.children[0],p.original[0]);
 p.windowEvents.storage({key:'sweetcherry.nodeFragments',newValue:'true'});await settle();assert.equal(p.count(),1);
 p.windowEvents.pagehide();assert.equal(p.count(),0);p.windowEvents.pageshow();await settle();assert.equal(p.count(),1);
});
test('repeated references share only the current-note fetch cache and receive separate inclusions',async()=>{
 const p=setup({stored:'true',links:['2','2'],reply:id=>({contentId:id,name:'Source',syntax:'plain-text',html:'<div>text</div>'})});await settle();assert.equal(p.count(),2);assert.equal(p.requests.length,1);
 p.click();p.click();await settle();assert.equal(p.requests.length,2);
});

test('copied anchors are local and receive separate namespaces for repeated fragments',()=>{
 const start=source.indexOf('    function namespace('),end=source.indexOf('    /** Inline replacements',start);
 const context={core,URL,location:{href:'https://example.test/tree'},document:{getElementById:()=>null}};vm.createContext(context);vm.runInContext(source.slice(start,end),context);
 const root=element();root.dataset={fragmentEndpoint:'/nodes/fragments/',fragmentToken:'current'};
 function fixture(){const body=element(),anchor=element('span'),link=element('a'),own=element('a'),missing=element('a');anchor.id='çapa';link.href='#%C3%A7apa';own.href='/nodes/2?_tenantView=old#%C3%A7apa';missing.href='#missing';body.append(anchor,link,own,missing);return {body,anchor,link,own,missing};}
 const one=fixture(),two=fixture();context.namespace(one.body,'first',root,'2');context.namespace(two.body,'second',root,'2');
 assert.equal(one.anchor.id,'first-1');assert.equal(two.anchor.id,'second-1');assert.equal(one.link.href,'#first-1');assert.equal(one.own.href,'#first-1');assert.match(one.missing.href,/\/nodes\/2\?_tenantView=current#missing$/);assert.equal(one.missing.target,'_blank');
});

test('source visibility is independent, persistent and does not reload included content',async()=>{
 const p=setup({stored:'true',links:['2']});await settle();assert.equal(p.count(),1);
 const source=p.root.querySelectorAll('.node-fragment-source')[0],lineBreak=p.root.querySelectorAll('.node-fragment-source-break')[0];
 assert.equal(source.hidden,false);assert.equal(lineBreak.hidden,false);const calls=p.requests.length;
 p.sourceClick();assert.equal(source.hidden,true);assert.equal(lineBreak.hidden,true);assert.equal(p.hideStored(),'true');assert.equal(p.requests.length,calls);assert.equal(p.count(),1);
 p.windowEvents.storage({key:'sweetcherry.nodeFragments.hideSource',newValue:'false'});assert.equal(source.hidden,false);assert.equal(lineBreak.hidden,false);
 const next=setup({stored:'true',hideStored:'true',links:['2']});await settle();assert.equal(next.root.querySelectorAll('.node-fragment-source')[0].hidden,true);
});
test('source hiding may be selected before enabling fragments and applies to late arrivals',async()=>{
 const off=setup({links:['2']});off.sourceClick();assert.equal(off.requests.length,0);off.click();await settle();assert.equal(off.root.querySelectorAll('.node-fragment-source')[0].hidden,true);
 const late=setup({stored:'true',delay:true,links:['2']});late.sourceClick();late.pending.splice(0).forEach(resolve=>resolve());await settle();assert.equal(late.root.querySelectorAll('.node-fragment-source')[0].hidden,true);
});

test('mounting an inline inclusion keeps the original command paragraph and surrounding wrappers',()=>{
 const start=source.indexOf('    function mount('),end=source.indexOf('    async function expand',start),context={};vm.createContext(context);vm.runInContext(source.slice(start,end),context);
 const paragraph=element('p'),before=element('strong'),link=element('a'),after=element('em'),fragment=element('span');paragraph.append(before,link,after);context.mount(link,fragment,paragraph);
 assert.deepEqual(paragraph.children,[before,fragment,after]);assert.equal(fragment.parentElement,paragraph);
});
