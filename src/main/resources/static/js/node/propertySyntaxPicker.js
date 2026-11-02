(() => {
    'use strict';
    const syntax=document.getElementById('codeSyntax'),radio=document.getElementById('typeCode');if(!syntax||!radio)return;
    const picker=document.getElementById('propertySyntaxPicker'),search=document.getElementById('propertySyntaxSearch'),details=document.getElementById('propertySyntaxDetails'),selected=document.getElementById('propertySyntaxSelected'),markdown=document.getElementById('propertyMarkdown');
    const choices=Array.from(picker.querySelectorAll('[data-syntax]'));
    const locked=syntax.dataset.locked==='true',placeholder=syntax.options[0].textContent;
    function sync(){
        syntax.disabled=locked||!radio.checked;search.disabled=syntax.disabled;
        choices.forEach(button=>{button.disabled=syntax.disabled;button.setAttribute('aria-pressed',String(button.dataset.syntax===syntax.value));});
        selected.textContent=syntax.value||placeholder;
        details.querySelector('summary').setAttribute('aria-disabled',String(syntax.disabled));
        if(syntax.disabled)details.open=false;
    }
    function filter(){const term=search.value.trim().toLocaleLowerCase();choices.forEach(button=>button.hidden=!button.dataset.syntax.toLocaleLowerCase().includes(term));if(!syntax.disabled)details.open=true;}
    function choose(value){if(syntax.disabled)return;syntax.value=value;syntax.dispatchEvent(new Event('change',{bubbles:true}));details.open=false;}
    syntax.hidden=true;picker.hidden=false; // Keep native select as the submitted value and no-script fallback.
    document.querySelectorAll('input[name="nodeType"]').forEach(item=>item.addEventListener('change',sync));
    syntax.addEventListener('change',()=>{search.setCustomValidity('');sync();});
    syntax.addEventListener('invalid',event=>{event.preventDefault();details.open=true;search.focus();search.setCustomValidity(placeholder);search.reportValidity();});search.addEventListener('input',filter);
    search.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();const first=choices.find(button=>!button.hidden&&!button.disabled);if(first)choose(first.dataset.syntax);}});
    details.addEventListener('toggle',()=>{if(syntax.disabled)details.open=false;});
    choices.forEach(button=>button.addEventListener('click',()=>choose(button.dataset.syntax)));
    markdown.addEventListener('click',()=>{if(locked)return;radio.checked=true;radio.dispatchEvent(new Event('change',{bubbles:true}));choose('markdown');search.value='';filter();details.open=false;});
    sync();
})();
