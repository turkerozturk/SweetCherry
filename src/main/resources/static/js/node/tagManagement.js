(() => {
    'use strict';
    const action=document.getElementById('tagAction'),name=document.getElementById('managedTagName'),fields=document.getElementById('tagRenameFields');
    if(!action||!name||!fields)return;
    const update=()=>{const rename=action.value==='RENAME';fields.hidden=!rename;name.disabled=!rename;name.required=rename;};
    action.addEventListener('change',update);update();
})();
