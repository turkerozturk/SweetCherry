const test = require('node:test'), assert = require('node:assert/strict');
const state = require('../../main/resources/static/js/reminders/alarmState.js');
const id = '11111111-1111-4111-8111-111111111111', key = '22222222-2222-4222-8222-222222222222';
const item = {id, key, nodeId:'9223372036854775807', name:'<Node>', message:'<script>text</script>', dueText:'2026-11-17 10:00 · UTC', repeat:'yearly'};
const feed = () => ({storeId:id, items:[item], total:1, canAct:true});
test('feed retains exact string IDs and rejects unsafe targets, duplicates and malformed data', () => {
 assert.equal(state.valid(feed()),true);
 assert.equal(state.valid({...feed(),items:[{...item,nodeId:'javascript:alert(1)'}]}),false);
 assert.equal(state.valid({...feed(),items:[item,item],total:2}),false);
 assert.equal(state.valid({...feed(),storeId:'../other'}),false);
 assert.equal(state.valid({...feed(),items:[{...item,key:undefined}]}),false);
 assert.equal(state.valid({...feed(),total:5001}),false);
});
test('receipts suppress repeat sounds across serialized tab/page state but keep visible alerts', () => {
 const first = state.present([item],{},100); assert.equal(first.fresh,1);
 const otherTab = state.present([item],JSON.parse(JSON.stringify(first.ledger)),101);
 assert.equal(otherTab.fresh,0);assert.equal(otherTab.rows.length,1);
 const closed = state.dismiss(item,otherTab.ledger,102);
 assert.equal(state.present([item],closed,103).rows.length,0);
 const nextOccurrence = {...item,key:'33333333-3333-4333-8333-333333333333'};
 assert.equal(state.present([nextOccurrence],closed,104).fresh,1);
});
test('new snooze/annual keys re-alert and corrupt receipts are ignored without unbounded growth', () => {
 assert.equal(Object.keys(state.ledger({['__proto__']:{seenAt:1,dismissed:true}})).length,0);
 assert.equal(Object.keys(state.ledger({[state.key(item)]:{seenAt:'bad',dismissed:false}})).length,0);
 let receipts={};for(let n=0;n<1100;n++)receipts[id+'/'+n.toString(16).padStart(8,'0')+'-2222-4222-8222-222222222222']={seenAt:n,dismissed:false};
 assert.equal(Object.keys(state.ledger(receipts)).length,1000);
});
test('newer browser receipts win and a quota failure can keep in-memory dismissal', () => {
 const seen=state.present([item],{},100).ledger, closed=state.dismiss(item,seen,200);
 assert.equal(state.present([item],state.merge(closed,seen),201).rows.length,0);
 assert.equal(state.present([item],state.merge(seen,closed),201).rows.length,0);
});

test('an unarmed background page cannot consume the sound receipt of an armed page', () => {
 const visual = state.present([item],{},100);
 const armed = state.claimSound([item],visual.ledger,101); assert.equal(armed.claimed,true);
 assert.equal(state.claimSound([item],JSON.parse(JSON.stringify(armed.ledger)),102).claimed,false);
 assert.equal(state.present([item],armed.ledger,103).rows.length,1);
});
