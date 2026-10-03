/* A failed refresh must not acknowledge a new revision, so the next navigation retries it. */
(() => {
    'use strict';
    window.SweetCherryNavigationRevision = () => {
        let previous = null;
        return {
            async check(revision, refresh) {
                if (typeof revision !== 'string' || !revision) throw new Error('Invalid navigation revision');
                if (revision === previous) return false;
                const changed = previous !== null;
                if (changed) await refresh();
                previous = revision;
                return changed;
            }
        };
    };
})();
