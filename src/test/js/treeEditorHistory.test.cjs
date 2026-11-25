const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/node/treeWorkspace.js'),'utf8');
const handler=source.slice(source.indexOf("    window.addEventListener('popstate', event =>"),source.indexOf("    pane.addEventListener('scroll'"));
function setup(){const moves=[],reads=[],replacements=[];let listener,allow=false;
 const context={URL,location:{href:'https://notes.test/tree?nodeId=2'},window:{addEventListener:(name,fn)=>listener=fn},
 inlineEditor:{active:()=>true,canLeave:()=>allow},history:{go:n=>moves.push(n),replaceState:(...args)=>replacements.push(args)},state:{selected:3},historyState:id=>({nodeId:id,scTreeIndex:2}),url:()=>'/tree?nodeId=3',select:(...args)=>reads.push(args)};
 vm.createContext(context);vm.runInContext('let historyIndex=2,restoringHistory=false;\n'+handler,context);
 return {moves,reads,replacements,pop:state=>listener({state}),set allow(value){allow=value;}};
}
test('rejecting browser Back restores the original history entry without loading another node',()=>{
 const c=setup();c.pop({scTreeIndex:1});assert.deepEqual(c.moves,[1]);assert.equal(c.reads.length,0);
 c.pop({scTreeIndex:2});assert.equal(c.reads.length,0);c.allow=true;c.pop({scTreeIndex:1});assert.deepEqual(c.reads,[['2',false,true]]);
});
test('rejecting browser Forward moves back instead of replacing the forward history chain',()=>{
 const c=setup();c.pop({scTreeIndex:3});assert.deepEqual(c.moves,[-1]);assert.equal(c.replacements.length,0);assert.equal(c.reads.length,0);
});
