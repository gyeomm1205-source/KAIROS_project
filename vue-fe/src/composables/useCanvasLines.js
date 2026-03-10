/**
 * useCanvasLines.js
 */
import { watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useThemeStore } from '@/stores/useThemeStore'
import { storeToRefs } from 'pinia'

export const WEEK_LANE_SPACING = 28
export const WEEK_TOP_MARGIN   = 28

const ymd = s => { const [y,m,d] = s.split('-').map(Number); return new Date(y,m-1,d) }
const ds  = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`

const LINE_ALPHA = 0.92
const SYS_W      = 18
const SYS_ALPHA  = 0.22
const GAP_EXTRA  = 6
const DOT_R      = 5.5
const DOT_BDR    = 3.5 
const PLUG_PX    = 18
const BEND_PLUG  = [15, 25, 35, 45]  

const MONTH_BOT  = 10
const REGULAR_SP = 13   
const HL_SP      = 24   

const isHL = (t) => t && (t.isHighlight || t.id?.startsWith('hl_') || t.name?.includes('프롬프트') || t.name?.includes('블로그'))

function w(ctx, fn) { ctx.save(); fn(); ctx.restore() }

function pLine(x1, y1, x2, y2) { const p = new Path2D(); p.moveTo(x1, y1); p.lineTo(x2, y2); return p; }
function pBend(sx, sy, ex, ey, bendX) {
  const p = new Path2D();
  const dy = ey - sy; const ady = Math.abs(dy);
  const R = Math.min(ady * 0.45, WEEK_LANE_SPACING * 0.55, 12);
  const dY = dy > 0 ? 1 : -1; const dX = ex >= bendX ? 1 : -1;
  if (ady < 1) { p.moveTo(sx, sy); p.lineTo(ex, ey); }
  else {
    p.moveTo(sx, sy); p.lineTo(bendX, sy);
    p.arcTo(bendX, sy, bendX, sy+dY*R, R);
    p.lineTo(bendX, ey-dY*R);
    p.arcTo(bendX, ey, bendX+dX*R, ey, R);
    p.lineTo(ex, ey);
  }
  return p;
}
function pDetour(sx, sy, ex, ey, bypassX) {
  const p = new Path2D();
  const dy = ey - sy; const ady = Math.abs(dy);
  const R = Math.min(ady * 0.35, 9);
  const dY = dy > 0 ? 1 : -1;
  p.moveTo(sx, sy);
  p.arcTo(bypassX, sy, bypassX, sy+dY*R, R);
  p.lineTo(bypassX, ey-dY*R);
  p.arcTo(bypassX, ey, bypassX-R, ey, R);
  p.lineTo(ex, ey);
  return p;
}

function drawPath(ctx, p, color, lw, alpha, gap=false) {
  w(ctx, () => {
    ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    if (gap) {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.globalAlpha = 1; ctx.strokeStyle = 'rgba(0,0,0,1)'; ctx.lineWidth = lw + GAP_EXTRA;
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = alpha; ctx.strokeStyle = color; ctx.lineWidth = lw;
    }
    ctx.stroke(p);
  });
}
function drawHLPath(ctx, p, color, alphaMult) {
  w(ctx, () => {
    ctx.globalCompositeOperation = 'destination-over';
    ctx.globalAlpha = SYS_ALPHA * alphaMult;
    ctx.strokeStyle = color; ctx.lineWidth = SYS_W; ctx.lineCap = 'round'; ctx.stroke(p);
  });
}

function buildLayout({ mode: vMode, W, allTracks, schedules, focusedDay, cy, cm, firstDow, zoneEl, scrollEl }) {
  const sorted = [...allTracks].sort((a,b) => {
    if(a.isHighlight && !b.isHighlight) return 1;
    if(!a.isHighlight && b.isHighlight) return -1;
    return a.index - b.index
  })
  
  const sysSet = new Set(sorted.filter(t => isHL(t)).map(t => t.id))
  const isSys  = id => sysSet.has(id)
  
  // ★ 1:1:1:1:1:1:1 비율을 위한 정확한 7등분 계산
  const CW = W / 7;
  
  const firstCell = scrollEl ? scrollEl.querySelector?.('.calendar-cell') : document.querySelector('.calendar-cell')
  const rowH = firstCell ? firstCell.getBoundingClientRect().height : (parseInt(getComputedStyle(document.documentElement).getPropertyValue('--month-row-height')) || 150)

  let rangeStart, rangeEnd, totalRows, wLaneY = null

  if (vMode === 'week') {
    const fd = ymd(focusedDay)
    const ws = new Date(fd); ws.setDate(fd.getDate() - fd.getDay())
    const we = new Date(ws); we.setDate(ws.getDate() + 6)
    rangeStart = ds(ws); rangeEnd = ds(we); totalRows = 1
    
    wLaneY = {}
    let currentWIdx = null;
    let currentWOffset = WEEK_TOP_MARGIN;
    sorted.forEach((t) => {
      if (currentWIdx !== t.index) {
        if (currentWIdx !== null) currentWOffset += WEEK_LANE_SPACING;
        currentWIdx = t.index;
      }
      wLaneY[t.id] = currentWOffset;
    })
  } else {
    const cellNodes = scrollEl ? scrollEl.querySelectorAll('.calendar-cell') : [];
    if (cellNodes.length > 0) {
      rangeStart = cellNodes[0].id.replace('day-', '');
      rangeEnd   = cellNodes[cellNodes.length - 1].id.replace('day-', '');
      totalRows  = cellNodes.length / 7;
    } else {
      rangeStart = ds(new Date(cy, cm-1, 1));
      rangeEnd   = ds(new Date(cy, cm, 0));
      totalRows  = Math.ceil((new Date(cy,cm,0).getDate() + firstDow) / 7);
    }
  }

  const getTrackOffset = (trackId) => {
    let offset = 0;
    let currentIdx = null;
    for (const t of sorted) {
      if (currentIdx !== t.index) {
        if (currentIdx !== null) offset += isHL(t) ? HL_SP : REGULAR_SP;
        currentIdx = t.index;
      }
      if (t.id === trackId) return offset;
    }
    return offset;
  }

  const mLaneY = (trackId, row) => {
    const t = allTracks.find(x => x.id === trackId)
    if (!t) return (row+1)*rowH - MONTH_BOT
    return (row+1)*rowH - MONTH_BOT - getTrackOffset(trackId) - (isSys(trackId) ? 3 : 0)
  }
  const getLaneY = (trackId, row=0) => vMode === 'week' ? wLaneY[trackId] : mLaneY(trackId, row)
  
  const cell = d => {
    if (!d) return null
    if (vMode === 'week') {
      if (d < rangeStart || d > rangeEnd) return null
      return { row: 0, col: ymd(d).getDay() }
    }
    const targetDate = ymd(d); const startDate = ymd(rangeStart);
    const diffDays = Math.round((targetDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays < 0 || diffDays >= totalRows * 7) return null;
    return { row: Math.floor(diffDays / 7), col: diffDays % 7 };
  }
  const getX = id => {
    const el = document.getElementById(`node-${id}`)
    if (!el) return null
    const baseEl = vMode === 'week' ? zoneEl : scrollEl
    if (!baseEl) return null
    const r = el.getBoundingClientRect(), base = baseEl.getBoundingClientRect()
    return Math.round(r.left - base.left + baseEl.scrollLeft + (r.width / 2))
  }
  return { sorted, isSys, CW, rowH, totalRows, rangeStart, rangeEnd, wLaneY, mLaneY, getLaneY, getX, cell, scrollEl, zoneEl }
}

export function useCanvasLines(calendarWrapper, lineCanvas, currentView, currentYear, currentMonth, focusedDay, weekGraphZone, monthScrollBody, hiddenTracks, interactionState, callbacks) {
  const store = useCalendarStore()
  const themeStore = useThemeStore() 
  const { schedules, connections, tracks, allTracks } = storeToRefs(store)
  const getTrack = id => allTracks.value.find(t=>t.id===id) || tracks.value[0]
  
  const getBg = () => {
    const themeEl = document.querySelector('.app-root') || document.body
    const cssVarBorder = getComputedStyle(themeEl).getPropertyValue('--node-border').trim()
    const cssVarSurface = getComputedStyle(themeEl).getPropertyValue('--bg-surface').trim()
    if (cssVarBorder) return cssVarBorder
    if (cssVarSurface) return cssVarSurface
    return (themeEl.classList.contains('theme-dark') || document.body.classList.contains('is-dark')) ? '#1e1e24' : '#ffffff'
  }
  const getHidden = () => hiddenTracks?.value ?? new Set()

  let ro=null, slideAnim=null
  let currentHitPaths = []; 
  let cvsParent = null;

  const drawLines = (offset=0) => {
    const cvs  = lineCanvas.value;  if (!cvs) return
    const wrap = calendarWrapper?.value; if (!wrap) return
    const isW  = currentView.value === 'week'
    const bg   = getBg(); const hidden = getHidden()
    let W, H, ctx

    if (isW) {
      const zone = weekGraphZone?.value; if (!zone) return
      W=zone.offsetWidth; H=zone.offsetHeight; if (!W||!H) return
      cvs.width=W; cvs.height=H; cvs.style.width=W+'px'; cvs.style.height=H+'px'
      ctx=cvs.getContext('2d'); ctx.clearRect(0,0,W,H)
      const L = buildLayout({ mode:'week', W, allTracks:allTracks.value, schedules:schedules.value, focusedDay:focusedDay?.value||'', zoneEl:zone, scrollEl:null })
      if (offset) { ctx.save(); ctx.translate(offset,0) }
      executeDrawGraph(ctx, W, H, 'week', L, bg, hidden)
      if (offset) ctx.restore()
    } else {
      const se = monthScrollBody?.value || wrap
      W=se.offsetWidth; H=se.scrollHeight; if (!W||!H) return
      cvs.width=W; cvs.height=H; cvs.style.width=W+'px'; cvs.style.height=H+'px'
      ctx=cvs.getContext('2d'); ctx.clearRect(0,0,W,H)
      const cy=currentYear.value, cm=currentMonth.value
      const L = buildLayout({ mode:'month', W, allTracks:allTracks.value, schedules:schedules.value, cy, cm, firstDow:new Date(cy,cm-1,1).getDay(), zoneEl:null, scrollEl:se })
      executeDrawGraph(ctx, W, H, 'month', L, bg, hidden)
    }
    attachEvents(); 
  }

  function executeDrawGraph(ctx, W, H, vMode, layout, bg, hidden) {
    const { sorted, isSys, CW, totalRows, rangeStart, getLaneY, getX, cell, zoneEl } = layout
    const isHidden = id => hidden.has(id)
    const baseLineW = vMode === 'week' ? 3.5 : 2;

    const rowDates = []
    if (vMode === 'week') rowDates.push({ s: rangeStart, e: layout.rangeEnd })
    else {
      const startD = ymd(rangeStart);
      for(let r=0; r<totalRows; r++) {
        const s = new Date(startD); s.setDate(s.getDate() + r*7);
        const e = new Date(s);      e.setDate(e.getDate() + 6);
        rowDates.push({ s: ds(s), e: ds(e) });
      }
    }

    const exactRowY = {}
    const getExactLaneY = (trackId, r) => {
      const key = `${trackId}-${r}`
      if (exactRowY[key] !== undefined) return exactRowY[key]
      if (vMode === 'week') {
        exactRowY[key] = Math.round(layout.wLaneY[trackId]);
        return exactRowY[key];
      }
      exactRowY[key] = Math.round(getLaneY(trackId, r))
      return exactRowY[key]
    }

    const getRowX = (node, r) => {
      const rd = rowDates[r]; 
      
      // ★ 주간 뷰: 각 칸의 정중앙 계산 (7등분 동일 비율)
      if (vMode === 'week') {
        if (node.day < rd.s) return -10;
        if (node.day > rd.e) return W + 10;
        const d = ymd(node.day);
        const colIdx = Math.round((d.getTime() - ymd(rd.s).getTime()) / (1000 * 3600 * 24));
        return Math.round((colIdx + 0.5) * CW);
      }

      if (node.day < rd.s) return -2; 
      if (node.day > rd.e) return Math.round(W + 2)
      const x = getX(node.id); if (x != null) return x
      const c = cell(node.day); if (c) return Math.round((c.col + 0.5) * CW)
      return 0
    }

    const edges = []
    const fromC={}, toC={}
    connections.value.forEach(c => {
      const s=schedules.value.find(n=>n.id===c.from), e=schedules.value.find(n=>n.id===c.to)
      if (!s || !e) return
      fromC[c.from]=(fromC[c.from]||0)+1; toC[c.to]=(toC[c.to]||0)+1
    })

    connections.value.forEach(c => {
      const s = schedules.value.find(x=>x.id===c.from), e = schedules.value.find(x=>x.id===c.to)
      if (!s || !e || isHidden(s.track) || isHidden(e.track)) return
      const isBranch = (fromC[s.id]||0)>=1 && (toC[e.id]||0)<=1
      const color = isBranch ? (getTrack(e.track)?.color||'#aaa') : (getTrack(s.track)?.color||'#aaa')
      edges.push({ id: `e_${s.id}_${e.id}`, from: s, to: e, track: s.track, targetTrack: e.track, color, isCross: s.track !== e.track })
    })

    sorted.forEach(t => {
      if (!isSys(t.id) || isHidden(t.id)) return    
      const ns = schedules.value.filter(s => s.track === t.id).sort((a,b) => a.day.localeCompare(b.day))
      for (let i = 0; i < ns.length - 1; i++) {
        if (!edges.some(e => e.from.id === ns[i].id && e.to.id === ns[i+1].id)) {
          edges.push({ id: `e_${ns[i].id}_${ns[i+1].id}`, from: ns[i], to: ns[i+1], track: t.id, targetTrack: t.id, color: t.color, isCross: false })
        }
      }
    })

    const getTrackIdx = tid => layout.sorted.findIndex(t => t.id === tid)
    const branchesTo = {}
    edges.forEach(e => { if (e.isCross) { if (!branchesTo[e.to.id]) branchesTo[e.to.id] = []; branchesTo[e.to.id].push(e) } })
    Object.values(branchesTo).forEach(branches => {
      branches.forEach(b => { b.sIdx = getTrackIdx(b.track); b.tIdx = getTrackIdx(b.targetTrack); b.dist = Math.abs(b.sIdx - b.tIdx) })
      branches.filter(b => b.sIdx < b.tIdx).sort((a,b) => b.dist - a.dist).forEach((b, i) => { b.ib = i })
      branches.filter(b => b.sIdx > b.tIdx).sort((a,b) => b.dist - a.dist).forEach((b, i) => { b.ib = i })
    })

    const getAlphaMult = (itemType, item) => {
      let { hovered, clicked } = interactionState?.value || {};
      if (hovered && hovered.type === 'track' && hidden.has(hovered.data)) hovered = null;
      if (!hovered && !clicked) return 1;

      let isHL = false;
      if (hovered) {
        if (hovered.type === 'track') {
          if (itemType === 'edge') isHL = (item.track === hovered.data || item.targetTrack === hovered.data);
          if (itemType === 'node') isHL = (item.track === hovered.data);
        } else if (hovered.type === 'node') {
          if (itemType === 'node') isHL = (item.id === hovered.data.id);
        } else if (hovered.type === 'edge') {
          if (itemType === 'edge') isHL = (item.id === hovered.data.id); 
          if (itemType === 'node') isHL = (item.id === hovered.data.from.id || item.id === hovered.data.to.id);
        }
      } else if (clicked) {
        if (clicked.type === 'node') {
          const cId = clicked.data.id; const cTrack = clicked.data.track;
          if (itemType === 'edge') isHL = (item.track === cTrack && item.targetTrack === cTrack) || item.from.id === cId || item.to.id === cId;
          if (itemType === 'node') {
             isHL = (item.track === cTrack);
             if (!isHL) isHL = edges.some(e => (e.from.id === cId && e.to.id === item.id) || (e.to.id === cId && e.from.id === item.id));
          }
        } else if (clicked.type === 'edge') {
          if (itemType === 'edge') isHL = (item.id === clicked.data.id);
          if (itemType === 'node') isHL = (item.id === clicked.data.from.id || item.id === clicked.data.to.id);
        }
      }
      return isHL ? 1 : 0.15;
    };

    const L1=[], L2_gaps=[], L2_draws=[], L_hl=[]; const hitPaths = [];

    for (let r=0; r<totalRows; r++) {
      const rd = rowDates[r]
      edges.forEach(edge => {
        if (edge.to.day < rd.s || edge.from.day > rd.e) return 
        const sx = getRowX(edge.from, r), ex = getRowX(edge.to, r), sy = getExactLaneY(edge.track, r)
        const alphaMult = getAlphaMult('edge', edge); const finalAlpha = LINE_ALPHA * alphaMult;
        let path1, path2;

        if (!edge.isCross) {
          path1 = pLine(sx, sy, ex, sy); hitPaths.push({ edge, path: path1 });
          if (isSys(edge.track)) L_hl.push(() => drawHLPath(ctx, path1, edge.color, alphaMult));
          else L1.push(() => drawPath(ctx, path1, edge.color, baseLineW, finalAlpha));
        } else {
          const isTargetInRow = (edge.to.day >= rd.s && edge.to.day <= rd.e)
          if (!isTargetInRow) {
            path1 = pLine(sx, sy, ex, sy); hitPaths.push({ edge, path: path1 });
            L1.push(() => drawPath(ctx, path1, edge.color, baseLineW, finalAlpha));
          } else {
            const ey = getExactLaneY(edge.targetTrack, r); const finalEy = ey; const ib = edge.ib ?? 0; const bx = ex - BEND_PLUG[Math.min(ib, BEND_PLUG.length-1)]
            const addDraw = (p) => {
              if (alphaMult < 1) L1.push(() => drawPath(ctx, p, edge.color, baseLineW, finalAlpha, false));
              else { L2_gaps.push(() => drawPath(ctx, p, edge.color, baseLineW, 1, true)); L2_draws.push(() => drawPath(ctx, p, edge.color, baseLineW, finalAlpha, false)); }
            }
            if (Math.abs(ex - sx) < CW * 0.85 && sx >= 0 && sx <= W) {
              const bypassX = Math.round(Math.max(sx, ex) + CW * 0.4 + ib * 10)
              path1 = pDetour(sx, sy, ex, finalEy, bypassX); hitPaths.push({ edge, path: path1 }); addDraw(path1);
            } else if (isSys(edge.targetTrack)) {
              const px = Math.round(ex - PLUG_PX - ib * 10)
              path1 = pLine(sx, sy, px, sy); path2 = pBend(px, sy, ex, finalEy, px);
              hitPaths.push({ edge, path: path1 }); hitPaths.push({ edge, path: path2 });
              L1.push(() => drawPath(ctx, path1, edge.color, baseLineW, finalAlpha)); addDraw(path2);
            } else {
              path1 = pBend(sx, sy, ex, finalEy, bx); hitPaths.push({ edge, path: path1 }); addDraw(path1);
            }
          }
        }
      })
    }

    L1.forEach(fn => fn()); L2_gaps.forEach(fn => fn()); L2_draws.forEach(fn => fn()); L_hl.forEach(fn => fn());
    currentHitPaths = hitPaths;
  }

  const getMousePos = (e) => {
    const cvs = lineCanvas.value; if (!cvs) return null;
    const rect = cvs.getBoundingClientRect();
    return { x: (e.clientX - rect.left) * (cvs.width / rect.width), y: (e.clientY - rect.top) * (cvs.height / rect.height), cvs };
  }

  const isInteractiveEl = (el) => el && el.closest('.calendar-node, .schedule-dot, .week-node, .week-node-dot, .schedule-label-chip, .ptl-header-track-badge, .btn-add-schedule, .cell-header, .calendar-cell, .week-lane-label');

  const handleMouseMove = (e) => {
    const cvs = lineCanvas.value; if (!cvs) return;
    if (isInteractiveEl(e.target) && !e.target.classList.contains('calendar-cell')) {
      cvs.parentElement.style.cursor = '';
      if (callbacks?.onEdgeHover) callbacks.onEdgeHover(null, null);
      return;
    }
    const pos = getMousePos(e); if (!pos) return;
    const ctx = pos.cvs.getContext('2d');
    ctx.lineWidth = 10; 
    let foundEdge = null;
    for (let i = currentHitPaths.length - 1; i >= 0; i--) {
      if (ctx.isPointInStroke(currentHitPaths[i].path, pos.x, pos.y)) { foundEdge = currentHitPaths[i].edge; break; }
    }
    pos.cvs.parentElement.style.cursor = foundEdge ? 'pointer' : '';
    if (callbacks?.onEdgeHover) callbacks.onEdgeHover(foundEdge, e);
  };

  const handleMouseClick = (e) => {
    if (isInteractiveEl(e.target) && !e.target.classList.contains('calendar-cell')) return;

    const pos = getMousePos(e); if (!pos) return;
    const ctx = pos.cvs.getContext('2d');
    ctx.lineWidth = 10;
    let foundEdge = null;
    for (let i = currentHitPaths.length - 1; i >= 0; i--) {
      if (ctx.isPointInStroke(currentHitPaths[i].path, pos.x, pos.y)) { foundEdge = currentHitPaths[i].edge; break; }
    }
    if (foundEdge) {
      e.stopPropagation(); 
      if (callbacks?.onEdgeClick) callbacks.onEdgeClick(foundEdge, e);
    }
  };

  const handleMouseLeave = () => { if (callbacks?.onEdgeHover) callbacks.onEdgeHover(null, null); };

  function attachEvents() {
    if (!lineCanvas.value) return;
    const parent = lineCanvas.value.parentElement;
    if (parent && parent !== cvsParent) {
      if (cvsParent) {
        cvsParent.removeEventListener('mousemove', handleMouseMove, true);
        cvsParent.removeEventListener('click', handleMouseClick, true);
        cvsParent.removeEventListener('mouseleave', handleMouseLeave, true);
      }
      cvsParent = parent;
      cvsParent.addEventListener('mousemove', handleMouseMove, true);
      cvsParent.addEventListener('click', handleMouseClick, true);
      cvsParent.addEventListener('mouseleave', handleMouseLeave, true);
    }
  }

  const SLIDE_MS = 300
  function startSlideAnimation(dir) {
    if (slideAnim?.rafId) cancelAnimationFrame(slideAnim.rafId)
    const zone = weekGraphZone?.value; if (!zone) return
    const PW=zone.offsetWidth, d=dir==='prev'?1:-1, t0=performance.now()
    const tick = now => {
      const p=Math.min((now-t0)/SLIDE_MS,1), e=1-Math.pow(1-p,3)
      drawLines(d*PW*(1-e))
      if (p<1) slideAnim.rafId=requestAnimationFrame(tick)
      else { slideAnim=null; drawLines(0) }
    }
    slideAnim = { rafId: requestAnimationFrame(tick) }
  }

  let rafId=null
  const requestDraw = () => {
    if (rafId) cancelAnimationFrame(rafId)
    rafId=requestAnimationFrame(()=>{ rafId=requestAnimationFrame(()=>{ rafId=null; drawLines(0) }) })
  }
  const scheduleRD = async () => { await nextTick(); requestDraw() }

  watch([currentView,currentYear,currentMonth,schedules,connections,tracks,allTracks, hiddenTracks], scheduleRD, { deep:true })
  if (focusedDay) watch(focusedDay, async()=>{ await nextTick(); await nextTick(); requestDraw() })
  if (interactionState) watch(interactionState, requestDraw, { deep: true })
  watch(() => themeStore.themeClass, async () => { await nextTick(); requestDraw() })

  const obs = el => { if(el&&ro) ro.observe(el) }
  watch(calendarWrapper,             async(el)=>{ obs(el); await nextTick(); requestDraw() })
  watch(()=>monthScrollBody?.value,  async(el)=>{ obs(el); await nextTick(); requestDraw() })
  watch(()=>weekGraphZone?.value,    async(el)=>{ obs(el); await nextTick(); requestDraw() })

  onMounted(() => {
    if (typeof ResizeObserver!=='undefined') ro=new ResizeObserver(()=>requestDraw())
    const tryDraw=(n=0)=>{
      const zone=weekGraphZone?.value||calendarWrapper?.value; const wrap=calendarWrapper?.value
      if (zone&&wrap) {
        if (ro) { ro.observe(zone); if(wrap!==zone) ro.observe(wrap) }
        if (zone.offsetWidth>0) drawLines(0)
        else if (n<12) setTimeout(()=>tryDraw(n+1),80)
      } else if (n<12) setTimeout(()=>tryDraw(n+1),80)
    }
    setTimeout(()=>tryDraw(),80)
    window.addEventListener('resize', requestDraw)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', requestDraw)
    if (ro) ro.disconnect()
    if (rafId) cancelAnimationFrame(rafId)
    if (slideAnim?.rafId) cancelAnimationFrame(slideAnim.rafId)
    if (cvsParent) {
      cvsParent.removeEventListener('mousemove', handleMouseMove, true);
      cvsParent.removeEventListener('click', handleMouseClick, true);
      cvsParent.removeEventListener('mouseleave', handleMouseLeave, true);
    }
  })

  return { drawLines, requestDraw, startSlideAnimation }
}