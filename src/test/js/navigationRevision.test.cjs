const test=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');const path=require('node:path');
function tracker(){const window={};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/navigationRevision.js'),'utf8'),{window});return window.SweetCherryNavigationRevision();}
test('unchanged hierarchy preserves loaded branches while a new revision refreshes them',async()=>{
    const t=tracker();let refreshed=0;const refresh=async()=>{refreshed++;};
    assert.equal(await t.check('before',refresh),false);assert.equal(await t.check('before',refresh),false);assert.equal(refreshed,0);
    assert.equal(await t.check('after',refresh),true);assert.equal(refreshed,1);
    assert.equal(await t.check('after',refresh),false);
});
test('failed external-change refresh retries on the next navigation',async()=>{
    const t=tracker();await t.check('before',async()=>{});
    await assert.rejects(t.check('after',async()=>{throw Error('network');}));
    let retried=false;assert.equal(await t.check('after',async()=>{retried=true;}),true);assert.equal(retried,true);
});
test('invalid revision does not discard the previous tree',async()=>{
    const t=tracker();await t.check('before',async()=>{});
    await assert.rejects(t.check(undefined,async()=>{assert.fail();}));
    assert.equal(await t.check('before',async()=>{assert.fail();}),false);
});
