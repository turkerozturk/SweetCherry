const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const state=require('../../main/resources/static/js/reminders/alarmState.js');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/reminders/alarms.js'),'utf8');
const ID='11111111-1111-4111-8111-111111111111', KEY='22222222-2222-4222-8222-222222222222';
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function element(tag='div'){
 const item={tagName:tag,children:[],listeners:{},isConnected:true,dataset:{},append(...items){this.children.push(...items);},replaceChildren(...items){this.children=items;},addEventListener(name,fn){this.listeners[name]=fn;}};
 Object.defineProperty(item,'innerHTML',{set(){throw Error('Untrusted HTML insertion');}});return item;
}
function tab(shared){
 const enable=element('button'),sound=element('button'),cards=element(),status=element(),form=element('form'),widget=element('details');form.fields=[['_csrf','csrf-token']];
 widget.dataset={endpoint:'/nodes/reminders/alarms',nodes:'/nodes/',token:'view',scope:'Notes',start:'start',stop:'stop',soundOn:'sound',soundOff:'mute',error:'error',stale:'stale',quiet:'quiet',due:'due',disabled:'disabled'};
 const selectors={'[data-alarm-enable]':enable,'[data-alarm-sound]':sound,'[data-alarm-cards]':cards,'[data-alarm-status]':status,form};widget.querySelector=key=>selectors[key];
 const runtime={widget,enable,sound,cards,status,interval:null,cleared:false};
 class Audio{constructor(){this.state='suspended';this.currentTime=0;this.destination={};}async resume(){this.state='running';}async suspend(){this.state='suspended';}createGain(){return {gain:{setValueAtTime(){},linearRampToValueAtTime(){}},connect(){},disconnect(){}};}createOscillator(){return {frequency:{},connect(){},disconnect(){},start(){shared.tones++;},stop(){}};}}
 class Form{constructor(value){this.values=new Map(value.fields);}set(key,value){this.values.set(key,value);}}
 const events={};runtime.events=events;const window={SweetCherryAlarmState:state,AudioContext:Audio,addEventListener(name,fn){events[name]=fn;}};
 const context={window,document:{hidden:false,hasFocus:()=>true,getElementById:()=>widget,createElement:element,addEventListener(){}},navigator:{locks:{request:async(name,options,fn)=>{
  if(shared.locks.has(name))return fn(null);shared.locks.add(name);try{return await fn({name});}finally{shared.locks.delete(name);}
 }}},location:{href:'https://example.test/tree'},URL,AbortController,FormData:Form,localStorage:{getItem:key=>shared.storage.get(key)??null,setItem:(key,value)=>shared.storage.set(key,String(value))},setTimeout:()=>1,clearTimeout(){},setInterval:fn=>{runtime.interval=fn;return 1;},clearInterval(){runtime.cleared=true;},fetch:async(url,options)=>{
  let data,status=200;
  if(shared.stale){data={error:'stale-source'};status=409;}
  else if(options.method==='POST'){shared.posts.push(new Map(options.body.values));shared.feed={...shared.feed,items:[],total:0};data={ok:true};}
  else data=JSON.parse(JSON.stringify(shared.feed));
  return {ok:status===200,status,headers:{get:()=> 'application/json'},json:async()=>data};
 }};
 vm.runInNewContext(source,context);return runtime;
}
function environment(){return {storage:new Map([['sweetcherry-reminder-enabled:Notes','true']]),locks:new Set(),tones:0,posts:[],feed:{storeId:ID,items:[],total:0,canAct:true}};}
const alarm=()=>({id:ID,key:KEY,nodeId:'9223372036854775807',name:'<Node>',message:'<script>text</script>',dueText:'2026-11-17 10:00 UTC',repeat:'yearly'});
test('two tabs sound only once per occurrence, use literal text and send CSRF with server actions',async()=>{
 const shared=environment(),one=tab(shared),two=tab(shared);await tick();await tick();
 await one.sound.listeners.click();await two.sound.listeners.click();assert.equal(shared.tones,4); // two test tones per page
 shared.feed={...shared.feed,items:[alarm()],total:1};await Promise.all([one.interval(),two.interval()]);await two.interval();
 assert.equal(shared.tones,6); // only one two-tone alert, despite both tabs checking
 const card=one.cards.children[0]||two.cards.children[0];assert.equal(card.children[0].textContent,'<Node>');assert.equal(card.children[1].textContent,'<script>text</script>');
 assert.match(card.children[3].children[0].href,/\/nodes\/9223372036854775807\?_tenantView=view$/);
 await card.children[3].children[2].listeners.click();assert.equal(shared.posts.length,1);assert.equal(shared.posts[0].get('_csrf'),'csrf-token');assert.equal(shared.posts[0].get('_tenantView'),'view');assert.equal(shared.posts[0].get('key'),KEY);
});
test('reader dismissal is browser-local and stale tenants stop polling and silence the widget',async()=>{
 const shared=environment();shared.feed={...shared.feed,items:[alarm()],total:1,canAct:false};const page=tab(shared);await tick();await tick();
 const close=page.cards.children[0].children[3].children[1];await close.listeners.click();await page.interval();
 assert.equal(shared.posts.length,0);assert.equal(page.cards.children.length,0);
 shared.stale=true;await page.interval();assert.equal(page.cleared,true);assert.equal(page.status.textContent,'stale');assert.equal(page.enable.disabled,true);
});

test('disabling the shared source preference stops other pages as well', async()=>{
 const shared=environment(),page=tab(shared);await tick();await tick();await page.sound.listeners.click();
 page.events.storage({key:'sweetcherry-reminder-enabled:Notes',newValue:'false'});
 assert.equal(page.enable.textContent,'start');assert.equal(page.sound.disabled,true);assert.equal(page.cards.children.length,0);
});
