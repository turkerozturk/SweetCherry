const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/drawio/renderer.js'),'utf8');
function setup({height=500}={}){
 const listeners={},viewport={clientWidth:800,clientHeight:600,scrollLeft:0,scrollTop:0,listeners:{},addEventListener(name,handler){this.listeners[name]=handler;}},surface={style:{},replaceChildren(){},querySelector:()=>({setAttribute(){}})},reports=[];
 let graph;class Graph{constructor(){graph=this;this.view={scale:1,translate:{x:0,y:0},scaleAndTranslate(scale,x,y){this.scale=scale;this.translate={x,y};},setScale(scale){this.scale=scale;},setTranslate(x,y){this.translate={x,y};}};this.flags={};}
  setEnabled(value){this.flags.enabled=value;}setConnectable(value){this.flags.connectable=value;}setTooltips(value){this.flags.tooltips=value;}setPanning(value){this.flags.panning=value;}getGraphBounds(){return {x:0,y:0,width:1000*this.view.scale,height:height*this.view.scale};}getModel(){return {};}addClickHandler(color,callback){this.linkCallback=callback;eventBus.addListener(doc,'mouseleave',()=>{});}refresh(){}destroy(){this.destroyed=true;}}
 const hiddenClasses=new Set(),body={classList:{toggle(name,on){if(on)hiddenClasses.add(name);else hiddenClasses.delete(name);}}};
 const documentListeners=new Set(),eventBus={consume(event){event.consumed=true;},addListener(node,type,handler){documentListeners.add(handler);},removeListener(node,type,handler){documentListeners.delete(handler);}},doc={body,getElementById:id=>id==='drawio-viewport'?viewport:surface};
 const opened=[];const parent={},port={postMessage:data=>reports.push(data),start(){},close(){}};
 vm.runInNewContext(source,{document:doc,URL,mxEvent:eventBus,window:{parent,navigator:{userActivation:{isActive:true}},open:(...args)=>opened.push(args),addEventListener:(name,handler)=>listeners[name]=handler},Graph,mxStencilRegistry:{},mxStencil:{},mxUtils:{parseXml:()=>({documentElement:{nodeName:'mxGraphModel'}})},mxCodec:class{decode(){}}});
 return {documentListeners,opened,hiddenClasses,reports,listeners,parent,port,viewport,surface,get graph(){return graph;},connect(from=parent){listeners.message({source:from,data:{type:'sweetcherry-drawio-connect'},ports:[port]});},command:data=>port.onmessage({data})};
}
test('renderer accepts only a parent channel and creates a non-editable HTML-label graph',()=>{
 const s=setup();s.connect({});assert.equal(s.reports.length,0);s.connect();assert.equal(s.reports[0].type,'ready');s.command({type:'render',version:4,xml:'<mxGraphModel/>',maxPageBytes:16777216});
 assert.deepEqual(s.graph.flags,{enabled:false,connectable:false,tooltips:false,panning:false});assert.equal(s.graph.isHtmlLabel(),true);assert.equal(s.graph.getLinkForCell(),null);assert.equal(s.graph.getImage(),null);assert.equal(s.graph.mathEnabled,false);assert.equal(s.reports.at(-1).version,4);
});
test('zoom is bounded and a replacement page disposes the previous graph',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});const old=s.graph;for(let n=0;n<40;n++)s.command({type:'zoom',factor:1.2});assert.equal(s.graph.view.scale,4);
 for(let n=0;n<80;n++)s.command({type:'zoom',factor:1/1.2});assert.equal(s.graph.view.scale,.02);s.command({type:'reset'});assert.equal(s.graph.view.scale,1);
 s.command({type:'render',version:2,xml:'<mxGraphModel/>',maxPageBytes:16777216});assert.equal(old.destroyed,true);assert.equal(s.documentListeners.size,1);assert.notEqual(s.graph,old);s.listeners.keydown({key:'Escape'});assert.equal(s.reports.at(-1).type,'escape');
});

test('fit width uses the available width while tall content remains scrollable',()=>{
 const s=setup({height:3000});s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});assert(s.graph.view.scale<.3);s.command({type:'fit-width'});assert.equal(s.graph.view.scale,.752);assert.equal(s.viewport.scrollTop,0);assert(parseFloat(s.surface.style.height)>600);
});
test('ordinary wheel zooms and Shift-wheel retains native scrolling',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});const old=s.graph.view.scale;let prevented=false;
 s.viewport.listeners.wheel({deltaY:-100,shiftKey:false,preventDefault(){prevented=true;}});assert(prevented);assert(s.graph.view.scale>old);
 const current=s.graph.view.scale;s.viewport.listeners.wheel({deltaY:100,shiftKey:true,preventDefault(){assert.fail('Shift-wheel must scroll');}});assert.equal(s.graph.view.scale,current);
});

test('wheel zoom preserves the world point below the pointer at both scroll positions and the left boundary',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});
 s.viewport.getBoundingClientRect=()=>({left:50,top:75});
 function point(x,y){const v=s.graph.view;return [(s.viewport.scrollLeft+x)/v.scale-v.translate.x,(s.viewport.scrollTop+y)/v.scale-v.translate.y];}
 for(const boundary of [false,true]){
  s.viewport.scrollLeft=boundary?0:250;s.viewport.scrollTop=boundary?0:120;
  const before=point(100,150);s.viewport.listeners.wheel({deltaY:boundary?100:-100,clientX:150,clientY:225,shiftKey:false,preventDefault(){}});
  const after=point(100,150);assert(Math.abs(before[0]-after[0])<1e-8);assert(Math.abs(before[1]-after[1])<1e-8);assert(s.viewport.scrollLeft>=0);assert(s.viewport.scrollTop>=0);
 }
});
test('placeholder visibility survives replacement pages through explicit render state',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216,showPlaceholders:false});assert(s.hiddenClasses.has('sc-hide-placeholders'));
 s.command({type:'placeholders',show:true});assert(!s.hiddenClasses.has('sc-hide-placeholders'));
 s.command({type:'render',version:2,xml:'<mxGraphModel/>',maxPageBytes:16777216,showPlaceholders:false});assert(s.hiddenClasses.has('sc-hide-placeholders'));
});

test('safe shape links use synchronous isolated popups and page links use the private channel',()=>{
 const s=setup();s.connect();s.command({type:'render',version:8,xml:'<mxGraphModel/>',maxPageBytes:16777216});
 assert.equal(s.graph.getLinkForCell({scLink:'https://example.test/manual'}),'https://example.test/manual');
 s.graph.openLink('https://example.test/manual');assert.equal(s.opened.length,1);assert.deepEqual(s.opened[0],['https://example.test/manual','_blank','noopener,noreferrer']);
 const event={isTrusted:true};s.graph.linkCallback(event,'data:page/id,page-two');assert.equal(event.consumed,true);assert.equal(s.reports.at(-1).type,'navigate');assert.equal(s.reports.at(-1).link,'data:page/id,page-two');assert.equal(s.reports.at(-1).version,8);
 s.graph.linkCallback({isTrusted:false},'data:page/id,untrusted');assert.equal(s.reports.at(-1).link,'data:page/id,page-two');
 for(const link of ['javascript:alert(1)','data:action/json,{}','data:text/html,evil','https://user:secret@example.test/','/relative','//example.test','file:///secret','https://example.test/\n','https:example.test']){assert.equal(s.graph.getLinkForCell({scLink:link}),null);s.graph.openLink(link);}
 assert.equal(s.opened.length,1);
});
test('native label links keep keyboard and click navigation with safe target and no referrer',()=>{
 const s=setup();s.connect();s.command({type:'render',version:2,xml:'<mxGraphModel/>',maxPageBytes:16777216});
 function click(href,trusted=true){const attributes={href},anchor={getAttribute:key=>attributes[key],setAttribute:(key,value)=>attributes[key]=value};let prevented=false;s.viewport.listeners.click({isTrusted:trusted,target:{closest:()=>anchor},preventDefault(){prevented=true;}});return {attributes,prevented};}
 let result=click('https://example.test/manual');assert.equal(result.prevented,false);assert.equal(result.attributes.target,'_blank');assert.equal(result.attributes.rel,'noopener noreferrer');assert.equal(s.opened.length,0);
 result=click('data:page/id,second');assert.equal(result.prevented,true);assert.equal(s.reports.at(-1).link,'data:page/id,second');
 assert.equal(click('javascript:alert(1)').prevented,true);assert.equal(click('https://example.test/',false).prevented,true);
});
