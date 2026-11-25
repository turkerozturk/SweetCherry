(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherrySplitView=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    function install({win=window,workspace,token,getSelected,selectLocal,reveal,stop}){
        const doc=win.document,core=win.SweetCherrySplitState;
        if(core.embedded(win)){
            const notify=(type,detail={})=>win.frameElement.dispatchEvent(new win.CustomEvent('sweetcherry:split-pane',{detail:{type,token,...detail}}));
            return {navigate:selectLocal,hasEditor:()=>false,ready(){},loaded(id,name,path){notify('loaded',{id,name,path});},message(text){notify('message',{text});},stop(message){notify('stop',{message});},restoreFocus(id){notify('focus',{id});}};
        }
        let storage;try{storage=win.sessionStorage;}catch(_){}
        const key='sweetcherry.split:'+token,state=core.read(storage,key);
        const requested=new URL(win.location.href).searchParams.get('split')==='1';
        const readers=doc.getElementById('workspace-readers'),left=doc.getElementById('workspace-left'),right=doc.getElementById('workspace-right');
        const frame=doc.getElementById('workspace-right-frame'),separator=doc.getElementById('workspace-content-splitter');
        const status=doc.getElementById('workspace-status'),external=doc.getElementById('workspace-right-open');
        let enabled=false,disposed=false,child=null,selection=0;
        function save(){try{storage?.setItem(key,JSON.stringify(state));}catch(_){} }
        function url(id){const target=new URL(win.location.href);target.search='';target.hash='';target.searchParams.set('nodeId',String(core.node(id)));target.searchParams.set('_tenantView',token);return target.pathname+target.search;}
        function api(){try{return frame.contentWindow?.SweetCherryWorkspace;}catch(_){return null;}}
        function render(){
            readers.dataset.splitOpen=String(enabled);readers.dataset.splitActive=state.active;
            right.hidden=!enabled;separator.hidden=!enabled;
            readers.style.setProperty('--split-ratio',state.ratio+'%');separator.setAttribute('aria-valuenow',String(state.ratio));
            separator.setAttribute('aria-orientation',win.matchMedia('(max-width:900px)').matches?'horizontal':'vertical');
            doc.querySelectorAll('[data-tree-split-toggle]').forEach(button=>button.setAttribute('aria-pressed',String(enabled)));
            doc.querySelectorAll('[data-split-activate]').forEach(button=>button.setAttribute('aria-pressed',String(state.active===button.dataset.splitActivate)));
            external.href=url(state.node);
            win.dispatchEvent(new win.CustomEvent('sweetcherry:split-layout'));
        }
        async function activate(side){
            if(disposed)return;
            state.active=enabled&&side==='right'?'right':'left';render();save();
            const current=++selection;
            if(state.active==='right'){
                if(!child)return;
                status.replaceChildren(child.status.cloneNode(true));doc.title=child.name+' — SweetCherry';
                await reveal(child.id,child.path);
            }else{
                const fragment=doc.querySelector('#workspace-content .workspace-node');
                const template=fragment?.querySelector('template[data-workspace-status]');
                if(template)status.replaceChildren(template.content.cloneNode(true));
                if(fragment)doc.title=fragment.dataset.nodeTitle+' — SweetCherry';
                await reveal(getSelected(),fragment?.querySelector('[data-path-ids]')?.dataset.pathIds||'');
            }
            return current===selection;
        }
        function open(){
            if(disposed||enabled)return;
            enabled=true;state.open=true;state.node=state.node===null?core.node(getSelected()):state.node;state.active='right';child=null;
            frame.src=url(state.node);render();save();
        }
        function close(force=false){
            if(!enabled)return true;
            // Removing a frame does not reliably trigger an editor's beforeunload guard.
            try{const reader=api();if(!force&&reader?.canLeaveEditor&&!reader.canLeaveEditor())return false;
                const childDoc=frame.contentDocument;
                const editing=childDoc?.querySelector('#richEditForm, #plainEditForm, [data-title-form]:not([hidden])')||(!api()&&childDoc?.querySelector('form'));
                if(!force&&editing&&!win.confirm(workspace.dataset.splitCloseConfirm))return false;
            }catch(_){}
            enabled=false;state.open=false;state.active='left';selection++;child=null;
            frame.removeAttribute('src');render();save();
            if(!force)activate('left').catch(()=>{});
            return true;
        }
        async function navigate(id,push=true,revealPath=false){
            if(disposed)return;
            if(!enabled||state.active!=='right')return selectLocal(id,push,revealPath);
            const reader=api();
            if(reader)return reader.select(core.node(id));
            frame.src=url(id);
        }
        frame.addEventListener('sweetcherry:split-pane',event=>{
            const message=event.detail;
            if(!enabled||disposed||message?.token!==token)return;
            if(message.type==='stop'){stop(typeof message.message==='string'?message.message:workspace.dataset.stale);return;}
            if(message.type==='message'){if(state.active==='right'&&typeof message.text==='string')status.textContent=message.text;return;}
            if(message.type==='focus'){
                if(state.active==='right')reveal(core.node(message.id),child?.path||'').then(()=>win.SweetCherryTreeKeyboard?.focusNode(doc.getElementById('workspace-tree'),String(message.id))).catch(()=>{});
                return;
            }
            if(message.type!=='loaded'||!Number.isSafeInteger(Number(message.id))||Number(message.id)<0||typeof message.name!=='string'||typeof message.path!=='string')return;
            const reader=api(),template=reader?.status();if(!template)return;
            state.node=core.node(message.id);child={id:state.node,name:message.name,path:message.path,status:template};render();save();
            if(state.active==='right')activate('right').catch(()=>{});
        });
        frame.addEventListener('load',()=>{
            if(!enabled)return;
            try{const childDoc=frame.contentDocument;if(!childDoc)return;
                childDoc.addEventListener('pointerdown',()=>{if(state.active!=='right')activate('right').catch(()=>{});},true);
                childDoc.addEventListener('focusin',()=>{if(state.active!=='right')activate('right').catch(()=>{});},true);
            }catch(_){}
        });
        for(const type of ['pointerdown','focusin'])left.addEventListener(type,()=>{if(state.active!=='left')activate('left').catch(()=>{});});
        doc.addEventListener('click',event=>{
            const toggle=event.target.closest('[data-tree-split-toggle]'),button=event.target.closest('[data-split-activate]');
            if(toggle){event.preventDefault();if(enabled)close();else open();}
            else if(button){event.preventDefault();activate(button.dataset.splitActivate).catch(()=>{});}
            else if(event.target.closest('[data-split-close]')){event.preventDefault();close();}
        });
        separator.addEventListener('pointerdown',event=>{separator.setPointerCapture(event.pointerId);doc.body.classList.add('resizing');});
        separator.addEventListener('pointermove',event=>{
            if(!separator.hasPointerCapture(event.pointerId))return;
            const rect=readers.getBoundingClientRect(),vertical=win.matchMedia('(max-width:900px)').matches;
            const value=100*(vertical?event.clientY-rect.top:event.clientX-rect.left)/(vertical?rect.height:rect.width);
            state.ratio=Math.max(20,Math.min(80,value));render();
        });
        for(const type of ['pointerup','pointercancel'])separator.addEventListener(type,()=>{doc.body.classList.remove('resizing');save();});
        separator.addEventListener('keydown',event=>{
            const directions={ArrowLeft:-5,ArrowUp:-5,ArrowRight:5,ArrowDown:5};
            if(directions[event.key]){event.preventDefault();state.ratio=Math.max(20,Math.min(80,state.ratio+directions[event.key]));render();save();}
        });
        win.addEventListener('resize',render);
        const result={navigate,message(){},loaded(){if(!enabled||state.active==='left')return;activate('right').catch(()=>{});},restoreFocus(){},
            ready(){const query=new URL(win.location.href);if(query.searchParams.has('split')){query.searchParams.delete('split');win.history.replaceState(win.history.state,'',query.pathname+query.search+query.hash);}const active=state.active;if(state.open||requested){open();if(!requested&&active==='left')activate('left').catch(()=>{});}else render();},
            hasEditor:()=>api()?.editorActive?.()===true,
            stop(message){disposed=true;if(api()?.editorActive?.()){api().invalidateEditor(message);return;}close(true);},activeRight:()=>enabled&&state.active==='right',
            move(direction){const reader=api();if(reader)reader.move(direction);}};
        win.SweetCherryTreeSplit=result;return result;
    }
    return {install};
});
