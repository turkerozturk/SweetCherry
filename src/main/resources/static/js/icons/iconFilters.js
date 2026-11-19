(() => {
    'use strict';
    document.querySelectorAll('[data-icon-category]').forEach(select=>{
        select.addEventListener('change',()=>select.form.requestSubmit());
    });
})();
