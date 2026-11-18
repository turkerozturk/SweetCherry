const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/common/restartWatch.js'),'utf8');
async function setup(responses){
 const timers=new Map(),events={},requests=[],destinations=[];let id=0;
 const status={dataset:{waiting:'Waiting',slow:'Still waiting'},textContent:''};
 const context={AbortController,document:{querySelector:s=>s==='[data-restart-watch]'?{dataset:{instance:'old',endpoint:'/app/application-ready',home:'/app/'}}:status},
 window:{addEventListener:(k,fn)=>events[k]=fn},location:{replace:url=>destinations.push(url)},
 setTimeout:(fn,ms)=>{timers.set(++id,{fn,ms});return id;},clearTimeout:id=>timers.delete(id),
 fetch:async(url,options)=>{requests.push({url,options});const answer=responses.shift();if(answer instanceof Error)throw answer;return{ok:true,json:async()=>answer};}};
 vm.runInNewContext(source,context);const settle=()=>new Promise(resolve=>setImmediate(resolve));await settle();
 return{requests,destinations,status,events,async next(){const entry=[...timers].find(([,t])=>t.ms===2000);if(!entry)return;const [key,timer]=entry;timers.delete(key);await timer.fn();await settle();}};
}
test('restart waits for a different ready instance, not the old server or an unready new context',async()=>{
 const c=await setup([{instance:'old',ready:true},{instance:'new',ready:false},{instance:'new',ready:true}]);
 assert.deepEqual(c.destinations,[]);await c.next();assert.deepEqual(c.destinations,[]);await c.next();assert.deepEqual(c.destinations,['/app/']);
 assert.equal(c.requests[0].options.credentials,'omit');assert.equal(c.requests[0].options.cache,'no-store');assert.equal(c.requests[0].options.redirect,'error');
});
test('network failure and malformed readiness keep retrying, leaving the page stops navigation',async()=>{
 const c=await setup([new Error('offline'),{ready:true},{instance:'new',ready:true}]);
 assert.equal(c.status.textContent,'Waiting');await c.next();assert.deepEqual(c.destinations,[]);c.events.pagehide();await c.next();assert.deepEqual(c.destinations,[]);assert.equal(c.requests.length,2);
});
