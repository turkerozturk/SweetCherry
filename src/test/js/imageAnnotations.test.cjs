const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const core=require('../../main/resources/static/js/imageAnnotationDrawing.js');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
test('coordinates stay in original image pixels when the displayed canvas is scaled or scrolled',()=>{
 assert.deepEqual(core.point(60,45,{left:10,top:20,width:100,height:50},1000,500),{x:500,y:250});
 assert.deepEqual(core.point(-10,500,{left:10,top:20,width:100,height:50},1000,500),{x:0,y:500});
});
test('undo, redo and reset retain drawing objects rather than storing large bitmap histories',()=>{
 const history=core.history(),a={tool:'arrow'},b={tool:'rect'};history.add(a);history.add(b);history.undo();assert.deepEqual(history.marks,[a]);history.redo();assert.deepEqual(history.marks,[a,b]);
 history.reset();assert.deepEqual(history.marks,[]);history.undo();assert.deepEqual(history.marks,[a,b]);history.undo();history.add({tool:'pen'});assert.equal(history.redoable,false);
});
test('arrows have two heads and rectangles/ellipses draw correctly in reverse drag directions',()=>{
 const calls=[];const ctx=new Proxy({}, {get:(obj,key)=>obj[key]||((...args)=>calls.push([key,...args])),set:(obj,key,value)=>{obj[key]=value;return true;}});
 const shape={tool:'arrow',width:4,color:'#e01b24',points:[{x:0,y:0},{x:100,y:0}]};core.draw(ctx,shape);assert.equal(calls.filter(v=>v[0]==='lineTo').length,3);
 calls.length=0;core.draw(ctx,{...shape,tool:'rect',points:[{x:100,y:50},{x:10,y:20}]});assert.deepEqual(calls.find(v=>v[0]==='rect'),['rect',10,20,90,30]);
 calls.length=0;core.draw(ctx,{...shape,tool:'ellipse',points:[{x:100,y:50},{x:10,y:20}]});assert.deepEqual(calls.find(v=>v[0]==='ellipse').slice(0,5),['ellipse',55,35,45,15]);
});
class Element {
 constructor(){this.events={};this.disabled=false;this.value='';this.open=false;}
 addEventListener(type,handler){this.events[type]=handler;}
 click(){return this.events.click?.({});}
 showModal(){this.open=true;}
 close(){this.open=false;}
}
function setup(postStatus=204,openStatus=200){
 const names=['canvas','status','controls','undo','redo','preview','dialog','save','cancel','tool','color','width','reset','result','reload'];
 const items=Object.fromEntries(names.map(name=>[name,new Element()]));const calls=[],windowEvents={},docEvents={},revoked=[];
 const ctx={save(){},restore(){},clearRect(){},drawImage(){},beginPath(){},moveTo(){},lineTo(){},stroke(){},rect(){},ellipse(){},arc(){},fill(){}};
 Object.assign(items.canvas,{width:0,height:0,getContext:()=>ctx,focus(){},setPointerCapture(){},getBoundingClientRect:()=>({left:0,top:0,width:200,height:100}),toBlob:fn=>queueMicrotask(()=>fn(new Blob(['PNG'],{type:'image/png'})))});
 items.tool.value='arrow';items.color.value='#ff0000';items.width.value='4';
 const root={dataset:{endpoint:'/image-gallery/1/7/annotation',tenantView:'current',loading:'Loading',error:'Error',readonly:'Readonly',unsupported:'Unsupported',confirm:'Confirm',leave:'Leave?',conflict:'Conflict',saved:'Saved'},querySelector:selector=>selector.includes('input[name="_csrf"]')?{value:'csrf'}:items[selector.slice('[data-annotation-'.length,-1)]};
 const doc={getElementById:()=>root,addEventListener:(key,value)=>{docEvents[key]=value;}};
 const win={SweetCherryImageDrawing:core,addEventListener:(key,value)=>{windowEvents[key]=value;},confirm:()=>true};
 const location={href:'http://app/image-gallery/1/7/annotate',reload(){}};
 class Url extends URL {static createObjectURL(){return 'blob:preview';}static revokeObjectURL(value){revoked.push(value);}}
 class Image {constructor(){this.naturalWidth=20;this.naturalHeight=10;}set src(value){queueMicrotask(()=>this.onload());}}
 const fetch=async(url,options)=>{calls.push({url,options});if(options.method==='POST')return {ok:postStatus===204,status:postStatus};return {ok:openStatus===200,status:openStatus,headers:{get:()=> 'application/json'},json:async()=>({revision:'a'.repeat(64),width:20,height:10,png:'data:image/png;base64,AAA='})};};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/imageAnnotations.js'),'utf8'),{window:win,document:doc,location,URL:Url,Image,fetch,Blob,AbortController,queueMicrotask});
 function stroke(){items.canvas.events.pointerdown({pointerId:1,button:0,clientX:10,clientY:10,preventDefault(){}});items.canvas.events.pointermove({pointerId:1,clientX:100,clientY:60});items.canvas.events.pointerup({pointerId:1,clientX:100,clientY:60});}
 return {items,calls,stroke,windowEvents,docEvents,revoked};
}
test('drawing, undo and cancelled preview never post; only explicit overwrite sends PNG with revision, CSRF and confirmation',async()=>{
 const c=setup();await flush();c.stroke();assert.equal(c.calls.length,1);c.items.undo.click();assert.equal(c.items.preview.disabled,true);c.items.redo.click();
 await c.items.preview.click();assert.equal(c.items.dialog.open,true);assert.equal(c.calls.length,1);c.items.cancel.click();assert.equal(c.calls.length,1);
 await c.items.preview.click();await c.items.save.click();assert.equal(c.calls.length,2);
 const request=c.calls[1];assert.equal(request.url.searchParams.get('_tenantView'),'current');assert.equal(request.url.searchParams.get('confirmed'),'true');assert.equal(request.url.searchParams.get('revision'),'a'.repeat(64));assert.equal(request.options.headers['X-CSRF-TOKEN'],'csrf');assert.equal(request.options.headers['Content-Type'],'image/png');assert.ok(request.options.body instanceof Blob);
 assert.equal(c.items.status.textContent,'Saved');assert.equal(c.items.dialog.open,false);assert.equal(c.items.controls.disabled,true);assert.ok(c.revoked.includes('blob:preview'));
});
test('a conflict keeps unsaved drawings protected and read-only sources never enable drawing or saving',async()=>{
 const c=setup(409);await flush();c.stroke();await c.items.preview.click();await c.items.save.click();assert.equal(c.items.status.textContent,'Conflict');let prevented=false;c.windowEvents.beforeunload({preventDefault(){prevented=true;}});assert.equal(prevented,true);
 const locked=setup(204,403);await flush();locked.stroke();await locked.items.preview.click();assert.equal(locked.calls.length,1);assert.equal(locked.items.status.textContent,'Readonly');assert.equal(locked.items.dialog.open,false);
});
