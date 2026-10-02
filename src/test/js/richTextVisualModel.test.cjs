const test=require('node:test'),assert=require('node:assert/strict');
const model=require('../../main/resources/static/js/node/richTextVisualModel.js');
function text(value){return {nodeType:3,nodeValue:value};}
function element(tag,children=[],attributes){const node={nodeType:1,tagName:tag,dataset:{},childNodes:children};if(attributes)node.dataset.richAttributes=JSON.stringify(attributes);return node;}
function root(children){const node=element('DIV',children);node.contains=target=>{const visit=n=>n===target||n.childNodes&&n.childNodes.some(visit);return !!visit(node);};return node;}

test('visual paragraphs preserve newline, Unicode and combined/unknown attributes',()=>{
 const editor=root([element('DIV',[element('SPAN',[text('A😀')],{weight:'heavy',future:'keep'}),element('SPAN',[text('B')],{style:'italic'})]),element('DIV',[element('SPAN',[text('C')],{})])]);
 const runs=model.scan(editor).runs;
 assert.equal(runs.map(run=>run.text).join(''),'A😀B\nC');assert.equal(runs[0].attributes.future,'keep');assert.equal(runs[1].attributes.style,'italic');
});
test('blank and trailing paragraphs preserve line separators',()=>{
 const editor=root([element('DIV',[element('SPAN',[text('A')],{})]),element('DIV',[element('BR')]),element('DIV',[element('BR')])]);
 assert.equal(model.scan(editor).runs.map(run=>run.text).join(''),'A\n\n');
});
test('empty attributed spans keep metadata',()=>{
 const editor=root([element('DIV',[element('SPAN',[],{future:'empty'})])]);
 assert.deepEqual(model.scan(editor).runs,[{text:'',attributes:{future:'empty'}}]);
});
test('visual selection translates UTF-16 offsets to code-point positions',()=>{
 const value=text('A😀B'),editor=root([element('DIV',[element('SPAN',[value],{})])]);
 editor.ownerDocument={getSelection:()=>({rangeCount:1,anchorNode:value,anchorOffset:1,focusNode:value,focusOffset:3})};
 assert.deepEqual(model.selection(editor),[1,2]);
});
test('restored visual selection never splits a surrogate pair',()=>{
 const value=text('A😀B'),editor=root([element('DIV',[element('SPAN',[value],{})])]);let start,end;
 editor.ownerDocument={createRange:()=>({setStart(node,offset){start=[node,offset];},setEnd(node,offset){end=[node,offset];}}),getSelection:()=>({removeAllRanges(){},addRange(){}})};
 model.restore(editor,1,2);assert.equal(start[0],value);assert.equal(start[1],1);assert.equal(end[1],3);
});

test('protected object DOM becomes one marker, not its image or label text',()=>{
 const slot=element('SPAN',[element('IMG')]);slot.dataset.richObject='image:12';
 const editor=root([element('DIV',[element('SPAN',[text('A')],{}),slot,element('SPAN',[text('B')],{})])]);
 const runs=model.scan(editor).runs;
 assert.equal(runs.map(run=>run.text).join(''),'A\uFFFCB');
 assert.deepEqual(runs[1].attributes,{__sweet_object:'image:12'});
});

test('caret pads around a lone object never enter the text or offset model',()=>{
 const left=text('\u200B'),right=text('\u200B');
 const a=element('SPAN',[left]),b=element('SPAN',[right]);a.dataset.richCaret=b.dataset.richCaret='true';
 const slot=element('SPAN',[text('Codebox')]);slot.dataset.richObject='codebox:0';
 const editor=root([element('DIV',[a,slot,b])]);
 assert.equal(model.scan(editor).runs.map(run=>run.text).join(''),'\uFFFC');
 editor.ownerDocument={getSelection:()=>({rangeCount:1,anchorNode:right,anchorOffset:1,focusNode:right,focusOffset:1})};
 assert.deepEqual(model.selection(editor),[1,1]);
});
