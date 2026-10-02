const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

/** Executes the real editor script against a small DOM double to test model/history behavior. */
function editor(initialRuns, maxFileBytes) {
    const handlers = new Map();
    function element(id = '') {
        return {id, value:'', style:{}, dataset:{}, childNodes:[], selectionStart:0, selectionEnd:0,
            hidden:true, disabled:false, addEventListener(type, fn) {handlers.set(id + ':' + type, fn);},
            replaceChildren() {this.childNodes = [];}, appendChild(child) {this.childNodes.push(child);},
            showModal() {this.open=true;}, close() {this.open=false;}, click() {const handler=handlers.get(id+':click');if(handler)handler({});},
            focus() {}, setSelectionRange(start, end) {this.selectionStart = start; this.selectionEnd = end;}};
    }
    const ids = Object.fromEntries(['richText','richPreview','richEditForm','selectionHint','richXml','clearFormat','richUndo','richRedo','richImages','richAddImage','richImageFile','richImageStatus','richFiles','richAddFile','richFilePicker','richLink','richLinkDialog','richLinkUrl','richLinkError','richLinkApply','richLinkRemove','richLinkCancel'].map(id => [id, element(id)]));
    const bold = element('bold'); bold.dataset = {format:'weight', value:'heavy'};
    const align = element('align'); align.dataset = {choice:'justification'};
    let saved;
    const document = {
        getElementById: id => ids[id],
        querySelectorAll: selector => selector === '[data-format]' ? [bold] : selector === '[data-choice]' ? [align] : [],
        createElement: () => element(),
        createTextNode: text => ({text}),
        implementation: {createDocument() {
            const root = element();
            return {documentElement:root, createElement() {
                return {attributes:{}, setAttribute(key, value) {this.attributes[key] = value;}, appendChild(text) {this.text = text.text;}};
            }, createTextNode(text) {return {text};}};
        }}
    };
    const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/node/richTextEditor.js'), 'utf8');
    vm.runInNewContext(source, {document, URL, window:{richEditorDocument:{runs:initialRuns || [{text:'A😀B\nC', attributes:{future:'keep'}}]},richEditorObjects:{},richEditorMaxFileBytes:maxFileBytes},
        crypto:{randomUUID:()=> 'test-uuid'},
        FileReader:class {readAsDataURL(file){this.result=file.data;this.onload();}},
        XMLSerializer:class {serializeToString(xml) {saved = JSON.parse(JSON.stringify(xml.documentElement.childNodes)); return '<node/>';}}});
    function fire(id, type, event = {}) {handlers.get(id + ':' + type)(event);}
    function select(start, end) {ids.richText.selectionStart = start; ids.richText.selectionEnd = end; fire('richText','select');}
    function runs() {fire('richEditForm','submit'); return saved;}
    function key(key, shiftKey = false) {
        let prevented = false;
        fire('richEditForm','keydown', {key, ctrlKey:true, shiftKey, preventDefault() {prevented = true;}});
        assert.equal(prevented, true);
    }
    function type(value) {
        fire('richText','beforeinput', {inputType:'insertText'});
        ids.richText.value = value;
        ids.richText.selectionStart = ids.richText.selectionEnd = value.length;
        fire('richText','input');
    }
    return {ids, align, fire, select, runs, key, type};
}

function text(runs) {return runs.map(run => run.text).join('');}

test('undo and redo restore formatting and unknown attributes with Unicode selection', () => {
    const e = editor(); e.select(1,3); e.fire('bold','click');
    assert.equal(e.runs()[1].attributes.weight, 'heavy');
    e.key('z'); assert.equal(e.runs().length, 1); assert.equal(e.runs()[0].attributes.future, 'keep');
    e.key('y'); assert.equal(e.runs()[1].attributes.weight, 'heavy'); assert.equal(text(e.runs()), 'A😀B\nC');
});

test('undo text insertion restores the previous formatting, redo restores insertion', () => {
    const e = editor(); e.select(1,3); e.fire('bold','click'); e.type('A😀XB\nC');
    e.key('z'); assert.equal(text(e.runs()), 'A😀B\nC'); assert.equal(e.runs()[1].attributes.weight, 'heavy');
    e.key('y'); assert.equal(text(e.runs()), 'A😀XB\nC');
});

test('a new edit after undo discards the previous redo branch', () => {
    const e = editor(); e.select(1,3); e.fire('bold','click'); e.key('z'); e.type('New');
    assert.equal(e.ids.richRedo.disabled, true); e.key('y'); assert.equal(text(e.runs()), 'New');
});

test('Ctrl Shift Z and toolbar buttons share the same history', () => {
    const e = editor(); e.select(1,3); e.fire('bold','click'); e.fire('richUndo','click');
    e.key('z',true); assert.equal(e.runs()[1].attributes.weight, 'heavy');
    e.fire('richUndo','click'); assert.equal(e.ids.richUndo.disabled, true);
});

test('native history requests use the complete editor model', () => {
    const e = editor(); e.select(1,3); e.fire('bold','click');
    let prevented = false;
    e.fire('richText','beforeinput',{inputType:'historyUndo', preventDefault() {prevented = true;}});
    assert.equal(prevented,true); assert.equal(e.runs().length,1);
});

test('preview aligns each paragraph and keeps mixed inline formatting', () => {
    const e = editor(); e.select(0,4); e.align.value = 'center'; e.fire('align','change');
    assert.equal(e.ids.richPreview.childNodes[0].style.textAlign,'center');
    assert.equal(e.ids.richPreview.childNodes[1].style.textAlign,'left');
    e.select(1,3); e.fire('bold','click');
    assert.equal(e.ids.richPreview.childNodes[0].style.textAlign,'center');
    assert.equal(e.ids.richPreview.childNodes[0].childNodes[1].style.fontWeight,'bold');
});


test('rendering keeps a final empty paragraph after Enter', () => {
    const e = editor(); e.type('A\n');
    assert.equal(e.ids.richPreview.childNodes.length,2);
    assert.equal(e.ids.richPreview.childNodes[1].childNodes.length,1);
    assert.equal(text(e.runs()),'A\n');
});

test('copy/paste omits protected object characters without removing existing slots', () => {
    const e=editor([{text:'A',attributes:{}},{text:'\uFFFC',attributes:{__sweet_object:'image:1'}},{text:'B',attributes:{}}]);
    e.select(0,3);let copied;
    e.fire('richText','copy',{preventDefault(){},clipboardData:{setData(type,value){copied=value;}}});
    assert.equal(copied,'AB');
    e.select(3,3);e.fire('richText','paste',{preventDefault(){},clipboardData:{files:[],getData(){return 'X\uFFFCY';}}});
    assert.equal(text(e.runs()),'A\uFFFCBXY');
    assert.equal(e.runs().filter(run=>run.attributes.__sweet_object).length,1);
});
test('new image slot and submitted payload follow undo/redo together', async () => {
    const e=editor();e.select(1,1);
    e.ids.richImageFile.files=[{type:'image/png',size:3,data:'data:image/png;base64,AQID',name:'test.png'}];
    e.fire('richImageFile','change');await new Promise(resolve=>setImmediate(resolve));
    assert.equal(text(e.runs()),'A\uFFFC😀B\nC');
    assert.deepEqual(JSON.parse(e.ids.richImages.value),{'new-image:test-uuid':'data:image/png;base64,AQID'});
    e.key('z');assert.equal(text(e.runs()),'A😀B\nC');assert.deepEqual(JSON.parse(e.ids.richImages.value),{});
    e.key('y');assert.equal(text(e.runs()),'A\uFFFC😀B\nC');assert.ok(JSON.parse(e.ids.richImages.value)['new-image:test-uuid']);
});

test('attachment binary/name stay paired with its slot through undo/redo',async()=>{
 const e=editor();e.select(1,1);
 e.ids.richFilePicker.files=[{type:'application/octet-stream',size:3,name:'Türkçe test.bin',data:'data:application/octet-stream;base64,AQID'}];
 e.fire('richFilePicker','change');await new Promise(resolve=>setImmediate(resolve));
 assert.equal(text(e.runs()),'A\uFFFC😀B\nC');
 assert.deepEqual(JSON.parse(e.ids.richFiles.value),{'new-file:test-uuid':{name:'Türkçe test.bin',data:'AQID'}});
 e.key('z');e.runs();assert.deepEqual(JSON.parse(e.ids.richFiles.value),{});
 e.key('y');e.runs();assert.equal(JSON.parse(e.ids.richFiles.value)['new-file:test-uuid'].name,'Türkçe test.bin');
});
test('tenant size limit blocks both image and attachment before file reading',async()=>{
 const e=editor(undefined,2);e.select(1,1);
 const file={type:'image/png',size:3,name:'a.png',data:'data:image/png;base64,AQID'};
 e.ids.richImageFile.files=[file];e.fire('richImageFile','change');
 e.ids.richFilePicker.files=[file];e.fire('richFilePicker','change');
 await new Promise(resolve=>setImmediate(resolve));
 assert.equal(text(e.runs()),'A😀B\nC');assert.deepEqual(JSON.parse(e.ids.richFiles.value),{});assert.deepEqual(JSON.parse(e.ids.richImages.value),{});
});

test('external link creation, removal and undo preserve formatted selected text',()=>{
 const e=editor();e.select(1,3);e.fire('bold','click');e.fire('richLink','click');
 assert.equal(e.ids.richLinkDialog.open,true);
 e.ids.richLinkUrl.value='https://example.org/?a=1&b=2';e.fire('richLinkApply','click');
 assert.equal(e.runs()[1].attributes.link,'webs https://example.org/?a=1&b=2');
 assert.equal(e.runs()[1].attributes.weight,'heavy');
 e.fire('richLink','click');e.fire('richLinkRemove','click');assert.equal(e.runs()[1].attributes.link,undefined);
 e.key('z');assert.equal(e.runs()[1].attributes.link,'webs https://example.org/?a=1&b=2');
});
test('caret edits the target across adjacent differently formatted link runs',()=>{
 const e=editor([{text:'AB',attributes:{link:'webs https://example.org/',weight:'heavy'}},{text:'C',attributes:{link:'webs https://example.org/',style:'italic'}},{text:'D',attributes:{}}]);
 e.select(1,1);e.fire('richLink','click');assert.equal(e.ids.richLinkUrl.value,'https://example.org/');
 e.ids.richLinkUrl.value='https://example.net/';e.fire('richLinkApply','click');
 const runs=e.runs();assert.equal(runs[0].attributes.link,'webs https://example.net/');assert.equal(runs[1].attributes.link,'webs https://example.net/');
 assert.equal(runs[0].attributes.weight,'heavy');assert.equal(runs[1].attributes.style,'italic');assert.equal(runs[2].attributes.link,undefined);
});
test('invalid link targets and cancel never alter content',()=>{
 const e=editor();e.select(0,1);e.fire('richLink','click');
 e.ids.richLinkUrl.value='javascript:alert(1)';e.fire('richLinkApply','click');assert.equal(e.ids.richLinkError.hidden,false);
 assert.equal(e.ids.richLinkDialog.open,true);assert.equal(e.runs()[0].attributes.link,undefined);
 e.fire('richLinkCancel','click');assert.equal(e.ids.richLinkDialog.open,false);assert.equal(text(e.runs()),'A😀B\nC');
});
test('remove external link skips protected objects and preserves internal links',()=>{
 const e=editor([{text:'A',attributes:{link:'webs https://example.org/'}},{text:'\uFFFC',attributes:{__sweet_object:'image:1'}},{text:'B',attributes:{link:'node 12'}}]);
 e.select(0,3);e.fire('richLink','click');e.fire('richLinkRemove','click');
 const runs=e.runs();assert.equal(runs[0].attributes.link,undefined);assert.equal(runs[1].attributes.__sweet_object,'image:1');assert.equal(runs[2].attributes.link,'node 12');
});
