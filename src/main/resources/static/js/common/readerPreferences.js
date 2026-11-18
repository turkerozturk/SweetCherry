(() => {
    'use strict';
    if(window.SweetCherryPreferences || !window.SweetCherryReaderPreferences)return;
    const core=window.SweetCherryReaderPreferences;
    let storage;try{storage=window.localStorage;}catch(_){}
    const preferences=core.create(storage);window.SweetCherryPreferences=preferences;
    const page=document.documentElement;
    const configured=core.color(page.style.getPropertyValue('--sc-node-background'))||'white';
    function apply(){
        page.style.setProperty('--sc-node-background',preferences.background()||configured);
        document.querySelectorAll('[data-confirm-toggle]').forEach(button=>{
            const enabled=preferences.confirmationEnabled(button.dataset.confirmToggle);
            button.setAttribute('aria-checked',String(enabled));button.querySelector('[data-confirm-mark]').textContent=enabled?'✓':'—';
        });
        scan();
    }
    function scan(){
        document.querySelectorAll('[data-node-reading] .rich-text-preview').forEach(root=>{
            core.leadingBlanks(root).forEach(line=>line.setAttribute('data-reading-leading-blank',''));
        });
        // The query marker means that an explicit delete command requested the faster path.
        document.querySelectorAll('a[data-node-delete]').forEach(link=>{
            const url=new URL(link.href,location.href);
            if(preferences.confirmationEnabled('deletion'))url.searchParams.delete('_skipNodeConfirmation');
            else url.searchParams.set('_skipNodeConfirmation','1');
            link.href=url.href;
        });
    }
    const dialog=document.querySelector('[data-node-background-dialog]');
    if(dialog){
        const text=dialog.querySelector('[data-node-background-value]');
        const picker=dialog.querySelector('[data-node-background-picker]');
        const error=dialog.querySelector('[data-node-background-error]');
        const names=dialog.querySelector('[data-node-background-names]');
        for(const name of Object.keys(core.colors)){const option=document.createElement('option');option.value=name;names.append(option);}
        function syncPicker(){const value=core.color(text.value);if(!value)return;let hex=core.colors[value]||value;if(hex.length===5||hex.length===9)hex=hex.slice(0,-(hex.length===5?1:2));if(/^#[0-9a-f]{6}$/.test(hex))picker.value=hex;else if(/^#[0-9a-f]{3}$/.test(hex))picker.value='#'+[...hex.slice(1)].map(x=>x+x).join('');}
        text.addEventListener('input',()=>{error.hidden=true;syncPicker();});
        picker.addEventListener('input',()=>{text.value=picker.value;error.hidden=true;});
        dialog.querySelector('[data-node-background-form]').addEventListener('submit',event=>{
            event.preventDefault();if(!preferences.setBackground(text.value)){error.hidden=false;text.focus();return;}apply();dialog.close();
        });
        dialog.querySelector('[data-node-background-reset]').addEventListener('click',()=>{preferences.resetBackground();apply();dialog.close();});
        dialog.querySelector('[data-node-background-close]').addEventListener('click',()=>dialog.close());
        document.addEventListener('click',event=>{if(!event.target.closest('[data-node-background-open]'))return;text.value=preferences.background()||configured;error.hidden=true;syncPicker();dialog.showModal();text.focus();});
    }
    document.addEventListener('click',event=>{
        const button=event.target.closest('[data-confirm-toggle]');if(!button)return;
        event.preventDefault();preferences.toggle(button.dataset.confirmToggle);apply();
    });
    window.addEventListener('storage',event=>{if(event.key===null||event.key.startsWith('sweetcherry.reader.')){preferences.reload();apply();}});
    new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});
    apply();
    const url=new URL(location.href),form=document.querySelector('[data-node-delete-confirm]');
    if(form && url.searchParams.get('_skipNodeConfirmation')==='1' && !preferences.confirmationEnabled('deletion')){
        // Remove the marker before POST; refreshing/back navigation must not repeat an automatic submit.
        url.searchParams.delete('_skipNodeConfirmation');history.replaceState(history.state,'',url);
        form.requestSubmit();
    }
})();
