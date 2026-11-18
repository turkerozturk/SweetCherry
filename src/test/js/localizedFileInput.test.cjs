const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/common/localizedFileInput.js'),'utf8');
test('file controls use translated captions and literal filenames and retain native file submission',()=>{
 const handlers={},buttonEvents={},formEvents={},name={},controls={hidden:true};let clicked=0;
 const input={hidden:false,files:[],click(){clicked++;},addEventListener:(k,fn)=>handlers[k]=fn,form:{addEventListener:(k,fn)=>formEvents[k]=fn}};
 const browse={focus(){},addEventListener:(k,fn)=>buttonEvents[k]=fn};
 const root={dataset:{empty:'Dosya seçilmedi'},querySelector:s=>({'[data-localized-file]':input,'[data-file-controls]':controls,'[data-file-browse]':browse,'[data-file-name]':name})[s]};
 vm.runInNewContext(source,{document:{querySelectorAll:()=>[root]}});
 assert.equal(input.hidden,true);assert.equal(controls.hidden,false);assert.equal(name.textContent,'Dosya seçilmedi');
 buttonEvents.click();assert.equal(clicked,1);input.files=[{name:'<file>&.zip'}];handlers.change();assert.equal(name.textContent,'<file>&.zip');
 input.files=[];handlers.change();assert.equal(name.textContent,'Dosya seçilmedi');formEvents.reset();assert.equal(name.textContent,'Dosya seçilmedi');
});
