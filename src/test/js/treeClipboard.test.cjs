const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/node/treeClipboard.js'), 'utf8');

function setup({confirmed = true, clipboard = true} = {}) {
    const copied = [];
    const requests = [];
    const confirmations = [];
    const textArea = {focus() {}, select() { this.selected = true; }};
    const dialog = {
        querySelector: () => textArea, addEventListener() {}, showModal() { this.open = true; }
    };
    const feedback = {dataset: {confirm: 'Copy?', copied: 'Copied', failed: 'Unavailable'}};
    let click;
    vm.runInNewContext(script, {
        document: {
            getElementById: id => id === 'tree-clipboard-dialog' ? dialog : feedback,
            addEventListener: (_, handler) => { click = handler; }
        },
        window: {confirm(message) { confirmations.push(message); return confirmed; }},
        navigator: clipboard ? {clipboard: {writeText: async text => copied.push(text)}} : {},
        fetch: async url => {
            requests.push(url);
            return {ok: true, headers: {get: () => 'text/plain;charset=UTF-8'}, text: async () => 'Başlık\n\nİçerik'};
        }
    });
    return {copied, requests, confirmations, dialog, textArea,
        click: dataset => click({target: {closest: () => ({dataset, isConnected: true})}})};
}

test('copies the exact numeric footer ID only after confirmation', async () => {
    const denied = setup({confirmed: false});
    await denied.click({copyNodeId: '9007199254740993'});
    assert.deepEqual(denied.copied, []);

    const accepted = setup();
    await accepted.click({copyNodeId: '9007199254740993'});
    assert.deepEqual(accepted.confirmations, ['Copy?\n9007199254740993']);
    assert.deepEqual(accepted.copied, ['9007199254740993']);
    assert.deepEqual(accepted.requests, []);
});

test('copies the title and body together and offers selectable text when clipboard access is unavailable', async () => {
    const page = setup();
    await page.click({copyNode: '/nodes/42/clipboard?_tenantView=a'});
    assert.deepEqual(page.requests, ['/nodes/42/clipboard?_tenantView=a']);
    assert.deepEqual(page.copied, ['Başlık\n\nİçerik']);

    const fallback = setup({clipboard: false});
    await fallback.click({copyNode: '/nodes/42/clipboard?_tenantView=a'});
    assert.equal(fallback.dialog.open, true);
    assert.equal(fallback.textArea.selected, true);
    assert.equal(fallback.textArea.value, 'Başlık\n\nİçerik');
});
