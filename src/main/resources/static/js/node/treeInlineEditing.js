(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.SweetCherryTreeEditing=api;})(typeof window==='object'?window:globalThis,()=>{
    'use strict';
    function editorUrl(value,id,base,token){
        const url=new URL(value,base),origin=new URL(base).origin;
        const match=url.pathname.match(/^(.*)\/nodes\/(content|richtext\/edit)\/([1-9][0-9]*)$/);
        if(url.origin!==origin||!match||match[1]!==new URL(base).pathname.replace(/\/tree$/,'')||match[3]!==String(id)||url.username||url.password||url.hash)throw Error('editor URL');
        url.search='';url.searchParams.set('_tenantView',token);return url;
    }
    function install({win=window,workspace,content,token,getSelected,onSaved}){
        const doc=win.document;let current=null;
        const label=name=>workspace.dataset['editor'+name]||name;
        const reader=()=>content.querySelector('.workspace-node');
        function child(){try{return current?.frame.contentWindow?.SweetCherryInlineEditor;}catch(_){return null;}}
        function sync(){try{current?.frame.contentDocument?.documentElement.setAttribute('data-inline-compact',doc.documentElement.getAttribute('data-tree-layout-compact')==='true'?'true':'false');}catch(_){}}
        function canLeave(){
            if(!current)return true;
            const api=child();
            if(!current.invalid&&api?.ready()&&!current.loading&&!api.busy())current.notice.hidden=true;
            if(current.loading||api?.busy()){current.notice.hidden=false;current.message.textContent=label('Busy');return false;}
            if((!api||api.dirty())&&!win.confirm(label('Leave')))return false;
            api?.approveLeave();return true;
        }
        function detach(){
            const old=current;if(!old)return null;current=null;win.clearTimeout(old.timer);
            old.shell.remove();old.reader.hidden=false;old.reader.inert=false;
            content.classList.remove('workspace-editing');
            return old;
        }
        async function finish(check=true){
            if(check&&!canLeave())return false;
            const old=detach();if(!old)return true;
            if(old.changed)await onSaved(old.id,old.scroll);
            else {old.scroller?.focus({preventScroll:true});}
            return true;
        }
        function beforeNavigate(){if(!canLeave())return false;detach();return true;}
        function open(value,id=getSelected()){
            if(current||!reader()||content.getAttribute('aria-busy')==='true')return false;
            let url;try{url=editorUrl(value,id,win.location.href,token);}catch(_){return false;}
            const node=reader(),scroller=node.querySelector('.workspace-rendered');
            const shell=doc.createElement('section');shell.className='workspace-inline-editor';
            const notice=doc.createElement('div');notice.className='workspace-editor-notice';notice.setAttribute('role','status');
            const message=doc.createElement('span');message.textContent=label('Loading');
            const back=doc.createElement('button');back.type='button';back.className='btn btn-sm btn-secondary';back.textContent=label('Return');
            const external=doc.createElement('a');external.href=url.href;external.target='_blank';external.rel='noopener';external.textContent=label('Separate');
            notice.append(message,back,external);
            const frame=doc.createElement('iframe');frame.className='workspace-editor-frame';frame.dataset.treeEditor='true';frame.title=label('Title');frame.referrerPolicy='same-origin';
            current={frame,shell,notice,message,reader:node,scroller,id:String(id),url,loading:true,changed:false,invalid:false,
                scroll:{top:scroller?.scrollTop||0,left:scroller?.scrollLeft||0}};
            const ticket=current;
            back.addEventListener('click',()=>{if(current===ticket){ticket.loading=false;finish().catch(()=>{});}});
            frame.addEventListener('load',()=>{
                if(current!==ticket)return;
                win.clearTimeout(ticket.timer);ticket.loading=false;sync();
                if(ticket.invalid){child()?.invalidate();return;}
                try{
                    const actual=new URL(frame.contentWindow.location.href);
                    if(actual.origin!==url.origin||actual.pathname!==url.pathname||!child()?.ready())throw Error();
                    notice.hidden=true;
                    // Focus in a nested editor must also activate its outer split pane.
                    for(const type of ['pointerdown','focusin'])frame.contentDocument.addEventListener(type,()=>shell.dispatchEvent(new win.Event('focusin',{bubbles:true})),true);
                }catch(_){notice.hidden=false;message.textContent=label('Error');}
            });
            ticket.timer=win.setTimeout(()=>{if(current===ticket){ticket.loading=false;notice.hidden=false;message.textContent=label('Error');}},30000);
            frame.src=url.href;node.hidden=true;node.inert=true;content.classList.add('workspace-editing');
            shell.append(notice,frame);content.append(shell);return true;
        }
        async function saved(frame,destination,continuing){
            if(!current||current.frame!==frame||current.invalid)return false;
            const ticket=current,target=new URL(destination,ticket.url),prefix=ticket.url.pathname.slice(0,ticket.url.pathname.indexOf('/nodes/'));
            if(target.origin!==ticket.url.origin||target.search||target.hash||target.pathname!==(continuing?ticket.url.pathname:prefix+'/nodes/'+ticket.id))return false;
            ticket.changed=true;
            if(continuing){
                ticket.loading=true;ticket.notice.hidden=false;ticket.message.textContent=label('Loading');
                ticket.timer=win.setTimeout(()=>{if(current===ticket){ticket.loading=false;ticket.notice.hidden=false;ticket.message.textContent=label('Error');}},30000);
                ticket.frame.contentWindow.location.replace(ticket.url.href);
            }else await finish(false);
            return true;
        }
        function invalidate(message){
            if(!current)return;current.invalid=true;current.notice.hidden=false;current.message.textContent=message;
            child()?.invalidate();
        }
        win.addEventListener('sweetcherry:split-layout',sync);
        const api={open,canLeave,beforeNavigate,finish,saved,invalidate,active:()=>!!current,owns:frame=>current?.frame===frame};
        win.SweetCherryTreeEditor=api;return api;
    }
    return {install,editorUrl};
});
