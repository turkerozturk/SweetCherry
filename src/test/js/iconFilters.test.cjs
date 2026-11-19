const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
test('changing the category submits the existing GET form, retaining search and sort fields',()=>{
 let change,calls=0;const form={requestSubmit(){calls++;}};
 vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/icons/iconFilters.js'),'utf8'),{document:{querySelectorAll:()=>[{form,addEventListener:(type,fn)=>{assert.equal(type,'change');change=fn;}}]}});
 assert.equal(calls,0);change();assert.equal(calls,1);
});
