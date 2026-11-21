// Minimal DOM for the real, text-node-only symbol renderer and picker. innerHTML is forbidden.
function dom(){
 let active=null;
 class Text{constructor(value){this.nodeType=3;this.nodeValue=value;this.parentElement=null;}get nextSibling(){const p=this.parentElement;return p?p.children[p.children.indexOf(this)+1]||null:null;}after(...nodes){this.parentElement.insertAfter(this,nodes);}get textContent(){return this.nodeValue;}get isConnected(){return true;}replaceWith(node){this.parentElement.replace(this,node);}}
 class Element{
  constructor(tag){this.nodeType=tag==='#fragment'?11:1;this.tagName=tag.toUpperCase();this.children=[];this.attrs={};this.dataset={};this.listeners={};this.value='';this.disabled=false;this.readOnly=false;this.maxLength=-1;this.selectionStart=0;this.selectionEnd=0;this.open=false;this.className='';this.style={};}
  get attributes(){return Object.entries(this.attrs).map(([name,value])=>({name,value}));}get nextSibling(){const p=this.parentElement;return p?p.children[p.children.indexOf(this)+1]||null:null;}get isConnected(){return true;}get parentNode(){return this.parentElement;}set innerHTML(_){throw Error('Unsafe HTML');}
  get childNodes(){return this.children;}get textContent(){return this.children.map(n=>n.textContent).join('');}set textContent(value){this.replaceChildren(new Text(value));}
  append(...nodes){for(const node of nodes){if(node.nodeType===11){this.append(...node.children);continue;}node.parentElement=this;this.children.push(node);}}
  insertAfter(old,nodes){const index=this.children.indexOf(old);for(const n of nodes)n.parentElement=this;this.children.splice(index+1,0,...nodes);}
  replaceChildren(...nodes){this.children=[];this.append(...nodes);}replace(old,node){const index=this.children.indexOf(old),nodes=node.nodeType===11?[...node.children]:[node];for(const n of nodes)n.parentElement=this;this.children.splice(index,1,...nodes);old.parentElement=null;}
  setAttribute(key,value){this.attrs[key]=String(value);if(key.startsWith('data-'))this.dataset[key.slice(5).replace(/-([a-z])/g,(_,c)=>c.toUpperCase())]=String(value);}
  getAttribute(key){return this.attrs[key]??null;}hasAttribute(key){return key in this.attrs;}
  matches(selector){return selector.split(',').some(part=>{part=part.trim();if(part.includes(' '))return false;if(part.startsWith('.'))return this.className.split(' ').includes(part.slice(1));if(part.startsWith('[style*='))return (this.attrs.style||'').includes('monospace');if(part.startsWith('[')){const attrs=[...part.matchAll(/\[([^=\]]+)(?:="([^\"]*)")?\]/g)];return attrs.length>0&&attrs.every(m=>this.hasAttribute(m[1])&&(m[2]===undefined||this.getAttribute(m[1])===m[2]));}return this.tagName===part.toUpperCase();});}
  closest(selector){let node=this;while(node){if(node.matches?.(selector))return node;node=node.parentElement;}return null;}
  querySelectorAll(selector){const result=[];for(const node of this.children)if(node.nodeType!==3){if(node.matches(selector))result.push(node);result.push(...node.querySelectorAll(selector));}return result;}
  querySelector(selector){return this.querySelectorAll(selector)[0]||null;}addEventListener(type,handler){this.listeners[type]=handler;}dispatchEvent(event){this.listeners[event.type]?.(event);return true;}
  focus(){active=this;}select(){this.selectionStart=0;this.selectionEnd=this.value.length;}setSelectionRange(a,b){this.selectionStart=a;this.selectionEnd=b;}
  setRangeText(text,a,b){this.value=this.value.slice(0,a)+text+this.value.slice(b);this.selectionStart=this.selectionEnd=a+text.length;}
  showModal(){this.open=true;}close(){this.open=false;this.listeners.close?.({});}click(){this.listeners.click?.({target:this,preventDefault(){}});}
 }
 const document={body:null,currentScript:{dataset:{ctbBase:'/app/img/ctbicons/'}},listeners:{},createElement:tag=>new Element(tag),createTextNode:value=>new Text(value),createDocumentFragment:()=>new Element('#fragment'),createTreeWalker(root){const nodes=[];function collect(node){for(const child of node.children||[])if(child.nodeType===3)nodes.push(child);else collect(child);}collect(root);return{nextNode:()=>nodes.shift()||null};},addEventListener(type,handler){this.listeners[type]=handler;},get activeElement(){return active;},execCommand:()=>false};
 return{document,Element,Text};
}
module.exports={dom};
