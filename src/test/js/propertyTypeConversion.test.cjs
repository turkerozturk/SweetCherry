const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/propertyTypeConversion.js'),'utf8');
function setup(fetcher=async()=>({ok:true,redirected:false,headers:{get:()=> 'application/json'},json:async()=>({revision:'a'.repeat(64)})})){
 const events={},buttons={};let saved=0,focused=false,type='PLAIN_TEXT';
 const form={dataset:{richText:'true',conversionUrl:'/nodes/properties/1/conversion',conversionLoading:'Checking',conversionFailed:'Failed'},elements:{confirmRichTextConversion:{value:'false'},conversionRevision:{value:''},_tenantView:{value:'view'}},querySelector:()=>({value:type}),addEventListener:(name,fn)=>events[name]=fn,requestSubmit(){let cancelled=false;events.submit({preventDefault(){cancelled=true;}});if(!cancelled)saved++;}};
 const dialog={close(){this.open=false;},showModal(){this.open=true;},addEventListener:(name,fn)=>buttons[name]=fn};
 const yes={addEventListener:(_,fn)=>buttons.yes=fn},no={addEventListener:(_,fn)=>buttons.no=fn,focus(){focused=true;}},status={};
 const elements={nodePropertiesForm:form,nodeConversionDialog:dialog,nodeConversionStatus:status,nodeConversionYes:yes,nodeConversionNo:no};
 vm.runInNewContext(source,{document:{getElementById:id=>elements[id]},location:{href:'https://notes.example/nodes/properties/1'},fetch:fetcher,URL,AbortController,setTimeout,clearTimeout});
 return {form,dialog,buttons,status,submit:()=>events.submit({preventDefault(){}}),saved:()=>saved,focused:()=>focused,setType:v=>type=v};
}
const tick=()=>new Promise(r=>setImmediate(r));
test('cancel defaults to No and never submits a destructive change',async()=>{const s=setup();const pending=s.submit();await tick();assert.equal(s.dialog.open,true);assert.equal(s.focused(),true);s.buttons.no();await pending;assert.equal(s.saved(),0);assert.equal(s.form.elements.confirmRichTextConversion.value,'false');assert.equal(s.form.inert,false);});
test('confirmation submits the verified revision exactly once',async()=>{const s=setup();const pending=s.submit();await tick();s.buttons.yes();await pending;assert.equal(s.saved(),1);assert.equal(s.form.elements.confirmRichTextConversion.value,'true');assert.equal(s.form.elements.conversionRevision.value,'a'.repeat(64));});
test('Escape cancels and leaves the form available',async()=>{const s=setup();const pending=s.submit();await tick();s.buttons.cancel({preventDefault(){}});await pending;assert.equal(s.saved(),0);assert.equal(s.form.inert,false);});
test('failed or redirected verification cannot submit or open confirmation',async()=>{for(const response of [{ok:false},{ok:true,redirected:true},{ok:true,revision:'invalid'}]){const s=setup(async()=>({...response,headers:{get:()=> 'application/json'},json:async()=>({revision:response.revision||'a'.repeat(64)})}));await s.submit();assert.equal(s.saved(),0);assert.equal(s.dialog.open,undefined);assert.equal(s.status.textContent,'Failed');assert.equal(s.form.inert,false);}});
test('keeping Rich Text does not ask for conversion',async()=>{let requests=0;const s=setup(async()=>{requests++;throw Error();});s.setType('RICH_TEXT');await s.submit();assert.equal(requests,0);assert.equal(s.dialog.open,undefined);});
