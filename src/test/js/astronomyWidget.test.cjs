const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/common/astronomyWidget.js'), 'utf8');

function page(fetch, buttons = true) {
    const content = {}; let listener;
    const button = {dataset: {astronomyLoading: 'Loading', astronomyError: 'Failed'},
        getAttribute: () => 'popover-id', addEventListener: (name, callback) => {assert.equal(name, 'shown.bs.popover'); listener = callback;}};
    vm.runInNewContext(script, {fetch, document: {querySelectorAll: () => buttons ? [button] : [],
        getElementById: () => ({querySelector: () => content})}});
    return {content, open: () => listener()};
}

test('loads the opened popover using the same-origin session', async () => {
    const result = page(async (url, options) => {
        assert.equal(url, '/astronomy'); assert.equal(options.credentials, 'same-origin');
        return {ok: true, redirected: false, text: async () => '<table>Offline</table>'};
    });
    await result.open(); assert.equal(result.content.innerHTML, '<table>Offline</table>');
});
test('does not insert a redirected login page into the popover', async () => {
    const result = page(async () => ({ok: true, redirected: true}));
    await result.open(); assert.equal(result.content.textContent, 'Failed');
    assert.equal(result.content.innerHTML, undefined);
});
test('disabled widget does not request astronomy data', () => {
    page(() => {throw new Error('Unexpected request');}, false);
});
