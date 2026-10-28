const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

/** Executes the real editor script against a small DOM double to test model/history behavior. */
function editor(initialRuns, maxFileBytes, objects, clipboard, visual = false, options = {}) {
    const handlers = new Map();
    function element(id = '') {
        return {id, value:'', style:{}, dataset:{}, childNodes:[], selectionStart:0, selectionEnd:0,
            hidden:true, disabled:false, addEventListener(type, fn) {handlers.set(id + ':' + type, fn);},
            replaceChildren(...nodes) {this.childNodes = nodes;}, appendChild(child) {this.childNodes.push(child);},
            showModal() {this.open=true;}, close() {this.open=false;}, click() {const handler=handlers.get(id+':click');if(handler)handler({});},
            setAttribute() {}, contains(target) { return this.childNodes.some(n => n === target || n.contains && n.contains(target)); },
            focus() {}, setSelectionRange(start, end) {this.selectionStart = start; this.selectionEnd = end;}};
    }
    const ids = Object.fromEntries(['richPasteImage','richClipboardHint','richAutoLink','richAutoLinkError','richSeparator','richObjects','richAllowDeletion','richDeletionIcon','richObjectDialog','richObjectList','richObjectDelete','richObjectCancel','richDeleted','richDeletionHint','richText','richPreview','richEditForm','selectionHint','richXml','clearFormat','richUndo','richRedo','richImages','richAddImage','richImageFile','richImageStatus','richFiles','richAddFile','richFilePicker','richLink','richLinkDialog','richLinkUrl','richLinkError','richLinkApply','richLinkRemove','richLinkCancel','richTables','richTable','richTableDialog','richTableRows','richTableColumns','richTableCells','richTableError','richTableApply','richTableResize','richTableCancel','richPreviewTitle','richTextLabel'].map(id => [id, element(id)]));
    if (options.codeboxes) {
        for (const id of ['richCodebox','richCodeboxDialog','richCodeboxText','richCodeboxSearch','richCodeboxLanguages','richCodeboxSelected','richCodeboxError','richCodeboxCancel','richCodeboxApply','richCodeboxes']) ids[id]=element(id);
        ids.richCodeboxLanguages.querySelectorAll=()=>[];
        ids.richCodeboxSearch.dispatchEvent=()=>{};
    }
    if (visual) { ids.richVisualMode=element('richVisualMode'); ids.richVisualMode.checked=true; }
    const bold = element('bold'); bold.dataset = {format:'weight', value:'heavy'};
    const align = element('align'); align.dataset = {choice:'justification'};
    const symbol=element('symbol');symbol.dataset={insertSymbol:'☐'};
    let saved, dirty, windowObject, recovery, uuid=0;
    ids.richEditForm.querySelector=()=>({value:"initial-revision"});
    const document = {
        getElementById: id => ids[id],
        querySelectorAll: selector => selector === '[data-insert-symbol]' ? [symbol] : selector === '[data-format]' ? [bold] : selector === '[data-choice]' ? [align] : [],
        createElement: () => element(),
        createDocumentFragment: () => element(),
        createTextNode: text => ({text}),
        implementation: {createDocument() {
            const root = element();
            return {documentElement:root, createElement() {
                return {attributes:{}, setAttribute(key, value) {this.attributes[key] = value;}, appendChild(text) {this.text = text.text;}};
            }, createTextNode(text) {return {text};}};
        }}
    };
    const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/node/richTextEditor.js'), 'utf8');
    vm.runInNewContext(source, {document, URL, Event:class {}, window:windowObject={EditorRecovery:options.recovery?{install:(form,adapter)=>recovery=adapter}:undefined,isSecureContext:options.secure!==false,RichTextEditorSupport:require('../../main/resources/static/js/node/richTextEditorSupport.js'),RichTextImageClipboard:require('../../main/resources/static/js/node/richTextImageClipboard.js'),EditorLeaveGuard:{install:(_form,check)=>{dirty=check;}},richEditorDocument:{runs:initialRuns || [{text:'A😀B\nC', attributes:{future:'keep'}}]},richEditorObjects:objects || {},RichTextClipboard:clipboard,richEditorMaxFileBytes:maxFileBytes,
        RichTextVisualModel:visual ? {selection:()=>[Array.from(ids.richText.value.slice(0,ids.richText.selectionStart)).length,Array.from(ids.richText.value.slice(0,ids.richText.selectionEnd)).length],restore:(_,a,b)=>{ids.richText.selectionStart=Array.from(ids.richText.value).slice(0,a).join('').length;ids.richText.selectionEnd=Array.from(ids.richText.value).slice(0,b).join('').length;}} : null},
        crypto:options.crypto||{randomUUID:()=> uuid++===0?'test-uuid':'test-uuid-'+uuid},
        navigator:{clipboard:options.clipboardReader},
        FileReader:class {readAsDataURL(file){this.result=file.data;this.onload();}},
        XMLSerializer:class {serializeToString(xml) {saved = JSON.parse(JSON.stringify(xml.documentElement.childNodes)); return '<node/>';}}});
    function fire(id, type, event = {}) {return handlers.get(id + ':' + type)(event);}
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
    return {ids, align, fire, select, runs, key, type, symbol, recovery:()=>recovery,dirty:()=>dirty(), internal:()=>windowObject.RichTextInternalLinkBridge, bridge:()=>windowObject.RichTextImportBridge};
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

test('new table cells and protected slot travel together through undo/redo',()=>{
 const e=editor();e.select(1,1);e.fire('richTable','click');
 const rows=e.ids.richTableCells.childNodes[0].childNodes;
 rows[0].childNodes[0].childNodes[0].value='Header';rows[1].childNodes[0].childNodes[0].value='Body';
 e.fire('richTableApply','click');assert.equal(text(e.runs()),'A\uFFFC😀B\nC');
 assert.equal(JSON.parse(e.ids.richTables.value)['new-table:test-uuid'][0][0],'Header');
 e.key('z');e.runs();assert.deepEqual(JSON.parse(e.ids.richTables.value),{});
 e.key('y');e.runs();assert.equal(JSON.parse(e.ids.richTables.value)['new-table:test-uuid'][1][0],'Body');
});
test('table cell edits and cancel preserve the existing slot with independent history',()=>{
 const e=editor([{text:'\uFFFC',attributes:{__sweet_object:'grid:0'}}],undefined,{'grid:0':{label:'Table',tableRows:[['Original']]}});
 const slot=e.ids.richPreview.childNodes[0].childNodes.find(node=>node.dataset.richObject);
 slot.childNodes[0].click();assert.equal(e.ids.richTableRows.disabled,true);
 e.ids.richTableCells.childNodes[0].childNodes[0].childNodes[0].childNodes[0].value='Changed';
 e.fire('richTableApply','click');e.runs();assert.equal(JSON.parse(e.ids.richTables.value)['grid:0'][0][0],'Changed');
 e.key('z');e.runs();assert.deepEqual(JSON.parse(e.ids.richTables.value),{});assert.equal(text(e.runs()),'\uFFFC');
 e.key('y');e.runs();assert.equal(JSON.parse(e.ids.richTables.value)['grid:0'][0][0],'Changed');
 const next=e.ids.richPreview.childNodes[0].childNodes.find(node=>node.dataset.richObject);next.childNodes[0].click();
 e.ids.richTableCells.childNodes[0].childNodes[0].childNodes[0].childNodes[0].value='Cancelled';
 e.fire('richTableCancel','click');e.runs();assert.equal(JSON.parse(e.ids.richTables.value)['grid:0'][0][0],'Changed');
});
test('table resize preserves cells and refuses to discard nonempty removed cells',()=>{
 const e=editor();e.fire('richTable','click');
 e.ids.richTableCells.childNodes[0].childNodes[2].childNodes[2].childNodes[0].value='Keep';
 e.ids.richTableRows.value=1;e.ids.richTableColumns.value=1;e.fire('richTableResize','click');
 assert.equal(e.ids.richTableError.hidden,false);assert.equal(e.ids.richTableCells.childNodes[0].childNodes.length,3);
 e.ids.richTableRows.value=4;e.ids.richTableColumns.value=4;e.fire('richTableResize','click');
 assert.equal(e.ids.richTableError.hidden,true);assert.equal(e.ids.richTableCells.childNodes[0].childNodes[2].childNodes[2].childNodes[0].value,'Keep');
});

test('formatted copy/paste keeps attributes and participates in the same undo history',()=>{
 const clipboard=require('../../main/resources/static/js/node/richTextClipboard.js');
 const e=editor(undefined,undefined,undefined,clipboard);e.select(1,3);e.fire('bold','click');
 const data={};e.fire('richText','copy',{preventDefault(){},clipboardData:{setData(type,value){data[type]=value;}}});
 assert.equal(data['text/plain'],'😀');assert.match(data['text/html'],/font-weight:bold/);
 e.select(5,5);e.fire('richText','paste',{preventDefault(){},clipboardData:{files:[],getData:type=>data[type]||''}});
 assert.equal(text(e.runs()),'A😀B\n😀C');assert.equal(e.runs().find(run=>run.text==='😀'&&run.attributes.weight==='heavy').attributes.weight,'heavy');
 e.key('z');assert.equal(text(e.runs()),'A😀B\nC');e.key('y');assert.equal(text(e.runs()),'A😀B\n😀C');
});
test('structured HTML paste creates table payloads together with styled text as one undo step',()=>{
 const clipboard={...require('../../main/resources/static/js/node/richTextClipboard.js'),fromHtml:()=>[
  {text:'Heading\n',attributes:{scale:'h2',foreground:'#3584e4'}},{table:[['Header'],['Body']]},{text:'After',attributes:{style:'italic'}}]};
 const e=editor(undefined,undefined,undefined,clipboard);e.select(0,0);
 e.fire('richText','paste',{preventDefault(){},clipboardData:{files:[],getData:type=>type==='text/html'?'<table/>':''}});
 assert.equal(text(e.runs()),'Heading\n\uFFFCAfterA😀B\nC');
 assert.deepEqual(JSON.parse(e.ids.richTables.value)['new-table:test-uuid'],[['Header'],['Body']]);
 e.key('z');assert.equal(text(e.runs()),'A😀B\nC');assert.deepEqual(JSON.parse(e.ids.richTables.value),{});
 e.key('y');assert.equal(e.runs()[0].attributes.scale,'h2');
});

const objectRuns = () => [{text:'A',attributes:{}},{text:'\uFFFC',attributes:{__sweet_object:'image:1'}},{text:'B',attributes:{}}];
test('object dialog is locked by default and cannot remove a selected widget', () => {
    const e=editor(objectRuns()); e.fire('richObjects','click');
    assert.equal(e.ids.richObjectDelete.disabled,true);
    e.ids.richObjectList.childNodes[0].childNodes[0].checked=true;
    e.fire('richObjectDelete','click'); assert.equal(text(e.runs()),'A\uFFFCB');
    assert.equal(e.ids.richDeleted.value,'[]');
});
test('explicit deletion and its request are restored together by undo and redo', () => {
    const e=editor(objectRuns());e.ids.richAllowDeletion.checked=true;e.fire('richAllowDeletion','change');
    e.fire('richObjects','click');e.ids.richObjectList.childNodes[0].childNodes[0].checked=true;
    e.fire('richObjectDelete','click');assert.equal(text(e.runs()),'AB');
    assert.equal(e.ids.richText.value,'AB');
    assert.deepEqual(JSON.parse(e.ids.richDeleted.value),['image:1']);
    e.key('z');assert.equal(text(e.runs()),'A\uFFFCB');assert.equal(e.ids.richDeleted.value,'[]');
    e.key('y');assert.equal(text(e.runs()),'AB');assert.equal(e.ids.richDeleted.value,'["image:1"]');
});
test('relocking a staged deletion prevents saving', () => {
    const e=editor(objectRuns());e.ids.richAllowDeletion.checked=true;e.fire('richObjects','click');
    e.ids.richObjectList.childNodes[0].childNodes[0].checked=true;e.fire('richObjectDelete','click');
    e.ids.richAllowDeletion.checked=false;let blocked=false;
    e.fire('richEditForm','submit',{preventDefault(){blocked=true;}});
    assert.equal(blocked,true);assert.equal(e.ids.richDeletionHint.hidden,false);
});
test('removing a pending object sends no database deletion reference', () => {
    const e=editor([{text:'\uFFFC',attributes:{__sweet_object:'new-image:test'}}]);
    e.ids.richAllowDeletion.checked=true;e.fire('richObjects','click');
    e.ids.richObjectList.childNodes[0].childNodes[0].checked=true;e.fire('richObjectDelete','click');
    assert.equal(text(e.runs()),'');assert.equal(e.ids.richDeleted.value,'[]');
});

 test('separator inserts 33 tildes plus newline and shares rich-text undo history', () => {
    const e=editor([{text:'A😀B',attributes:{weight:'heavy',future:'keep'}}]);
    e.select(1,3); e.fire('richSeparator','click');
    assert.equal(text(e.runs()),'A'+'~'.repeat(33)+'\nB');
    assert.equal(e.runs()[1].attributes.weight,'heavy');
    e.fire('richUndo','click'); assert.equal(text(e.runs()),'A😀B');
    e.fire('richRedo','click'); assert.equal(text(e.runs()),'A'+'~'.repeat(33)+'\nB');
});

test('only leading visual boxes cycle, preserving attributes and undo/redo across formatted runs', () => {
    const e=editor([{text:'  ',attributes:{}},{text:'☐ Task',attributes:{weight:'heavy',future:'keep'}},{text:'\nx ☐ text\n😀 ☐ text\n\t☑ done\n☒ failed',attributes:{}}],null,null,null,true);
    const boxes=()=>{const found=[];function walk(n){if(n.dataset&&n.dataset.checkOffset!==undefined)found.push(n);for(const c of n.childNodes||[])walk(c);}walk(e.ids.richPreview);return found;};
    assert.deepEqual(boxes().map(b=>b.textContent),['☐','☑','☒']);
    const original=text(e.runs());
    for(const expected of ['☑','☒','☐']) {
        const box=boxes()[0];e.fire('richPreview','click',{target:{closest:()=>box},detail:1,preventDefault(){}});
        assert.equal(Array.from(text(e.runs()))[2],expected);
        assert.equal(e.runs().find(r=>r.text===expected).attributes.future,'keep');
    }
    assert.equal(text(e.runs()),original);
    e.fire('richUndo','click');assert.equal(Array.from(text(e.runs()))[2],'☒');
    e.fire('richRedo','click');assert.equal(text(e.runs()),original);
});


test('dirty state follows content and undo, ignoring selections and mode controls', () => {
    const e=editor([{text:'Original',attributes:{}}]);
    assert.equal(e.dirty(),false);e.select(0,4);assert.equal(e.dirty(),false);
    e.type('Changed');assert.equal(e.dirty(),true);
    e.key('z');assert.equal(e.dirty(),false);e.key('y');assert.equal(e.dirty(),true);
    e.key('z');e.select(0,4);e.fire('bold','click');assert.equal(e.dirty(),true);
    e.fire('bold','click');assert.equal(e.dirty(),false); // split runs alone are not edits
});
test('inserted list symbols retain selection, formatting and undo',()=>{
    const e=editor([{text:'A😀B',attributes:{weight:'heavy'}}]);
    for(const symbol of ['☐','☑','☒','•']) {
        e.select(1,3);e.symbol.dataset.insertSymbol=symbol;e.fire('symbol','click');
        assert.equal(text(e.runs()),'A'+symbol+' B');assert.equal(e.dirty(),true);
        e.key('z');assert.equal(text(e.runs()),'A😀B');assert.equal(e.dirty(),false);
    }
});
test('selected URL shortcut leaves external-link dialog intact and rejects unsafe addresses',()=>{
    for(const value of ['https://example.org/a?b=1','www.example.org','javascript:alert(1)','file:///tmp/a','not a url']) {
        const e=editor([{text:value,attributes:{weight:'heavy'}}]);e.select(0,value.length);e.fire('richAutoLink','click');
        if(value.startsWith('https:')||value.startsWith('www.')) {
            assert.equal(e.runs()[0].attributes.link,'webs '+new URL(value.startsWith('www.')?'https://'+value:value).href);
            assert.equal(e.runs()[0].attributes.weight,'heavy');e.key('z');assert.equal(e.dirty(),false);
        } else {assert.equal(e.ids.richAutoLinkError.hidden,false);assert.equal(e.dirty(),false);}
        e.select(0,value.length);e.fire('richLink','click');assert.equal(e.ids.richLinkDialog.open,true);
    }
});


test('clipboard image items with an empty files list insert an image instead of image-only HTML',async()=>{
 const e=editor();e.select(1,1);
 const file={type:'image/png',size:3,data:'data:image/png;base64,AQID'};
 e.fire('richText','paste',{preventDefault(){},clipboardData:{items:[{kind:'file',type:'image/png',getAsFile:()=>file}],files:[],getData:type=>type==='text/html'?'<img>':''}});
 await new Promise(resolve=>setImmediate(resolve));
 assert.equal(text(e.runs()),'A\uFFFC😀B\nC');assert.ok(JSON.parse(e.ids.richImages.value)['new-image:test-uuid']);
 e.key('z');assert.equal(e.dirty(),false);
});
test('web import inserts text, image and table as one undoable save payload at its captured cursor',()=>{
 const e=editor();e.select(1,3);const ticket=e.bridge().capture();
 assert.equal(e.bridge().insert(ticket,[{text:'Imported',attributes:{weight:'heavy'}},{image:'data:image/png;base64,AQID',label:'Picture'},{table:[['H'],['V']]}]),true);
 assert.equal(text(e.runs()),'AImported\uFFFC\uFFFCB\nC');
 assert.equal(Object.keys(JSON.parse(e.ids.richImages.value)).length,1);assert.equal(Object.keys(JSON.parse(e.ids.richTables.value)).length,1);
 e.key('z');assert.equal(text(e.runs()),'A😀B\nC');assert.deepEqual(JSON.parse(e.ids.richImages.value),{});assert.equal(e.dirty(),false);
 e.key('y');assert.equal(Object.keys(JSON.parse((e.runs(),e.ids.richImages.value))).length,1);
});
test('web import rejects stale state, oversized images and protected selections without altering content',()=>{
 const e=editor();const ticket=e.bridge().capture();e.type('Changed');
 assert.throws(()=>e.bridge().insert(ticket,[{text:'Lost',attributes:{}}]));assert.equal(text(e.runs()),'Changed');
 const unchanged=e.bridge().capture();assert.throws(()=>e.bridge().insert(unchanged,[{image:'data:image/svg+xml;base64,AQID'}]));assert.equal(text(e.runs()),'Changed');
 const protectedEditor=editor(objectRuns());protectedEditor.select(1,2);
 assert.equal(protectedEditor.bridge().insert(protectedEditor.bridge().capture(),[{text:'Replace',attributes:{}}]),false);
 assert.equal(text(protectedEditor.runs()),'A\uFFFCB');
});


test('mobile HTTP can add generic-MIME PNG cloud files without randomUUID',async()=>{
 const e=editor(undefined,undefined,undefined,undefined,false,{secure:false,crypto:{getRandomValues(bytes){bytes.fill(42);return bytes;}}});
 const file=Object.assign(new Blob([new Uint8Array([137,80,78,71,13,10,26,10])],{type:'application/octet-stream'}),{name:'cloud.png',data:'data:application/octet-stream;base64,AQID'});
 e.ids.richImageFile.files=[file];e.fire('richImageFile','change');await new Promise(resolve=>setImmediate(resolve));
 assert.equal(text(e.runs()),'\uFFFCA😀B\nC');const images=JSON.parse(e.ids.richImages.value);
 assert.equal(Object.keys(images).length,1);assert.match(Object.keys(images)[0],/^new-image:[a-f0-9-]+$/);assert.equal(Object.values(images)[0],'data:image/png;base64,AQID');
 e.key('z');assert.equal(e.dirty(),false);
});
test('mobile beforeinput DataTransfer uses the rich paste converter and undo',()=>{
 const clipboard={fromHtml:()=>[{text:'Rich',attributes:{weight:'heavy'}}]};const e=editor(undefined,undefined,undefined,clipboard,true);
 let prevented=false;e.fire('richPreview','beforeinput',{inputType:'insertFromPaste',cancelable:true,preventDefault(){prevented=true;},dataTransfer:{files:[],getData:type=>type==='text/html'?'<b>Rich</b>':type==='text/plain'?'Plain':''}});
 assert.equal(prevented,true);assert.equal(e.runs()[0].attributes.weight,'heavy');e.key('z');assert.equal(e.dirty(),false);
});
test('clipboard button preserves HTML on secure contexts and explains HTTP without reading',async()=>{
 const clipboard={fromHtml:()=>[{text:'Rich',attributes:{weight:'heavy'}}]};let reads=0;
 const reader={read:async()=>{reads++;return [{types:['text/html','text/plain'],getType:async type=>new Blob([type==='text/html'?'<b>Rich</b>':'Plain'])}];}};
 const secure=editor(undefined,undefined,undefined,clipboard,false,{clipboardReader:reader});await secure.fire('richPasteImage','click');assert.equal(secure.runs()[0].attributes.weight,'heavy');
 const http=editor(undefined,undefined,undefined,clipboard,false,{secure:false,clipboardReader:reader});http.ids.richClipboardHint.dataset.http='Use HTTPS or native paste';await http.fire('richPasteImage','click');assert.equal(reads,1);assert.equal(http.ids.richClipboardHint.textContent,'Use HTTPS or native paste');assert.equal(http.dirty(),false);
});


test('codebox insert, edit, undo/redo and payload exclusion after undo use editor history', () => {
    const e=editor([{text:'AB',attributes:{}}],undefined,{},undefined,false,{codeboxes:true});
    e.select(1,1);e.fire('richCodebox','click');e.ids.richCodeboxText.value='const x = "<tag>";';
    e.fire('richCodeboxLanguages','change',{target:{matches:()=>true,value:'javascript'}});
    e.fire('richCodeboxApply','click');
    const key=e.runs().find(run=>run.attributes.__sweet_object).attributes.__sweet_object;
    assert.match(key,/^new-codebox:/);assert.equal(e.dirty(),true);
    assert.deepEqual(JSON.parse(e.ids.richCodeboxes.value)[key],{text:'const x = "<tag>";',syntax:'javascript'});
    e.key('z');assert.equal(text(e.runs()),'AB');assert.deepEqual(JSON.parse(e.ids.richCodeboxes.value),{});
    e.key('y');assert.equal(text(e.runs()),'A\uFFFCB');
});

test('existing codebox edits restore on undo and cancel does not make editor dirty', () => {
    const e=editor([{text:'\uFFFC',attributes:{__sweet_object:'codebox:0'}}],undefined,
      {'codebox:0':{label:'codebox',codebox:{text:'old',syntax:'java'}}},undefined,false,{codeboxes:true});
    // Open the protected slot's edit button from its rendered preview.
    function buttons(node){return [node,...(node.childNodes||[]).flatMap(buttons)];}
    let edit=buttons(e.ids.richPreview).find(node=>node.textContent?.includes('(java)'));
    edit.click();e.ids.richCodeboxText.value='changed';e.fire('richCodeboxCancel','click');assert.equal(e.dirty(),false);
    edit.click();e.ids.richCodeboxText.value='changed';e.fire('richCodeboxApply','click');
    assert.equal(e.dirty(),true);e.runs();assert.equal(JSON.parse(e.ids.richCodeboxes.value)['codebox:0'].text,'changed');
    e.key('z');assert.equal(e.dirty(),false);e.runs();assert.deepEqual(JSON.parse(e.ids.richCodeboxes.value),{});
});


test('internal link insertion and default-label replacement form one undo step',()=>{
 const e=editor([{text:'A😀B',attributes:{weight:'heavy'}}]);e.select(1,1);
 const ticket=e.internal().open();e.internal().apply(ticket,'Target','node 11');
 assert.equal(text(e.runs()),'ATarget😀B');assert.equal(e.runs()[1].attributes.link,'node 11');
 assert.equal(e.runs()[1].attributes.weight,'heavy');e.key('z');assert.equal(text(e.runs()),'A😀B');assert.equal(e.dirty(),false);
 e.key('y');assert.equal(text(e.runs()),'ATarget😀B');
});
test('caret expands contiguous formatted internal link; edit and remove preserve formatting',()=>{
 const e=editor([{text:'A',attributes:{link:'node 11',weight:'heavy'}},{text:'B',attributes:{link:'node 11',style:'italic'}},{text:'C',attributes:{link:'webs https://example.org'}}]);
 e.select(1,1);let ticket=e.internal().open();assert.equal(ticket.text,'AB');assert.equal(ticket.link,'node 11');
 e.internal().apply(ticket,'AB','node 22 çapa');assert.equal(e.runs()[0].attributes.link,'node 22 çapa');assert.equal(e.runs()[1].attributes.style,'italic');
 ticket=e.internal().open();e.internal().apply(ticket,ticket.text,null);
 assert.equal(e.runs()[0].attributes.link,undefined);assert.equal(e.runs()[1].attributes.link,undefined);assert.equal(e.runs()[2].attributes.link,'webs https://example.org');
 e.key('z');assert.equal(e.runs()[0].attributes.link,'node 22 çapa');
});
test('internal link dialog rejects object selections and stale editor changes',()=>{
 const e=editor([{text:'X',attributes:{}},{text:'\uFFFC',attributes:{__sweet_object:'image:1'}}],undefined,{'image:1':{label:'Image'}});
 e.select(0,2);assert.throws(()=>e.internal().open(),/objects/);
 e.select(0,1);const ticket=e.internal().open();e.fire('bold','click');assert.throws(()=>e.internal().apply(ticket,'X','node 11'),/changed/);
});

test('rich recovery preserves formatted history and staged image metadata/payload',async()=>{
 const e=editor(undefined,undefined,undefined,undefined,false,{recovery:true});
 e.select(1,3);e.fire('bold','click');
 e.select(0,0);e.ids.richImageFile.files=[{name:'draft.png',type:'image/png',size:1,data:'data:image/png;base64,YQ=='}];
 e.fire('richImageFile','change');await new Promise(resolve=>setImmediate(resolve));
 const draft=JSON.parse(JSON.stringify(e.recovery().snapshot()));
 assert.equal(Object.keys(draft.pendingImages).length,1);assert.equal(Object.values(draft.newObjects)[0].label,'draft.png');
 const fresh=editor(undefined,undefined,undefined,undefined,false,{recovery:true});fresh.recovery().restore(draft);
 assert.equal(fresh.dirty(),true);assert.equal(fresh.ids.richText.value.startsWith('\uFFFC'),true);
 fresh.runs();assert.equal(Object.keys(JSON.parse(fresh.ids.richImages.value)).length,1);
 fresh.key('z');assert.equal(fresh.ids.richText.value,'A😀B\nC');assert.equal(fresh.runs()[1].attributes.weight,'heavy');
 fresh.key('z');assert.equal(fresh.dirty(),false);fresh.key('y');fresh.key('y');assert.equal(fresh.ids.richText.value.startsWith('\uFFFC'),true);
});
