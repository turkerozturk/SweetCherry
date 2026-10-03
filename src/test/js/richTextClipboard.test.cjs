const test=require('node:test'),assert=require('node:assert/strict');
const clipboard=require('../../main/resources/static/js/node/richTextClipboard.js');
const text=nodeValue=>({nodeType:3,nodeValue});
function element(tag,children=[],attrs={}){
 const node={nodeType:1,tagName:tag,childNodes:children,getAttribute:key=>attrs[key]??null};
 node.children=children.filter(child=>child.nodeType===1);
 node.querySelectorAll=tag=>{const found=[];function visit(current){for(const child of current.children||[]){if(child.tagName===tag.toUpperCase())found.push(child);visit(child);}}visit(node);return found;};
 return node;
}
const fragment=children=>({nodeType:11,childNodes:children});
const content=items=>items.map(item=>item.text||'\uFFFC').join('');

test('HTML semantic and inline CSS formatting become combined CTB attributes',()=>{
 const source=fragment([element('P',[element('STRONG',[element('EM',[element('SPAN',[text('Hi')],{style:'color:rgb(53,132,228);background-color:#abc;text-decoration:underline line-through'})])])],{style:'text-align:center'})]);
 const [run]=clipboard.fromDom(source);
 assert.equal(run.text,'Hi');assert.deepEqual(run.attributes,{justification:'center',weight:'heavy',style:'italic',foreground:'#3584e4',background:'#aabbcc',underline:'single',strikethrough:'true'});
});
test('blocks, explicit breaks and preformatted whitespace preserve readable line structure',()=>{
 const items=clipboard.fromDom(fragment([element('P',[text('One'),element('BR'),text('Two')]),element('PRE',[text('A  B\nC')])]));
 assert.equal(content(items),'One\nTwo\nA  B\nC');assert.equal(items.find(item=>item.text==='A  B\nC').attributes.family,'monospace');
});
test('HTTP links and remote image references survive without image binary or unsafe targets',()=>{
 const items=clipboard.fromDom(fragment([element('A',[text('Safe')],{href:'https://example.org/?a=1&b=2'}),element('IMG',[],{src:'https://example.org/a.png',alt:'Picture'}),element('A',[text('Plain')],{href:'javascript:alert(1)'})]));
 assert.equal(items[0].attributes.link,'webs https://example.org/?a=1&b=2');
 assert.equal(items[1].text,'Picture');assert.equal(items[1].attributes.link,'webs https://example.org/a.png');assert.equal(items[2].attributes.link,undefined);
});
test('rectangular plain tables become structured rows; merged cells fall back to text',()=>{
 const rows=[element('TR',[element('TH',[text('Header')])]),element('TR',[element('TD',[text('Line'),element('BR'),text('Two')])])];
 const items=clipboard.fromDom(fragment([element('TABLE',[element('TBODY',rows)])]));
 assert.deepEqual(items[0].table,[['Header'],['Line\nTwo']]);
 const merged=clipboard.fromDom(fragment([element('TABLE',[element('TR',[element('TD',[text('Merged')],{colspan:'2'})])])]));
 assert.equal(content(merged),'Merged');assert.equal(merged.some(item=>item.table),false);
});
test('script, stylesheet, SVG and event attributes never become editor content',()=>{
 const items=clipboard.fromDom(fragment([element('SCRIPT',[text('alert(1)')]),element('STYLE',[text('body{}')]),element('SVG',[text('bad')]),element('SPAN',[text('Safe')],{onclick:'bad()',style:'background-image:url(https://example.org/);color:expression(bad())'})]));
 assert.equal(content(items),'Safe');assert.deepEqual(items[0].attributes,{});
});
test('private copy metadata preserves formatting but rejects object IDs and unsupported styles',()=>{
 const items=clipboard.fromInternal(JSON.stringify([{text:'A\uFFFC😀',attributes:{weight:'heavy',foreground:'#3584e4',__sweet_object:'image:1',evil:'x',link:'webs javascript:alert(1)'}}]));
 assert.deepEqual(items,[{text:'A😀',attributes:{weight:'heavy',foreground:'#3584e4'}}]);
 assert.throws(()=>clipboard.fromInternal('[null]'));
});
test('HTML copy escapes text and link targets rather than inserting source markup',()=>{
 const html=clipboard.toHtml([{text:'<img onerror=x>&',attributes:{weight:'heavy',link:'webs https://example.org/?a=1&b=2'}}]);
 assert.match(html,/&lt;img/);assert.match(html,/&amp;b=2/);assert.match(html,/font-weight:bold/);assert.doesNotMatch(html,/<img/);
});
test('clipboard size and traversal limits reject instead of silently truncating',()=>{
 assert.throws(()=>clipboard.fromDom(fragment([text('a'.repeat(200001))])));
 assert.throws(()=>clipboard.fromDom(fragment(Array.from({length:10001},()=>text('A')))));
});
