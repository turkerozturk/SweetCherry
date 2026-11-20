const test=require('node:test'),assert=require('node:assert/strict');
const {install}=require('../../main/resources/static/js/node/nodeWebBookmarks.js');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
function setup(fetcher){
 const frame={attrs:{sandbox:'allow-scripts'},hidden:true,setAttribute(k,v){this.attrs[k]=v;},removeAttribute(k){delete this[k];}},status={},button={listeners:{},addEventListener(k,v){this.listeners[k]=v;}};
 const root={isConnected:true,dataset:{endpoint:'/nodes/web-bookmarks/9',tenantView:'current',loading:'Loading',error:'Error',stale:'Stale'},querySelector:key=>key==='[data-web-bookmark-frame]'?frame:key==='[data-web-bookmark-status]'?status:button};
 const events={},calls=[];let scan;
 const doc={body:{},querySelectorAll:()=>root.isConnected?[root]:[],addEventListener(k,v){events[k]=v;}};
 const win={document:doc,location:{href:'http://app/tree'},fetch:async(url,options)=>{calls.push({url,options});return fetcher(calls.length,options);},MutationObserver:class{constructor(callback){scan=callback;}observe(){}},addEventListener(k,v){events[k]=v;}};
 install(win);return {root,frame,status,button,events,calls,scan:()=>scan()};
}
const response=html=>({ok:true,status:200,headers:{get:()=> 'application/json'},json:async()=>({html})});
test('inserting a bookmark fetches once with the current token and only assigns sandboxed srcdoc',async()=>{
 const c=setup(()=>response('<p>Safe</p>'));await flush();
 assert.equal(c.calls.length,1);assert.equal(c.calls[0].url.searchParams.get('_tenantView'),'current');assert.equal(c.calls[0].options.cache,'no-store');assert.equal(c.calls[0].options.redirect,'error');
 assert.equal(c.frame.srcdoc,'<p>Safe</p>');assert.equal(c.frame.attrs.sandbox,'');assert.equal(c.frame.hidden,false);assert.equal(c.status.textContent,'');
 c.scan();c.events['sweetcherry:content-loaded']();await flush();assert.equal(c.calls.length,1);
 c.button.listeners.click();await flush();assert.equal(c.calls.length,2);
});
test('detaching a node aborts its request and a late result cannot update the old view',async()=>{
 let finish;const c=setup(()=>new Promise(resolve=>finish=resolve));c.root.isConnected=false;c.scan();assert.equal(c.calls[0].options.signal.aborted,true);
 finish(response('<p>Old</p>'));await flush();assert.equal(c.frame.srcdoc,undefined);
});
test('stale sessions disable retries and invalid responses leave the frame hidden',async()=>{
 const stale=setup(()=>({ok:false,status:409}));await flush();assert.equal(stale.status.textContent,'Stale');assert.equal(stale.button.disabled,true);assert.equal(stale.frame.hidden,true);
 const invalid=setup(()=>({ok:true,status:200,headers:{get:()=> 'text/html'}}));await flush();assert.equal(invalid.status.textContent,'Error');assert.equal(invalid.frame.hidden,true);
 const bad=setup(()=>response({html:'not a string'}));await flush();assert.equal(bad.status.textContent,'Error');assert.equal(bad.frame.srcdoc,undefined);
});
test('superseded loads cannot overwrite a newer response and cached-page restoration reloads',async()=>{
 const pending=[];const c=setup(()=>new Promise(resolve=>pending.push(resolve)));
 c.button.listeners.click();pending[1](response('<p>New</p>'));await flush();pending[0](response('<p>Old</p>'));await flush();assert.equal(c.frame.srcdoc,'<p>New</p>');
 c.events.pagehide();assert.equal(c.calls.at(-1).options.signal.aborted,true);c.events.pageshow({persisted:true});assert.equal(c.calls.length,3);pending[2](response('<p>Fresh</p>'));await flush();assert.equal(c.frame.srcdoc,'<p>Fresh</p>');
});
