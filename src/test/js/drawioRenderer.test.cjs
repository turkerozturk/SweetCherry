const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/drawio/renderer.js'),'utf8');
function setup(){
 const listeners={},viewport={clientWidth:800,clientHeight:600,scrollLeft:0,scrollTop:0,addEventListener(){}},surface={style:{},replaceChildren(){},querySelector:()=>({setAttribute(){}})},reports=[];
 let graph;class Graph{constructor(){graph=this;this.view={scale:1,translate:{x:0,y:0},scaleAndTranslate(scale,x,y){this.scale=scale;this.translate={x,y};},setScale(scale){this.scale=scale;},setTranslate(x,y){this.translate={x,y};}};this.flags={};}
  setEnabled(value){this.flags.enabled=value;}setConnectable(value){this.flags.connectable=value;}setTooltips(value){this.flags.tooltips=value;}setPanning(value){this.flags.panning=value;}getGraphBounds(){return {x:0,y:0,width:1000*this.view.scale,height:500*this.view.scale};}getModel(){return {};}refresh(){}destroy(){this.destroyed=true;}}
 const parent={},port={postMessage:data=>reports.push(data),start(){},close(){}};
 vm.runInNewContext(source,{document:{getElementById:id=>id==='drawio-viewport'?viewport:surface},window:{parent,addEventListener:(name,handler)=>listeners[name]=handler},Graph,mxStencilRegistry:{},mxStencil:{},mxUtils:{parseXml:()=>({documentElement:{nodeName:'mxGraphModel'}})},mxCodec:class{decode(){}}});
 return {reports,listeners,parent,port,get graph(){return graph;},connect(from=parent){listeners.message({source:from,data:{type:'sweetcherry-drawio-connect'},ports:[port]});},command:data=>port.onmessage({data})};
}
test('renderer accepts only a parent channel and creates a non-editable plain-text graph',()=>{
 const s=setup();s.connect({});assert.equal(s.reports.length,0);s.connect();assert.equal(s.reports[0].type,'ready');s.command({type:'render',version:4,xml:'<mxGraphModel/>'});
 assert.deepEqual(s.graph.flags,{enabled:false,connectable:false,tooltips:false,panning:false});assert.equal(s.graph.isHtmlLabel(),false);assert.equal(s.graph.getLinkForCell(),null);assert.equal(s.graph.getImage(),null);assert.equal(s.graph.mathEnabled,false);assert.equal(s.reports.at(-1).version,4);
});
test('zoom is bounded and a replacement page disposes the previous graph',()=>{
 const s=setup();s.connect();s.command({type:'render',version:1,xml:'<mxGraphModel/>'});const old=s.graph;for(let n=0;n<40;n++)s.command({type:'zoom',factor:1.2});assert.equal(s.graph.view.scale,4);
 for(let n=0;n<80;n++)s.command({type:'zoom',factor:1/1.2});assert.equal(s.graph.view.scale,.02);s.command({type:'reset'});assert.equal(s.graph.view.scale,1);
 s.command({type:'render',version:2,xml:'<mxGraphModel/>'});assert.equal(old.destroyed,true);assert.notEqual(s.graph,old);s.listeners.keydown({key:'Escape'});assert.equal(s.reports.at(-1).type,'escape');
});
