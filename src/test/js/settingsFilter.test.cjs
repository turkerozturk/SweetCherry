const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/common/settingsFilter.js'),'utf8');
function page(){
 const row=(label,value)=>({hidden:false,control:{value,disabled:false},querySelectorAll(selector){return selector==='td'?[{textContent:label},{},{textContent:'true'}]:[this.control];}});
 const rows=[row('Hatırlatıcılar myapp.reminders.enabled','true'),row('Görsel alarm myapp.reminders.alarms-enabled','false'),row('Ağaç myapp.tree.default-width','600')];
 const count={dataset:{label:'shown'}},empty={},wrapper={},events={},searchEvents={},clearEvents={};
 const search={value:'',closest:()=>form,addEventListener:(name,fn)=>searchEvents[name]=fn,focus(){this.focused=true;}};
 const selectors={'[data-settings-count]':count,'[data-settings-empty]':empty,'[data-settings-filter]':wrapper,'[data-settings-clear]':{addEventListener:(name,fn)=>clearEvents[name]=fn}};
 const form={querySelectorAll:()=>rows,querySelector:key=>selectors[key],addEventListener:(name,fn)=>events[name]=fn};
 vm.runInNewContext(source,{document:{querySelector:()=>search}});
 return {rows,count,empty,wrapper,search,events,searchEvents,clearEvents,filter(text){search.value=text;searchEvents.input();}};
}
test('settings filter matches Turkish labels, YAML keys and values using all terms',()=>{
 const p=page();assert.equal(p.count.textContent,'3 / 3 shown');assert.equal(p.wrapper.hidden,false);
 p.filter('hatirlaticilar true');assert.deepEqual(p.rows.map(x=>x.hidden),[false,true,true]);
 p.filter('alarms-enabled false');assert.deepEqual(p.rows.map(x=>x.hidden),[true,false,true]);
 p.filter('agac 600');assert.deepEqual(p.rows.map(x=>x.hidden),[true,true,false]);
 p.filter('missing');assert.equal(p.empty.hidden,false);assert.equal(p.count.textContent,'0 / 3 shown');
 assert.ok(p.rows.every(row=>row.control.disabled===false));
 p.clearEvents.click();assert.ok(p.rows.every(row=>!row.hidden));assert.equal(p.search.value,'');
});
test('changed values refresh results, Escape clears and hidden invalid fields become visible',()=>{
 const p=page();p.filter('700');p.rows[2].control.value='700';p.events.change();assert.equal(p.rows[2].hidden,false);
 p.searchEvents.keydown({key:'Escape',preventDefault(){}});assert.ok(p.rows.every(row=>!row.hidden));
 p.filter('missing');let focused=false;p.events.invalid({target:{closest:()=>p.rows[0],focus(){focused=true;}}});
 assert.ok(focused);assert.ok(p.rows.every(row=>!row.hidden));
});
