const test=require('node:test'),assert=require('node:assert/strict');
const core=require('../../main/resources/static/js/common/readerPreferencesState.js');
function storage(){const values=new Map();return{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)};}
test('three confirmations default on, persist independently and can be reenabled',()=>{
 const store=storage(),first=core.create(store);
 for(const kind of ['creation','duplication','deletion'])assert.equal(first.confirmationEnabled(kind),true);
 first.toggle('creation');first.toggle('deletion');const next=core.create(store);
 assert.equal(next.confirmationEnabled('creation'),false);assert.equal(next.confirmationEnabled('duplication'),true);assert.equal(next.confirmationEnabled('deletion'),false);
 next.toggle('creation');first.reload();assert.equal(first.confirmationEnabled('creation'),true);
 assert.equal(next.confirmationEnabled('backups'),true);next.toggle('backups');assert.equal(next.confirmationEnabled('backups'),true);
});
test('unavailable storage keeps this page usable and defaults safe',()=>{
 const prefs=core.create({getItem(){throw Error('disabled');},setItem(){throw Error('disabled');},removeItem(){throw Error('disabled');}});
 assert.equal(prefs.confirmationEnabled('creation'),true);prefs.toggle('creation');assert.equal(prefs.confirmationEnabled('creation'),false);
 assert.equal(prefs.setBackground('Ivory'),true);assert.equal(prefs.background(),'ivory');prefs.resetBackground();assert.equal(prefs.background(),null);
});
test('all named colors and hex forms work, CSS expressions and invalid names do not',()=>{
 assert.equal(Object.keys(core.colors).length,148);
 for(const name of Object.keys(core.colors))assert.equal(core.color(name.toUpperCase()),name);
 for(const value of ['#abc','#abcd','#ABCDEF','#ABCDEF00'])assert.equal(core.color(value),value.toLowerCase());
 for(const value of ['url(file:///x)','red;display:none','var(--color)','notacolor','#12','#12345','transparent','currentColor',null])assert.equal(core.color(value),null);
 const store=storage(),prefs=core.create(store);prefs.setBackground('#ffeeaa');assert.equal(core.create(store).background(),'#ffeeaa');
 assert.equal(prefs.setBackground('bad'),false);assert.equal(prefs.background(),'#ffeeaa');prefs.resetBackground();assert.equal(core.create(store).background(),null);
});
