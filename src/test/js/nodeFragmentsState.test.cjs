const test=require('node:test'),assert=require('node:assert/strict');
const core=require('../../main/resources/static/js/common/nodeFragmentsState.js');
test('only current-origin internal node links are eligible, with exact long IDs',()=>{
 const base='https://example.test/tree?nodeId=1';
 for(const href of ['/nodes/2','/nodes/9223372036854775807?_tenantView=old#anchor'])assert.ok(core.target(href,base));
 for(const href of ['https://other.test/nodes/2','javascript:alert(1)','#anchor','/nodes/0','/nodes/1/edit','/nodes/9223372036854775808','/attachments/1/0/download','https://user:pass@example.test/nodes/1'])assert.equal(core.target(href,base),null,href);
 assert.equal(core.target('/context/nodes/9007199254740993',base,'/context/nodes/').id,'9007199254740993');
});
test('fragment responses require string IDs and bounded HTML',()=>{
 const fragment={contentId:'9007199254740993',name:'<node>',syntax:'custom-colors',html:'<p>text</p>'};assert.equal(core.valid(fragment),true);
 for(const value of [{...fragment,contentId:9007199254740992},{...fragment,html:'x'.repeat(2097153)},{...fragment,name:null}])assert.equal(core.valid(value),false);
});
test('request and rendered-character budgets cannot be bypassed by many small links',()=>{
 const budget=core.budget(2,10);assert.equal(budget.reserve(),true);assert.equal(budget.reserve(),true);assert.equal(budget.reserve(),false);
 assert.equal(budget.accept(6),true);assert.equal(budget.accept(5),false);assert.equal(budget.accept(-1),false);assert.equal(budget.accept(4),true);assert.equal(budget.accept(1),false);
});
