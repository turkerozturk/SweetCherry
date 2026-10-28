const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
function editor(recoveryEnabled=false) {
 const handlers={},els={};let dirty,recovery;
 for(const id of ['nodeText','plainEditForm','plainUndo','plainRedo','plainSeparator'])els[id]={value:id==='nodeText'?'A😀B':'',selectionStart:0,selectionEnd:0,addEventListener:(type,fn)=>handlers[id+':'+type]=fn,focus(){},setSelectionRange(a,b){this.selectionStart=a;this.selectionEnd=b;},setRangeText(value,a,b){this.value=this.value.slice(0,a)+value+this.value.slice(b);this.selectionStart=this.selectionEnd=a+value.length;}};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/plainTextEditor.js'),'utf8'),{document:{getElementById:id=>els[id]},window:{EditorRecovery:recoveryEnabled?{install:(form,adapter)=>recovery=adapter}:undefined,EditorLeaveGuard:{install:(form,check)=>dirty=check}}});
 const fire=(id,type,event={})=>handlers[id+':'+type](event);
 return {els,fire,recovery:()=>recovery,dirty:()=>dirty(),type(value){fire('nodeText','beforeinput',{inputType:'insertText'});els.nodeText.value=value;els.nodeText.selectionStart=els.nodeText.selectionEnd=value.length;fire('nodeText','input');}};
}
test('plain undo/redo tracks typed text, selection and a new redo branch',()=>{
 const e=editor();assert.equal(e.dirty(),false);e.type('Changed');assert.equal(e.dirty(),true);
 e.fire('plainUndo','click');assert.equal(e.els.nodeText.value,'A😀B');assert.equal(e.dirty(),false);
 e.fire('plainRedo','click');assert.equal(e.els.nodeText.value,'Changed');
 e.fire('plainUndo','click');e.type('New');assert.equal(e.els.plainRedo.disabled,true);
});
test('separator and native/key shortcuts share history; composition is one undo',()=>{
 const e=editor();e.els.nodeText.selectionStart=1;e.els.nodeText.selectionEnd=3;e.fire('plainSeparator','click');
 assert.equal(e.els.nodeText.value,'A'+'~'.repeat(33)+'\nB');
 e.fire('nodeText','beforeinput',{inputType:'historyUndo',preventDefault(){}});assert.equal(e.els.nodeText.value,'A😀B');
 e.fire('nodeText','keydown',{key:'Z',metaKey:true,shiftKey:true,preventDefault(){}});assert.equal(e.dirty(),true);
 e.fire('plainUndo','click');e.fire('nodeText','compositionstart');e.type('İ');e.type('İş');e.fire('nodeText','compositionend');e.fire('plainUndo','click');assert.equal(e.els.nodeText.value,'A😀B');
});

test('plain recovery restores text, cursor and undo/redo without submitting or refreshing',()=>{
 const e=editor(true);e.type('First draft');e.type('Second draft');
 const state=JSON.parse(JSON.stringify(e.recovery().snapshot()));
 const fresh=editor(true);fresh.recovery().restore(state);assert.equal(fresh.els.nodeText.value,'Second draft');
 fresh.fire('plainUndo','click');assert.equal(fresh.els.nodeText.value,'First draft');
 fresh.fire('plainRedo','click');assert.equal(fresh.els.nodeText.value,'Second draft');assert.equal(fresh.dirty(),true);
});
