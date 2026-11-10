const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
function picker(locked){
 const elements={};
 for(const id of ['codeSyntax','typeCode','propertySyntaxPicker','propertySyntaxSearch','propertySyntaxDetails','propertySyntaxSelected','propertyMarkdown','propertyHtml'])elements[id]={value:'',checked:false,dataset:{locked:String(locked)},options:[{textContent:'Choose'}],handlers:{},addEventListener(type,handler){this.handlers[type]=handler;},dispatchEvent(event){this.handlers[event.type]?.(event);},setAttribute(){},setCustomValidity(){}};
 elements.codeSyntax.value='plain-text';elements.propertySyntaxPicker.querySelectorAll=()=>[];elements.propertySyntaxDetails.querySelector=()=>({setAttribute(){}});
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/node/propertySyntaxPicker.js'),'utf8'),{document:{getElementById:id=>elements[id],querySelectorAll:()=>[elements.typeCode]},Event});
 return elements;
}
test('HTML shortcut selects the submitted html syntax and CODE type',()=>{const e=picker(false);e.propertyHtml.handlers.click();assert.equal(e.typeCode.checked,true);assert.equal(e.codeSyntax.value,'html');assert.equal(e.codeSyntax.disabled,false);assert.equal(e.propertySyntaxSelected.textContent,'html');});
test('HTML shortcut respects syntax locks',()=>{const e=picker(true);e.propertyHtml.handlers.click();assert.equal(e.typeCode.checked,false);assert.equal(e.codeSyntax.value,'plain-text');assert.equal(e.codeSyntax.disabled,true);});
