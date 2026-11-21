const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const {dom}=require('./helpers/symbolDom.cjs');const folder=path.join(__dirname,'../../main/resources/static/js/symbols');
function setup({readonly=false,copyFails=false,rich=false}={}){
 const d=dom(),window={},copied=[],calls=[];const context={window,document:d.document,NodeFilter:{SHOW_TEXT:4},MutationObserver:class{observe(){}},Event:class{constructor(type){this.type=type;}},navigator:{clipboard:{async writeText(text){if(copyFails)throw Error('denied');copied.push(text);}}}};
 for(const name of ['catalog.js','inlineSymbols.js'])vm.runInNewContext(fs.readFileSync(path.join(folder,name),'utf8'),context);
 const dialog=new d.Element('dialog');dialog.dataset={empty:'Empty',copied:'Copied',copyFailed:'Use Ctrl+C',changed:'Changed',long:'Too long',count:'Results'};
 const nodes={};for(const name of ['query','kind','results','status','value','preview','copy','insert','prev','next','count','close','cancel']){const el=new d.Element(['query','value'].includes(name)?'input':name==='kind'?'select':['results','status','count','preview'].includes(name)?'div':'button');el.setAttribute('data-symbol-'+name,'');nodes[name]=el;dialog.append(el);}nodes.kind.value='all';
 const input=new d.Element('input');input.value='A😀B';input.maxLength=255;input.selectionStart=1;input.selectionEnd=3;input.readOnly=readonly;
 const trigger=new d.Element('button');trigger.setAttribute('data-symbol-picker-open','');if(rich)trigger.setAttribute('data-symbol-rich','');else trigger.dataset.symbolTarget='node-name';
 d.document.getElementById=id=>id==='symbol-picker'?dialog:id==='node-name'?input:null;
 if(rich)window.RichTextSymbolBridge={capture(){calls.push('capture');return {selection:[1,2],content:'before'};},insert(ticket,text){calls.push({ticket,text});return [10,10];},restore(range){calls.push({restore:range});}};
 vm.runInNewContext(fs.readFileSync(path.join(folder,'picker.js'),'utf8'),context);
 function open(){d.document.listeners.click({target:trigger,preventDefault(){}});}
 function choose(marker){nodes.query.value=marker;nodes.query.listeners.input();assert.equal(nodes.results.children.length,1);nodes.results.children[0].click();}
 return {...d,dialog,nodes,input,copied,calls,open,choose};
}
test('filtered picker inserts a literal icon marker at the original UTF-16 title selection without submitting',()=>{
 const s=setup();s.open();s.choose('{{fa-solid fa-image}}');s.nodes.insert.click();assert.equal(s.input.value,'A{{fa-solid fa-image}}B');assert.equal(s.input.selectionStart,1+'{{fa-solid fa-image}}'.length);assert.equal(s.dialog.open,false);
});
test('read-only target offers copy only, and clipboard failure selects the literal value',async()=>{
 const s=setup({readonly:true});s.open();s.choose('{{ctb:ct_camera}}');assert.equal(s.nodes.insert.disabled,true);await s.nodes.copy.listeners.click();assert.deepEqual(s.copied,['{{ctb:ct_camera}}']);assert.equal(s.input.value,'A😀B');
 const failure=setup({copyFails:true});failure.open();failure.choose('{{ctb:ct_camera}}');await failure.nodes.copy.listeners.click();assert.equal(failure.nodes.status.textContent,'Use Ctrl+C');assert.equal(failure.nodes.value.selectionEnd,'{{ctb:ct_camera}}'.length);
});
test('Unicode copying/inserting preserves the character, while title limits and stale content reject edits',async()=>{
 const s=setup();s.open();s.nodes.results.children[0].click();await s.nodes.copy.listeners.click();assert.equal(s.copied[0],'😀');
 s.input.value='changed';s.nodes.insert.click();assert.equal(s.input.value,'changed');assert.equal(s.nodes.status.textContent,'Changed');assert.equal(s.dialog.open,true);
 const limited=setup();limited.input.maxLength=4;limited.open();limited.choose('{{fa-solid fa-image}}');limited.nodes.insert.click();assert.equal(limited.input.value,'A😀B');assert.equal(limited.nodes.status.textContent,'Too long');
});
test('rich editor insertion uses its model bridge and restores the new caret; cancel restores the old selection',()=>{
 const s=setup({rich:true});s.open();s.choose('{{fa-solid fa-image}}');s.nodes.insert.click();assert.equal(s.calls[0],'capture');assert.equal(s.calls[1].text,'{{fa-solid fa-image}}');assert.deepEqual(Array.from(s.calls[2].restore),[10,10]);
 const cancelled=setup({rich:true});cancelled.open();cancelled.nodes.cancel.click();assert.equal(cancelled.calls.length,2);assert.deepEqual(Array.from(cancelled.calls[1].restore),[1,2]);
});
