const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const code=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/drawioPreview.js'),'utf8');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function fixture({status=200,limit="",maxPageBytes=16777216,pages=[{name:'First',xml:'<mxGraphModel/>'},{name:'<img onerror=evil()>',xml:'<mxGraphModel/>'}]}={}){
 function el(){const classes=new Set();return {listeners:{},dataset:{},disabled:true,textContent:'',children:[],selectedIndex:0,attributes:{},classList:{contains:x=>classes.has(x),add:x=>classes.add(x),remove:x=>classes.delete(x)},addEventListener(){},setAttribute(k,v){this.attributes[k]=v;},replaceChildren(){this.children=[];},append(child){this.children.push(child);},set innerHTML(_){throw Error('Unsafe HTML');}};}
 function node(){const out=el();out.addEventListener=(name,handler)=>out.listeners[name]=handler;return out;}
 const keys=['iframe','[data-drawio-pages]','[data-drawio-status]','[data-drawio-filename]','[data-drawio-scale]','[data-drawio-panel]','[data-drawio-fullscreen]','[data-drawio-prev]','[data-drawio-next]'];const nodes=Object.fromEntries(keys.map(k=>[k,node()]));
 const buttons=['fit','reset','zoom','zoom'].map(type=>Object.assign(node(),{dataset:{drawioCommand:type,factor:'in'}}));
 const root=node();root.dataset={endpoint:'/app/attachments/42/7/drawio/data?_tenantView=current',loading:'Loading',error:'Error',stale:'Stale',limit:'Limit exceeded:'};root.querySelector=key=>nodes[key];root.querySelectorAll=()=>buttons;
 const document=node();document.getElementById=()=>root;document.createElement=()=>node();const window=node();
 let framePort;const messages=[],requests=[];
 class Channel{constructor(){const one={start(){},close(){},postMessage(data){messages.push(data);}},two={postMessage(data){one.onmessage?.({data});}};this.port1=one;this.port2=two;}}
 nodes.iframe.dataset.src='/app/attachments/drawio/frame?_tenantView=current';nodes.iframe.contentWindow={postMessage(data,target,ports){assert.equal(data.type,'sweetcherry-drawio-connect');assert.equal(target,'*');framePort=ports[0];}};
 vm.runInNewContext(code,{window,document,MessageChannel:Channel,AbortController,setTimeout:()=>1,clearTimeout(){},fetch:async(url,options)=>{requests.push({url,options});return {ok:status===200,status,headers:{get:()=> 'application/json'},json:async()=>status===422?{error:'large',limit}:{filename:'diagram.drawio',pages,maxPageBytes}};}});
 return {nodes,buttons,messages,requests,window,document,connect(){nodes.iframe.listeners.load();framePort.postMessage({type:'ready'});},reply(data){framePort.postMessage(data);}};
}
test('multiple page names remain literal text and selection sends only that page to the isolated renderer',async()=>{
 const s=fixture();await tick();s.connect();const select=s.nodes['[data-drawio-pages]'];assert.equal(select.children.length,2);assert.equal(select.children[1].textContent,'2. <img onerror=evil()>');assert.equal(s.messages[0].type,'render');assert(!JSON.stringify(s.messages[0]).includes('current'));
 select.selectedIndex=1;select.listeners.change();assert.equal(s.messages.at(-1).version,2);assert.equal(s.requests.length,1);assert.equal(s.requests[0].options.redirect,'error');
});
test('late render replies cannot change zoom/status for a different selected page',async()=>{
 const s=fixture();await tick();s.connect();s.nodes['[data-drawio-next]'].listeners.click();s.reply({type:'rendered',version:1,scale:4});assert.equal(s.nodes['[data-drawio-scale]'].textContent,'');
 s.reply({type:'rendered',version:2,scale:.5});assert.equal(s.nodes['[data-drawio-scale]'].textContent,'50%');assert.equal(s.nodes['[data-drawio-status]'].textContent,'');assert(s.buttons.every(button=>!button.disabled));
});
test('fit and zoom commands stay local and fullscreen fallback can be closed from the frame',async()=>{
 const s=fixture();await tick();s.connect();s.buttons[0].listeners.click();assert.equal(s.messages.at(-1).type,'fit');s.buttons[2].listeners.click();assert.equal(s.messages.at(-1).factor,1.2);
 await s.nodes['[data-drawio-fullscreen]'].listeners.click();assert(s.nodes['[data-drawio-panel]'].classList.contains('drawio-expanded'));s.reply({type:'escape'});assert(!s.nodes['[data-drawio-panel]'].classList.contains('drawio-expanded'));assert.equal(s.requests.length,1);
});
test('stale source requests display an error without rendering any diagram',async()=>{
 const s=fixture({status:409});await tick();s.connect();assert.equal(s.nodes['[data-drawio-status]'].textContent,'Stale');assert.equal(s.messages.length,0);assert(s.buttons.every(button=>button.disabled));
});

test('limit errors identify the adjustable YAML setting',async()=>{
 const s=fixture({status:422,limit:'max-page-bytes'});await tick();assert.equal(s.nodes['[data-drawio-status]'].textContent,'Limit exceeded: myapp.drawio-preview.max-page-bytes');assert.equal(s.messages.length,0);
});
test('server-configured page budgets work above the old browser caps',async()=>{
 const pages=Array.from({length:256},(_,n)=>({name:'Page '+n,xml:n===0?'<mxGraphModel>'+(' '.repeat(2*1024*1024))+'</mxGraphModel>':'<mxGraphModel/>'}));
 const s=fixture({pages});await tick();s.connect();assert.equal(s.nodes['[data-drawio-pages]'].children.length,256);assert.equal(s.messages[0].maxPageBytes,16777216);assert(s.messages[0].xml.length>2*1024*1024);
});
