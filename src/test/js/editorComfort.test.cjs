const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/editorComfort.js'),'utf8');
function setup(saved='100',blocked=false){
 const handlers={},frames=[],styles={},storage=new Map([['sweetcherry.editorTextZoom',saved],['sweetcherry.nodeContentZoom','170']]),labels=[{}];
 const buttons=['out','reset','in'].map(action=>({dataset:{editorZoom:action},closest:()=>buttons.find(b=>b.dataset.editorZoom===action)}));
 const surface={clientHeight:400,scrollHeight:1500,scrollTop:0,style:{setProperty(k,v){styles[k]=v;}},addEventListener(k,v){handlers['surface:'+k]=v;},scrollIntoView(){handlers.scrolled=true;}};
 let range=[20,20],text='x'.repeat(20),restores=0;
 const document={readyState:'complete',activeElement:surface,scrollingElement:{scrollHeight:2000},documentElement:{style:{setProperty(k,v){styles[k]=v;}}},querySelectorAll:selector=>selector==='[data-editor-zoom-value]'?labels:buttons,addEventListener(k,v){handlers[k]=v;}};
 const window={addEventListener(k,v){handlers['window:'+k]=v;},scrollTo(value){handlers.pageScroll=value.top;}};
 vm.runInNewContext(source,{window,document,localStorage:{getItem:k=>{if(blocked)throw Error();return storage.get(k);},setItem(k,v){if(blocked)throw Error();storage.set(k,v);}},requestAnimationFrame:fn=>frames.push(fn),ResizeObserver:class{constructor(fn){handlers.resize=fn;}observe(){}}});
 window.SweetCherryEditorComfort.register({surfaces:[surface],surface:()=>surface,capture:()=>({range:[...range],text}),restore(ticket){if(ticket.text!==text)return;range=[...ticket.range];document.activeElement=surface;restores++;},atEnd:()=>range[0]===text.length&&range[1]===text.length});
 const flush=()=>{while(frames.length)frames.shift()();};
 const click=(action,pointer=true)=>{const event={target:buttons.find(b=>b.dataset.editorZoom===action),preventDefault(){},stopPropagation(){}};if(pointer)handlers.pointerdown(event);handlers.click(event);};
 return{handlers,styles,storage,labels,buttons,surface,document,flush,click,restores:()=>restores,range:()=>range,setRange:value=>range=value,setText:value=>text=value};
}
test('initial scroll reaches the end with blank writing room and does not alter text or selection',()=>{
 const s=setup();s.flush();assert.equal(s.surface.scrollTop,1500);assert.equal(s.styles['--editor-writing-gap'],'140px');assert.equal(s.handlers.pageScroll,2000);assert.deepEqual(s.range(),[20,20]);assert.equal(s.restores(),0);
});
test('editor zoom persists separately from reader zoom and preserves selected text for pointer and keyboard use',()=>{
 const s=setup('130');s.setRange([2,7]);s.click('in');assert.equal(s.labels[0].textContent,'140%');assert.equal(s.storage.get('sweetcherry.editorTextZoom'),'140');assert.equal(s.storage.get('sweetcherry.nodeContentZoom'),'170');assert.deepEqual(s.range(),[2,7]);
 s.click('reset',false);assert.equal(s.styles['--editor-text-scale'],'1');assert.deepEqual(s.range(),[2,7]);assert.equal(s.restores(),2);
});
test('invalid or unavailable storage uses safe bounds and remote preference updates do not steal focus',()=>{
 const s=setup('999');assert.equal(s.labels[0].textContent,'100%');for(let i=0;i<30;i++)s.click('out');assert.equal(s.labels[0].textContent,'70%');assert.equal(s.buttons[0].disabled,true);
 s.document.activeElement={};const count=s.restores();s.handlers['window:storage']({key:'sweetcherry.editorTextZoom',newValue:'200'});assert.equal(s.buttons[2].disabled,true);assert.equal(s.restores(),count);
 const privateMode=setup('120',true);privateMode.click('in');assert.equal(privateMode.labels[0].textContent,'110%');
});
test('delayed initial scroll yields to user selections, recovered drafts and dialog focus',()=>{
 for(const change of [s=>s.setRange([1,1]),s=>s.setText('recovered'),s=>s.document.activeElement={}]){const s=setup();change(s);s.flush();assert.equal(s.surface.scrollTop,0);assert.equal(s.handlers.pageScroll,undefined);}
 const s=setup();s.flush();s.surface.scrollTop=0;s.setRange([3,3]);s.handlers['surface:input']();s.flush();assert.equal(s.surface.scrollTop,0);s.setRange([20,20]);s.handlers['surface:input']();s.flush();assert.equal(s.surface.scrollTop,1500);
});
