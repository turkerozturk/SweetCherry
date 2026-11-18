(() => {
    'use strict';
    document.querySelectorAll('[data-file-picker]').forEach(root=>{
        const input=root.querySelector('[data-localized-file]'),controls=root.querySelector('[data-file-controls]');
        const browse=root.querySelector('[data-file-browse]'),name=root.querySelector('[data-file-name]');
        function update(){name.textContent=input.files?.[0]?.name||root.dataset.empty;}
        browse.addEventListener('click',()=>input.click());
        input.addEventListener('change',update);
        input.addEventListener('invalid',event=>{event.preventDefault();browse.focus();input.click();});
        input.form?.addEventListener('reset',()=>{name.textContent=root.dataset.empty;});
        // Leave the native, required, accessible input operational if JavaScript is unavailable.
        input.hidden=true;controls.hidden=false;update();
    });
})();
