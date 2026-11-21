const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const {dom}=require('./helpers/symbolDom.cjs'),folder=path.join(__dirname,'../../main/resources/static/js/symbols');
function setup(){const d=dom(),window={},loaded=[],fonts=[],paint=[];const create=d.document.createElement;
 d.document.fonts={load:async(...args)=>fonts.push(args)};
 d.document.createElement=tag=>tag==='canvas'?{getContext(){return{scale(){},measureText(value){return{width:value.length*7,actualBoundingBoxLeft:0,actualBoundingBoxRight:40,actualBoundingBoxAscent:40,actualBoundingBoxDescent:0};},fillText(...args){paint.push(args);},drawImage(...args){paint.push(args);}};},toDataURL:()=> 'data:image/png;base64,c2FmZQ=='}:create(tag);
 const context={window,document:d.document,NodeFilter:{SHOW_TEXT:4},MutationObserver:class{observe(){}},Image:class{set src(value){assert(value.startsWith('/app/img/ctbicons/')||value.startsWith('data:image/png;base64,'),value);loaded.push(value);queueMicrotask(()=>this.onload());}}};
 for(const file of ['catalog.js','inlineSymbols.js','images.js'])vm.runInNewContext(fs.readFileSync(path.join(folder,file),'utf8'),context);
 return{core:window.SweetCherryInlineSymbols,images:window.SweetCherrySymbolImages,loaded,fonts,paint};}
test('canvas symbols use only validated local images and installed fonts, with bounded cached requests',async()=>{
 const s=setup(),ctb=s.core.resolve('ctb:ct_camera'),solid=s.core.resolve('fa-solid fa-image');
 assert.match(await s.images.icon(ctb),/^data:image\/png;base64,/);await s.images.icon(ctb);assert.deepEqual(s.loaded,['/app/img/ctbicons/ct_camera.png']);
 await s.images.icon(solid,'#123456');assert(s.fonts[0][0].includes('900'));assert(s.fonts[0][0].includes('Font Awesome 7 Free'));assert.equal(s.fonts[0][1],solid.glyph);
 await s.images.icon(s.core.resolve('fa-brands fa-github'));assert(s.fonts[1][0].includes('Font Awesome 7 Brands'));
 await assert.rejects(s.images.icon({...solid,value:'{{fa-solid fa-invented}}'}),/Unknown symbol/);
});
test('canvas labels paint icons in text order and bound overlong Unicode labels',async()=>{
 const s=setup();await s.images.label('A {{fa-solid fa-image}} B',{width:155,bold:true});
 assert(s.paint.some(p=>p[0]==='A'));assert(s.paint.some(p=>typeof p[0]==='object'));assert(s.paint.some(p=>p[0]==='B'));
 s.paint.length=0;await s.images.label('😀'.repeat(200),{width:50});assert(s.paint.length<10);assert(s.paint.some(p=>p[0]==='…'));
});
