<template>
  <div class="app-layout" @click="handleBackdropClick">
    <AppSidebar />

    <main class="main-content">
      <CalendarHeader
        :date-text="headerDateText"
        :current-view="currentView"
        :current-date="parseDate(focusedDay)"
        :visible-tracks="tracksInCurrentView" 
        @navigate="navigate"
        @change-view="setView"
        @open-track-modal="isTrackModalOpen = true"
        @jump-to-date="jumpToDate"
        @update:hiddenTracks="onHiddenTracksChange"
        @hover-track="onTrackHover" 
      />

      <div v-if="currentView === 'month'" class="calendar-area">
        <div ref="calendarWrapper" class="calendar-wrapper">
          <div class="calendar-header-row grid-cols-7 month-dow-header">
            <div v-for="(d, i) in DAY_LABELS" :key="d" class="day-header"
              :class="{ 'day-header--sat': i===6, 'day-header--sun': i===0 }">
              {{ d }}
            </div>
          </div>
          
          <div class="month-scroll-body" ref="monthScrollBody" @scroll.passive="handleMonthScroll">
            <div class="calendar-grid grid-cols-7" :style="{ gridAutoRows: 'var(--month-row-height, 150px)' }">
              <CalendarCell
                v-for="cell in monthCells"
                :key="cell.dateStr"
                :date-str="cell.dateStr"
                :current-month="currentMonth" 
                :schedules="store.getSchedulesForDay(cell.dateStr)"
                :active-tooltip-id="activeTooltipId"
                :is-today="cell.dateStr === todayStr"
                :is-selected="cell.dateStr === selectedDay"
                :hidden-tracks="hiddenTracks"
                :dimmed-node-ids="dimmedNodeIds"
                @cell-click="handleCellClick"
                @add-schedule="openCreateModal"
                @toggle-tooltip="toggleTooltip"
                @edit-schedule="openEditModal"
                @delete-schedule="handleDeleteSchedule"
                @day-detail="openDayDetailModal"
                @hover-node="onNodeHover"
              />
            </div>
            <canvas ref="lineCanvas" class="line-canvas" />
          </div>
        </div>

        <Transition name="floatbar">
          <div v-if="selectedSchedules.length" class="floating-action-bar">
            <span class="sel-count">{{ selectedSchedules.length }}개 선택됨</span>
            <button class="btn-sel-delete" @click="deleteSelected"><i class="fas fa-trash" /> 삭제</button>
            <button class="btn-sel-clear" @click="selectedSchedules = []"><i class="fas fa-times" /></button>
          </div>
        </Transition>
      </div>

      <div v-else-if="currentView === 'week'" class="calendar-area">
        <div ref="calendarWrapper" class="week-wrapper" @touchstart.passive="onWeekTouchStart" @touchend.passive="onWeekTouchEnd">
           <div class="calendar-header-row grid-cols-7">
            <div v-for="(day, idx) in weekDays" :key="`wh-${day}`" class="week-col-header" :class="{ 'day-header--sat': idx===6, 'day-header--sun': idx===0 }">
              <span class="week-col-label">{{ DAY_LABELS[idx] }}</span>
              <span class="week-col-date" :class="{ 'is-today': day === todayStr }">{{ dateOf(day) }}</span>
            </div>
          </div>
          <div ref="weekGraphZone" class="week-graph-zone" :style="{ height: weekGraphHeight + 'px' }">
            <canvas ref="lineCanvas" class="line-canvas" />
            <template v-for="(day, colIdx) in weekDays" :key="`nodes-col-${colIdx}`">
              <WeekGraphNode v-for="s in store.getSchedulesForDay(day)" :key="s.id" v-show="!hiddenTracks.has(s.track)" :schedule="s" :col-idx="colIdx" :is-dimmed="dimmedNodeIds.has(s.id)" @hover="onNodeHover" />
            </template>
          </div>
        </div>
      </div>
    </main>

    <Transition name="fade">
      <div 
        v-if="edgeTooltip.visible" 
        class="edge-tooltip-popup" 
        :style="{ 
          left: edgeTooltip.x + 'px', 
          top: (edgeTooltip.baseY - currentScrollY) + 'px', 
          transform: `translate(${edgeTooltip.translateX}, -100%)` 
        }"
      >
        <div class="et-track" :style="{ color: edgeTooltip.edge.color }">{{ store.getTrackById(edgeTooltip.edge.track)?.name }}</div>
        <div class="et-nodes">
          <span>{{ edgeTooltip.edge.from.tooltip?.title || edgeTooltip.edge.from.text }}</span>
          <i class="fas fa-arrow-right" />
          <span>{{ edgeTooltip.edge.to.tooltip?.title || edgeTooltip.edge.to.text }}</span>
        </div>
      </div>
    </Transition>

    <Transition name="pop-remote">
      <div 
        v-if="edgeRemote.visible" 
        class="edge-remote-modal" 
        :style="{ left: edgeRemote.x + 'px', top: (edgeRemote.baseY - currentScrollY) + 'px' }"
      >
        <div class="er-header">
          <span class="er-track-name" :style="{ color: edgeRemote.edge.color }">
            {{ store.getTrackById(edgeRemote.edge.track)?.name }} 연결선
          </span>
          <button class="er-close" @click.stop="closeRemote"><i class="fas fa-times"/></button>
        </div>
        <div class="er-body">
          <div class="er-node" @click.stop="jumpToNode(edgeRemote.edge.from)">
            <div class="er-node-color" :style="{ background: store.getTrackById(edgeRemote.edge.from.track)?.color }"></div>
            <div class="er-node-info">
              <div class="er-node-day">{{ formatNodeDate(edgeRemote.edge.from.day) }}</div>
              <div class="er-node-title">{{ edgeRemote.edge.from.tooltip?.title || edgeRemote.edge.from.text }}</div>
            </div>
          </div>
          <div class="er-arrow"><i class="fas fa-link"/></div>
          <div class="er-node" @click.stop="jumpToNode(edgeRemote.edge.to)">
            <div class="er-node-color" :style="{ background: store.getTrackById(edgeRemote.edge.to.track)?.color }"></div>
            <div class="er-node-info">
              <div class="er-node-day">{{ formatNodeDate(edgeRemote.edge.to.day) }}</div>
              <div class="er-node-title">{{ edgeRemote.edge.to.tooltip?.title || edgeRemote.edge.to.text }}</div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <NodeFormModal v-model="isScheduleModalOpen" :mode="modalMode" :initial-form="modalInitialForm" :edit-node-id="editTargetId" @save="handleSaveSchedule" />
    <BranchManageModal v-model="isTrackModalOpen" />
    <DayDetailModal v-model="isDayDetailOpen" :day-str="dayDetailTarget" :schedules="store.getSchedulesForDay(dayDetailTarget)" @add-schedule="(d) => { isDayDetailOpen = false; openCreateModal(d) }" @edit-schedule="(s) => { isDayDetailOpen = false; openEditModal(s) }" @delete-schedule="(id) => { handleDeleteSchedule(id) }" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useCanvasLines, WEEK_LANE_SPACING, WEEK_TOP_MARGIN } from '@/composables/useCanvasLines'

import AppSidebar        from '@/components/AppSidebar.vue'
import CalendarHeader    from '@/components/CalendarHeader.vue'
import CalendarCell      from '@/components/calendar/CalendarCell.vue'
import WeekGraphNode     from '@/components/calendar/WeekGraphNode.vue'
import DayDetailModal    from '@/components/calendar/DayDetailModal.vue'
import NodeFormModal     from '@/components/modal/NodeFormModal.vue'
import BranchManageModal from '@/components/modal/BranchManageModal.vue'

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']
const store = useCalendarStore()
const { schedules, connections, allTracks } = storeToRefs(store)

const today = new Date()
const todayStr = toDateStr(today)
const currentView = ref('month')
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)
const focusedDay = ref(todayStr)
const selectedDay = ref(null)
const activeTooltipId = ref(null)
const selectedSchedules = ref([])

const currentScrollY = ref(0)
const anchorDate = ref(new Date(today.getFullYear(), today.getMonth(), 1))

const calendarWrapper = ref(null)
const monthScrollBody = ref(null)
const weekGraphZone = ref(null)
const lineCanvas = ref(null)
const isScheduleModalOpen = ref(false)
const isTrackModalOpen = ref(false)
const isDayDetailOpen = ref(false)
const dayDetailTarget = ref(todayStr)
const modalMode = ref('create')
const editTargetId = ref(null)
const modalInitialForm = ref({})

const hiddenTracks = ref(new Set())
function onHiddenTracksChange(set) { hiddenTracks.value = new Set(set) }

const interactionState = ref({ hovered: null, clicked: null })
const edgeTooltip = ref({ visible: false, x: 0, baseY: 0, translateX: '-50%', edge: null })
const edgeRemote = ref({ visible: false, x: 0, baseY: 0, edge: null })

function onTrackHover(trackId) { interactionState.value.hovered = trackId ? { type: 'track', data: trackId } : null }
function onNodeHover(schedule) { interactionState.value.hovered = schedule ? { type: 'node', data: schedule } : null }

function onEdgeHover(edge, e) {
  if (edge) {
    interactionState.value.hovered = { type: 'edge', data: edge }
    if (e) {
      const x = e.clientX; const w = window.innerWidth;
      let translateX = `-${(x / w) * 100}%`;
      edgeTooltip.value = { visible: true, x, baseY: e.clientY - 20 + currentScrollY.value, translateX, edge }
    }
  } else {
    if (interactionState.value.hovered?.type === 'edge') interactionState.value.hovered = null
    edgeTooltip.value.visible = false
  }
}

function onEdgeClick(edge, e) {
  if (edge) {
    interactionState.value.clicked = { type: 'edge', data: edge }
    activeTooltipId.value = null
    edgeTooltip.value.visible = false 

    if (e) {
      const w = window.innerWidth;
      const h = window.innerHeight;
      let x = e.clientX + 15;
      let y = e.clientY + 15;
      
      if (x + 240 > w) x = w - 240; 
      if (y + 180 > h) y = h - 180; 

      edgeRemote.value = { visible: true, x, baseY: y + currentScrollY.value, edge }
    }
  }
}

function closeRemote() {
  edgeRemote.value.visible = false;
  if (interactionState.value.clicked?.type === 'edge') {
    interactionState.value.clicked = null;
  }
}

function jumpToNode(node) {
  const d = parseDate(node.day);
  jumpToDate(d);
  
  edgeRemote.value.visible = false;
  activeTooltipId.value = node.id;
  interactionState.value.clicked = { type: 'node', data: node };
}

function formatNodeDate(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  return `${m}월 ${d}일`;
}

const dimmedNodeIds = computed(() => {
  const ids = new Set()
  const { hovered, clicked } = interactionState.value
  
  let activeHover = hovered;
  if (hovered && hovered.type === 'track' && hiddenTracks.value.has(hovered.data)) activeHover = null;
  if (!activeHover && !clicked) return ids

  schedules.value.forEach(s => {
    if (hiddenTracks.value.has(s.track)) return;
    let isHL = false

    if (activeHover) {
      if (activeHover.type === 'track') isHL = (s.track === activeHover.data)
      else if (activeHover.type === 'node') isHL = (s.id === activeHover.data.id)
      else if (activeHover.type === 'edge') isHL = (s.id === activeHover.data.from.id || s.id === activeHover.data.to.id)
    } else if (clicked) {
      if (clicked.type === 'node') {
        const cId = clicked.data.id; const cTrack = clicked.data.track;
        isHL = (s.track === cTrack);
        if (!isHL) isHL = connections.value.some(c => (c.from === cId && c.to === s.id) || (c.to === cId && c.from === s.id));
      }
      else if (clicked.type === 'edge') isHL = (s.id === clicked.data.from.id || s.id === clicked.data.to.id)
    }
    if (!isHL) ids.add(s.id)
  })
  return ids
})

const { requestDraw } = useCanvasLines(
  calendarWrapper, lineCanvas, currentView, currentYear, currentMonth, focusedDay,
  weekGraphZone, monthScrollBody, hiddenTracks, interactionState, { onEdgeHover, onEdgeClick }
)

const monthCells = computed(() => {
  const cells = []
  const start = new Date(anchorDate.value.getFullYear(), anchorDate.value.getMonth() - 6, 1)
  const d = new Date(start)
  d.setDate(d.getDate() - d.getDay())
  for (let i = 0; i < 52 * 7; i++) {
    cells.push({ dateStr: toDateStr(d), month: d.getMonth() + 1, year: d.getFullYear() })
    d.setDate(d.getDate() + 1)
  }
  return cells
})

const tracksInCurrentView = computed(() => {
  const datesInView = currentView.value === 'month' ? monthCells.value.map(c => c.dateStr) : weekDays.value
  const idsWithData = new Set()
  schedules.value.forEach(s => { if (datesInView.includes(s.day)) idsWithData.add(s.track) })
  return allTracks.value.filter(t => !t.isEnded || idsWithData.has(t.id) || t.isHighlight)
})

const headerDateText = computed(() => `${currentYear.value}년 ${currentMonth.value}월`)

let scrollTicking = false;
function handleMonthScroll(e) {
  if (e && e.target) currentScrollY.value = e.target.scrollTop;

  if (!scrollTicking) {
    window.requestAnimationFrame(() => {
      const el = monthScrollBody.value
      if (el) {
        const centerLine = el.scrollTop + el.clientHeight / 2
        const firstCell = el.querySelector('.calendar-cell')
        if (firstCell) {
          const rowHeight = firstCell.offsetHeight || 150
          const rowIndex = Math.floor(centerLine / rowHeight)
          const target = monthCells.value[rowIndex * 7 + 3]
          if (target && (target.month !== currentMonth.value || target.year !== currentYear.value)) {
            currentMonth.value = target.month
            currentYear.value = target.year
          }
        }
      }
      scrollTicking = false;
    });
    scrollTicking = true;
  }
}

function scrollToDate(dateStr, behavior = 'smooth') {
  const el = document.getElementById(`day-${dateStr}`)
  if (el && monthScrollBody.value) monthScrollBody.value.scrollTo({ top: el.offsetTop - 10, behavior })
}

function navigate(dir) {
  if (currentView.value === 'month') {
    let d;
    if (dir === 'prev') d = new Date(currentYear.value, currentMonth.value - 2, 1);
    else if (dir === 'next') d = new Date(currentYear.value, currentMonth.value, 1);
    else { jumpToDate(today); return; }
    
    const el = document.getElementById(`day-${toDateStr(d)}`);
    if (el) scrollToDate(toDateStr(d), 'smooth');
    else jumpToDate(d);
  } else if (currentView.value === 'week') {
    const d = parseDate(focusedDay.value)
    if (dir === 'prev') d.setDate(d.getDate() - 7)
    else if (dir === 'next') d.setDate(d.getDate() + 7)
    else { focusedDay.value = todayStr; return }
    focusedDay.value = toDateStr(d); currentMonth.value = d.getMonth() + 1; currentYear.value = d.getFullYear()
  }
}

function jumpToDate(date) {
  anchorDate.value = date
  currentYear.value = date.getFullYear()
  currentMonth.value = date.getMonth() + 1
  focusedDay.value = toDateStr(date)
  if (currentView.value === 'month') {
    nextTick(() => scrollToDate(toDateStr(new Date(currentYear.value, currentMonth.value - 1, 1)), 'auto'))
  }
}

function setView(v) { 
  currentView.value = v; 
  currentScrollY.value = 0; 
  const d = parseDate(focusedDay.value); currentYear.value = d.getFullYear(); currentMonth.value = d.getMonth() + 1; 
  if (v === 'month') nextTick(() => initMonthScroll()); 
}

function toDateStr(d) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
function parseDate(str) { const [y,m,d] = str.split('-').map(Number); return new Date(y, m-1, d) }

function handleBackdropClick(e) {
  if (e && e.target.closest('.edge-remote-modal')) return;
  activeTooltipId.value = null; 
  interactionState.value.clicked = null; 
  edgeRemote.value.visible = false;
}

function openCreateModal(dateStr) { selectedDay.value = dateStr; focusedDay.value = dateStr; modalMode.value = 'create'; editTargetId.value = null; modalInitialForm.value = { day: dateStr, track: 'main', title: '', text: '', time: '09:00', tags: '', parentIds: [], childIds: [] }; isScheduleModalOpen.value = true; activeTooltipId.value = null }
function openEditModal(schedule) { focusedDay.value = schedule.day; selectedDay.value = schedule.day; modalMode.value = 'edit'; editTargetId.value = schedule.id; modalInitialForm.value = { day: schedule.day, track: schedule.track, title: schedule.tooltip?.title||'', text: schedule.text||'', time: schedule.tooltip?.time||'09:00', tags: schedule.tooltip?.tags?.join(', ')||'', parentIds: connections.value.filter(e => e.to === schedule.id).map(e => e.from), childIds: connections.value.filter(e => e.from === schedule.id).map(e => e.to) }; isScheduleModalOpen.value = true; activeTooltipId.value = null }
function handleCellClick(dateStr) { if (selectedDay.value !== dateStr) activeTooltipId.value = null; selectedDay.value = dateStr; focusedDay.value = dateStr; interactionState.value.clicked = null; edgeRemote.value.visible = false; }
function openDayDetailModal(dateStr) { dayDetailTarget.value = dateStr; isDayDetailOpen.value = true }
function toggleTooltip(id) { activeTooltipId.value = activeTooltipId.value === id ? null : id; if (activeTooltipId.value) { const s = schedules.value.find(s => s.id === id); if (s) { selectedDay.value = s.day; focusedDay.value = s.day; interactionState.value.clicked = { type: 'node', data: s }; } } else { interactionState.value.clicked = null; } edgeRemote.value.visible = false; }

async function handleSaveSchedule(payload) {
  const { parentIds, childIds, ...data } = payload
  if (modalMode.value === 'create') {
    const newId = await store.createSchedule(data); store.updateConnectionsForSchedule(newId, parentIds || [], childIds || [])
  } else {
    await store.updateSchedule(editTargetId.value, data); store.updateConnectionsForSchedule(editTargetId.value, parentIds || [], childIds || [])
  }
  isScheduleModalOpen.value = false
}
async function handleDeleteSchedule(id) { if (!confirm('이 일정을 삭제하시겠습니까?')) return; await store.deleteSchedule(id); activeTooltipId.value = null }
function deleteSelected() { if (!selectedSchedules.value.length) return; if (!confirm(`선택한 ${selectedSchedules.value.length}개의 일정을 삭제하시겠습니까?`)) return; selectedSchedules.value.forEach(id => store.deleteSchedule(id)); selectedSchedules.value = [] }

function initMonthScroll() { 
  const firstDayStr = `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}-01`;
  scrollToDate(firstDayStr, 'auto');
}

watch(currentYear, (y) => store.fetchHolidaysForYear(y))
onMounted(() => { 
  store.fetchHolidaysForYear(currentYear.value); 
  nextTick(() => initMonthScroll()) 
})

let _touchStartX = 0
function onWeekTouchStart(e) { _touchStartX = e.touches[0].clientX }
function onWeekTouchEnd(e) { const dx = e.changedTouches[0].clientX - _touchStartX; if (Math.abs(dx) > 50) navigate(dx < 0 ? 'next' : 'prev') }
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ★ 1. 외부의 CalendarHeader 컴포넌트 강제 최상단 배치 */
:deep(.calendar-header) {
  position: relative !important;
  z-index: 100 !important;
}

.calendar-area { flex: 1; overflow: hidden; position: relative; display: flex; flex-direction: column; }

/* 달력 래퍼는 Z-index 없이 흐름을 타게 둡니다 */
.calendar-wrapper { position: relative; width: 100%; height: 100%; background: var(--bg-surface); display: flex; flex-direction: column; overflow: hidden; }

/* ★ 2. 요일 헤더를 90으로 지정 (메인 헤더 밑, 모달 위) */
.calendar-header-row { display: grid; border-bottom: 1px solid var(--border); background: var(--bg-elevated); z-index: 90; position: sticky; top: 0; }
.grid-cols-7 { grid-template-columns: repeat(7, 1fr); }
.day-header { padding: 10px 0; text-align: center; font-size: 12px; font-weight: 700; color: var(--text-faint); }
.day-header--sat { color: var(--sat-color) !important; }
.day-header--sun { color: var(--sun-color) !important; }

/* ★ 3. 스크롤 내용물을 가두는 컨테이너 (여기에 10을 주어 안쪽 달력 요소들이 밖을 침범하지 못하게 함) */
.month-scroll-body { flex: 1; overflow-y: auto; overflow-x: hidden; position: relative; z-index: 10; scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) var(--scrollbar-track); }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); position: relative; }
.line-canvas { position: absolute; top: 0; left: 0; pointer-events: none; z-index: 2; }

/* ★ 4. 툴팁 (fixed 유지 + Z-index 80으로 요일 헤더 아래로 쏙 숨겨짐) */
.edge-tooltip-popup { 
  position: fixed; z-index: 80; pointer-events: none; 
  background: var(--bg-surface); border: 1px solid var(--border); 
  border-radius: 8px; padding: 10px 14px; 
  box-shadow: 0 4px 16px rgba(0,0,0,0.15); margin-top: -10px;
  display: flex; flex-direction: column; gap: 6px;
  min-width: max-content; white-space: nowrap;
}
.et-track { font-size: 11px; font-weight: 800; font-family: 'Escoredream', sans-serif; }
.et-nodes { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; font-family: 'Escoredream', sans-serif; color: var(--text-primary); }
.et-nodes i { color: var(--text-faint); font-size: 11px; }

/* ★ 5. 엣지 리모컨 모달 (fixed 유지 + Z-index 80으로 요일 헤더 아래로 쏙 숨겨짐) */
.edge-remote-modal {
  position: fixed; z-index: 80; width: 240px;
  background: var(--bg-surface); border: 1.5px solid var(--border-mid);
  border-radius: 12px; box-shadow: 0 12px 40px rgba(0,0,0,0.25);
  display: flex; flex-direction: column; overflow: hidden;
}
.er-header {
  padding: 12px 14px; background: var(--bg-elevated); border-bottom: 1px solid var(--border);
  display: flex; justify-content: space-between; align-items: center;
}
.er-track-name { font-size: 12px; font-weight: 800; font-family: 'Escoredream', sans-serif; }
.er-close {
  background: none; border: none; color: var(--text-faint); font-size: 14px;
  cursor: pointer; transition: 0.15s; padding: 0 4px; line-height: 1;
}
.er-close:hover { color: var(--text-primary); }
.er-body { padding: 14px; display: flex; flex-direction: column; gap: 6px; }
.er-node {
  display: flex; gap: 10px; align-items: center;
  padding: 10px 14px; border-radius: 10px; border: 1px solid var(--border);
  background: var(--bg-elevated); cursor: pointer; transition: all 0.15s ease;
}
.er-node:hover {
  border-color: var(--accent); background: rgba(59,130,246,0.06);
  transform: translateX(4px); box-shadow: 0 2px 8px rgba(59,130,246,0.1);
}
.er-node-color {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 4px rgba(0,0,0,0.2);
}
.er-node-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.er-node-day { font-size: 10px; color: var(--text-faint); margin-bottom: 4px; font-family: monospace; font-weight: 600; letter-spacing: 0.05em; }
.er-node-title { font-size: 13px; font-weight: 700; color: var(--text-primary); font-family: 'Escoredream', sans-serif; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.er-arrow { text-align: center; color: var(--border-mid); font-size: 14px; margin: -2px 0; }

/* 애니메이션 */
.pop-remote-enter-active { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.pop-remote-leave-active { transition: all 0.15s ease; }
.pop-remote-enter-from, .pop-remote-leave-to { opacity: 0; transform: scale(0.95) translateY(-10px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.floating-action-bar { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 200; display: flex; align-items: center; gap: 10px; padding: 8px 18px; background: var(--bg-elevated); border: 1.5px solid var(--accent); border-radius: 999px; box-shadow: 0 4px 24px rgba(0,0,0,0.3); white-space: nowrap; }
.sel-count { font-size: 13px; font-weight: 700; color: var(--accent); }
.btn-sel-delete { display: flex; align-items: center; gap: 5px; padding: 5px 12px; border: 1px solid #ef4444; background: rgba(239,68,68,0.08); color: #ef4444; border-radius: 7px; font-size: 12px; font-weight: 700; cursor: pointer; transition: all 0.15s; }
.btn-sel-delete:hover { background: #ef4444; color: #fff; }
.btn-sel-clear { background: none; border: none; color: var(--text-faint); font-size: 16px; cursor: pointer; padding: 2px 4px; }
.btn-sel-clear:hover { color: var(--text-primary); }
</style>