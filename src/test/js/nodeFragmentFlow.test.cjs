const test=require('node:test'),assert=require('node:assert/strict');
const {inline}=require('../../main/resources/static/js/common/nodeFragmentFlow.js');
function node(tag,text){
 const n={tagName:tag?.toUpperCase(),nodeType:tag?1:3,parent:null,childNodes:[],attrs:{},value:text||'',
 get firstChild(){return this.childNodes[0];},get attributes(){return Object.entries(this.attrs).map(([name,value])=>({name,value}));},
 get textContent(){return this.nodeType===3?this.value:this.childNodes.map(c=>c.textContent).join('');},
 setAttribute(k,v){this.attrs[k]=v;},getAttribute(k){return this.attrs[k]??null;},
 append(...children){for(const child of children){child.remove();child.parent=this;this.childNodes.push(child);}},
 remove(){if(this.parent){const siblings=this.parent.childNodes;siblings.splice(siblings.indexOf(this),1);this.parent=null;}},
 replaceWith(next){const parent=this.parent,index=parent.childNodes.indexOf(this);this.remove();next.remove();next.parent=parent;parent.childNodes.splice(index,0,next);},
 before(next){next.remove();const parent=this.parent;next.parent=parent;parent.childNodes.splice(parent.childNodes.indexOf(this),0,next);}};
 if(tag&&text)n.append(node(null,text));return n;
}
const doc={createElement:tag=>node(tag)};
function text(root){return root.tagName==='BR'?'\n':root.nodeType===3?root.value:root.childNodes.map(text).join('');}
test('rich-text filename starts at the insertion point and keeps a following suffix on the same line',()=>{
 const body=node('body'),wrapper=node('div'),line=node('div','sweetcherry_1.11.0_all_os_portable.zip');line.setAttribute('style','white-space:pre-wrap;text-align:left;margin:0');wrapper.append(line);body.append(wrapper);
 inline(body,doc);assert.equal(body.firstChild.tagName,'SPAN');assert.equal(body.firstChild.firstChild.tagName,'SPAN');
 assert.equal('Get-FileHash -Algorithm SHA256 .\\'+text(body)+' /suffix','Get-FileHash -Algorithm SHA256 .\\sweetcherry_1.11.0_all_os_portable.zip /suffix');
 assert.match(body.firstChild.firstChild.getAttribute('style'),/pre-wrap/);
});
test('actual BR boundaries preserve multiline values, blank lines and inline styling',()=>{
 const body=node('body'),wrapper=node('div'),first=node('div','first'),blank=node('div'),last=node('div');blank.append(node('br'));const bold=node('strong','last');last.append(bold);wrapper.append(first,blank,last);body.append(wrapper);
 inline(body,doc);assert.equal(text(body),'first\n\nlast');assert.equal(body.firstChild.childNodes.at(-1).firstChild,bold);
 const plain=node('body');plain.append(node('div','A\nB'));inline(plain,doc);assert.equal(text(plain),'A\nB');
});
test('terminal blank lines remain and tables keep their block structure',()=>{
 const body=node('body'),last=node('div');last.append(node('br'));body.append(node('div','A'),last,node(null,'\n'));inline(body,doc);assert.equal(text(body),'A\n\n');
 const table=node('table'),cell=node('td','cell');table.append(cell);const mixed=node('body');mixed.append(table);inline(mixed,doc);assert.equal(mixed.firstChild,table);assert.equal(table.firstChild,cell);
});

test('mixed inline text and paragraph blocks retain internal boundaries without adding edge breaks',()=>{
 const body=node('body');body.append(node(null,'prefix'),node('div','middle'),node('strong','suffix'));inline(body,doc);
 assert.equal(text(body),'prefix\nmiddle\nsuffix');
});

test('embedded codeboxes keep their scrolling wrapper and preformatted block',()=>{
 const body=node('body'),box=node('div'),pre=node('pre','A\nB');box.setAttribute('style','max-width:100%;overflow-x:auto');box.append(pre);body.append(box);inline(body,doc);assert.equal(body.firstChild,box);assert.equal(box.firstChild,pre);assert.equal(text(body),'A\nB');
});
