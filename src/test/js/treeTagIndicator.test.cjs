const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Exercise the actual row renderer without starting network navigation or the reading pane.
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/node/treeWorkspace.js'), 'utf8');
const renderer = source.slice(source.indexOf('    function row(child) {'), source.indexOf('    /** Loads only one branch'));
function element(tagName) {
    return {tagName, children: [], dataset: {}, style: {}, attributes: {},
        append(...nodes) { this.children.push(...nodes); },
        setAttribute(key, value) { this.attributes[key] = value; },
        addEventListener() {}};
}
function render(enabled, hasTags, tags, metadata={}) {
    const context = {document: {createElement: element}, workspace: {dataset: {showTagIndicator: enabled, hasTags: 'Node has tags'}},
        image: () => element('img'), statusIcons: () => null, url: () => '/tree?nodeId=55'};
    vm.createContext(context);
    vm.runInContext(renderer, context);
    return context.row({nodeId: 55, name: '<Tagged name>', iconName: 'ct_code', titleColor: '#3584E4', bold: true, hasTags, tags,...metadata})
        .children[0].children[1];
}
test('tagged rows have a small accessible marker after the unchanged literal title', () => {
    const link = render('true', true);
    assert.equal(link.children[1].textContent, '<Tagged name>');
    assert.equal(link.children[1].style.fontWeight, 'bold');
    const marker = link.children[2];
    assert.equal(marker.tagName, 'sup');
    assert.equal(marker.className, 'workspace-tag-indicator');
    assert.equal(marker.textContent, '#');
    assert.equal(marker.title, 'Node has tags');
    assert.equal(marker.attributes['aria-label'], 'Node has tags');
});
test('disabled setting and untagged or older navigation responses never show a marker', () => {
    for (const [enabled, tags] of [['false',true], ['true',false], ['true',undefined], [undefined,true]])
        assert.equal(render(enabled,tags).children.length, 2);
});

test('hover and accessible descriptions list literal Unicode tags without interpreting markup', () => {
    const marker = render('true', true, 'todo önemli <script>alert(1)</script>').children[2];
    assert.equal(marker.title, 'Node has tags: todo önemli <script>alert(1)</script>');
    assert.equal(marker.attributes['aria-label'], marker.title);
    assert.equal(marker.children.length, 0);
    assert.equal(marker.textContent, '#');
});

test('tree metadata prefixes are literal siblings before the unchanged name and tag marker',()=>{
 const link=render('true',true,'todo',{displayId:'[000055]',displayDate:'2026-11-22 <img>'});
 assert.equal(link.children[1].className,'workspace-tree-id');assert.equal(link.children[1].textContent,'[000055]');
 assert.equal(link.children[2].className,'workspace-tree-date');assert.equal(link.children[2].textContent,'2026-11-22 <img>');
 assert.equal(link.children[2].children.length,0);assert.equal(link.children[3].textContent,'<Tagged name>');
 assert.equal(link.children[4].className,'workspace-tag-indicator');
});
