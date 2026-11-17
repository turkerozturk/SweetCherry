/* Bounded browser-local presentation receipts. Server actions own portable dismissal/snooze state. */
((root, factory) => {
    if (typeof module === 'object' && module.exports) module.exports = factory();
    else root.SweetCherryAlarmState = factory();
})(globalThis, () => {
    'use strict';
    const uuid = value => typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(value);
    function valid(feed) {
        if (!feed || !Array.isArray(feed.items) || feed.items.length > 20 || !Number.isSafeInteger(feed.total) || feed.total < feed.items.length || feed.total > 5000 || typeof feed.canAct !== 'boolean') return false;
        if (!uuid(feed.storeId) && !(feed.storeId === '' && feed.items.length === 0 && feed.total === 0)) return false;
        const ids = new Set();
        return feed.items.every(item => {
            if (!item || !uuid(item.id) || !uuid(item.key) || ids.has(item.id) || typeof item.nodeId !== 'string' || !/^[1-9][0-9]{0,18}$/.test(item.nodeId)) return false;
            ids.add(item.id);
            return typeof item.name === 'string' && item.name.length <= 1000 && typeof item.message === 'string' && item.message.length <= 2000
                && typeof item.dueText === 'string' && item.dueText.length <= 200 && ['none', 'yearly'].includes(item.repeat);
        });
    }
    const key = item => item.id + '/' + item.key;
    function ledger(value) {
        const clean = Object.create(null);
        if (!value || typeof value !== 'object' || Array.isArray(value)) return clean;
        for (const [id, receipt] of Object.entries(value)) {
            const parts = id.split('/');
            if (parts.length === 2 && parts.every(uuid) && receipt && Number.isFinite(receipt.seenAt) && receipt.seenAt >= 0 && typeof receipt.dismissed === 'boolean') clean[id] = {seenAt: receipt.seenAt, dismissed: receipt.dismissed, sounded: receipt.sounded === true};
        }
        return Object.fromEntries(Object.entries(clean).sort((a,b) => b[1].seenAt-a[1].seenAt).slice(0,1000));
    }
    function merge(one, two) {
        const combined = ledger(one);
        for (const [id, receipt] of Object.entries(ledger(two))) {
            if (!combined[id]) combined[id] = receipt;
            else combined[id] = {seenAt: Math.max(receipt.seenAt, combined[id].seenAt), dismissed: receipt.dismissed || combined[id].dismissed, sounded: receipt.sounded || combined[id].sounded};
        }
        return ledger(combined);
    }
    function present(items, previous, now) {
        const next = ledger(previous), rows = []; let fresh = 0;
        for (const item of items) {
            const id = key(item), receipt = next[id];
            if (receipt?.dismissed) continue;
            if (!receipt) { fresh++; next[id] = {seenAt: now, dismissed: false, sounded: false}; }
            rows.push(item);
        }
        return {rows, fresh, ledger: ledger(next)};
    }
    function claimSound(items, previous, now) {
        const next = ledger(previous); let claimed = false;
        for (const item of items) {
            const id = key(item), receipt = next[id];
            if (receipt && !receipt.dismissed && !receipt.sounded) { next[id] = {...receipt, seenAt: Math.max(now,receipt.seenAt), sounded: true}; claimed = true; }
        }
        return {claimed, ledger: ledger(next)};
    }
    function dismiss(item, previous, now) {
        const next = ledger(previous); next[key(item)] = {seenAt: now, dismissed: true, sounded: true}; return ledger(next);
    }
    return {valid, key, ledger, merge, present, claimSound, dismiss};
});
