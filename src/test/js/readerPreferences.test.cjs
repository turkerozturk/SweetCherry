const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const core=require('../../main/resources/static/js/common/readerPreferencesState.js');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/common/readerPreferences.js'),'utf8');
function setup(disabledDelete=false,marker=false){
 const values=new Map(disabledDelete?[['sweetcherry.reader.deletion','false']]:[]),docEvents={},winEvents={},attributes={};let submitted=0,replaced;
 const link={href:'https://example.test/app/nodes/delete/1?view=reader&_tenantView=tenant'};
 const buttons=['creation','duplication','deletion'].map(kind=>({dataset:{confirmToggle:kind},mark:{},setAttribute(k,v){this[k]=v;},querySelector(){return this.mark;}}));
 const style={value:'ivory',getPropertyValue(){return this.value;},setProperty(k,v){this.value=v;}};
 const blank={tagName:'DIV',id:'',children:[{tagName:'BR'}],firstElementChild:{tagName:'BR'},textContent:'',setAttribute(k,v){attributes[k]=v;}};
 const ctx={URL,window:{SweetCherryReaderPreferences:core,localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},addEventListener:(k,fn)=>winEvents[k]=fn},
 location:{href:'https://example.test/app/nodes/delete/1'+(marker?'?_skipNodeConfirmation=1':'')},history:{state:{},replaceState:(s,t,url)=>replaced=url},
 document:{documentElement:{style},body:{},querySelector:s=>s==='[data-node-delete-confirm]'?{requestSubmit(){submitted++;}}:null,
 querySelectorAll:s=>s==='[data-confirm-toggle]'?buttons:s==='a[data-node-delete]'?[link]:s==='[data-node-reading] .rich-text-preview'?[{children:[blank]}]:[],addEventListener:(k,fn)=>docEvents[k]=fn},MutationObserver:class{observe(){}}};
 vm.runInNewContext(source,ctx);
 return{ctx,style,buttons,link,values,attributes,click(button){docEvents.click({target:{closest:()=>button},preventDefault(){}});},storage(){winEvents.storage({key:'sweetcherry.reader.background'});},get submitted(){return submitted;},get replaced(){return replaced;}};
}
test('menu state applies to current and newly visited reading views',()=>{
 const c=setup();assert.equal(c.style.value,'ivory');assert.equal(c.buttons[0]['aria-checked'],'true');
 c.click(c.buttons[0]);assert.equal(c.buttons[0]['aria-checked'],'false');assert.equal(c.buttons[1]['aria-checked'],'true');
 assert.equal(Object.hasOwn(c.attributes,'data-reading-leading-blank'),false);
 c.values.set('sweetcherry.reader.background','#ffeeaa');c.storage();assert.equal(c.style.value,'#ffeeaa');
 c.values.delete('sweetcherry.reader.background');c.storage();assert.equal(c.style.value,'ivory');
});
test('disabled deletion requires an explicit command marker and removes it before POST',()=>{
 const enabled=setup(false,true);assert.equal(enabled.submitted,0);
 const noCommand=setup(true,false);assert.equal(noCommand.submitted,0);assert.equal(new URL(noCommand.link.href).searchParams.get('_skipNodeConfirmation'),'1');
 const fast=setup(true,true);assert.equal(fast.submitted,1);assert.equal(new URL(fast.link.href).searchParams.get('_tenantView'),'tenant');assert.equal(fast.replaced.searchParams.has('_skipNodeConfirmation'),false);
 fast.click(fast.buttons[2]);assert.equal(new URL(fast.link.href).searchParams.has('_skipNodeConfirmation'),false);
});

test('reading preferences preserve leading blank rich-text lines',()=>{
 const c=setup();assert.equal(Object.hasOwn(c.attributes,'data-reading-leading-blank'),false);
 c.click(c.buttons[0]);assert.equal(Object.hasOwn(c.attributes,'data-reading-leading-blank'),false);
});
