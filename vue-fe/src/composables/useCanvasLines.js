/**
 * useCanvasLines.js — Git-graph 연결선 렌더러
 * ═══════════════════════════════════════════════════════════════
 * [수정됨] 형광펜 트랙 한정: 설정(connections) 없이도 자기들끼리 자동 직진 연결
 * [유지됨] 같은 날짜 수직 연결선(Spine) 완전 제거
 * [유지됨] 형광펜 트랙 3px 상향 조정 및 두께 동적 렌더링
 */

import { watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useThemeStore } from '@/stores/useThemeStore'
import { storeToRefs } from 'pinia'

export const WEEK_LANE_SPACING = 28
export const WEEK_TOP_MARGIN   = 28

const ymd = s => { const [y,m,d] = s.split('-').map(Number); return new Date(y,m-1,d) }
const ds  = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`

// ── 렌더 상수 ────────────────────────────────────────────────
const LINE_W     = 2.5
const LINE_ALPHA = 0.92
const SYS_W      = 18
const SYS_ALPHA  = 0.22
const GAP_EXTRA  = 6
const DOT_R      = 5.5
const DOT_BDR    = 2.5
const PLUG_PX    = 18
const BEND_PLUG  = [15, 25, 35, 45]  

const MONTH_BOT  = 10
const REGULAR_SP = 13   
const HL_SP      = 24   

const isHL = (t) => t && (t.isHighlight || t.id?.startsWith('hl_') || t.name?.includes('프롬프트') || t.name?.includes('블로그'))

// ── Canvas 기초 그리기 ───────────────────────────────────────
function w(ctx, fn) { ctx.save(); fn(); ctx.restore() }

function applyMode(ctx, gap, color, lw, alpha) {
  ctx.lineCap = 'round'; ctx.lineJoin = 'round'
  if (gap) {
    ctx.globalCompositeOperation = 'destination-out'
    ctx.strokeStyle = 'rgba(0,0,0,1)'
    ctx.lineWidth   = lw + GAP_EXTRA
  } else {
    ctx.globalCompositeOperation = 'source-over'
    ctx.globalAlpha = alpha
    ctx.strokeStyle = color
    ctx.lineWidth   = lw
  }
}

function rLine(ctx, x1,y1,x2,y2, color, lw=LINE_W, alpha=LINE_ALPHA, gap=false) {
  w(ctx, () => {
    applyMode(ctx, gap, color, lw, alpha)
    ctx.beginPath(); ctx.moveTo(x1,y1); ctx.lineTo(x2,y2); ctx.stroke()
  })
}

function rBend(ctx, sx,sy, ex,ey, color, lw=LINE_W, alpha=LINE_ALPHA, bendX, gap=false) {
  w(ctx, () => {
    applyMode(ctx, gap, color, lw, alpha)
    const dy  = ey - sy
    const ady = Math.abs(dy)
    const R   = Math.min(ady * 0.45, WEEK_LANE_SPACING * 0.55, 12)
    const dY  = dy > 0 ? 1 : -1
    const dX  = ex >= bendX ? 1 : -1
    ctx.beginPath()
    if (ady < 1) {
      ctx.moveTo(sx,sy); ctx.lineTo(ex,ey)
    } else {
      ctx.moveTo(sx, sy)
      ctx.lineTo(bendX, sy)
      ctx.arcTo(bendX, sy, bendX, sy+dY*R, R)
      ctx.lineTo(bendX, ey-dY*R)
      ctx.arcTo(bendX, ey, bendX+dX*R, ey, R)
      ctx.lineTo(ex, ey)
    }
    ctx.stroke()
  })
}

function rDetour(ctx, sx,sy, ex,ey, color, lw=LINE_W, alpha=LINE_ALPHA, bypassX, gap=false) {
  w(ctx, () => {
    applyMode(ctx, gap, color, lw, alpha)
    const dy  = ey - sy
    const ady = Math.abs(dy)
    const R   = Math.min(ady * 0.35, 9)
    const dY  = dy > 0 ? 1 : -1
    ctx.beginPath()
    ctx.moveTo(sx, sy)
    ctx.arcTo(bypassX, sy, bypassX, sy+dY*R, R)
    ctx.lineTo(bypassX, ey-dY*R)
    ctx.arcTo(bypassX, ey, bypassX-R, ey, R)
    ctx.lineTo(ex, ey)
    ctx.stroke()
  })
}

function rHL(ctx, x1, x2, y, color) {
  w(ctx, () => {
    ctx.globalCompositeOperation = 'destination-over'
    ctx.globalAlpha = SYS_ALPHA
    ctx.strokeStyle = color
    ctx.lineWidth   = SYS_W
    ctx.lineCap     = 'round'
    ctx.beginPath(); ctx.moveTo(x1,y); ctx.lineTo(x2,y); ctx.stroke()
  })
}

function rDot(ctx, x, y, color, bg) {
  w(ctx, () => {
    ctx.globalCompositeOperation = 'source-over'
    ctx.globalAlpha = 1
    ctx.fillStyle   = bg
    ctx.beginPath(); ctx.arc(x, y, DOT_R+DOT_BDR, 0, Math.PI*2); ctx.fill()
    ctx.fillStyle   = color
    ctx.beginPath(); ctx.arc(x, y, DOT_R, 0, Math.PI*2); ctx.fill()
  })
}

// ── Layout Engine ────────────────────────────────────────────
function buildLayout({ mode: vMode, W, allTracks, schedules,
                       focusedDay, cy, cm, firstDow, zoneEl, scrollEl }) {
  const sorted = [...allTracks].sort((a,b) => a.index - b.index)
  const sysSet = new Set(sorted.filter(t => isHL(t)).map(t => t.id))
  const isSys  = id => sysSet.has(id)
  const CW     = W / 7
  const firstCell = scrollEl
    ? scrollEl.querySelector?.('.calendar-cell')
    : document.querySelector('.calendar-cell')
  const rowH = firstCell
    ? firstCell.getBoundingClientRect().height
    : (parseInt(getComputedStyle(document.documentElement).getPropertyValue('--month-row-height')) || 150)

  let rangeStart, rangeEnd, totalRows, wLaneY = null

  if (vMode === 'week') {
    const fd = ymd(focusedDay)
    const ws = new Date(fd); ws.setDate(fd.getDate() - fd.getDay())
    const we = new Date(ws); we.setDate(ws.getDate() + 6)
    rangeStart = ds(ws); rangeEnd = ds(we); totalRows = 1
    wLaneY = {}
    sorted.forEach((t,i) => { wLaneY[t.id] = WEEK_TOP_MARGIN + i * WEEK_LANE_SPACING - (isSys(t.id) ? 3 : 0) })
  } else {
    rangeStart = ds(new Date(cy, cm-1, 1))
    rangeEnd   = ds(new Date(cy, cm, 0))
    totalRows  = Math.ceil((new Date(cy,cm,0).getDate() + firstDow) / 7)
  }

  const getTrackOffset = (trackId) => {
    let offset = 0
    for (const t of sorted) {
      if (t.id === trackId) return offset
      offset += isHL(t) ? HL_SP : REGULAR_SP
    }
    return offset
  }

  const mLaneY = (trackId, row) => {
    const t = allTracks.find(x => x.id === trackId)
    if (!t) return (row+1)*rowH - MONTH_BOT
    return (row+1)*rowH - MONTH_BOT - getTrackOffset(trackId) - (isSys(trackId) ? 3 : 0)
  }

  const getLaneY = (trackId, row=0) =>
    vMode === 'week' ? (wLaneY[trackId] ?? WEEK_TOP_MARGIN) : mLaneY(trackId, row)

  const cell = d => {
    if (!d) return null
    if (vMode === 'week') {
      if (d < rangeStart || d > rangeEnd) return null
      return { row: 0, col: ymd(d).getDay() }
    }
    const [dy,dm,dd] = d.split('-').map(Number)
    if (dy !== cy || dm !== cm) return null
    const ci = (dd - 1) + firstDow
    return { row: Math.floor(ci/7), col: ci%7 }
  }

  const getX = id => {
    const el = document.getElementById(`node-${id}`)
    if (!el) return null
    const baseEl = vMode === 'week' ? zoneEl : scrollEl
    if (!baseEl) return null
    const r = el.getBoundingClientRect()
    const base = baseEl.getBoundingClientRect()
    return Math.round(r.left - base.left + baseEl.scrollLeft + (r.width / 2))
  }

  return { sorted, isSys, CW, rowH, totalRows, rangeStart, rangeEnd,
           wLaneY, mLaneY, getLaneY, getX, cell, scrollEl, zoneEl }
}

// ── Draw Engine ───────────────────────────────────────────────
function drawGraph(ctx, W, H, vMode, layout, conns, scheds, allTracks, getTrack, bg, hidden=new Set()) {
  const { sorted, isSys, CW, totalRows, rangeStart, getLaneY, getX, cell, scrollEl, zoneEl } = layout
  const isHidden = id => hidden.has(id)

  const rowDates = []
  for (let r=0; r<totalRows; r++) {
    if (vMode === 'week') {
      rowDates.push({ s: rangeStart, e: layout.rangeEnd })
    } else {
      const cy = ymd(rangeStart).getFullYear()
      const cm = ymd(rangeStart).getMonth() + 1
      const firstDow = new Date(cy, cm-1, 1).getDay()
      const d1 = new Date(cy, cm-1, r*7 - firstDow + 1)
      const d2 = new Date(cy, cm-1, r*7 - firstDow + 7)
      rowDates.push({ s: ds(d1), e: ds(d2) })
    }
  }

  const exactRowY = {}
  
  const getExactLaneY = (trackId, r) => {
    const key = `${trackId}-${r}`
    if (exactRowY[key] !== undefined) return exactRowY[key]

    if (vMode === 'week') {
      const rd = rowDates[r]
      const node = scheds.find(s => s.track === trackId && s.day >= rd.s && s.day <= rd.e)
      
      if (node) {
        const el = document.getElementById(`node-${node.id}`)
        if (el && zoneEl) {
          const rect = el.getBoundingClientRect()
          const base = zoneEl.getBoundingClientRect()
          exactRowY[key] = Math.round(rect.top - base.top + zoneEl.scrollTop + (rect.height / 2))
          return exactRowY[key]
        }
      }
    }
    
    exactRowY[key] = Math.round(getLaneY(trackId, r))
    return exactRowY[key]
  }

  const getRowX = (node, r) => {
    const rd = rowDates[r]
    if (node.day < rd.s) return -2
    if (node.day > rd.e) return Math.round(W + 2)
    const x = getX(node.id)
    if (x != null) return x
    const c = cell(node.day)
    if (c) return Math.round((c.col + 0.5) * CW)
    return 0
  }

  const edges = []
  const fromC={}, toC={}
  
  conns.forEach(c => {
    const s=scheds.find(n=>n.id===c.from), e=scheds.find(n=>n.id===c.to)
    if (!s || !e) return
    fromC[c.from]=(fromC[c.from]||0)+1
    toC[c.to]   =(toC[c.to]   ||0)+1
  })

  // 1. 명시적 연결 추가
  conns.forEach(c => {
    const s = scheds.find(x=>x.id===c.from)
    const e = scheds.find(x=>x.id===c.to)
    if (!s || !e) return
    if (isHidden(s.track) || isHidden(e.track)) return
    
    const isBranch = (fromC[s.id]||0)>=1 && (toC[e.id]||0)<=1
    const color = isBranch ? (getTrack(e.track)?.color||'#aaa') : (getTrack(s.track)?.color||'#aaa')
    const isCross = s.track !== e.track
    edges.push({ from: s, to: e, track: s.track, targetTrack: e.track, color, isCross })
  })

  // 2. ★ 형광펜 트랙 한정: 명시적 연결이 없어도 자동 직진 연결 추가
  sorted.forEach(t => {
    if (!isSys(t.id)) return    // 형광펜(시스템) 트랙만 허용
    if (isHidden(t.id)) return  // 숨김 처리된 트랙은 무시

    // 해당 트랙의 모든 일정을 시간순으로 정렬
    const ns = scheds.filter(s => s.track === t.id).sort((a,b) => a.day.localeCompare(b.day))
    
    for (let i = 0; i < ns.length - 1; i++) {
      // 이미 edges 배열에 두 노드 간의 명시적 연결이 있는지 확인 (중복 방지)
      const alreadyExists = edges.some(e => e.from.id === ns[i].id && e.to.id === ns[i+1].id)
      
      if (!alreadyExists) {
        edges.push({ 
          from: ns[i], 
          to: ns[i+1], 
          track: t.id, 
          targetTrack: t.id, 
          color: t.color, 
          isCross: false 
        })
      }
    }
  })

  const getTrackIdx = tid => layout.sorted.findIndex(t => t.id === tid)
  const branchesTo = {}
  
  edges.forEach(e => {
    if (e.isCross) {
      if (!branchesTo[e.to.id]) branchesTo[e.to.id] = []
      branchesTo[e.to.id].push(e)
    }
  })

  Object.values(branchesTo).forEach(branches => {
    branches.forEach(b => {
      b.sIdx = getTrackIdx(b.track)
      b.tIdx = getTrackIdx(b.targetTrack)
      b.dist = Math.abs(b.sIdx - b.tIdx)
    })
    
    const above = branches.filter(b => b.sIdx < b.tIdx).sort((a,b) => b.dist - a.dist)
    above.forEach((b, i) => { b.ib = i })
    
    const below = branches.filter(b => b.sIdx > b.tIdx).sort((a,b) => b.dist - a.dist)
    below.forEach((b, i) => { b.ib = i })
  })

  const L1=[], L2_gaps=[], L2_draws=[], L_hl=[], L3=[]
  const q2 = (gF, dF) => { L2_gaps.push(gF); L2_draws.push(dF) }

  for (let r=0; r<totalRows; r++) {
    const rd = rowDates[r]

    edges.forEach(edge => {
      if (edge.to.day < rd.s || edge.from.day > rd.e) return 

      const sx = getRowX(edge.from, r)
      const ex = getRowX(edge.to, r)
      const sy = getExactLaneY(edge.track, r)

      if (!edge.isCross) {
        if (isSys(edge.track)) {
          L_hl.push(() => rHL(ctx, sx, ex, sy, edge.color))
        } else {
          L1.push(() => rLine(ctx, sx, sy, ex, sy, edge.color))
        }
      } else {
        const isTargetInRow = (edge.to.day >= rd.s && edge.to.day <= rd.e)

        if (!isTargetInRow) {
          L1.push(() => rLine(ctx, sx, sy, ex, sy, edge.color))
        } else {
          const ey = getExactLaneY(edge.targetTrack, r)
          const finalEy = ey 
          const ib = edge.ib ?? 0 
          const bx = ex - BEND_PLUG[Math.min(ib, BEND_PLUG.length-1)]

          if (Math.abs(ex - sx) < CW * 0.85 && sx >= 0 && sx <= W) {
            const bypassX = Math.round(Math.max(sx, ex) + CW * 0.4 + ib * 10)
            q2(
              () => rDetour(ctx, sx, sy, ex, finalEy, edge.color, LINE_W, 1, bypassX, true),
              () => rDetour(ctx, sx, sy, ex, finalEy, edge.color, LINE_W, LINE_ALPHA, bypassX, false)
            )
          } else if (isSys(edge.targetTrack)) {
            const px = Math.round(ex - PLUG_PX - ib * 10)
            L1.push(() => rLine(ctx, sx, sy, px, sy, edge.color))
            q2(
              () => rBend(ctx, px, sy, ex, finalEy, edge.color, LINE_W, 1, px, true),
              () => rBend(ctx, px, sy, ex, finalEy, edge.color, LINE_W, LINE_ALPHA, px, false)
            )
          } else {
            q2(
              () => rBend(ctx, sx, sy, ex, finalEy, edge.color, LINE_W, 1, bx, true),
              () => rBend(ctx, sx, sy, ex, finalEy, edge.color, LINE_W, LINE_ALPHA, bx, false)
            )
          }
        }
      }
    })
  }

  scheds.forEach(s => {
    if (isHidden(s.track)) return
    const c = cell(s.day); if (!c) return
    const t = getTrack(s.track); if (!t) return
    const x = getX(s.id);  if (x==null) return
    const y = getExactLaneY(s.track, c.row)
    L3.push(() => rDot(ctx, x, y, t.color, bg))
  })

  L1.forEach(fn => fn())               
  L2_gaps.forEach(fn => fn())          
  L2_draws.forEach(fn => fn())         
  L_hl.forEach(fn => fn())             
  L3.forEach(fn => fn())               
}

// ── 메인 Composable ──────────────────────────────────────────
export function useCanvasLines(
  calendarWrapper, lineCanvas, currentView,
  currentYear, currentMonth, focusedDay,
  weekGraphZone, monthScrollBody,
  hiddenTracks
) {
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
    const isDark = themeEl.classList.contains('theme-dark') || document.body.classList.contains('is-dark')
    return isDark ? '#1e1e24' : '#ffffff'
  }

  const getHidden = () => hiddenTracks?.value ?? new Set()

  let ro=null, slideAnim=null

  const drawLines = (offset=0) => {
    const cvs  = lineCanvas.value;  if (!cvs) return
    const wrap = calendarWrapper?.value; if (!wrap) return
    const isW  = currentView.value === 'week'
    const bg   = getBg()
    const hidden = getHidden()
    let W, H, ctx

    if (isW) {
      const zone = weekGraphZone?.value; if (!zone) return
      W=zone.offsetWidth; H=zone.offsetHeight; if (!W||!H) return
      cvs.width=W; cvs.height=H; cvs.style.width=W+'px'; cvs.style.height=H+'px'
      ctx=cvs.getContext('2d'); ctx.clearRect(0,0,W,H)
      const L = buildLayout({ mode:'week', W, allTracks:allTracks.value,
        schedules:schedules.value, focusedDay:focusedDay?.value||'',
        zoneEl:zone, scrollEl:null })
      if (offset) { ctx.save(); ctx.translate(offset,0) }
      drawGraph(ctx,W,H,'week',L,connections.value,schedules.value,allTracks.value,getTrack,bg,hidden)
      if (offset) ctx.restore()
    } else {
      const se = monthScrollBody?.value || wrap
      W=se.offsetWidth; H=se.scrollHeight; if (!W||!H) return
      cvs.width=W; cvs.height=H; cvs.style.width=W+'px'; cvs.style.height=H+'px'
      ctx=cvs.getContext('2d'); ctx.clearRect(0,0,W,H)
      const cy=currentYear.value, cm=currentMonth.value
      const L = buildLayout({ mode:'month', W, allTracks:allTracks.value,
        schedules:schedules.value, cy, cm, firstDow:new Date(cy,cm-1,1).getDay(),
        zoneEl:null, scrollEl:se })
      drawGraph(ctx,W,H,'month',L,connections.value,schedules.value,allTracks.value,getTrack,bg,hidden)
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
    rafId=requestAnimationFrame(()=>{
      rafId=requestAnimationFrame(()=>{ rafId=null; drawLines(0) })
    })
  }
  const scheduleRD = async () => { await nextTick(); requestDraw() }

  watch([currentView,currentYear,currentMonth,schedules,connections,tracks,allTracks],
        scheduleRD, { deep:true })
  if (focusedDay) watch(focusedDay, async()=>{ await nextTick(); await nextTick(); requestDraw() })

  watch(() => themeStore.themeClass, async () => {
    await nextTick()
    requestDraw()
  })

  const obs = el => { if(el&&ro) ro.observe(el) }
  watch(calendarWrapper,             async(el)=>{ obs(el); await nextTick(); requestDraw() })
  watch(()=>monthScrollBody?.value,  async(el)=>{ obs(el); await nextTick(); requestDraw() })
  watch(()=>weekGraphZone?.value,    async(el)=>{ obs(el); await nextTick(); requestDraw() })

  onMounted(() => {
    if (typeof ResizeObserver!=='undefined') ro=new ResizeObserver(()=>requestDraw())
    const tryDraw=(n=0)=>{
      const zone=weekGraphZone?.value||calendarWrapper?.value
      const wrap=calendarWrapper?.value
      if (zone&&wrap) {
        if (ro) { ro.observe(zone); if(wrap!==zone) ro.observe(wrap) }
        if (zone.offsetWidth>0) drawLines(0)
        else if (n<12) setTimeout(()=>tryDraw(n+1),80)
      } else if (n<12) setTimeout(()=>tryDraw(n+1),80)
    }
    setTimeout(()=>tryDraw(),80)
    window.addEventListener('resize', requestDraw)
    window.addEventListener('scroll', requestDraw, true)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', requestDraw)
    window.removeEventListener('scroll', requestDraw, true)
    if (ro) ro.disconnect()
    if (rafId) cancelAnimationFrame(rafId)
    if (slideAnim?.rafId) cancelAnimationFrame(slideAnim.rafId)
  })

  return { drawLines, requestDraw, startSlideAnimation }
}