const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/drawio/renderer.js'),'utf8');
function setup({height=500}={}){
 const listeners={},viewport={clientWidth:800,clientHeight:600,scrollLeft:0,scrollTop:0,listeners:{},addEventListener(name,handler){this.listeners[name]=handler;}},surface={style:{},replaceChildren(){},querySelector:()=>({setAttribute(){}})},reports=[];
 let graph;class Graph{constructor(){graph=this;this.view={scale:1,translate:{x:0,y:0},scaleAndTranslate(scale,x,y){this.scale=scale;this.translate={x,y};},setScale(scale){this.scale=scale;},setTranslate(x,y){this.translate={x,y};}};this.flags={};}
  setEnabled(value){this.flags.enabled=value;}setConnectable(value){this.flags.connectable=value;}setTooltips(value){this.flags.tooltips=value;}setPanning(value){this.flags.panning=value;}getGraphBounds(){return {x:0,y:0,width:1000*this.view.scale,height:height*this.view.scale};}getModel(){return {};}refresh(){}destroy(){this.destroyed=true;}}
 const parent={},port={postMessage:data=>reports.push(data),start(){},close(){}};
 vm.runInNewContext(source,{document:{getElementById:id=>id==='drawio-viewport'?viewport:surface},window:{parent,addEventListener:(name,handler)=>listeners[name]=handler},Graph,mxStencilRegistry:{},mxStencil:{},mxUtils:{parseXml:()=>({documentElement:{nodeName:'mxGraphModel'}})},mxCodec:class{decode(){}}});
 return {reports,listeners,parent,port,viewport,surface,get graph(){return graph;},connect(from=parent){listeners.message({source:from,data:{type:'sweetcherry-drawio-connect'},ports:[port]});},command:data=>port.onmessage({data})};
}
test('renderer accepts only a parent channel and creates a non-editable HTML-label graph',()=>{
 const s=setup();s.connect({});assert.equal(s.reports.length,0);s.connect();assert.equal(s.reports[0].type,'ready');s.command({type:'render',version:4,xml:'<mxGraphModel/>',maxPageBytes:16777216});
 assert.deepEqual(s.graph.flags,{enabled:false,connectable:false,tooltips:false,panning:false});assert.equal(s.graph.isHtmlLabel(),true);assert.equal(s.graph.getLinkForCell(),null);assert.equal(s.graph.getImage(),null);assert.equal(s.graph.mathEnabled,false);assert.equal(s.reports.at(-1).version,4);
});
test('zoom is bounded and a replacement page disposes the previous graph',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});const old=s.graph;for(let n=0;n<40;n++)s.command({type:'zoom',factor:1.2});assert.equal(s.graph.view.scale,4);
 for(let n=0;n<80;n++)s.command({type:'zoom',factor:1/1.2});assert.equal(s.graph.view.scale,.02);s.command({type:'reset'});assert.equal(s.graph.view.scale,1);
 s.command({type:'render',version:2,xml:'<mxGraphModel/>',maxPageBytes:16777216});assert.equal(old.destroyed,true);assert.notEqual(s.graph,old);s.listeners.keydown({key:'Escape'});assert.equal(s.reports.at(-1).type,'escape');
});

test('fit width uses the available width while tall content remains scrollable',()=>{
 const s=setup({height:3000});s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});assert(s.graph.view.scale<.3);s.command({type:'fit-width'});assert.equal(s.graph.view.scale,.752);assert.equal(s.viewport.scrollTop,0);assert(parseFloat(s.surface.style.height)>600);
});
test('ordinary wheel zooms and Shift-wheel retains native scrolling',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>',maxPageBytes:16777216});const old=s.graph.view.scale;let prevented=false;
 s.viewport.listeners.wheel({deltaY:-100,shiftKey:false,preventDefault(){prevented=true;}});assert(prevented);assert(s.graph.view.scale>old);
 const current=s.graph.view.scale;s.viewport.listeners.wheel({deltaY:100,shiftKey:true,preventDefault(){assert.fail('Shift-wheel must scroll');}});assert.equal(s.graph.view.scale,current);
});
