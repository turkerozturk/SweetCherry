(() => {
 'use strict';
 const root=document.querySelector('.template-report');if(!root)return;
 const rows=[...root.querySelectorAll('[data-report-record]')],search=root.querySelector('[data-report-search]'),incomplete=root.querySelector('[data-report-incomplete]');
 const locale=document.documentElement.lang||'en';
 const normal=text=>text.toLocaleLowerCase(locale);
 const texts=rows.map(row=>normal(row.textContent));
 function filter(){const words=normal(search.value).trim().split(/\s+/u).filter(Boolean);let visible=0;rows.forEach((row,index)=>{row.hidden=!(words.every(word=>texts[index].includes(word))&&(!incomplete.checked||row.dataset.incomplete==='true'));if(!row.hidden)visible++;});root.querySelector('[data-report-count]').textContent=visible+' / '+rows.length;root.querySelector('[data-report-no-match]').hidden=visible!==0||rows.length===0;}
 search.addEventListener('input',filter);incomplete.addEventListener('change',filter);filter();
 const stamp=date=>date.getFullYear()+'-'+String(date.getMonth()+1).padStart(2,'0')+'-'+String(date.getDate()).padStart(2,'0');
 const now=new Date(),today=stamp(now),target=rows.find(row=>row.dataset.recordName===today),todayButton=root.querySelector('[data-report-today]');
 todayButton.disabled=!target;
 function goToday(){if(!target)return;if(target.hidden){search.value='';incomplete.checked=false;filter();}target.classList.add('report-today');target.scrollIntoView({block:'center',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'});}
 todayButton.addEventListener('click',goToday);
 if(target){target.classList.add('report-today');try{const key='template-report:'+root.dataset.source+':'+root.dataset.templateId+':'+location.pathname;if(!sessionStorage.getItem(key)){sessionStorage.setItem(key,'1');requestAnimationFrame(goToday);}}catch{/* Manual button remains available when storage is disabled. */}}
 root.querySelectorAll('.field-content').forEach(cell=>{const text=cell.textContent.toUpperCase();if(text.includes('TODO '))cell.classList.add('report-todo');if(text.includes('FOCUS '))cell.classList.add('report-focus');});
 const end=new Date(now);end.setDate(end.getDate()+90);const alarms=root.querySelector('[data-report-alarms]'),list=alarms.querySelector('ul');
 rows.filter(row=>/^\d{4}-\d{2}-\d{2}$/.test(row.dataset.recordName)&&row.dataset.recordName>=today&&row.dataset.recordName<=stamp(end)).forEach(row=>row.querySelectorAll('[data-field-key="dailyalarm"] .field-content').forEach(field=>{const li=document.createElement('li');li.textContent=row.dataset.recordName+' — '+field.textContent;list.append(li);}));alarms.hidden=!list.children.length;
 root.querySelector('[data-report-copy]').addEventListener('click',async event=>{const button=event.currentTarget;try{await navigator.clipboard.writeText(button.dataset.copyText);const before=button.textContent;button.textContent=button.dataset.done;setTimeout(()=>button.textContent=before,2000);}catch{const input=document.createElement('textarea');input.value=button.dataset.copyText;root.append(input);input.select();document.execCommand('copy');input.remove();}});
})();
