const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const {dom}=require('./helpers/symbolDom.cjs');
const folder=path.join(__dirname,'../../main/resources/static/js/symbols');
function setup(){const d=dom(),window={};const context={window,document:d.document,NodeFilter:{SHOW_TEXT:4},MutationObserver:class{observe(){}},fetch(){assert.fail('No network');}};vm.runInNewContext(fs.readFileSync(path.join(folder,'catalog.js'),'utf8'),context);vm.runInNewContext(fs.readFileSync(path.join(folder,'inlineSymbols.js'),'utf8'),context);return{...d,window,core:window.SweetCherryInlineSymbols};}
test('catalogue references installed Free Font Awesome styles, existing PNGs and native Unicode',()=>{
 const s=setup(),items=s.window.SweetCherrySymbolCatalog.items;assert.equal(items.filter(i=>i.kind==='unicode').length,236);assert.equal(items.filter(i=>i.kind==='ctb').length,373);
 assert(items.some(i=>i.value==='{{fa-solid fa-image}}'));assert(items.some(i=>i.value==='{{fa-brands fa-github}}'));
 for(const item of items.filter(i=>i.kind!=='unicode'))assert.equal(s.core.resolve(item.value.slice(2,-2)).name,item.name,item.value);
 for(const item of items.filter(i=>i.kind==='ctb'))assert(fs.existsSync(path.join(__dirname,'../../main/resources/static/img/ctbicons',item.name+'.png')),item.name);
});
test('only exact allowlisted markers resolve; arbitrary HTML, paths, classes and report placeholders stay literal',()=>{
 const s=setup();assert.equal(s.core.resolve('fa-solid fa-image').name,'image');assert.equal(s.core.resolve('fas fa-image').name,'image');assert.equal(s.core.resolve('CTB:CT_CAMERA').name,'ct_camera');
 for(const marker of ['fa-solid fa-image fa-spin','fa-solid fa-imaginary','ctb:../../private','ctb:https://evil/x','<img onerror=evil()>','NODE_NAME'])assert.equal(s.core.resolve(marker),null,marker);
 const text='{{NODE_NAME}} {{fa-solid fa-image}}';const parts=s.core.tokens(text);assert.equal(parts[0],'{{NODE_NAME}} ');assert.equal(parts[1].item.name,'image');assert.equal(s.core.tokens('{{{fa-solid fa-image}}}')[0],'{{{fa-solid fa-image}}}');
});
test('rendering creates only known DOM icons, preserves surrounding text and does not parse title HTML',()=>{
 const s=setup(),root=s.document.createElement('span');root.setAttribute('data-inline-symbols','');root.textContent='<script>evil()</script> {{fa-solid fa-image}} + {{ctb:ct_camera}}';s.core.render(root);
 assert(root.textContent.startsWith('<script>evil()</script> '));assert.equal(root.querySelectorAll('script').length,0);assert.equal(root.querySelectorAll('.sc-inline-symbol').length,2);
 assert.equal(root.querySelector('img').src,'/app/img/ctbicons/ct_camera.png');assert.equal(root.querySelector('img').className,'sc-inline-icon');assert.equal(root.querySelector('i').className,'fa-solid fa-image');
 s.core.render(root);assert.equal(root.querySelectorAll('.sc-inline-symbol').length,2);
});
test('code, editable surfaces, tables, plain fragments and picker text are not decorated',()=>{
 for(const kind of ['code','pre','table','editable','plain','picker','monospace']){
  const s=setup(),root=s.document.createElement('div'),child=s.document.createElement(['code','pre','table'].includes(kind)?kind:'span');
  if(kind==='editable')child.setAttribute('contenteditable','true');if(kind==='plain')child.setAttribute('data-symbols-skip','');if(kind==='picker')child.setAttribute('data-symbol-picker','');if(kind==='monospace')child.setAttribute('style','font-family:monospace');
  child.textContent='{{fa-solid fa-image}}';root.append(child);s.core.render(root);assert.equal(child.textContent,'{{fa-solid fa-image}}',kind);assert.equal(root.querySelectorAll('.sc-inline-symbol').length,0,kind);
 }
});

test('markers typed over identical adjacent formatting runs render without changing surrounding text',()=>{
 const s=setup(),root=s.document.createElement('div');
 for(const text of ['Start ','{','{fa-','solid ','fa-image','}}',' end']){const span=s.document.createElement('span');span.setAttribute('style','color:red;');span.textContent=text;root.append(span);}
 s.core.render(root);assert.equal(root.querySelectorAll('.sc-inline-symbol').length,1);assert.equal(root.textContent,'Start  end');
 const mixed=s.document.createElement('div');for(const [text,color] of [['{{fa-solid ','red'],['fa-image}}','blue']]){const span=s.document.createElement('span');span.setAttribute('style','color:'+color);span.textContent=text;mixed.append(span);}s.core.render(mixed);assert.equal(mixed.textContent,'{{fa-solid fa-image}}');assert.equal(mixed.querySelectorAll('.sc-inline-symbol').length,0);
});

test('explicit table node labels render while neighboring rich grids and form values stay literal',()=>{
 const s=setup(),table=s.document.createElement('table'),row=s.document.createElement('tr'),name=s.document.createElement('td'),data=s.document.createElement('td');
 name.setAttribute('data-inline-symbols','');name.textContent='{{fa-solid fa-image}} name';data.textContent='{{fa-solid fa-image}} grid';row.append(name,data);table.append(row);s.core.render(table);
 assert.equal(name.querySelectorAll('.sc-inline-symbol').length,1);assert.equal(data.textContent,'{{fa-solid fa-image}} grid');
});
test('map label HTML escapes text and marker attributes; native text labels never interpret HTML',()=>{
 const s=setup(),html=s.core.html('<img onerror=x> " {{fa-solid fa-image}} {{ctb:ct_camera}} {{record.name}}');
 assert(html.includes('&lt;img onerror=x&gt;'));assert(!html.includes('<img onerror='));assert(html.includes('fa-solid fa-image'));assert(html.includes('/app/img/ctbicons/ct_camera.png'));assert(html.includes('{{record.name}}'));
 assert.equal(s.core.plain('X {{fa-solid fa-image}} Y {{record.name}}'),'X [Image] Y {{record.name}}');
});
test('native label updates are idempotent and unrelated template expressions never cause observer loops',()=>{
 const s=setup(),title=s.document.createElement('title');title.textContent='{{record.name}}';
 Object.defineProperty(title,'textContent',{get:()=> '{{record.name}}',set(){assert.fail('Unchanged text must not be rewritten');}});
 title.setAttribute('title','{{record.name}}');title.setAttribute=()=>assert.fail('Unchanged attribute must not be rewritten');s.core.scan(title);
 const known=s.document.createElement('title');known.textContent='Node {{fa-solid fa-image}}';s.core.scan(known);assert.equal(known.textContent,'Node [Image]');s.core.scan(known);assert.equal(known.textContent,'Node [Image]');
});
