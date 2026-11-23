const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/node/dttBridge.js'), 'utf8');

function setup({ dirtyNote = false, dirtyStatus = false, discard = false, ok = true } = {}) {
    const events = {};
    const requests = [];
    const confirmations = [];
    const note = { defaultValue: 'saved', value: dirtyNote ? 'unsaved' : 'saved' };
    const status = { options: [{ value: '1', defaultSelected: true }], value: dirtyStatus ? '2' : '1' };
    const form = { elements: { namedItem: name => ({ note, status })[name] } };
    const feedback = { classList: { add() {}, remove() {} }, textContent: '' };
    const panel = {
        dataset: { tenantView: 'view-token', loadError: 'Could not load' },
        isConnected: true,
        querySelector: selector => selector === '[data-dtt-form]' ? form : feedback,
        querySelectorAll: () => [date, note, status],
        replaceWith(next) { this.replacement = next; }
    };
    const date = {
        value: '2026-11-20', defaultValue: '2026-11-23',
        matches: selector => selector === '[data-dtt-date]', closest: () => panel, checkValidity: () => true,
        dataset: { endpoint: '/app/nodes/dtt/1/entry', discard: 'Discard changes?' }
    };
    const next = { querySelector: () => ({ focus() {} }) };
    vm.runInNewContext(script, {
        URL, location: { href: 'https://example.test/app/tree' },
        document: { addEventListener(name, callback) { events[name] = callback; } },
        window: { confirm(message) { confirmations.push(message); return discard; } },
        fetch: async (url, options) => { requests.push({ url, options }); return { ok, text: async () => '' }; },
        DOMParser: class { parseFromString() { return { querySelector: () => next }; } }
    });
    return { date, panel, feedback, next, requests, confirmations, change: () => events.change({ target: date }) };
}

test('changing date with unsaved notes or status can be cancelled without fetching', async () => {
    for (const flags of [{ dirtyNote: true }, { dirtyStatus: true }]) {
        const page = setup(flags);
        await page.change();
        assert.deepEqual(page.confirmations, ['Discard changes?']);
        assert.equal(page.requests.length, 0);
        assert.equal(page.date.value, page.date.defaultValue);
    }
});

test('accepted date changes fetch the selected date and keep the old panel if loading fails', async () => {
    const accepted = setup({ dirtyNote: true, discard: true });
    await accepted.change();
    assert.equal(accepted.requests[0].url.pathname, '/app/nodes/dtt/1/entry');
    assert.equal(accepted.requests[0].url.searchParams.get('_tenantView'), 'view-token');
    assert.equal(accepted.requests[0].url.searchParams.get('date'), '2026-11-20');
    assert.equal(accepted.panel.replacement, accepted.next);

    const failure = setup({ ok: false });
    await failure.change();
    assert.equal(failure.confirmations.length, 0);
    assert.equal(failure.date.value, failure.date.defaultValue);
    assert.equal(failure.feedback.textContent, 'Could not load');
    assert.equal(failure.panel.replacement, undefined);
});
