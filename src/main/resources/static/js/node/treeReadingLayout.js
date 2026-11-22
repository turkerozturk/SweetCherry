/* Browser-local reading preferences shared by the tree and its split reader. */
(function(root,factory){
    const api=factory();
    if(typeof module==='object'&&module.exports)module.exports=api;
    else {root.SweetCherryTreeReadingLayout=api;api.install(root);}
})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    const key='sweetcherry.treeReadingLayout';
    const defaults={title:true,compact:false,header:true,breadcrumbs:true,backlinks:true,toolbar:true,paneBar:true};
    function read(storage,metadata=false){
        const result={...defaults,ids:metadata,dates:metadata};
        try{const saved=JSON.parse(storage.getItem(key)||'{}');
            for(const name of Object.keys(result))if(typeof saved?.[name]==='boolean')result[name]=saved[name];
        }catch(_){}
        return result;
    }
    function install(win){
        const doc=win.document,html=doc.documentElement;
        let storage;try{storage=win.localStorage;}catch(_){}
        const metadata=html.dataset.treeMetadataEnabled==='true';
        let state=read(storage,metadata);
        function apply(){
            for(const [name,value] of Object.entries(state))html.setAttribute('data-tree-layout-'+name,String(value));
            doc.querySelectorAll('[data-tree-layout-setting]').forEach(input=>{input.checked=state[input.dataset.treeLayoutSetting];});
            win.dispatchEvent(new win.CustomEvent('sweetcherry:split-layout'));
        }
        apply();
        // Make icon-only actions keep both their accessible names and mouse hints.
        function labelActions(){
            doc.querySelectorAll('.workspace-toolbar .btn:not(.dropdown-item)').forEach(button=>{
                const label=button.textContent.trim();
                if(!label)return;
                if(!button.hasAttribute('aria-label'))button.setAttribute('aria-label',label);
                if(!button.title)button.title=label;
            });
        }
        function save(){try{storage?.setItem(key,JSON.stringify(state));}catch(_){}apply();}
        function ready(){
            const dialog=doc.getElementById('tree-reading-layout');
            doc.querySelectorAll('[data-tree-layout-open]').forEach(button=>button.addEventListener('click',()=>dialog.showModal()));
            dialog?.querySelector('[data-tree-layout-close]')?.addEventListener('click',()=>dialog.close());
            dialog?.querySelector('[data-tree-layout-reset]')?.addEventListener('click',()=>{state={...defaults,ids:metadata,dates:metadata};save();});
            doc.addEventListener('change',event=>{
                const name=event.target.dataset?.treeLayoutSetting;
                if(Object.hasOwn(state,name)){state[name]=event.target.checked;save();}
            });
            dialog?.addEventListener('click',event=>{
                if(event.target.closest('[data-tree-split-toggle], [data-split-activate], [data-split-close]'))dialog.close();
            });
            labelActions();apply();
            doc.addEventListener('sweetcherry:content-loaded',labelActions);
        }
        win.addEventListener('storage',event=>{if(event.key===key||event.key===null){state=read(storage,metadata);apply();}});
        if(doc.readyState==='loading')doc.addEventListener('DOMContentLoaded',ready,{once:true});else ready();
    }
    return {key,read,install};
});
