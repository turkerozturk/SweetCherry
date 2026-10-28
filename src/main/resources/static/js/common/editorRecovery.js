(() => {
    'use strict';
    const DATABASE='SweetCherry-editor-recovery-v1', STORE='drafts', MAX_BYTES=32*1024*1024, RETENTION=7*86400000;
    /** Browser-local drafts are independent of the web server and never submit database writes. */
    function openDatabase() {
        return new Promise((resolve,reject)=>{
            const request=indexedDB.open(DATABASE,1);
            request.onupgradeneeded=()=>{const store=request.result.createObjectStore(STORE,{keyPath:'id'});store.createIndex('scope','scope');};
            request.onsuccess=()=>resolve(request.result);
            request.onerror=()=>reject(request.error);
            request.onblocked=()=>reject(new Error('blocked'));
        });
    }
    function operation(db,mode,callback) {
        return new Promise((resolve,reject)=>{
            const transaction=db.transaction(STORE,mode);let result;
            const request=callback(transaction.objectStore(STORE));
            if(request)request.onsuccess=()=>{result=request.result;};
            transaction.oncomplete=()=>resolve(result);
            transaction.onerror=()=>reject(transaction.error);
            transaction.onabort=()=>reject(transaction.error||new Error('aborted'));
        });
    }
    function install(form,editor) {
        const panel=document.getElementById('editorRecovery');if(!panel)return;
        const message=key=>panel.dataset[key]||key;
        const get=id=>document.getElementById(id);
        const status=get('recoveryStatus'), enabled=get('recoveryEnabled'), interval=get('recoveryInterval');
        const choices=get('recoveryDrafts'), restore=get('recoveryRestore'), discard=get('recoveryDiscard'), download=get('recoveryDownload');
        const login=get('recoveryLogin'), rescue=get('recoveryRescue');
        const scope=panel.dataset.scope+':'+location.pathname;
        let db, records=[], ready=false, writing=false, saving=false, last='', restoredId=null, tick;
        // A fresh record per open editor prevents simultaneous tabs from overwriting each other's drafts.
        const id=String(Date.now())+'-'+Array.from(crypto.getRandomValues(new Uint32Array(4))).join('-');
        function say(key,detail=''){status.textContent=message(key)+(detail?' '+detail:'');}
        try {
            const settings=JSON.parse(localStorage.getItem('sc-editor-recovery-settings')||'{}');
            enabled.checked=settings.enabled!==false;interval.value=String(Math.max(1,Math.min(300,Number(settings.seconds)||5)));
        }catch(ignored){}
        function settings() {
            interval.value=String(Math.max(1,Math.min(300,Number(interval.value)||5)));
            try{localStorage.setItem('sc-editor-recovery-settings',JSON.stringify({enabled:enabled.checked,seconds:Number(interval.value)}));}catch(ignored){}
            clearInterval(tick);tick=setInterval(()=>persist(),Number(interval.value)*1000);
            say(enabled.checked?'ready':'disabled');
            if(enabled.checked)persist();
        }
        enabled.addEventListener('change',settings);interval.addEventListener('change',settings);
        const selected=()=>records.find(record=>record.id===choices.value);
        function buttons(){const record=selected();restore.disabled=!record || record.base!==editor.base;discard.disabled=download.disabled=!record;
            if(record && record.base!==editor.base)say('changed');}
        async function refresh() {
            records=(await operation(db,'readonly',store=>store.index('scope').getAll(scope))).filter(record=>record.version===1 && record.updated>=Date.now()-RETENTION).sort((a,b)=>b.updated-a.updated);
            const previous=choices.value;choices.replaceChildren();
            const empty=document.createElement('option');empty.value='';empty.textContent=message('choose');choices.append(empty);
            for(const record of records){const option=document.createElement('option');option.value=record.id;option.textContent=new Date(record.updated).toLocaleString()+(record.id===id?' ('+message('current')+')':'');choices.append(option);}
            if(records.some(record=>record.id===previous))choices.value=previous;
            else if(records.length && !ready){choices.value=records[0].id;say('found');}
            buttons();
        }
        choices.addEventListener('change',buttons);
        async function persist(force=false) {
            if(!db || writing || saving || !force && !enabled.checked || editor.busy?.())return;
            if(!editor.dirty()){if(last){await operation(db,'readwrite',store=>store.delete(id));last='';await refresh();}return;}
            writing=true;
            try {
                const payload=editor.snapshot(), serialized=JSON.stringify(payload);
                if(serialized===last)return;
                if((serialized.length+editor.base.length)*2>MAX_BYTES)throw new Error('size');
                await operation(db,'readwrite',store=>store.put({id,scope,version:1,base:editor.base,updated:Date.now(),payload}));
                last=serialized;say('stored',new Date().toLocaleTimeString());await refresh();
            }catch(error){say('storageFailed');}
            finally{writing=false;}
        }
        restore.addEventListener('click',()=>{
            const record=selected();if(!record || record.base!==editor.base)return;
            if(!window.confirm(message('restoreConfirm')))return;
            try{editor.restore(record.payload);restoredId=record.id;last='';say('restored');persist();}catch(error){say('invalid');}
        });
        discard.addEventListener('click',async()=>{
            const record=selected();if(!record || !window.confirm(message('discardConfirm')))return;
            try{await operation(db,'readwrite',store=>store.delete(record.id));if(record.id===id)last='';await refresh();say('discarded');}catch(error){say('storageFailed');}
        });
        function downloadPayload(payload) {
            const url=URL.createObjectURL(new Blob([JSON.stringify({format:'SweetCherry recovery draft',version:1,path:location.pathname,base:editor.base,payload},null,2)],{type:'application/json'}));
            const link=document.createElement('a');link.href=url;link.download='SweetCherry-draft-'+location.pathname.split('/').pop()+'.json';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
        }
        download.addEventListener('click',()=>{const record=selected();if(record)downloadPayload(record.payload);});
        rescue.addEventListener('click',()=>downloadPayload(editor.snapshot()));
        document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='hidden')persist();});
        window.addEventListener('pagehide',()=>persist());
        // The editor serializes its native fields first. Only an explicit JSON receipt allows navigation.
        form.addEventListener('submit',async event=>{
            if(event.defaultPrevented)return;event.preventDefault();if(saving || editor.busy?.())return;
            await persist();if(saving)return;
            const sent=JSON.stringify(editor.snapshot());
            const body=new URLSearchParams(new FormData(form));
            if(event.submitter?.name)body.set(event.submitter.name,event.submitter.value);
            saving=true;form.inert=true;say('saving');login.hidden=true;
            const controller=new AbortController(), timeout=setTimeout(()=>controller.abort(),30000);
            try {
                const response=await fetch(form.action,{method:'POST',body,credentials:'same-origin',headers:{'X-SweetCherry-Editor-Save':'1','Accept':'application/json'},signal:controller.signal});
                if(!response.ok || !(response.headers.get('Content-Type')||'').includes('application/json'))throw new Error('rejected');
                const receipt=await response.json(), destination=new URL(receipt.redirect,location.href);
                const action=new URL(form.action), node=action.pathname.split('/').pop();
                const prefix=action.pathname.slice(0,action.pathname.indexOf('/nodes/'));
                const expected=body.get('continueEditing')==='true'?action.pathname:prefix+'/nodes/'+node;
                if(receipt.saved!==true || destination.origin!==location.origin || destination.pathname!==expected || destination.search || destination.hash)throw new Error('receipt');
                if(sent!==JSON.stringify(editor.snapshot())){say('savedChanged');return;}
                if(db)try{await operation(db,'readwrite',store=>store.delete(id));if(restoredId)await operation(db,'readwrite',store=>store.delete(restoredId));}catch(ignored){}
                form.dispatchEvent(new Event('editor:saved'));
                location.assign(destination.href);
            }catch(error){say('saveFailed');login.hidden=false;}
            finally{clearTimeout(timeout);saving=false;form.inert=false;}
        });
        if(!panel.dataset.scope){say('storageFailed');enabled.disabled=true;return;}
        openDatabase().then(async opened=>{
            db=opened;
            // Bound retention without reading another user's draft into the UI.
            await operation(db,'readwrite',store=>{const cursor=store.openCursor();cursor.onsuccess=()=>{const entry=cursor.result;if(!entry)return;if(entry.value.updated<Date.now()-RETENTION)entry.delete();entry.continue();};});
            await refresh();ready=true;settings();if(records.length)say('found');
        }).catch(()=>{say('storageFailed');enabled.disabled=true;});
    }
    window.EditorRecovery={install};
})();
