const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function layout(mobile=true){
 const handlers={},attrs={},classes=new Set(),storage=new Map(),restored=[];let selection=[2,7];
 const elements={};for(const id of ['richEditorShell','richToolsToggle','richToolsClose','richControls','richToolsSplitter','richToolsBackdrop'])elements[id]={hidden:false,clientWidth:1000,style:{setProperty(){}},setAttribute(k,v){attrs[id+':'+k]=v;},addEventListener(e,f){handlers[id+':'+e]=f;},setPointerCapture(){},getBoundingClientRect(){return{left:0};}};
 const media={matches:mobile,addEventListener(e,fn){handlers.media=fn;}};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/richTextEditorLayout.js'),'utf8'),{
 document:{getElementById:id=>elements[id],body:{classList:{toggle(c,on){on?classes.add(c):classes.delete(c);},contains:c=>classes.has(c),add:c=>classes.add(c),remove:c=>classes.delete(c)}},addEventListener(e,f){handlers['document:'+e]=f;},querySelector:()=>null},
 window:{matchMedia:()=>media,addEventListener(){},RichTextSelectionBridge:{capture:()=>[...selection],restore:ticket=>restored.push([...ticket])}},
 localStorage:{getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v)}});
 return{handlers,attrs,classes,storage,restored,elements,setSelection:value=>{selection=value;},event:{preventDefault(){},pointerId:1}};
}
test('mobile drawer keeps the selection and returns the latest formatting caret on close',()=>{
 const l=layout();l.handlers['richToolsToggle:pointerdown'](l.event);l.handlers['richToolsToggle:click']();
 assert.ok(l.classes.has('rich-tools-open'));assert.equal(l.attrs['richControls:aria-hidden'],'false');assert.equal(l.elements.richToolsBackdrop.hidden,false);
 l.setSelection([4,9]);l.handlers['richToolsClose:click']();assert.deepEqual(l.restored.at(-1),[4,9]);
 assert.equal(l.attrs['richControls:aria-hidden'],'true');assert.equal(l.elements.richToolsBackdrop.hidden,true);
});
test('desktop splitter restores selection and persists only a bounded width',()=>{
 const l=layout(false);l.handlers['richToolsSplitter:pointerdown'](l.event);l.handlers['richToolsSplitter:pointermove']({clientX:900});l.handlers['richToolsSplitter:pointerup']();
 assert.deepEqual(l.restored.at(-1),[2,7]);assert.equal(l.storage.get('sweetcherry.richToolsWidth'),'480');assert.equal(l.storage.size,1);
 assert.equal(l.attrs['richControls:aria-hidden'],'false');
});
test('Escape dismisses mobile tools without submitting the form or changing content',()=>{
 const l=layout();l.handlers['richToolsToggle:click']();l.handlers['document:keydown']({key:'Escape',preventDefault(){}});
 assert.equal(l.classes.has('rich-tools-open'),false);assert.deepEqual(l.restored.at(-1),[2,7]);assert.equal(l.storage.size,0);
});
