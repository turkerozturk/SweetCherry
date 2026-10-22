const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function setup(fetcher){
 const handlers={},panelHandlers={};const body={textContent:'',children:[],replaceChildren(node){this.children=[node];}};
 const panel={dataset:{loading:'Loading',error:'Failed'},querySelector:()=>body,addEventListener:(name,fn)=>panelHandlers[name]=fn};
 let shown=0;const bootstrap={Offcanvas:{getOrCreateInstance:()=>({show(){shown++;}})}};
 const info={links:[],querySelectorAll(){return this.links;}};
 const document={getElementById:()=>panel,addEventListener:(name,fn)=>handlers[name]=fn,importNode:node=>node};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/timeline.js'),'utf8'),{document,window:{bootstrap,location:{href:'https://example.test/nodes/timeline/modified'}},bootstrap,URL,AbortController,
  DOMParser:class{parseFromString(value){return{querySelector:()=>value==='metadata'?info:null};}},fetch:fetcher});
 const click=extra=>({target:{closest:()=>({href:'https://example.test/nodes/10/info?_tenantView=tenant'})},button:0,preventDefault(){},...extra});
 return{handlers,panelHandlers,body,info,click,get shown(){return shown;}};
}
test('node info opens a drawer, fetches with session and keeps tenant tokens on its links',async()=>{
 let options;const x=setup(async(url,opts)=>{options=opts;return{ok:true,redirected:false,text:async()=> 'metadata'};});
 const anchor={getAttribute:()=>'/nodes/11'};x.info.links.push(anchor);await x.handlers.click(x.click());
 assert.equal(x.shown,1);assert.equal(options.credentials,'same-origin');assert.equal(options.cache,'no-store');assert.equal(x.body.children[0],x.info);assert.equal(anchor.href,'/nodes/11?_tenantView=tenant');
});
test('redirected sign-in and unexpected HTML are never inserted',async()=>{
 for(const response of [{ok:true,redirected:true},{ok:true,redirected:false,text:async()=>'<html>Login</html>'}]){
  const x=setup(async()=>response);await x.handlers.click(x.click());assert.equal(x.body.textContent,'Failed');assert.equal(x.body.children.length,0);
 }
});
test('superseded metadata response cannot replace the newer selection',async()=>{
 const requests=[];const x=setup((url,opts)=>new Promise(resolve=>requests.push({opts,resolve})));
 const first=x.handlers.click(x.click()),second=x.handlers.click(x.click());assert.equal(requests[0].opts.signal.aborted,true);
 requests[1].resolve({ok:true,redirected:false,text:async()=> 'metadata'});await second;
 requests[0].resolve({ok:true,redirected:false,text:async()=> '<html>Old</html>'});await first;
 assert.equal(x.body.children[0],x.info);
});
test('modified clicks keep normal browser navigation',async()=>{let called=false;const x=setup(async()=>{called=true;});await x.handlers.click(x.click({ctrlKey:true}));assert.equal(called,false);assert.equal(x.shown,0);});
