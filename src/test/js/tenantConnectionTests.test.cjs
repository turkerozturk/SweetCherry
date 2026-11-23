const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/tenantConnectionTests.js'), 'utf8');

test('wizard tests send only their own fields plus CSRF and ignore stale results', async () => {
    const fields = {
        dttBaseUrl: {value: 'http://127.0.0.1:8081'}, dttTokenEnv: {value: 'DTT_BRIDGE_TOKEN'},
        _csrf: {value: 'csrf'}, type: {value: 'postgresql'}, password: {value: 'db-secret'}
    };
    const handlers = {}, statuses = {dtt: {}, database: {}}, buttons = {};
    for (const kind of Object.keys(statuses)) {
        buttons[kind] = {addEventListener: (_, handler) => { handlers[kind] = handler; }};
    }
    const form = {
        action: 'https://example.test/app/tenants/wizard',
        elements: {namedItem: key => fields[key]},
        querySelector: selector => {
            const key = selector.includes('"dtt"') ? 'dtt' : 'database';
            return selector.includes('-result') ? statuses[key] : buttons[key];
        }
    };
    const requests = [];
    let release;
    vm.runInNewContext(script, {
        URLSearchParams, AbortController, setTimeout, clearTimeout,
        document: {getElementById: id => id === 'tenantWizardForm' ? form : {dataset: {
            loading: 'Testing', success: 'Connected', failed: 'Failed'
        }}},
        fetch: (url, options) => {
            requests.push([url, options]);
            return new Promise(resolve => { release = () => resolve({
                ok: true, headers: {get: () => 'application/json'}, json: async () => ({result: 'success'})
            }); });
        }
    });

    const dtt = handlers.dtt();
    assert.equal(buttons.dtt.disabled, true);
    assert.equal(buttons.database.disabled, undefined);
    assert.equal(requests[0][0], form.action + '/test-dtt');
    assert.deepEqual([...requests[0][1].body.keys()], ['dttBaseUrl', 'dttTokenEnv', '_csrf']);
    assert.equal(requests[0][1].redirect, 'error');
    fields.dttBaseUrl.value = 'http://127.0.0.1:8082';
    release();
    await dtt;
    assert.equal(statuses.dtt.textContent, '');

    const database = handlers.database();
    assert.equal(requests[1][0], form.action + '/test-database');
    assert.deepEqual([...requests[1][1].body.keys()], ['type', 'password', '_csrf']);
    release();
    await database;
    assert.equal(statuses.database.textContent, 'Connected');
    assert.equal(buttons.database.disabled, false);
});
