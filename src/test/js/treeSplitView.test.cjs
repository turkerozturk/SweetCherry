const test=require('node:test'),assert=require('node:assert/strict');
const core=require('../../main/resources/static/js/node/treeSplitState.js');
const {install}=require('../../main/resources/static/js/node/treeSplitView.js');
class Element {
    constructor(){this.dataset={};this.listeners={};this.attributes={};this.style={setProperty:(key,value)=>{this.attributes[key]=value;}};}
    addEventListener(type,handler){(this.listeners[type]??=[]).push(handler);}
    dispatchEvent(event){for(const handler of this.listeners[event.type]||[])handler(event);return true;}
    emit(type,fields={}){return this.dispatchEvent({type,...fields});}
    setAttribute(key,value){this.attributes[key]=value;}
    removeAttribute(key){delete this[key];delete this.attributes[key];}
    replaceChildren(...nodes){this.nodes=nodes;}
    closest(selector){return this.matches?.includes(selector)?this:null;}
    setPointerCapture(id){this.capture=id;}
    hasPointerCapture(id){return this.capture===id;}
    getBoundingClientRect(){return {left:0,top:0,width:1000,height:500};}
}
const fragment=value=>({value,cloneNode(){return fragment(value);}});
function setup(saved={},query='',normalizeBeforeReady=false){
    const ids=['workspace-readers','workspace-left','workspace-right','workspace-right-frame','workspace-content-splitter','workspace-status','workspace-right-open','workspace-tree'];
    const elements=Object.fromEntries(ids.map(id=>[id,new Element()]));
    const controls=['left','right'].map(side=>{const el=new Element();el.dataset.splitActivate=side;el.matches=['[data-split-activate]'];return el;});
    const toggle=new Element();toggle.matches=['[data-tree-split-toggle]'];
    const doc=new Element(),win=new Element(),storage=new Map();
    storage.set('sweetcherry.split:tenant',JSON.stringify(saved));
    win.sessionStorage={getItem:key=>storage.get(key),setItem:(key,value)=>storage.set(key,value)};
    let leftId=10,editor=false,allowClose=true,confirmations=0,stops=0;const local=[],remote=[],reveals=[],moves=[];
    const leftFragment={dataset:{nodeTitle:'Left note'},querySelector:selector=>selector==='template[data-workspace-status]'?{content:fragment('left status')}:{dataset:{pathIds:'1,10'}}};
    doc.getElementById=id=>elements[id];
    doc.querySelectorAll=selector=>selector==='[data-tree-split-toggle]'?[toggle]:controls;
    doc.querySelector=()=>leftFragment;doc.body={classList:{add(){},remove(){}}};
    win.document=doc;win.parent=win;win.SweetCherrySplitState=core;
    win.location={href:'http://localhost:8080/tree?nodeId=10'+query,origin:'http://localhost:8080'};
    win.history={replaceState(state,title,url){win.location.href=new URL(url,win.location.href).href;}};
    win.matchMedia=()=>({matches:false});win.CustomEvent=class {constructor(type,options={}){this.type=type;this.detail=options.detail;}};
    win.confirm=()=>{confirmations++;return allowClose;};
    const frame=elements['workspace-right-frame'];
    frame.contentDocument=new Element();frame.contentDocument.querySelector=()=>editor?{}:null;
    frame.contentWindow={SweetCherryWorkspace:{select:async id=>remote.push(id),status:()=>fragment('right status'),move:direction=>moves.push(direction)}};
    const workspace={dataset:{splitCloseConfirm:'Close form?',stale:'Stale database'}};
    const api=install({win,workspace,token:'tenant',getSelected:()=>leftId,selectLocal:async id=>{leftId=Number(id);local.push(id);},reveal:async(id,path)=>reveals.push({id,path}),stop:()=>stops++});
    if(normalizeBeforeReady)win.history.replaceState({},'','/tree?nodeId=10');
    api.ready();
    return {api,win,doc,elements,toggle,controls,local,remote,reveals,moves,storage,
        click(target){doc.emit('click',{target,preventDefault(){}});},
        loaded(id=20,token='tenant'){frame.emit('sweetcherry:split-pane',{detail:{type:'loaded',token,id,name:'Right note',path:'2,'+id}});},
        get state(){return JSON.parse(storage.get('sweetcherry.split:tenant'));},
        set editor(value){editor=value;frame.contentWindow.SweetCherryWorkspace=value?null:frame.contentWindow.SweetCherryWorkspace;},
        set allowClose(value){allowClose=value;},get confirmations(){return confirmations;},get stops(){return stops;}};
}
test('split state tolerates unavailable or corrupt storage and rejects stored URLs as node identifiers',()=>{
    for(const storage of [undefined,{getItem(){throw Error();}},{getItem:()=>'{broken'}])assert.deepEqual(core.read(storage,'x'),{open:false,node:null,active:'left',ratio:50});
    const value=core.read({getItem:()=>JSON.stringify({open:true,node:'https://evil.test',active:'other',ratio:999})},'x');
    assert.deepEqual(value,{open:true,node:0,active:'left',ratio:80});
    assert.equal(core.node('-1'),0);assert.equal(core.node('9007199254740993'),0);assert.equal(core.node('55'),55);
});
test('embedding requires the designated same-origin frame and handles cross-origin access failures',()=>{
    const win={frameElement:{dataset:{splitFrame:'true'}},location:{origin:'https://notes.test'},parent:{location:{origin:'https://notes.test'}}};
    assert.equal(core.embedded(win),true);win.parent.location.origin='https://external.test';assert.equal(core.embedded(win),false);
    Object.defineProperty(win,'parent',{get(){throw Error('cross origin');}});assert.equal(core.embedded(win),false);
});
test('a closed split does not load a second document; opening routes only the active pane and closing restores left navigation',async()=>{
    const c=setup(),frame=c.elements['workspace-right-frame'];assert.equal(frame.src,undefined);
    await c.api.navigate(11);assert.deepEqual(c.local,[11]);
    c.click(c.toggle);assert.match(frame.src,/nodeId=11&_tenantView=tenant$/);assert.equal(c.state.active,'right');
    await c.api.navigate(22);assert.deepEqual(c.remote,[22]);assert.deepEqual(c.local,[11]);
    c.loaded(22);assert.equal(c.elements['workspace-status'].nodes[0].value,'right status');assert.equal(c.state.node,22);
    c.click(c.controls[0]);await c.api.navigate(12);assert.deepEqual(c.local,[11,12]);assert.deepEqual(c.remote,[22]);
    c.click(c.toggle);assert.equal(c.state.open,false);assert.equal(frame.src,undefined);assert.equal(c.elements['workspace-right'].hidden,true);
});
test('clicking or focusing a pane targets that pane and right move shortcuts never submit the left form',async()=>{
    const c=setup({open:true,node:25,active:'left'});assert.equal(c.api.activeRight(),false);
    c.loaded(25);assert.equal(c.win.document.title,'Left note — SweetCherry');
    c.elements['workspace-right-frame'].emit('load');
    c.elements['workspace-right-frame'].contentDocument.emit('focusin');assert.equal(c.api.activeRight(),true);
    c.api.move('UP');assert.deepEqual(c.moves,['UP']);
    await c.api.navigate(26);assert.deepEqual(c.remote,[26]);
    c.elements['workspace-left'].emit('pointerdown');await c.api.navigate(13);assert.deepEqual(c.local,[13]);
});
test('pane selection validates tenant identity, preserves the other pane and updates only navigation and status',()=>{
    const c=setup({open:true,node:20,active:'right'});c.loaded(99,'another-tenant');assert.equal(c.state.node,20);
    c.loaded(31);assert.deepEqual(c.local,[]);assert.deepEqual(c.remote,[]);assert.equal(c.state.node,31);
    assert.equal(c.win.document.title,'Right note — SweetCherry');assert.ok(c.reveals.some(item=>item.id===31&&item.path==='2,31'));
    assert.match(c.elements['workspace-right-open'].href,/nodeId=31&_tenantView=tenant$/);
    c.elements['workspace-right-frame'].emit('sweetcherry:split-pane',{detail:{type:'stop',token:'tenant'}});assert.equal(c.stops,1);
});
test('restoration is scoped to the tenant and explicit menu links open a split once',()=>{
    const c=setup({open:true,node:45,ratio:65,active:'left'});assert.match(c.elements['workspace-right-frame'].src,/nodeId=45/);
    assert.equal(c.elements['workspace-readers'].attributes['--split-ratio'],'65%');assert.equal(c.state.active,'left');
    const fresh=setup({},'&split=1');assert.equal(fresh.state.open,true);assert.equal(new URL(fresh.win.location.href).searchParams.has('split'),false);
    assert.equal(setup({},'&split=1',true).state.open,true,'workspace URL normalization must not discard an explicit open request');
});
test('closing an embedded form requires confirmation and a stale view forcibly stops navigation',async()=>{
    const c=setup({open:true,node:20});c.editor=true;c.allowClose=false;
    c.click(c.toggle);assert.equal(c.state.open,true);assert.equal(c.confirmations,1);
    c.allowClose=true;c.click(c.toggle);assert.equal(c.state.open,false);
    c.click(c.toggle);c.api.stop();await c.api.navigate(50);assert.deepEqual(c.local,[]);assert.deepEqual(c.remote,[]);assert.equal(c.state.open,false);
});
test('splitter keyboard resizing is bounded and remembered',()=>{
    const c=setup({open:true,ratio:79}),separator=c.elements['workspace-content-splitter'];
    separator.emit('keydown',{key:'ArrowRight',preventDefault(){}});assert.equal(c.state.ratio,80);
    for(let i=0;i<20;i++)separator.emit('keydown',{key:'ArrowLeft',preventDefault(){}});
    assert.equal(c.state.ratio,20);
});
test('the virtual root remains the right selection after closing and reopening',()=>{
    const c=setup({open:true,node:0});assert.match(c.elements['workspace-right-frame'].src,/nodeId=0&/);
    c.click(c.toggle);c.click(c.toggle);assert.match(c.elements['workspace-right-frame'].src,/nodeId=0&/);
});
test('loading errors from the active child are displayed and navigation from a form uses a guarded tree URL',async()=>{
    const c=setup({open:true,node:5,active:'right'}),frame=c.elements['workspace-right-frame'];
    frame.emit('sweetcherry:split-pane',{detail:{type:'message',token:'tenant',text:'Could not load'}});
    assert.equal(c.elements['workspace-status'].textContent,'Could not load');
    c.editor=true;await c.api.navigate(6);assert.match(frame.src,/nodeId=6&_tenantView=tenant$/);
});
test('embedded alarm initialization does not start another poller or audio context',()=>{
    const fs=require('node:fs'),vm=require('node:vm');const widget={};
    const context={document:{getElementById:()=>widget},window:{SweetCherryAlarmState:{},SweetCherrySplitState:{embedded:()=>true}}};
    vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'../../main/resources/static/js/reminders/alarms.js'),'utf8'),context);
    assert.equal(widget.hidden,true);
});
test('an embedded reader forwards its node and tenant but never creates a nested split',async()=>{
    const frame=new Element(),win={parent:{location:{origin:'http://local'}},location:{origin:'http://local'},frameElement:frame,SweetCherrySplitState:core};
    frame.dataset.splitFrame='true';win.CustomEvent=class{constructor(type,options){this.type=type;this.detail=options.detail;}};
    const messages=[];frame.addEventListener('sweetcherry:split-pane',event=>messages.push(event.detail));
    const selected=[];const api=install({win,token:'current',selectLocal:async id=>selected.push(id)});
    await api.navigate(5);api.loaded(5,'Node','1,5');api.restoreFocus(5);api.stop();
    assert.deepEqual(selected,[5]);assert.deepEqual(messages.map(item=>item.type),['loaded','focus','stop']);
    assert.ok(messages.every(item=>item.token==='current'));assert.equal(messages[0].path,'1,5');
});
