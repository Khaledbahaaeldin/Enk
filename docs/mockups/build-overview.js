// Builds enk-themes-overview.html with the v4 mockup embedded, so it works anywhere with no file loading.
// Usage: node docs/mockups/build-overview.js
const fs = require('fs'), path = require('path');
const dir = __dirname;
const src = fs.readFileSync(path.join(dir, 'enk-screens-v4-themes.html'), 'utf8');
const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Enk themes overview</title>
<style>
*{box-sizing:border-box}
html,body{margin:0;background:#0c0b0d;color:#cfc8ba;font:13px -apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,sans-serif}
.top{position:sticky;top:0;z-index:5;display:flex;gap:8px;align-items:center;padding:10px 16px;background:#0c0b0d;border-bottom:1px solid #1f1d22;flex-wrap:wrap}
.top button{font:inherit;color:#cfc8ba;background:#1a191d;border:1px solid #2e2c33;border-radius:7px;padding:6px 12px;cursor:pointer}
.top button.on{background:#f3eee3;color:#161514;border-color:#f3eee3}
.top span{color:#8b8578;margin-right:6px}
.grid{display:grid;grid-template-columns:repeat(6,max-content);gap:26px 18px;padding:18px;justify-content:center}
.cell h3{margin:0 0 8px;font:600 13px system-ui;letter-spacing:.02em;color:#f3eee3;width:calc(400px*var(--s))}
.cell h3 em{display:block;font-style:normal;color:#8b8578;font-weight:400;font-size:11px;margin-top:2px}
.fr{width:calc(400px*var(--s));height:calc(820px*var(--s));position:relative;border-radius:calc(44px*var(--s));overflow:hidden}
.fr iframe{position:absolute;left:0;top:0;width:400px;height:820px;border:0;transform:scale(var(--s));transform-origin:0 0}
</style>
</head>
<body>
<div class="top"><span>Screen</span><div id="scr"></div><span style="margin-left:14px">Mode</span><div id="mod"></div></div>
<div class="grid" id="grid"></div>
<script>
const SRC=${JSON.stringify(src).replace(/<\/script/g, '<\\/script')};
const THEMES=[['graphite','Graphite','The original: warm graphite, paper, orange'],['apricot','Apricot','Espresso · Burnt Peach · Soft Apricot'],['sky','Sky','Deep Mocha · Pale Sky · Vanilla Custard'],['slate','Slate','Onyx · Blue Slate · Alabaster Grey'],['mono','Mono','Neutral greys, no hue'],['dynamic','Dynamic','From the Android wallpaper (Material You)']];
const SEEDS=[[135,'Meadow'],[265,'Dusk'],[205,'Ocean'],[345,'Rose'],[35,'Amber']];
let seed=135;
const SCREENS=[['box','Box'],['settings','Setup'],['search','Find'],['trash','Trash'],['menu','Hold a note'],['divider','Sheet'],['alert','Alert']];
let screen='box',modes=['dark','light'];
const grid=document.getElementById('grid');
function s(){return Math.max(.26,Math.min(.5,(innerWidth-36-5*18)/6/400))}
function render(){document.documentElement.style.setProperty('--s',s());grid.innerHTML='';
 modes.forEach(m=>THEMES.forEach(([k,n,c])=>{const cell=document.createElement('div');cell.className='cell';
  cell.innerHTML='<h3>'+n+' · '+m[0].toUpperCase()+m.slice(1)+'<em>'+c+'</em></h3><div class="fr"></div>';
  const f=document.createElement('iframe');f.name='bare=1&theme='+k+'&mode='+m+'&screen='+screen+'&seed='+seed;f.srcdoc=SRC;cell.querySelector('.fr').appendChild(f);grid.appendChild(cell)}))}
function btns(id,list,cur,on){const box=document.getElementById(id);box.style.display='inline-flex';box.style.gap='6px';box.innerHTML='';list.forEach(([k,l])=>{const b=document.createElement('button');b.textContent=l;b.className=cur(k)?'on':'';b.onclick=()=>{on(k);build()};box.appendChild(b)})}
function build(){btns('scr',SCREENS,k=>k===screen,k=>{screen=k;render()});
 btns('mod',[['both','Both'],['dark','Dark'],['light','Light']],k=>(k==='both'&&modes.length===2)||(modes.length===1&&modes[0]===k),k=>{modes=k==='both'?['dark','light']:[k];render()})}
addEventListener('resize',()=>document.documentElement.style.setProperty('--s',s()));
const sd=document.createElement('span');sd.textContent='Wallpaper colour (Dynamic)';sd.style.marginLeft='14px';document.querySelector('.top').appendChild(sd);
const sb=document.createElement('div');sb.id='sd';document.querySelector('.top').appendChild(sb);
function seedBtns(){sb.style.display='inline-flex';sb.style.gap='6px';sb.innerHTML='';SEEDS.forEach(([h,n])=>{const b=document.createElement('button');b.textContent=n;b.className=h===seed?'on':'';b.onclick=()=>{seed=h;seedBtns();render()};sb.appendChild(b)})}
seedBtns();build();render();
<\/script>
</body>
</html>`;
fs.writeFileSync(path.join(dir, 'enk-themes-overview.html'), html);
console.log('wrote enk-themes-overview.html', html.length, 'bytes');
