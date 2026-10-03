const test=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');const path=require('node:path');
test('language switching preserves the selected node, tenant token, options and anchor',()=>{
    let handler,replaced;
    function $(selector){return selector==='#locales'?{change(fn){handler=fn;},val(){return 'en_US';}}:{ready(fn){fn();}};}
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/selectlanguage.js'),'utf8'),{
        $,URL,document:{},window:{location:{href:'https://example.test/tree?nodeId=53&_tenantView=token&lang=tr_TR#anchor',replace(value){replaced=value;}}}});
    handler();const result=new URL(replaced);
    assert.equal(result.pathname,'/tree');assert.equal(result.searchParams.get('nodeId'),'53');
    assert.equal(result.searchParams.get('_tenantView'),'token');assert.equal(result.searchParams.get('lang'),'en_US');assert.equal(result.hash,'#anchor');
});
