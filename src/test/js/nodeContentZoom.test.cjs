const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function zoom(stored='100',blocked=false) {
 const events={},buttons=['out','reset','in'].map(nodeZoom=>({dataset:{nodeZoom}})),labels=[{}];let scale;
 const storage={getItem(){if(blocked)throw Error();return stored;},setItem(_key,value){if(blocked)throw Error();stored=value;}};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/common/nodeContentZoom.js'),'utf8'),{localStorage:storage,window:{addEventListener:(type,fn)=>events[type]=fn},document:{documentElement:{style:{setProperty:(_key,value)=>scale=value}},querySelectorAll:selector=>selector==='[data-node-zoom-value]'?labels:buttons,addEventListener:(type,fn)=>events[type]=fn}});
 return {buttons,labels,scale:()=>scale,stored:()=>stored,events,click(index){events.click({target:{closest:()=>buttons[index]},preventDefault(){},stopImmediatePropagation(){}});}};
}
test('zoom persists across navigation, resets and stays within bounds',()=>{
 const z=zoom('130');assert.equal(z.scale(),'1.3');z.click(2);assert.equal(z.stored(),'140');
 assert.equal(zoom(z.stored()).labels[0].textContent,'140%');z.click(1);assert.equal(z.scale(),'1');
 for(let n=0;n<40;n++)z.click(2);assert.equal(z.scale(),'2');assert.equal(z.buttons[2].disabled,true);
 for(let n=0;n<40;n++)z.click(0);assert.equal(z.scale(),'0.7');assert.equal(z.buttons[0].disabled,true);
});
test('invalid or unavailable storage safely defaults; other tabs update zoom',()=>{
 for(const value of ['NaN','10000','95'])assert.equal(zoom(value).scale(),'1');
 const z=zoom('120',true);z.click(2);assert.equal(z.scale(),'1.1');
 z.events.storage({key:'sweetcherry.nodeContentZoom',newValue:'160'});assert.equal(z.scale(),'1.6');
});
