(() => {
    'use strict';
    const form=document.getElementById('tenantWizardForm'); if(!form)return;
    const type=document.getElementById('type'), path=document.getElementById('path'), name=document.getElementById('name');
    const dialog=document.getElementById('wizardFiles'), folder=document.getElementById('wizardFolder'), entries=document.getElementById('wizardEntries'), status=document.getElementById('wizardBrowseStatus');
    const create=document.getElementById('createEmpty'), filename=document.getElementById('databaseFilename');
    let parent='', autoName=name.value==='', sequence=0;
    name.addEventListener('input',()=>{autoName=false;});
    function defaultName(){if(autoName||name.value===''){name.value=create?.checked?(filename.value?filename.value+(/\.ctb$/i.test(filename.value)?'':'.ctb'):''):path.value.split(/[\\/]/).pop();autoName=true;}}
    path.addEventListener('change',defaultName);
    function connectionType(){
        const sqlite=type.value==='sqlite';
        if(create){create.disabled=!sqlite;if(!sqlite)create.checked=false;}
        const fresh=sqlite&&create?.checked;
        document.getElementById('wizardSqlite').hidden=!sqlite;document.getElementById('wizardRemote').hidden=sqlite;
        document.getElementById('wizardExistingCtb').hidden=Boolean(fresh);path.required=sqlite&&!fresh;path.disabled=Boolean(fresh);
        if(filename){filename.required=Boolean(fresh);filename.disabled=!fresh;document.getElementById('wizardNewCtb').hidden=!fresh;}
        document.getElementById('url').required=!sqlite;
    }
    create?.addEventListener('change',()=>{connectionType();if(create.checked)document.getElementById('writable').checked=true;defaultName();});
    filename?.addEventListener('input',defaultName);
    type.addEventListener('change',connectionType);connectionType();
    const browseUrl=form.action.replace(/\/$/,'')+'/browse';
    async function browse(value){
        const current=++sequence;status.textContent=status.dataset.loading;entries.replaceChildren();
        try {
            const response=await fetch(browseUrl+'?path='+encodeURIComponent(value),{headers:{Accept:'application/json'},cache:'no-store'});
            if(!response.ok||!response.headers.get('content-type')?.includes('application/json'))throw new Error();
            const data=await response.json();if(current!==sequence)return;
            folder.value=data.path;parent=data.parent;status.textContent=data.truncated?status.dataset.truncated:'';
            for(const entry of data.entries){
                const button=document.createElement('button');button.type='button';button.className='btn btn-outline-secondary d-block w-100 text-start my-1';button.textContent=(entry.directory?entries.dataset.folder:entries.dataset.file)+' — '+entry.name;
                button.addEventListener('click',()=>{if(entry.directory)browse(entry.path);else{path.value=entry.path;defaultName();dialog.close();}});entries.append(button);
            }
        }catch(_){if(current===sequence)status.textContent=status.dataset.error;}
    }
    document.getElementById('wizardBrowse').addEventListener('click',()=>{
        dialog.showModal();let start=path.value.replace(/[\\/][^\\/]*$/,'');
        if(!path.value.includes('/')&&!path.value.includes('\\'))start='';
        if(/^[A-Za-z]:$/.test(start))start+='\\';
        if(start===''&&path.value.startsWith('/'))start='/';browse(start);
    });
    document.getElementById('wizardFolderForm').addEventListener('submit',event=>{event.preventDefault();browse(folder.value);});
    document.getElementById('wizardRoots').addEventListener('click',()=>browse(''));
    document.getElementById('wizardUp').addEventListener('click',()=>browse(parent));
    document.getElementById('wizardClose').addEventListener('click',()=>dialog.close());
})();
