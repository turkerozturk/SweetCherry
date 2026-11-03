(() => {
 'use strict';
 const choice=document.getElementById('layoutRecord');if(!choice)return;
 function selected(){document.querySelectorAll('[data-record-view-form] input[name=recordId]').forEach(input=>input.value=choice.value);}
 choice.addEventListener('change',selected);document.querySelectorAll('[data-record-view-form]').forEach(form=>form.addEventListener('submit',selected));selected();
})();
