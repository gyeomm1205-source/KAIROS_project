<template>
  <div class="app-layout" @click="handleBackdropClick" :class="themeStore.isDark ? 'theme-dark' : 'theme-light'">
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
          
          <div class="month-scroll-body custom-scroll" ref="monthScrollBody" @scroll.passive="handleMonthScroll">
            <div class="calendar-grid grid-cols-7" :style="{ gridAutoRows: 'var(--month-row-height, 150px)' }">
              <CalendarCell
                v-for="cell in monthCells"
                :key="cell.dateStr"
                :date-str="cell.dateStr"
                :current-month="currentMonth" 
                :schedules="store.getSchedulesForDay(cell.dateStr).filter(s => !hiddenTracks.has(s.track))"
                :active-tooltip-id="activeTooltipId"
                :is-today="cell.dateStr === todayStr"
                :is-selected="cell.dateStr === selectedDay"
                :dimmed-node-ids="dimmedNodeIds"
                @cell-click="handleCellClick"
                @add-schedule="openCreateModal"
                @toggle-tooltip="toggleTooltip"
                @edit-schedule="openEditModal"
                @delete-schedule="handleDeleteSchedule"
                @day-detail="openDayDetailModal"
                @hover-node="onNodeHover"
                @open-ai-modal="openAIModal"
              />
            </div>
            <svg 
              v-if="store.showConnections" 
              ref="lineSvg" 
              class="line-svg" 
              :width="svgSize.w" 
              :height="svgSize.h" 
              @click.self="closeRemote"
            >
              <path
                v-for="conn in activeConnections"
                :key="conn.id"
                :d="conn.path"
                :stroke="conn.color"
                class="conn-path"
                :class="{ 
                  'is-default-dimmed': !interactionState.clicked && !interactionState.hovered,
                  'is-dimmed': (interactionState.clicked || interactionState.hovered) && !isEdgeHighlighted(conn.data) 
                }"
                @click.stop="onEdgeClick(conn.data, $event)"
                @mouseenter="onEdgeHover(conn.data, $event)"
                @mouseleave="onEdgeHover(null)"
              />
            </svg>
          </div>
        </div>

        <Transition name="floatbar">
          <div v-if="selectedSchedules.length" class="floating-action-bar">
            <span class="sel-count">{{ selectedSchedules.length }} SELECTED</span>
            <button class="btn-sel-delete" @click="deleteSelected"><i class="fas fa-trash" /> DELETE</button>
            <button class="btn-sel-clear" @click="selectedSchedules = []"><i class="fas fa-times" /></button>
          </div>
        </Transition>
      </div>

      <div v-else-if="currentView === 'week'" class="calendar-area">
        <div ref="calendarWrapper" class="week-wrapper custom-scroll"
          @wheel.passive="onWeekWheel"
          @touchstart.passive="onWeekTouchStart"
          @touchend.passive="onWeekTouchEnd">

          <div class="calendar-header-row week-grid-cols sticky-header">
            <div v-for="(day, idx) in weekDays" :key="`wh-${day}`"
              class="week-col-header"
              :class="{ 'day-header--sat': idx===6, 'day-header--sun': idx===0 }">
              <span class="week-col-label">{{ DAY_LABELS[idx] }}</span>
              <span class="week-col-date" :class="{ 'is-today text-accent-1': day === todayStr }">
                {{ dateOf(day) }}
                <span v-if="day === todayStr" class="week-today-tag">TODAY</span>
              </span>
            </div>
          </div>

          <div ref="weekGraphZone" class="week-graph-zone" :style="{ height: weekGraphHeight + 'px' }">
            <div class="week-bg-grid week-grid-cols">
              <div v-for="i in 7" :key="`bg-col-${i}`" class="week-bg-col"></div>
            </div>
            
            <div
              v-for="(track, tIdx) in sortedAllTracks"
              :key="`lane-bg-${track.id}`"
              class="week-lane-bg"
              :style="{ top: getWeekLaneY(track.id) + 'px', borderBottom: `1.5px dashed ${track.color}` }"
            />

            <div class="week-lane-labels">
              <div
                v-for="(track, tIdx) in sortedAllTracks"
                :key="track.id"
                class="week-lane-label"
                :class="{ 'week-lane-label--highlight': track.isHighlight }"
                :style="{ top: getWeekLaneY(track.id) + 'px', color: track.color, borderColor: track.color }"
                @mouseenter="onTrackHover(track.id)"
                @mouseleave="onTrackHover(null)"
              >
                <span class="lane-dot" :style="{ background: track.color }" />
                <span class="lane-name" :title="track.name">{{ getShortTrackName(track.name) }}</span>
                <span v-if="track.isHighlight" class="lane-hl-badge">✦</span>
              </div>
            </div>

            <svg 
              v-if="store.showConnections" 
              class="line-svg" 
              :width="svgSize.w" 
              :height="svgSize.h" 
              @click.self="closeRemote"
            >
              <path
                v-for="conn in activeConnections"
                :key="conn.id"
                :d="conn.path"
                :stroke="conn.color"
                class="conn-path"
                :class="{ 
                  'is-default-dimmed': !interactionState.clicked && !interactionState.hovered,
                  'is-dimmed': (interactionState.clicked || interactionState.hovered) && !isEdgeHighlighted(conn.data) 
                }"
                @click.stop="onEdgeClick(conn.data, $event)"
                @mouseenter="onEdgeHover(conn.data, $event)"
                @mouseleave="onEdgeHover(null)"
              />
            </svg>

            <template v-for="(day, colIdx) in weekDays" :key="`nodes-col-${colIdx}`">
              <WeekGraphNode
                v-for="s in store.getSchedulesForDay(day)"
                :key="s.id"
                v-show="!hiddenTracks.has(s.track)"
                :schedule="s"
                :col-idx="colIdx"
                :lane-y="getWeekLaneY(s.track)"
                :is-dimmed="dimmedNodeIds.has(s.id)"
                @hover="onNodeHover"
                @edit="openEditModal"
                @select="toggleTooltip(s.id)"
                @open-ai-modal="openAIModal"
              />
            </template>
          </div>

          <div class="week-card-zone week-grid-cols" :class="weekSlideAnimClass">
            <div v-for="(day, idx) in weekDays" :key="day"
              class="week-cell"
              :class="{
                'week-cell--today':    day === todayStr,
                'week-cell--selected': day === selectedDay,
                'week-cell--sat':      idx===6,
                'week-cell--sun':      idx===0
              }"
              @click.stop="handleWeekCellClick(day)"
            >
              <div class="week-cards">
                <WeekScheduleCard
                  v-for="s in store.getSchedulesForDay(day)" :key="s.id"
                  v-show="!hiddenTracks.has(s.track)"
                  :schedule="s"
                  @edit="openEditModal"
                  @delete="handleDeleteSchedule"
                  @mouseenter="onNodeHover(s)"
                  @mouseleave="onNodeHover(null)"
                />
              </div>
              <button class="btn-add-week mech-key" @click.stop="openCreateModal(day)">
                <i class="fas fa-plus" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <Transition name="fade">
      <div 
        v-if="edgeTooltip.visible" 
        class="edge-tooltip-popup" 
        :style="{ left: edgeTooltip.x + 'px', top: (edgeTooltip.baseY - currentScrollY) + 'px', transform: `translate(${edgeTooltip.translateX}, ${edgeTooltip.translateY})` }"
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
        :style="{ left: edgeRemote.x + 'px', top: (edgeRemote.baseY - currentScrollY) + 'px', transform: `translate(${edgeRemote.translateX}, ${edgeRemote.translateY})` }"
      >
        <div class="er-header">
          <span class="er-track-name" :style="{ color: edgeRemote.edge.color }">
            {{ store.getTrackById(edgeRemote.edge.track)?.name }} CONNECTION
          </span>
          <button class="er-close" @click.stop="closeRemote"><i class="fas fa-times"/></button>
        </div>
        <div class="er-body">
          <div class="er-node">
            <div class="er-node-color" :style="{ background: store.getTrackById(edgeRemote.edge.from.track)?.color }"></div>
            <div class="er-node-info">
              <div class="er-node-day">{{ formatNodeDate(edgeRemote.edge.from.day) }}</div>
              <div class="er-node-title">{{ edgeRemote.edge.from.tooltip?.title || edgeRemote.edge.from.text }}</div>
            </div>
            <button class="btn-er-jump" @click.stop="jumpToNode(edgeRemote.edge.from)">
              <i class="fas fa-location-arrow" /> JUMP
            </button>
          </div>
          <div class="er-arrow"><i class="fas fa-arrow-down"/></div>
          <div class="er-node">
            <div class="er-node-color" :style="{ background: store.getTrackById(edgeRemote.edge.to.track)?.color }"></div>
            <div class="er-node-info">
              <div class="er-node-day">{{ formatNodeDate(edgeRemote.edge.to.day) }}</div>
              <div class="er-node-title">{{ edgeRemote.edge.to.tooltip?.title || edgeRemote.edge.to.text }}</div>
            </div>
            <button class="btn-er-jump" @click.stop="jumpToNode(edgeRemote.edge.to)">
              <i class="fas fa-location-arrow" /> JUMP
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <NodeFormModal v-model="isScheduleModalOpen" :mode="modalMode" :initial-form="modalInitialForm" :edit-node-id="editTargetId" @save="handleSaveSchedule" @jump="handleModalJump" />
    <BranchManageModal v-model="isTrackModalOpen" />
    <DayDetailModal v-model="isDayDetailOpen" :day-str="dayDetailTarget" :schedules="store.getSchedulesForDay(dayDetailTarget)" @add-schedule="(d) => { isDayDetailOpen = false; openCreateModal(d) }" @edit-schedule="(s) => { isDayDetailOpen = false; openEditModal(s) }" @delete-schedule="(id) => { handleDeleteSchedule(id) }" />
    
    <AIReasoningModal 
      v-if="isAIModalOpen" 
      :node="aiModalNode" 
      @close="isAIModalOpen = false" 
    />

    <Teleport to="body">
      <div v-if="playEntryAnim" class="fly-overlay-entry">
        <div class="flying-curriculum-part2">
          <i class="fas fa-calendar-check" />
          <span>일정 생성 중...</span>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useThemeStore } from '@/stores/useThemeStore'

import AppSidebar        from '@/components/AppSidebar.vue'
import CalendarHeader    from '@/components/CalendarHeader.vue'
import CalendarCell      from '@/components/calendar/CalendarCell.vue'
import WeekScheduleCard  from '@/components/calendar/WeekScheduleCard.vue'
import WeekGraphNode     from '@/components/calendar/WeekGraphNode.vue'
import DayDetailModal    from '@/components/calendar/DayDetailModal.vue'
import NodeFormModal     from '@/components/modal/NodeFormModal.vue'
import BranchManageModal from '@/components/modal/BranchManageModal.vue'
import AIReasoningModal     from '@/components/modal/AIReasoningModal.vue'

const WEEK_LANE_SPACING = 28;
const WEEK_TOP_MARGIN = 28;
const DAY_LABELS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'] 

const store = useCalendarStore()
const themeStore = useThemeStore()
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
const calendarWrapper = ref(null)
const monthScrollBody = ref(null)
const weekGraphZone = ref(null)
const lineSvg = ref(null)
const svgSize = ref({ w: 0, h: 0 })
const activeConnections = ref([])
const weekSlideDir = ref(null)
const isScheduleModalOpen = ref(false)
const isTrackModalOpen = ref(false)
const isDayDetailOpen = ref(false)
const dayDetailTarget = ref(todayStr)
const modalMode = ref('create')
const editTargetId = ref(null)
const modalInitialForm = ref({})

// AI 추론 요약 모달 상태
const isAIModalOpen = ref(false)
const aiModalNode = ref(null)

const hiddenTracks = ref(new Set())
function onHiddenTracksChange(set) { hiddenTracks.value = new Set(set) }

const interactionState = ref({ hovered: null, clicked: null })
const edgeTooltip = ref({ visible: false, x: 0, baseY: 0, translateX: '-50%', translateY: '-100%', edge: null })
const edgeRemote = ref({ visible: false, x: 0, baseY: 0, translateX: '-50%', translateY: '-100%', edge: null })

// 🚀 플라잉 애니메이션 상태
const playEntryAnim = ref(false)

function onTrackHover(trackId) { interactionState.value.hovered = trackId ? { type: 'track', data: trackId } : null }
function onNodeHover(schedule) { interactionState.value.hovered = schedule ? { type: 'node', data: schedule } : null }

function onEdgeHover(edge, e) {
  if (edge) {
    interactionState.value.hovered = { type: 'edge', data: edge }
    if (e) {
      const x = e.clientX; const w = window.innerWidth;
      let translateX = `-${(x / w) * 100}%`;
      let translateY = '-100%';
      let yOffset = -15;

      if (e.clientY < 300) {
        translateY = '0%';
        yOffset = 15;
      }

      edgeTooltip.value = { visible: true, x, baseY: e.clientY + yOffset + currentScrollY.value, translateX, translateY, edge }
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
      const x = e.clientX; const w = window.innerWidth;
      let translateX = `-${(x / w) * 100}%`;
      let translateY = '-100%';
      let yOffset = -15;

      if (e.clientY < 380) {
        translateY = '0%';
        yOffset = 15;
      }

      edgeRemote.value = { visible: true, x, baseY: e.clientY + yOffset + currentScrollY.value, translateX, translateY, edge }
    }
  }
}

function closeRemote() { edgeRemote.value.visible = false; if (interactionState.value.clicked?.type === 'edge') interactionState.value.clicked = null; }

function jumpToNode(node) {
  if (!node || !node.day) return;
  const d = parseDate(node.day);
  if (isNaN(d.getTime())) return;

  isAdjustingScroll = true;
  anchorDate.value = new Date(d.getFullYear(), d.getMonth(), 1);
  startOffsetWeeks.value = 16; 
  endOffsetWeeks.value = 24; // 버퍼 넉넉히
  currentYear.value = d.getFullYear(); 
  currentMonth.value = d.getMonth() + 1;
  focusedDay.value = node.day;
  selectedDay.value = node.day;
  
  edgeRemote.value.visible = false; 
  activeTooltipId.value = node.id; 
  interactionState.value.clicked = { type: 'node', data: node };

  if (currentView.value === 'month') {
    // DOM이 생성될 때까지 여러 번 시도
    let attempts = 0;
    const tryScroll = () => {
      attempts++;
      const success = scrollToDate(node.day, 'auto', 'center');
      if (success) {
        setTimeout(() => {
          isAdjustingScroll = false;
          const cellEl = document.getElementById(`day-${node.day}`);
          if (cellEl) {
            cellEl.classList.remove('flash-target');
            void cellEl.offsetWidth;
            cellEl.classList.add('flash-target');
            setTimeout(() => cellEl.classList.remove('flash-target'), 1500);
          }
        }, 100);
      } else if (attempts < 20) {
        requestAnimationFrame(tryScroll);
      } else {
        isAdjustingScroll = false;
      }
    };
    nextTick(tryScroll);
  } else {
    isAdjustingScroll = false;
  }
}

function getShortTrackName(name) { return name ? name.trim().split(' ')[0] : ''; }
function formatNodeDate(dateStr) { const [y, m, d] = dateStr.split('-').map(Number); return `${m}월 ${d}일`; }

function isEdgeHighlighted(edge) {
  const { hovered, clicked } = interactionState.value;
  const active = clicked || hovered;
  if (!active) return false;
  if (active.type === 'edge') return active.data.from.id === edge.from.id && active.data.to.id === edge.to.id;
  // 노드 선택 시 연결된 선들 강조
  if (active.type === 'node') return edge.from.id === active.data.id || edge.to.id === active.data.id;
  return false;
}

const dimmedNodeIds = computed(() => {
  const ids = new Set()
  const { hovered, clicked } = interactionState.value
  let activeHL = clicked || hovered;
  if (!activeHL) return ids;

  // 트랙 호버는 기존 로직 유지 (원하면 제거 가능하지만 일단 유지)
  if (activeHL.type === 'track') {
    schedules.value.forEach(s => {
      if (!hiddenTracks.value.has(s.track) && s.track !== activeHL.data) ids.add(s.id);
    });
    return ids;
  }

  schedules.value.forEach(s => {
    if (hiddenTracks.value.has(s.track)) return;
    let isHL = false;
    
    if (activeHL.type === 'node') {
      const targetId = activeHL.data.id;
      // 1) 자기 자신 강조
      if (s.id === targetId) isHL = true;
      // 2) 1차 연결 노드 강조
      else if (store.connections.some(c => (c.from === targetId && c.to === s.id) || (c.to === targetId && c.from === s.id))) {
        isHL = true;
      }
    } 
    else if (activeHL.type === 'edge') {
      // 3) 선택된 선의 양 끝 노드 강조
      isHL = (s.id === activeHL.data.from.id || s.id === activeHL.data.to.id);
    }
    
    if (!isHL) ids.add(s.id);
  });
  return ids;
})

// ============================================================================
// 💡 SVG 기반 연결선 렌더링 (인터랙션 및 성능 최적화)
// ============================================================================
const updateConnections = () => {
  let scrollEl = currentView.value === 'month' ? monthScrollBody.value : weekGraphZone.value;
  if (!scrollEl) return;

  svgSize.value = { w: scrollEl.scrollWidth, h: scrollEl.scrollHeight };
  
  const scrollRect = scrollEl.getBoundingClientRect();
  const offsetTop = scrollRect.top - scrollEl.scrollTop;
  const offsetLeft = scrollRect.left - scrollEl.scrollLeft;

  const newConns = [];
  store.connections.forEach(conn => {
    const fromNode = document.getElementById(`node-${conn.from}`);
    const toNode = document.getElementById(`node-${conn.to}`);
    if (fromNode && toNode) {
      const sFrom = store.schedules.find(s => s.id === conn.from);
      const sTo = store.schedules.find(s => s.id === conn.to);
      if (!sFrom || !sTo || hiddenTracks.value.has(sFrom.track) || hiddenTracks.value.has(sTo.track)) return;
      
      const track = store.getTrackById(sTo.track);
      const fRect = fromNode.getBoundingClientRect();
      const tRect = toNode.getBoundingClientRect();

      const x1 = fRect.left + fRect.width / 2 - offsetLeft;
      const y1 = fRect.bottom - offsetTop;
      const x2 = tRect.left + tRect.width / 2 - offsetLeft;
      const y2 = tRect.top - offsetTop;

      let d = "";
      if (currentView.value === 'week') {
        const midY = (y1 + y2) / 2;
        d = `M ${x1} ${y1} C ${x1} ${midY}, ${x2} ${midY}, ${x2} ${y2}`;
      } else {
        const midX = (x1 + x2) / 2;
        d = `M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`;
      }

      newConns.push({
        id: `${conn.from}-${conn.to}`,
        path: d,
        color: track?.color || '#9ca3af',
        data: { ...conn, from: sFrom, to: sTo, color: track?.color }
      });
    }
  });
  activeConnections.value = newConns;
};

const requestUpdate = () => { requestAnimationFrame(updateConnections); };

onMounted(() => {
  store.fetchHolidaysForYear(currentYear.value); 
  nextTick(() => initMonthScroll());
  window.addEventListener('resize', requestUpdate);
  setTimeout(requestUpdate, 300);
});

onBeforeUnmount(() => { window.removeEventListener('resize', requestUpdate); });

watch([currentView, currentYear, currentMonth, focusedDay, hiddenTracks, interactionState], () => {
  nextTick(() => { setTimeout(requestUpdate, 50); });
}, { deep: true });
// ============================================================================

const anchorDate = ref(new Date(today.getFullYear(), today.getMonth(), 1))
const startOffsetWeeks = ref(12) 
const endOffsetWeeks = ref(16)   

const monthCells = computed(() => {
  const cells = []
  const start = new Date(anchorDate.value.getFullYear(), anchorDate.value.getMonth(), 1)
  start.setDate(start.getDate() - start.getDay()) 
  start.setDate(start.getDate() - startOffsetWeeks.value * 7)

  const totalWeeks = startOffsetWeeks.value + endOffsetWeeks.value + 6
  const d = new Date(start)

  for (let i = 0; i < totalWeeks * 7; i++) {
    cells.push({ dateStr: toDateStr(d), month: d.getMonth() + 1, year: d.getFullYear() })
    d.setDate(d.getDate() + 1)
  }
  return cells
})

let scrollTicking = false;
let isAdjustingScroll = false; 

function handleMonthScroll(e) {
  if (e && e.target) currentScrollY.value = e.target.scrollTop;
  if (isAdjustingScroll) return; 

  if (!scrollTicking) {
    window.requestAnimationFrame(() => {
      const el = monthScrollBody.value
      if (!el) { scrollTicking = false; return; }

      const centerLine = el.scrollTop + (el.clientHeight / 2) + 50 
      const firstCell = el.querySelector('.calendar-cell')
      if (firstCell) {
        const rowHeight = firstCell.offsetHeight || 150
        const rowIndex = Math.floor(centerLine / rowHeight)
        const target = monthCells.value[rowIndex * 7 + 3] 
        if (target && (target.month !== currentMonth.value || target.year !== currentYear.value)) {
          currentMonth.value = target.month; currentYear.value = target.year;
        }
      }

      const threshold = 1800; // 넓은 임계값

      if (el.scrollTop < threshold) {
        isAdjustingScroll = true;
        const oldScrollHeight = el.scrollHeight;
        const oldScrollTop = el.scrollTop;
        
        startOffsetWeeks.value += 16; 
        // 하단 제거 로직 삭제 (점프 방지)

        nextTick(() => {
          requestAnimationFrame(() => {
            const heightDiff = el.scrollHeight - oldScrollHeight;
            el.scrollTop = oldScrollTop + heightDiff;
            setTimeout(() => { isAdjustingScroll = false; }, 50);
          });
        });
      } 
      else if (el.scrollHeight - el.scrollTop - el.clientHeight < threshold) {
        isAdjustingScroll = true;
        endOffsetWeeks.value += 16; 
        
        // 상단 제거 로직 삭제 (점프 방기)
        setTimeout(() => { isAdjustingScroll = false; }, 100);
      }
      scrollTicking = false;
    });
    scrollTicking = true;
  }
}

function initMonthScroll() { 
  startOffsetWeeks.value = 12;
  endOffsetWeeks.value = 16;
  anchorDate.value = new Date(currentYear.value, currentMonth.value - 1, 1);
  nextTick(() => {
    const firstDayStr = `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}-01`;
    scrollToDate(firstDayStr, 'auto', 'top');
  });
}

function scrollToDate(dateStr, behavior = 'smooth', align = 'top') {
  const el = document.getElementById(`day-${dateStr}`);
  if (el && monthScrollBody.value) {
    let targetTop = el.offsetTop - 50; 
    if (align === 'center') {
      const containerH = monthScrollBody.value.clientHeight;
      targetTop = Math.max(0, el.offsetTop - (containerH / 2) + (el.clientHeight / 2));
    }
    monthScrollBody.value.scrollTo({ top: targetTop, behavior });
    return true;
  }
  return false;
}

function navigate(dir) {
  if (currentView.value === 'month') {
    let d;
    if (dir === 'prev') d = new Date(currentYear.value, currentMonth.value - 2, 1);
    else if (dir === 'next') d = new Date(currentYear.value, currentMonth.value, 1);
    else { jumpToDate(todayStr); return; }

    const diffMonths = Math.abs((d.getFullYear() - anchorDate.value.getFullYear()) * 12 + (d.getMonth() - anchorDate.value.getMonth()));

    if (diffMonths > 3) {
      // 대규모 이동 시 앵커 재설정 및 버퍼 초기화
      isAdjustingScroll = true;
      anchorDate.value = d;
      startOffsetWeeks.value = 16;
      endOffsetWeeks.value = 20;
      currentYear.value = d.getFullYear();
      currentMonth.value = d.getMonth() + 1;
      
      nextTick(() => {
        requestAnimationFrame(() => {
          scrollToDate(toDateStr(d), 'auto', 'top');
          setTimeout(() => { isAdjustingScroll = false; }, 100);
        });
      });
    } else {
      const targetEl = document.getElementById(`day-${toDateStr(d)}`);
      if (targetEl) {
        scrollToDate(toDateStr(d), 'smooth', 'top');
      } else { 
        // 요소가 없는 경우만 앵커 조정
        isAdjustingScroll = true;
        anchorDate.value = d;
        nextTick(() => {
          scrollToDate(toDateStr(d), 'auto', 'top');
          setTimeout(() => { isAdjustingScroll = false; }, 100);
        });
      }
    }
  } else if (currentView.value === 'week') {
    const d = parseDate(focusedDay.value)
    if (dir === 'prev') d.setDate(d.getDate() - 7)
    else if (dir === 'next') d.setDate(d.getDate() + 7)
    else { 
      focusedDay.value = todayStr; 
      nextTick(() => scrollToDate(todayStr, 'smooth', 'center'));
      return; 
    }
    weekSlideDir.value = dir; focusedDay.value = toDateStr(d); currentMonth.value = d.getMonth() + 1; currentYear.value = d.getFullYear()
  }
}

const tracksInCurrentView = computed(() => {
  const prefix = `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}`
  const idsWithDataInMonth = new Set()
  schedules.value.forEach(s => { if (s.day.startsWith(prefix)) idsWithDataInMonth.add(s.track) })
  return allTracks.value.filter(t => {
    if (t.isHighlight || t.id.includes('prompt') || t.id.includes('blog')) return true
    return !t.isEnded || idsWithDataInMonth.has(t.id)
  })
})

const sortedAllTracks = computed(() => {
  return [...store.allTracks].sort((a,b) => {
    if(a.isHighlight && !b.isHighlight) return 1;
    if(!a.isHighlight && b.isHighlight) return -1;
    return a.index - b.index
  })
})

const headerDateText = computed(() => `${currentYear.value} . ${String(currentMonth.value).padStart(2,'0')}`)

const weekDays = computed(() => {
  const d = parseDate(focusedDay.value); const day = d.getDay(); const start = new Date(d); start.setDate(d.getDate() - day);
  const days = []; for (let i = 0; i < 7; i++) { const cur = new Date(start); cur.setDate(start.getDate() + i); days.push(toDateStr(cur)); }
  return days
})
const dateOf = (dateStr) => parseInt(dateStr.split('-')[2], 10)

const weekGraphHeight = computed(() => {
  let maxOffset = WEEK_TOP_MARGIN; let currentIdx = null;
  for (const t of sortedAllTracks.value) { if (currentIdx !== t.index) { if (currentIdx !== null) maxOffset += WEEK_LANE_SPACING; currentIdx = t.index; } }
  return maxOffset + WEEK_TOP_MARGIN; 
})

const getWeekLaneY = (trackId) => {
  let offset = WEEK_TOP_MARGIN; let currentIdx = null;
  for (const t of sortedAllTracks.value) {
    if (currentIdx !== t.index) { if (currentIdx !== null) offset += WEEK_LANE_SPACING; currentIdx = t.index; }
    if (t.id === trackId) return offset;
  }
  return offset;
}

const weekSlideAnimClass = computed(() => { if (!weekSlideDir.value) return ''; return weekSlideDir.value === 'next' ? 'slide-in-left' : 'slide-in-right' })
watch(weekSlideDir, (v) => { if (v) setTimeout(() => { weekSlideDir.value = null }, 340) })

function setView(v) { 
  currentView.value = v; currentScrollY.value = 0; 
  const d = parseDate(focusedDay.value); currentYear.value = d.getFullYear(); currentMonth.value = d.getMonth() + 1; 
  if (v === 'month') nextTick(() => initMonthScroll()); 
}

function toDateStr(d) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
function parseDate(str) { const [y,m,d] = str.split('-').map(Number); return new Date(y, m-1, d) }

function openCreateModal(dateStr) { selectedDay.value = dateStr; focusedDay.value = dateStr; modalMode.value = 'create'; editTargetId.value = null; modalInitialForm.value = { day: dateStr, track: '', title: '', text: '', time: '09:00', tags: '', parentIds: [], childIds: [] }; isScheduleModalOpen.value = true; activeTooltipId.value = null }
function openEditModal(schedule) { 
  const t = store.allTracks.find(x => x.id === schedule.track);
  if (t && t.isEnded) { alert('종료된 트랙의 일정은 수정할 수 없습니다.'); return; }
  focusedDay.value = schedule.day; selectedDay.value = schedule.day; modalMode.value = 'edit'; editTargetId.value = schedule.id; modalInitialForm.value = { day: schedule.day, track: schedule.track, title: schedule.tooltip?.title||'', text: schedule.text||'', time: schedule.tooltip?.time||'09:00', tags: schedule.tooltip?.tags?.join(', ')||'', parentIds: connections.value.filter(e => e.to === schedule.id).map(e => e.from), childIds: connections.value.filter(e => e.from === schedule.id).map(e => e.to) }; isScheduleModalOpen.value = true; activeTooltipId.value = null 
}

function handleCellClick(dateStr) { if (selectedDay.value !== dateStr) activeTooltipId.value = null; selectedDay.value = dateStr; focusedDay.value = dateStr; interactionState.value.clicked = null;}
function handleWeekCellClick(dateStr) { if (selectedDay.value !== dateStr) activeTooltipId.value = null; selectedDay.value = dateStr; interactionState.value.clicked = null; }
function openDayDetailModal(dateStr) { dayDetailTarget.value = dateStr; isDayDetailOpen.value = true }
function toggleTooltip(id) { 
  activeTooltipId.value = activeTooltipId.value === id ? null : id; 
  if (activeTooltipId.value) { 
    const s = schedules.value.find(s => s.id === id); 
    if (s) { 
      selectedDay.value = s.day; 
      focusedDay.value = s.day; 
      interactionState.value.clicked = { type: 'node', data: s }; 
    } 
  } else { 
    interactionState.value.clicked = null; 
  } 
}
function openAIModal(s) {
  aiModalNode.value = s;
  isAIModalOpen.value = true;
}
function jumpToDate(date) {
  const d = date instanceof Date ? date : parseDate(date)
  if (isNaN(d.getTime())) return;
  const targetStr = toDateStr(d)
  
  isAdjustingScroll = true;
  currentYear.value = d.getFullYear()
  currentMonth.value = d.getMonth() + 1
  focusedDay.value = targetStr
  selectedDay.value = targetStr
  anchorDate.value = new Date(d.getFullYear(), d.getMonth(), 1)
  startOffsetWeeks.value = 16
  endOffsetWeeks.value = 24
  
  let attempts = 0;
  const tryScroll = () => {
    attempts++;
    const success = scrollToDate(targetStr, 'auto', 'center');
    if (success) {
      setTimeout(() => { isAdjustingScroll = false; }, 100);
    } else if (attempts < 20) {
      requestAnimationFrame(tryScroll);
    } else {
      isAdjustingScroll = false;
    }
  };
  nextTick(tryScroll);
}

async function handleSaveSchedule(payload) {
  const { parentIds, childIds, ...data } = payload
  if (modalMode.value === 'create') { const newId = await store.createSchedule(data); store.updateConnectionsForSchedule(newId, parentIds || [], childIds || []) } 
  else { await store.updateSchedule(editTargetId.value, data); store.updateConnectionsForSchedule(editTargetId.value, parentIds || [], childIds || []) }
  isScheduleModalOpen.value = false
}
function handleModalJump(dateStr) {
  isScheduleModalOpen.value = false
  jumpToDate(dateStr)
}

async function handleDeleteSchedule(id) { if (!confirm('이 일정을 삭제하시겠습니까?')) return; await store.deleteSchedule(id); activeTooltipId.value = null }
function deleteSelected() { if (!selectedSchedules.value.length) return; if (!confirm(`선택한 ${selectedSchedules.value.length}개의 일정을 삭제하시겠습니까?`)) return; selectedSchedules.value.forEach(id => store.deleteSchedule(id)); selectedSchedules.value = [] }

watch(currentYear, (y) => store.fetchHolidaysForYear(y))
onMounted(() => { 
  store.fetchHolidaysForYear(currentYear.value); 
  nextTick(() => {
    initMonthScroll();
    setTimeout(() => {
      scrollToDate(todayStr, 'smooth', 'center');
    }, 100);
  });

  // 🚀 온보딩 페이지에서 넘어왔는지 확인 후 파트 2 애니메이션 재생
  if (sessionStorage.getItem('playCalendarEntryAnim') === 'true') {
    sessionStorage.removeItem('playCalendarEntryAnim')
    playEntryAnim.value = true
    
    // 파트 2 비행 애니메이션(0.7초)이 완전히 끝나는 0.8초 시점에 뷰에서 제거
    setTimeout(() => {
      playEntryAnim.value = false
    }, 800)
  }
})

let _touchStartX = 0; let wheelTimeout = null;
function onWeekTouchStart(e) { _touchStartX = e.touches[0].clientX }
function onWeekTouchEnd(e) { const dx = e.changedTouches[0].clientX - _touchStartX; if (Math.abs(dx) > 50) navigate(dx < 0 ? 'next' : 'prev') }
function onWeekWheel(e) {
  if (Math.abs(e.deltaX) > 20 && Math.abs(e.deltaX) > Math.abs(e.deltaY)) {
    if (!wheelTimeout) { navigate(e.deltaX > 0 ? 'next' : 'prev'); wheelTimeout = setTimeout(() => { wheelTimeout = null }, 600); }
  }
}
</script>

<style scoped>
.custom-scroll { overflow-y: auto; overflow-x: hidden; -ms-overflow-style: none; scrollbar-width: none; overflow-anchor: auto; }
.custom-scroll::-webkit-scrollbar { display: none; }

.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ── 🎨 테마 변수 ── */
.theme-light { --k-bg: #FFFFFF; --k-housing: #F5F5F7; --k-key-bg: #FFFFFF; --k-key-border: #E5E5E7; --k-key-shadow: rgba(0,0,0,0.05); --k-border-main: #E5E5E7; --bg-base: #FFFFFF; --bg-surface: #F5F5F7; --bg-elevated: #FFFFFF; --border: #E5E5E7; --border-mid: #D1D1D6; --text-primary: #1D1D1F; --text-secondary: #424245; --text-muted: #86868B; --text-faint: #A1A1A6; --accent: #007AFF; --today-bg: rgba(0, 122, 255, 0.05); --sun-color: #FF3B30; --sat-color: #007AFF; --k-acc-1-bg: #FF3B30; --k-acc-1-shadow: #D70015; --k-acc-2-bg: #007AFF; --k-acc-2-shadow: #0040DD; --k-acc-3-bg: #34C759; --k-acc-3-shadow: #248A3D; }
.theme-dark { --k-bg: #000000; --k-housing: #1C1C1E; --k-key-bg: #2C2C2E; --k-key-border: #3A3A3C; --k-key-shadow: rgba(0,0,0,0.3); --k-border-main: #3A3A3C; --bg-base: #000000; --bg-surface: #1C1C1E; --bg-elevated: #2C2C2E; --border: #3A3A3C; --border-mid: #48484A; --text-primary: #F5F5F7; --text-secondary: #A1A1A6; --text-muted: #86868B; --text-faint: #636366; --accent: #0A84FF; --today-bg: rgba(10, 132, 255, 0.15); --sun-color: #FF453A; --sat-color: #0A84FF; --k-acc-1-bg: #FF453A; --k-acc-1-shadow: #D70015; --k-acc-2-bg: #0A84FF; --k-acc-2-shadow: #0040DD; --k-acc-3-bg: #32D74B; --k-acc-3-shadow: #248A3D; }

/* ── 공통 ── */
.calendar-area { flex: 1; overflow: hidden; position: relative; display: flex; flex-direction: column; background: var(--bg-base); }
.calendar-wrapper { position: relative; width: 100%; min-height: 100%; display: flex; flex-direction: column; }

/* ── 월간 뷰 전용 ── */
.month-scroll-body { flex: 1; position: relative; z-index: 10; scroll-behavior: auto; }
.calendar-header-row { display: grid; border-bottom: 1px solid var(--border-mid); background: var(--bg-surface); z-index: 150 !important; position: sticky; top: 0; }
.grid-cols-7 { grid-template-columns: repeat(7, 1fr); }
.day-header { padding: 14px 0; text-align: center; font-size: 13px; font-weight: 900; color: var(--text-primary); letter-spacing: 0.05em; border-right: 1px solid var(--border); }
.day-header:last-child { border-right: none; }
.day-header--sat { color: #2563eb !important; }
.day-header--sun { color: #dc2626 !important; }
.line-svg { position: absolute; top: 0; left: 0; pointer-events: none; z-index: 2; }
.conn-path { fill: none; stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); cursor: pointer; pointer-events: stroke; }
.conn-path:hover { stroke-width: 4 !important; opacity: 1 !important; }
.conn-path.is-default-dimmed { opacity: 0.15; }
.conn-path.is-dimmed { opacity: 0.05 !important; }
h.is-highlighted { stroke-width: 4; stroke-opacity: 1; }

.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); position: relative; }

/* ── Floating Action Bar ── */
.floating-action-bar {
  position: absolute; bottom: 32px; left: 50%; transform: translateX(-50%);
  background: var(--text-primary); color: var(--bg-base); border: 1px solid var(--text-primary); border-radius: 40px;
  padding: 10px 24px; display: flex; align-items: center; gap: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2); z-index: 200; font-weight: 900; letter-spacing: 0.05em;
}
.floating-action-bar button {
  background: transparent; color: var(--bg-base); border: 1px solid rgba(255,255,255,0.2);
  padding: 6px 16px; font-weight: 900; cursor: pointer; border-radius: 40px; transition: all 0.2s; letter-spacing: 0.05em;
  font-size: 11px;
}
.floating-action-bar button:hover { background: rgba(255,255,255,0.1); border-color: white; }

/* ── 주간 뷰(Week View) ── */
.week-wrapper { flex: 1; display: flex; flex-direction: column; position: relative; background: transparent; }
.sticky-header { position: sticky; top: 0; z-index: 90; }
.week-grid-cols { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }

.week-col-header { padding: 18px 0 14px; text-align: center; border-right: 1px solid var(--border); display: flex; flex-direction: column; align-items: center; gap: 6px; border-bottom: 1px solid var(--border-mid); }
.week-col-header:last-child { border-right: none; }
.week-col-label { font-size: 11px; font-weight: 800; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.1em; }
.week-col-date { font-size: 22px; font-weight: 900; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }
.week-holiday-name { font-size: 10px; font-weight: 800; color: #dc2626; margin-top: 4px; letter-spacing: -0.05em; }

.day-header--sat .week-col-label, .day-header--sat .week-col-date { color: #2563eb !important; }
.day-header--sun .week-col-label, .day-header--sun .week-col-date { color: #dc2626 !important; }
.day-header--holiday .week-col-label, .day-header--holiday .week-col-date { color: #dc2626 !important; }

.week-col-header:has(.is-today) .week-col-label,
.week-col-header:has(.is-today) .week-col-date,
.week-col-header:has(.is-today) .week-holiday-name { color: var(--bg-base) !important; }
.week-col-header:has(.is-today) { background: var(--bg-surface); color: var(--text-primary); }
.week-today-tag { font-size: 10px; background: var(--text-primary); border: none; padding: 2px 8px; color: var(--bg-base); font-weight: 900; letter-spacing: 0.05em; border-radius: 40px; }

/* 그래프 구역 */
.week-graph-zone { position: relative; border-bottom: 2px solid var(--border); flex-shrink: 0; background: transparent; z-index: 10; }
.week-bg-grid { position: absolute; inset: 0; pointer-events: none; z-index: 1; }
.week-bg-col { border-right: 1px solid var(--border); }
.week-bg-col:last-child { border-right: none; }
.week-lane-bg { position: absolute; left: 0; right: 0; height: 1px; transform: translateY(-50%); z-index: 0; pointer-events: none; background: transparent !important; }

/* 라벨 구역 */
.week-lane-labels { position: absolute; inset: 0; pointer-events: none; z-index: 45; }
.week-lane-label { 
  position: absolute; left: 12px; transform: translateY(-50%); margin-top: 0; 
  display: inline-flex; align-items: center; gap: 8px; padding: 4px 12px; 
  border-radius: 40px; border: 1px solid var(--border); background: var(--bg-elevated); 
  font-size: 10px; font-weight: 800; color: var(--text-primary); 
  cursor: pointer; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); pointer-events: auto; text-transform: uppercase;
  max-width: calc((100% / 12) - 12px); 
}
.week-lane-label:hover { border-color: var(--text-primary); z-index: 100; max-width: max-content; transform: translateY(-50%) scale(1.05); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.lane-dot { width: 6px; height: 6px; border-radius: 50%; border: 1px solid rgba(0,0,0,0.1); flex-shrink: 0; }
.lane-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: 'Inter', sans-serif; }
.lane-hl-badge { font-size: 9px; margin-left: 4px; flex-shrink: 0; }

/* 하단 카드 존 */
.week-card-zone { flex: 1; background: transparent; min-height: 100%; }

.week-cell { border-right: 1px solid var(--border); padding: 16px 8px 48px; display: flex; flex-direction: column; gap: 12px; position: relative; transition: background 0.1s; cursor: pointer; min-height: 100%; }
.week-cell:last-child { border-right: none; }
.week-cell:hover { background: var(--bg-hover); }
.week-cell--today { background: transparent !important; }
.week-cell--selected { outline: 2px solid var(--text-primary); outline-offset: -2px; background: transparent !important; }
.week-cards { display: flex; flex-direction: column; gap: 8px; }

.btn-add-week { position: absolute; bottom: 12px; left: 50%; transform: translateX(-50%); width: 32px; height: 32px; border-radius: 0; background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); cursor: pointer; font-size: 12px; display: flex; align-items: center; justify-content: center; opacity: 0; transition: all 0.1s; }
.week-cell:hover .btn-add-week { opacity: 1; }
.btn-add-week:hover { background: var(--text-primary); color: var(--bg-base); transform: scale(1.1); box-shadow: 2px 2px 0 var(--text-primary); }

@keyframes targetFlash { 0% { background-color: var(--text-primary); color: var(--bg-base); box-shadow: inset 0 0 0 4px var(--bg-base); } 100% { background-color: transparent; box-shadow: inset 0 0 0 0px transparent; } }
:deep(.flash-target) { animation: targetFlash 0.8s ease-out; border-radius: 0; }

/* ★ 툴팁 & 모달 통일 */
.edge-tooltip-popup { position: absolute; z-index: 80; pointer-events: none; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 12px; padding: 12px 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); display: flex; flex-direction: column; gap: 8px; min-width: max-content; white-space: nowrap; }
.et-track { font-size: 11px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.1em; color: var(--text-muted); }
.et-nodes { display: flex; align-items: center; gap: 10px; font-size: 13px; font-weight: 700; color: var(--text-primary); }
.et-nodes i { color: var(--text-muted); font-size: 11px; }

.edge-remote-modal { position: absolute; z-index: 500; width: 280px; background: var(--bg-base); border: 1px solid var(--text-primary); box-shadow: 0 16px 48px rgba(0,0,0,0.15); display: flex; flex-direction: column; overflow: hidden; }
.er-header { padding: 14px 16px; background: var(--bg-surface); border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
.er-track-name { font-size: 13px; font-weight: 900; letter-spacing: 0.05em; text-transform: uppercase; }
.er-close { background: none; border: none; color: var(--text-muted); font-size: 14px; cursor: pointer; transition: color 0.15s; padding: 0 4px; line-height: 1; }
.er-close:hover { color: var(--text-primary); }
.er-body { padding: 8px; display: flex; flex-direction: column; gap: 4px; }
.er-node { display: flex; align-items: center; gap: 12px; padding: 12px; border: 1px solid var(--border); background: var(--bg-base); transition: all 0.2s; }
.er-node:hover { border-color: var(--text-primary); background: var(--bg-surface); }
.er-node-color { width: 4px; height: 32px; border-radius: 2px; flex-shrink: 0; }
.er-node-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.er-node-day { font-size: 11px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.05em; }
.er-node-title { font-size: 13px; font-weight: 900; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 130px; }
.btn-er-jump { padding: 6px 10px; font-size: 10px; font-weight: 900; background: var(--text-primary); color: var(--bg-base); border: none; cursor: pointer; border-radius: 0; transition: all 0.15s; display: flex; align-items: center; gap: 6px; }
.btn-er-jump:hover { opacity: 0.8; transform: scale(1.05); }
.er-arrow { display: flex; justify-content: center; color: var(--text-muted); font-size: 10px; margin: 4px 0; }

@keyframes slideInFromLeft { from { transform: translateX(-6%); opacity: 0.5; } to { transform: translateX(0); opacity: 1; } }
@keyframes slideInFromRight { from { transform: translateX(6%); opacity: 0.5; } to { transform: translateX(0); opacity: 1; } }
.slide-in-left  { animation: slideInFromLeft  0.2s ease-out both; }
.slide-in-right { animation: slideInFromRight 0.2s ease-out both; }

/* 🚀 플라잉 애니메이션 Part 2 (사이드바로 쏙 날아감) */
.fly-overlay-entry { position: fixed; inset: 0; z-index: 9999; pointer-events: none; }
.flying-curriculum-part2 {
  position: absolute; display: flex; align-items: center; gap: 12px; padding: 24px 36px;
  background: var(--text-primary); color: var(--bg-base); font-size: 20px; font-weight: 900;
  border: 4px solid var(--text-primary); white-space: nowrap; font-family: 'Space Grotesk', 'Escoredream', sans-serif;
  /* Part 1에서 끝난 상태(압축된 알약)와 동일한 모습으로 시작 */
  top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.45); border-radius: 100px;
  animation: flyIntoNav 0.7s cubic-bezier(0.5, 0, 0.2, 1) forwards;
}
@keyframes flyIntoNav {
  0% { top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.45); opacity: 1; box-shadow: 0 0 40px rgba(0,0,0,0.3); }
  20% { top: 48%; left: 50%; transform: translate(-50%, -50%) scale(0.45); opacity: 1; box-shadow: 0 0 40px rgba(0,0,0,0.3); } /* 살짝 떴다가 */
  100% { top: 120px; left: 32px; transform: translate(-50%, -50%) scale(0.05); opacity: 0; } /* 사이드바 캘린더 아이콘 위치로 흡수 */
}
</style>