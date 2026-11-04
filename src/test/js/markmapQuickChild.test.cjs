const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/markmap/quickChild.js'), 'utf8');
const receipt = id => ({ok: true, status: 200, redirected: false, headers: {get: () => 'application/json'}, json: async () => ({id})});
function setup(fetcher = async () => receipt('22')) {
    const element = () => ({handlers: {}, disabled: false, value: '', textContent: '', focused: false,
        addEventListener(type, callback) { this.handlers[type] = callback; }, focus() { this.focused = true; }});
    const title = element(), parent = element(), cancel = element(), submit = element(), status = element(), parentLabel = element();
    const fields = {title, parentId: parent, _tenantView: {value: 'page-view'}, _csrf: {value: 'csrf-token'}, text: {value: ''}, syntax: {value: 'plain-text'}};
    const form = Object.assign(element(), {action: 'https://localhost/app/nodes/quick/create', valid: true,
        elements: {namedItem: name => fields[name]}, reportValidity() { return this.valid; }, querySelector: () => submit});
    const dialog = Object.assign(element(), {open: false, showModal() { this.open = true; }, close() { this.open = false; this.handlers.close?.(); }});
    const selectors = {'dialog': dialog, 'form': form, '[data-markmap-child-cancel]': cancel,
        '[data-markmap-child-status]': status, '#markmap-child-parent': parentLabel};
    const root = {dataset: Object.fromEntries(['saving', 'stale', 'denied', 'invalid', 'uncertain', 'reload'].map(key => [key, key])), querySelector: selector => selectors[selector]};
    const map = Object.assign(element(), {contains: () => true});
    const calls = [], destinations = [];
    const location = {href: 'https://localhost/app/markmap-quick-mind-map/11?level=1&icons=false&colors=true&foldChat=true', assign: url => destinations.push(url)};
    vm.runInNewContext(script, {document: {querySelector: () => root, getElementById: () => map}, window: {location},
        URL, URLSearchParams, AbortController, setTimeout, clearTimeout,
        FormData: class { constructor() { this.values = Object.entries(fields).filter(([, field]) => !field.disabled).map(([key, field]) => [key, field.value]); } [Symbol.iterator]() { return this.values[Symbol.iterator](); } },
        fetch: async (url, options) => { calls.push({url, options}); return fetcher(url, options); }});
    function open(id = '11', name = '<Shared parent>', level = '2') {
        const button = Object.assign(element(), {dataset: {parentId: id, revealLevel: level}, isConnected: true,
            closest: () => ({querySelector: () => ({textContent: name})})});
        const event = {target: {closest: () => button}, preventDefault() { this.prevented = true; }, stopPropagation() { this.stopped = true; }};
        map.handlers.click(event); return {button, event};
    }
    return {title, parent, cancel, submit, status, parentLabel, dialog, form, map, fields, calls, destinations, open,
        save: () => form.handlers.submit({preventDefault() {}}), close: () => cancel.handlers.click()};
}

test('plus chooses the exact shared occurrence, displays literal parent text and Cancel never writes', () => {
    const app = setup();
    const {button, event} = app.open('9223372036854775807', '<img onerror=alert(1)>');
    assert(event.prevented && event.stopped);
    assert.equal(app.parent.value, '9223372036854775807');
    assert.equal(app.parentLabel.textContent, '<img onerror=alert(1)>');
    assert(app.dialog.open && app.title.focused);
    app.close(); assert(!app.dialog.open); assert(button.focused); assert.equal(app.calls.length, 0);
    const navigation = {target: {closest: () => null}, preventDefault() { throw Error('Navigation blocked'); }};
    app.map.handlers.click(navigation); // Names/icons keep their own events.
});

test('creation posts only the selected parent and empty plain-text, with CSRF/source token, then reveals the child', async () => {
    const app = setup(); app.open('9223372036854775807'); app.title.value = '<child> & Türkçe';
    await app.save();
    assert.equal(app.calls.length, 1);
    const {url, options} = app.calls[0];
    assert.equal(url, 'https://localhost/app/nodes/quick/create');
    assert.equal(options.method, 'POST'); assert.equal(options.credentials, 'same-origin');
    assert.deepEqual(Object.fromEntries(options.body), {title: '<child> & Türkçe', parentId: '9223372036854775807',
        _tenantView: 'page-view', _csrf: 'csrf-token', text: '', syntax: 'plain-text'});
    const destination = new URL(app.destinations[0]);
    assert.equal(destination.pathname, '/app/markmap-quick-mind-map/11');
    assert.equal(destination.searchParams.get('level'), '2');
    for (const [key, value] of Object.entries({icons: 'false', colors: 'true', foldChat: 'true', childOf: '9223372036854775807', _tenantView: 'page-view'})) {
        assert.equal(destination.searchParams.get(key), value);
    }
    await app.save(); assert.equal(app.calls.length, 1); // No double creation during refresh.
});

test('an in-flight save rejects duplicate submit, cancel/Escape and switching parents', async () => {
    let finish; const app = setup(() => new Promise(resolve => { finish = resolve; }));
    app.open('11'); app.title.value = 'Child'; const first = app.save();
    await app.save(); app.close(); app.open('99');
    let prevented = false; app.dialog.handlers.cancel({preventDefault() { prevented = true; }});
    assert(prevented && app.dialog.open); assert.equal(app.parent.value, '11'); assert.equal(app.calls.length, 1);
    finish(receipt('22')); await first;
});

test('blank/invalid names do not write and explicit validation errors can be corrected', async () => {
    let count = 0; const app = setup(async () => ++count === 1 ? {ok: false, status: 400} : receipt('23'));
    app.open(); app.title.value = '   '; await app.save(); assert.equal(app.calls.length, 0);
    app.title.value = 'Bad'; await app.save(); assert.equal(app.status.textContent, 'invalid'); assert(!app.submit.disabled);
    assert.equal(app.title.value, 'Bad'); app.title.value = 'Corrected'; await app.save(); assert.equal(app.destinations.length, 1);
});

test('stale, denied, deleted-parent and uncertain results retain the name without reload or blind retry', async () => {
    for (const [fetcher, message] of [
        [async () => ({ok: false, status: 409}), 'stale'], [async () => ({ok: false, status: 403}), 'denied'],
        [async () => ({ok: false, status: 404}), 'invalid'], [async () => ({ok: false, status: 500}), 'uncertain'],
        [async () => { throw Error('Network lost after commit'); }, 'uncertain'],
        [async () => ({redirected: true}), 'uncertain'], [async () => receipt(22), 'uncertain']
    ]) {
        const app = setup(fetcher); app.open(); app.title.value = 'Keep me'; await app.save();
        assert.equal(app.status.textContent, message); assert.equal(app.title.value, 'Keep me');
        assert(app.dialog.open && app.submit.disabled && !app.cancel.disabled && !app.title.disabled);
        await app.save(); assert.equal(app.calls.length, 1); assert.equal(app.destinations.length, 0);
        app.close(); app.open(); assert(app.submit.disabled); assert.equal(app.status.textContent, 'reload');
    }
});

test('virtual root or malformed parent IDs never open creation', () => {
    for (const id of ['0', '-1', 'NaN', '1" onclick="bad', '']) {
        const app = setup(); app.open(id); assert(!app.dialog.open); assert.equal(app.calls.length, 0);
    }
});
