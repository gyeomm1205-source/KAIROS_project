<template>
  <div class="app-layout" @click="handleBackdropClick">
    <AppSidebar />

    <main class="main-content">
      <CalendarHeader
        :date-text="headerDateText"
        :current-view="currentView"
        :current-date="parseDate(focusedDay)"
        @navigate="navigate"
        @change-view="setView"
        @open-track-modal="isTrackModalOpen = true"
        @jump-to-date="jumpToDate"
        @update:hiddenTracks="onHiddenTracksChange"
      />

      <div v-if="currentView === 'month'" class="calendar-area">
        <div ref="calendarWrapper" class="calendar-wrapper">

          <div class="calendar-header-row grid-cols-7 month-dow-header">
            <div v-for="(d, i) in DAY_LABELS" :key="d" class="day-header"
              :class="{ 'day-header--sat': i===6, 'day-header--sun': i===0 }">
              {{ d }}
            </div>
          </div>

          <div
            class="month-scroll-body"
            :class="{ 'month-scroll-body--fit': monthWeekCount <= 5 }"
            ref="monthScrollBody"
            @scroll.passive="handleMonthScroll"
          >
            <div
              class="calendar-grid grid-cols-7"
              :class="{ 'calendar-grid--fit': monthWeekCount <= 5 }"
              :style="monthWeekCount <= 5 ? {} : { gridAutoRows: 'var(--month-row-height, 150px)' }"
            >
              <CalendarCell
                v-for="cell in monthCells"
                :key="cell.dateStr"
                :date-str="cell.dateStr"
                :current-month="cell.month"
                :schedules="store.getSchedulesForDay(cell.dateStr)"
                :active-tooltip-id="activeTooltipId"
                :is-today="cell.dateStr === todayStr"
                :is-selected="cell.dateStr === selectedDay"
                :hidden-tracks="hiddenTracks"
                @cell-click="handleCellClick"
                @add-schedule="openCreateModal"
                @toggle-tooltip="toggleTooltip"
                @edit-schedule="openEditModal"
                @delete-schedule="handleDeleteSchedule"
                @day-detail="openDayDetailModal"
              />
            </div>
            <canvas ref="lineCanvas" class="line-canvas" />
          </div>
        </div>

        <Transition name="floatbar">
          <div v-if="selectedSchedules.length" class="floating-action-bar">
            <span class="sel-count">{{ selectedSchedules.length }}개 선택됨</span>
            <button class="btn-sel-delete" @click="deleteSelected">
              <i class="fas fa-trash" /> 삭제
            </button>
            <button class="btn-sel-clear" @click="selectedSchedules = []">
              <i class="fas fa-times" />
            </button>
          </div>
        </Transition>
      </div>

      <div v-else-if="currentView === 'week'" class="calendar-area">
        <div ref="calendarWrapper" class="week-wrapper"
          @touchstart.passive="onWeekTouchStart"
          @touchend.passive="onWeekTouchEnd">

          <div class="calendar-header-row grid-cols-7">
            <div v-for="(day, idx) in weekDays" :key="`wh-${day}`"
              class="week-col-header"
              :class="{ 'day-header--sat': idx===6, 'day-header--sun': idx===0 }">
              <span class="week-col-label">{{ DAY_LABELS[idx] }}</span>
              <span class="week-col-date" :class="{ 'is-today': day === todayStr }">
                {{ dateOf(day) }}
                <span v-if="day === todayStr" class="week-today-tag">TODAY</span>
              </span>
            </div>
          </div>

          <div ref="weekGraphZone" class="week-graph-zone" :style="{ height: weekGraphHeight + 'px' }">
            <canvas ref="lineCanvas" class="line-canvas" />
            <div class="week-lane-labels">
              <div
                v-for="(track, tIdx) in sortedAllTracks"
                :key="track.id"
                class="week-lane-label"
                :class="{ 'week-lane-label--highlight': track.isHighlight }"
                :style="{
                  top: (WEEK_TOP_MARGIN + tIdx * WEEK_LANE_SPACING) + 'px',
                  color: track.color,
                  borderColor: track.color + '44'
                }"
              >
                <span class="lane-dot" :style="{ background: track.color }" />
                {{ track.name }}
                <span v-if="track.isHighlight" class="lane-hl-badge">✦</span>
              </div>
            </div>
            <div
              v-for="(track, tIdx) in sortedAllTracks"
              :key="`lane-bg-${track.id}`"
              class="week-lane-bg"
              :style="{
                top: (WEEK_TOP_MARGIN + tIdx * WEEK_LANE_SPACING) + 'px',
                background: track.color + (track.isHighlight ? '22' : '14')
              }"
            />
            <template v-for="(day, colIdx) in weekDays" :key="`nodes-col-${colIdx}`">
              <WeekGraphNode
                v-for="s in store.getSchedulesForDay(day)"
                :key="s.id"
                v-show="!hiddenTracks.has(s.track)"
                :schedule="s"
                :col-idx="colIdx"
                :total-cols="7"
                :is-active="activeTooltipId === s.id"
                :total-tracks="sortedAllTracks.length"
                @toggle-tooltip="toggleTooltip"
                @edit="openEditModal"
                @delete="handleDeleteSchedule"
              />
            </template>
          </div>

          <div class="week-card-zone grid-cols-7" :class="weekSlideAnimClass">
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
                  :schedule="s"
                  @edit="openEditModal"
                  @delete="handleDeleteSchedule"
                />
              </div>
              <button class="btn-add-week" @click.stop="openCreateModal(day)">
                <i class="fas fa-plus" />
              </button>
            </div>
          </div>

        </div>
      </div>

      <div v-else class="prompt-view-layout">

        <div class="prompt-graph-panel">

          <div class="prompt-graph-title">
            <div class="ptl-header-tracks">
              <span
                v-for="track in promptTracks"
                :key="track.id"
                v-show="!hiddenTracks.has(track.id)"
                class="ptl-header-track-badge"
                :style="{ color: track.color }"
              >
                <span class="ptl-head-dot" :style="{ background: track.color }" />
                {{ track.name }}
              </span>
            </div>
            <span class="ptl-count-badge">{{ allPromptSchedules.length }}개</span>
          </div>

          <div class="prompt-graph-inner">

            <div v-if="!allPromptSchedules.length" class="prompt-tl-empty">
              <i class="fas fa-code-branch" />
              <p>{{ currentMonth }}월에는 등록된 일정이 없어요</p>
            </div>

            <div v-else class="v-graph-timeline">
              
              <div class="v-graph-spines-container">
                <div
                  v-for="track in promptTracks"
                  :key="`bg-spine-${track.id}`"
                  class="v-graph-spine-line"
                  v-show="!hiddenTracks.has(track.id)"
                >
                  <div class="v-graph-spine-inner" :style="{ background: track.color }"></div>
                </div>
              </div>

              <template v-for="(group, gi) in groupedPromptSchedules" :key="group.day">
                
                <div class="v-graph-date-row" :id="`prompt-day-${group.day}`">
                  <div class="v-graph-date-badge">{{ formatShortDate(group.day) }} {{ getDayOfWeek(group.day) }}</div>
                </div>

                <div v-if="!group.schedules.length" class="v-graph-empty-day"></div>

                <div
                  v-for="s in group.schedules"
                  :key="s.id"
                  class="v-graph-row"
                  :class="{ 'is-selected': selectedPromptId === s.id }"
                  @click="selectPromptSchedule(s)"
                >
                  <div class="v-graph-time">{{ s.tooltip?.time || '—' }}</div>

                  <div class="v-graph-lanes">
                    <div
                      v-for="track in promptTracks"
                      :key="track.id"
                      class="v-graph-lane"
                      v-show="!hiddenTracks.has(track.id)"
                    >
                      <div
                        v-if="s.track === track.id"
                        class="v-graph-node"
                        :style="{ background: 'var(--bg-surface)', borderColor: track.color }"
                      >
                        <div class="v-graph-node-inner" :style="{ background: track.color }"></div>
                      </div>
                    </div>
                  </div>

                  <div class="v-graph-content">
                    <div class="v-graph-card" :style="{ borderLeftColor: promptTrackColor(s.track) }">
                      <div class="v-graph-card-header">
                        <span class="v-graph-track-name" :style="{ color: promptTrackColor(s.track) }">
                          {{ promptTrackName(s.track) }}
                        </span>
                        <div v-if="s.tooltip?.tags?.length" class="v-graph-tags">
                          <span v-for="t in s.tooltip.tags" :key="t" class="v-graph-tag">{{ t }}</span>
                        </div>
                      </div>
                      <div class="v-graph-title">{{ s.tooltip?.title || s.text }}</div>
                      
                      <i v-if="selectedPromptId === s.id" class="fas fa-chevron-right v-graph-cursor" :style="{ color: promptTrackColor(s.track) }"/>
                    </div>
                  </div>
                </div>

              </template>
            </div>
          </div>
        </div>

        <div class="prompt-chat-panel">
          <div class="prompt-chat-header">
            <template v-if="selectedPromptSchedule">
              <div class="prompt-schedule-info">
                <span
                  class="prompt-schedule-dot"
                  :style="{ background: promptTrackColor(selectedPromptSchedule.track) }"
                />
                <div>
                  <div class="prompt-chat-title">{{ selectedPromptSchedule.tooltip?.title || selectedPromptSchedule.text }}</div>
                  <div class="prompt-chat-subtitle">
                    {{ formatShortDate(selectedPromptSchedule.day) }} · {{ selectedPromptSchedule.tooltip?.time || '' }}
                    <span
                      class="prompt-track-badge"
                      :style="{ color: promptTrackColor(selectedPromptSchedule.track), borderColor: promptTrackColor(selectedPromptSchedule.track) + '40' }"
                    >{{ promptTrackName(selectedPromptSchedule.track) }}</span>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="prompt-schedule-info">
                <div class="prompt-welcome-icon-sm"><i class="fas fa-robot" /></div>
                <div>
                  <div class="prompt-chat-title">프롬프트 어시스턴트</div>
                  <div class="prompt-chat-subtitle">왼쪽에서 일정을 선택하세요</div>
                </div>
              </div>
            </template>
          </div>

          <div class="prompt-messages-area" ref="promptMessagesRef">
            <div v-if="!selectedPromptSchedule" class="prompt-welcome">
              <div class="prompt-welcome-icon"><i class="fas fa-robot" /></div>
              <p class="prompt-welcome-text">왼쪽 타임라인에서 일정을 선택하면<br>해당 일정의 프롬프트 내역이 표시됩니다.</p>
              <div class="prompt-suggestions">
                <button class="prompt-suggest-chip" @click="promptInput = '오늘 학습 계획을 요약해줘'">오늘 학습 계획 요약</button>
                <button class="prompt-suggest-chip" @click="promptInput = '다음 주 준비할 내용은?'">다음 주 준비 내용</button>
                <button class="prompt-suggest-chip" @click="promptInput = '부족한 부분 분석해줘'">부족한 부분 분석</button>
              </div>
            </div>

            <template v-else>
              <div v-if="!currentMessages.length" class="prompt-empty-msgs">
                <i class="fas fa-comment-dots" />
                <p>아직 프롬프트가 없습니다.<br>아래에 메시지를 입력하세요.</p>
              </div>
              <div
                v-for="msg in currentMessages"
                :key="msg.id"
                class="prompt-msg"
                :class="msg.role === 'user' ? 'prompt-msg--user' : 'prompt-msg--ai'"
              >
                <div class="prompt-msg-bubble">{{ msg.content }}</div>
                <div class="prompt-msg-time">{{ msg.time }}</div>
              </div>
            </template>
          </div>

          <div class="prompt-input-area">
            <textarea
              v-model="promptInput"
              class="prompt-textarea"
              :placeholder="selectedPromptSchedule ? '메시지를 입력하세요...' : '일정을 먼저 선택해주세요'"
              :disabled="!selectedPromptSchedule"
              rows="3"
              @keydown.enter.exact.prevent="sendPrompt"
            />
            <button class="prompt-send-btn" @click="sendPrompt" :disabled="!promptInput.trim() || !selectedPromptSchedule">
              <i class="fas fa-paper-plane" />
            </button>
          </div>
        </div>
      </div>
    </main>

    <NodeFormModal
      v-model="isScheduleModalOpen"
      :mode="modalMode"
      :initial-form="modalInitialForm"
      :edit-node-id="editTargetId"
      @save="handleSaveSchedule"
    />
    <BranchManageModal v-model="isTrackModalOpen" />

    <DayDetailModal
      v-model="isDayDetailOpen"
      :day-str="dayDetailTarget"
      :schedules="store.getSchedulesForDay(dayDetailTarget)"
      @add-schedule="(d) => { isDayDetailOpen = false; openCreateModal(d) }"
      @edit-schedule="(s) => { isDayDetailOpen = false; openEditModal(s) }"
      @delete-schedule="(id) => { handleDeleteSchedule(id) }"
    />
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
import WeekScheduleCard  from '@/components/calendar/WeekScheduleCard.vue'
import WeekGraphNode     from '@/components/calendar/WeekGraphNode.vue'
import DayDetailModal    from '@/components/calendar/DayDetailModal.vue'
import NodeFormModal     from '@/components/modal/NodeFormModal.vue'
import BranchManageModal from '@/components/modal/BranchManageModal.vue'

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

const store = useCalendarStore()
const { schedules, connections } = storeToRefs(store)

function toDateStr(date) {
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}
function parseDate(str) {
  if (!str) return new Date();
  const [y,m,d] = str.split('-').map(Number)
  return new Date(y, m-1, d)
}
function dateOf(str) { return new Date(str + 'T00:00:00').getDate() }

const today    = new Date()
const todayStr = toDateStr(today)

const currentView  = ref('month')
const currentYear  = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)
const focusedDay   = ref(todayStr)
const selectedDay  = ref(null)
const activeTooltipId = ref(null)
const selectedSchedules = ref([])

const isScheduleModalOpen = ref(false)
const isTrackModalOpen    = ref(false)
const isDayDetailOpen     = ref(false)
const dayDetailTarget     = ref(todayStr)
const modalMode           = ref('create')
const editTargetId        = ref(null)
const modalInitialForm    = ref({})

const calendarWrapper  = ref(null)
const monthScrollBody  = ref(null)
const weekSlideDir     = ref(null)
const weekGraphZone    = ref(null)
const lineCanvas       = ref(null)

const hiddenTracks = ref(new Set())
function onHiddenTracksChange(set) {
  hiddenTracks.value = new Set(set)  // 항상 새 객체 → ref 변경 감지 보장
}

const { drawLines, requestDraw, startSlideAnimation } = useCanvasLines(calendarWrapper, lineCanvas, currentView, currentYear, currentMonth, focusedDay, weekGraphZone, monthScrollBody, hiddenTracks)

function buildMonthCells(y, m) {
  const firstDay = new Date(y, m-1, 1)
  const lastDay  = new Date(y, m, 0)
  const startDow = firstDay.getDay()
  const cells    = []
  for (let i = startDow - 1; i >= 0; i--) cells.push({ dateStr: toDateStr(new Date(y, m-1, -i)), month: m })
  for (let d = 1; d <= lastDay.getDate(); d++) cells.push({ dateStr: toDateStr(new Date(y, m-1, d)), month: m })
  const rem = cells.length % 7
  if (rem !== 0) for (let d = 1; d <= 7 - rem; d++) cells.push({ dateStr: toDateStr(new Date(y, m, d)), month: m })
  return cells
}

const monthCells = computed(() => buildMonthCells(currentYear.value, currentMonth.value))
const monthWeekCount = computed(() => monthCells.value.length / 7)

const weekDays = computed(() => {
  const d   = parseDate(focusedDay.value)
  const dow = d.getDay()
  const start = new Date(d)
  start.setDate(d.getDate() - dow)
  return Array.from({ length: 7 }, (_, i) => {
    const day = new Date(start); day.setDate(start.getDate() + i)
    return toDateStr(day)
  })
})

const headerDateText = computed(() => {
  const y = currentYear.value, m = currentMonth.value
  if (currentView.value === 'week') {
    const d = parseDate(focusedDay.value)
    return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${Math.ceil(d.getDate()/7)}주차`
  }
  if (currentView.value === 'day' || currentView.value === 'prompt') {
    const d = parseDate(focusedDay.value)
    return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일`
  }
  return `${y}년 ${m}월`
})

function handleBackdropClick() { activeTooltipId.value = null }

function openDayDetailModal(dateStr) {
  dayDetailTarget.value = dateStr
  isDayDetailOpen.value = true
}

function handleCellClick(dateStr) {
  if (selectedDay.value !== dateStr) activeTooltipId.value = null
  selectedDay.value = dateStr
  focusedDay.value  = dateStr
}

function handleWeekCellClick(dateStr) {
  if (selectedDay.value !== dateStr) activeTooltipId.value = null
  selectedDay.value = dateStr
}

function toggleTooltip(id) {
  activeTooltipId.value = activeTooltipId.value === id ? null : id
  if (activeTooltipId.value) {
    const s = schedules.value.find(s => s.id === id)
    if (s) { selectedDay.value = s.day; focusedDay.value  = s.day }
  }
}

function scrollToPromptDay(dateStr) {
  if (currentView.value !== 'prompt') return
  nextTick(() => {
    setTimeout(() => {
      const el = document.getElementById(`prompt-day-${dateStr}`)
      const container = document.querySelector('.prompt-graph-inner')
      if (el && container) {
        container.scrollTo({ top: el.offsetTop, behavior: 'smooth' })
      }
    }, 100)
  })
}

function navigate(dir) {
  if (currentView.value === 'month' || currentView.value === 'prompt') {
    if (dir === 'prev') {
      if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value-- }
      else currentMonth.value--
      focusedDay.value = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-01`
      selectedDay.value = null
    } else if (dir === 'next') {
      if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++ }
      else currentMonth.value++
      focusedDay.value = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-01`
      selectedDay.value = null
    } else {
      currentYear.value  = today.getFullYear()
      currentMonth.value = today.getMonth() + 1
      focusedDay.value   = todayStr
      selectedDay.value  = todayStr
    }
    
    if (currentView.value === 'month') nextTick(() => initMonthScroll())
    if (currentView.value === 'prompt') scrollToPromptDay(focusedDay.value)
    
  } else if (currentView.value === 'week') {
    const d = parseDate(focusedDay.value)
    if (dir === 'prev')      d.setDate(d.getDate() - 7)
    else if (dir === 'next') d.setDate(d.getDate() + 7)
    else { focusedDay.value = todayStr; weekSlideDir.value = null; return }
    weekSlideDir.value = dir
    focusedDay.value   = toDateStr(d)
    currentMonth.value = d.getMonth() + 1
    currentYear.value  = d.getFullYear()
  }
}

function setView(v) {
  currentView.value = v
  const d = parseDate(focusedDay.value)
  currentYear.value  = d.getFullYear()
  currentMonth.value = d.getMonth() + 1
  if (v === 'month') nextTick(() => initMonthScroll())
  if (v === 'prompt') scrollToPromptDay(focusedDay.value)
}

function jumpToDate(date) {
  currentYear.value  = date.getFullYear()
  currentMonth.value = date.getMonth() + 1
  focusedDay.value   = toDateStr(date)
  selectedDay.value  = toDateStr(date)
  scrollToPromptDay(focusedDay.value)
}

function trackColor(id) { return store.getTrackById(id)?.color || '#6b7280' }

function openCreateModal(dateStr) {
  selectedDay.value  = dateStr
  focusedDay.value   = dateStr
  modalMode.value        = 'create'
  editTargetId.value     = null
  modalInitialForm.value = { day: dateStr, track: 'main', title: '', text: '', time: '09:00', tags: '', parentIds: [], childIds: [] }
  isScheduleModalOpen.value = true
  activeTooltipId.value     = null
}

function openEditModal(schedule) {
  focusedDay.value  = schedule.day
  selectedDay.value = schedule.day
  modalMode.value        = 'edit'
  editTargetId.value     = schedule.id
  modalInitialForm.value = {
    day: schedule.day, track: schedule.track,
    title: schedule.tooltip?.title||'', text: schedule.text||'',
    time: schedule.tooltip?.time||'09:00', tags: schedule.tooltip?.tags?.join(', ')||'',
    parentIds: connections.value.filter(e => e.to   === schedule.id).map(e => e.from),
    childIds:  connections.value.filter(e => e.from === schedule.id).map(e => e.to)
  }
  isScheduleModalOpen.value = true
  activeTooltipId.value     = null
}

async function handleSaveSchedule(payload) {
  const { parentIds, childIds, ...data } = payload
  if (modalMode.value === 'create') {
    const title = data.tooltip?.title?.trim()
    const exists = schedules.value.some(s => s.day === data.day && s.track === data.track && (s.tooltip?.title?.trim() === title))
    if (exists) { alert(`"${title}" 일정이 이미 존재합니다.`); return }
    const newId = await store.createSchedule(data)
    store.updateConnectionsForSchedule(newId, parentIds || [], childIds || [])
  } else {
    const title = data.tooltip?.title?.trim()
    const exists = schedules.value.some(s => s.id !== editTargetId.value && s.day === data.day && s.track === data.track && (s.tooltip?.title?.trim() === title))
    if (exists) { alert(`"${title}" 일정이 이미 존재합니다.`); return }
    await store.updateSchedule(editTargetId.value, data)
    store.updateConnectionsForSchedule(editTargetId.value, parentIds || [], childIds || [])
  }
  isScheduleModalOpen.value = false
}

async function handleDeleteSchedule(id) {
  if (!confirm('이 일정을 삭제하시겠습니까?')) return
  await store.deleteSchedule(id)
  activeTooltipId.value = null
}

const weekGraphHeight = computed(() => {
  const trackCount = store.allTracks.length
  return WEEK_TOP_MARGIN + (trackCount - 1) * WEEK_LANE_SPACING + WEEK_TOP_MARGIN
})

const weekSlideAnimClass = computed(() => {
  if (!weekSlideDir.value) return ''
  return weekSlideDir.value === 'next' ? 'slide-in-left' : 'slide-in-right'
})

watch(weekSlideDir, (v) => { if (v) setTimeout(() => { weekSlideDir.value = null }, 340) })
// Set은 deep watch 불가 → 레퍼런스 교체로 감지 (onHiddenTracksChange에서 new Set() 할당)
watch(hiddenTracks, async () => {
  await nextTick()
  await nextTick()   // DOM 업데이트 두 틱 대기 (v-show 반영 후 캔버스 재드로우)
  requestDraw?.()
})

const sortedAllTracks = computed(() => [...store.allTracks].sort((a, b) => a.index - b.index))
const promptInput      = ref('')
const promptMessagesRef = ref(null)

const selectedPromptId = ref(null)
const selectedPromptSchedule = computed(() => selectedPromptId.value ? schedules.value.find(s => s.id === selectedPromptId.value) || null : null)
const promptMessagesMap = ref({})
const currentMessages = computed(() => {
  if (!selectedPromptId.value) return []
  return promptMessagesMap.value[selectedPromptId.value] || []
})

const PROMPT_TRACK_IDS = ['prompt', 'blog']
const promptTracks = computed(() => {
  return store.allTracks.filter(t => PROMPT_TRACK_IDS.some(id => t.id?.toLowerCase().includes(id) || t.name?.toLowerCase().includes(id))).sort((a, b) => a.index - b.index)
})

const allPromptSchedules = computed(() => {
  const ids = promptTracks.value.map(t => t.id)
  return schedules.value.filter(s => {
    if (!ids.includes(s.track)) return false
    if (hiddenTracks.value.has(s.track)) return false
    const [y, m] = s.day.split('-').map(Number)
    return y === currentYear.value && m === currentMonth.value
  }).sort((a, b) => {
    const da = a.day + (a.tooltip?.time || '99:99')
    const db = b.day + (b.tooltip?.time || '99:99')
    return da.localeCompare(db)
  })
})

function formatShortDate(str) {
  const [, m, d] = str.split('-')
  return `${parseInt(m)}/${parseInt(d)}`
}
function getDayOfWeek(str) {
  const [y, m, d] = str.split('-').map(Number)
  return ['일', '월', '화', '수', '목', '금', '토'][new Date(y, m - 1, d).getDay()]
}

const groupedPromptSchedules = computed(() => {
  const map = new Map()
  const lastDayOfMonth = new Date(currentYear.value, currentMonth.value, 0).getDate();
  for (let d = 1; d <= lastDayOfMonth; d++) {
    const dateStr = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    map.set(dateStr, []);
  }
  allPromptSchedules.value.forEach(s => {
    if (map.has(s.day)) map.get(s.day).push(s)
  })
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([day, schedules]) => ({ day, schedules }))
})

function selectPromptSchedule(s) {
  selectedPromptId.value = s.id
  selectedDay.value = s.day
  focusedDay.value = s.day
  const [y, m] = s.day.split('-').map(Number)
  currentYear.value = y
  currentMonth.value = m
}

function promptTrackColor(id) { return store.getTrackById(id)?.color || '#6b7280' }
function promptTrackName(id)  { return store.getTrackById(id)?.name  || id }

function sendPrompt() {
  if (!promptInput.value.trim() || !selectedPromptId.value) return
  const sid = selectedPromptId.value
  if (!promptMessagesMap.value[sid]) promptMessagesMap.value[sid] = []
  const now = new Date()
  const time = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
  promptMessagesMap.value[sid].push({ id: Date.now() + '_u', role: 'user', content: promptInput.value.trim(), time })
  promptInput.value = ''
  nextTick(() => { if (promptMessagesRef.value) promptMessagesRef.value.scrollTop = promptMessagesRef.value.scrollHeight })
}

function deleteSelected() {
  if (!selectedSchedules.value.length) return
  if (!confirm(`선택한 ${selectedSchedules.value.length}개의 일정을 삭제하시겠습니까?`)) return
  selectedSchedules.value.forEach(id => store.deleteSchedule(id))
  selectedSchedules.value = []
}

function handleMonthScroll() {}
function initMonthScroll() { const el = monthScrollBody.value; if (el) el.scrollTop = 0 }
onMounted(() => { nextTick(() => initMonthScroll()) })

let _touchStartX = 0
function onWeekTouchStart(e) { _touchStartX = e.touches[0].clientX }
function onWeekTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - _touchStartX
  if (Math.abs(dx) > 50) navigate(dx < 0 ? 'next' : 'prev')
}
</script>

<style scoped>
.app-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.calendar-area {
  flex: 1; overflow: hidden;
  background: var(--bg-base);
  display: flex; flex-direction: column;
  position: relative;
}

.calendar-wrapper {
  position: relative; width: 100%; height: 100%;
  background: var(--bg-surface);
  display: flex; flex-direction: column;
  overflow: hidden;
}

.calendar-header-row {
  display: grid;
  border-bottom: 1px solid var(--border);
  background: var(--bg-elevated);
  flex-shrink: 0;
}
.month-dow-header {
  position: sticky; top: 0; z-index: 10;
}
.grid-cols-7 { grid-template-columns: repeat(7, 1fr); }

.day-header {
  padding: 10px 0; text-align: center;
  font-size: 12px; font-weight: 700; color: var(--text-faint);
  font-family: 'Escoredream', sans-serif;
}
.day-header--sm  { padding: 6px 0; font-size: 10px; }
.day-header--sat { color: var(--sat-color) !important; }
.day-header--sun { color: var(--sun-color) !important; }

.line-canvas {
  position: absolute;
  top: 0; left: 0;
  pointer-events: none;
  z-index: 2; 
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: var(--month-row-height, 150px);
  position: relative;
}
.calendar-grid:not(.calendar-grid--fit) {
  min-height: 0;
}

.floating-action-bar {
  position: absolute;
  bottom: 20px; left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  display: flex; align-items: center; gap: 10px;
  padding: 8px 18px;
  background: var(--bg-elevated);
  border: 1.5px solid var(--accent);
  border-radius: 999px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.3);
  white-space: nowrap;
}
.sel-count {
  font-size: 13px; font-weight: 700;
  color: var(--accent); font-family: 'Escoredream', sans-serif;
}
.btn-sel-delete {
  display: flex; align-items: center; gap: 5px;
  padding: 5px 12px; border: 1px solid #ef4444;
  background: rgba(239,68,68,0.08); color: #ef4444;
  border-radius: 7px; font-size: 12px; font-weight: 700;
  cursor: pointer; transition: all 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.btn-sel-delete:hover { background: #ef4444; color: #fff; }
.btn-sel-clear {
  background: none; border: none; color: var(--text-faint);
  font-size: 16px; cursor: pointer; padding: 2px 4px;
  line-height: 1;
}
.btn-sel-clear:hover { color: var(--text-primary); }
.floatbar-enter-active { transition: all 0.18s ease; }
.floatbar-leave-active { transition: all 0.14s ease; }
.floatbar-enter-from, .floatbar-leave-to { opacity: 0; transform: translateX(-50%) translateY(8px); }

.month-scroll-body {
  flex: 1; overflow-y: auto; overflow-x: hidden;
  position: relative;
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) var(--scrollbar-track);
  overscroll-behavior-y: none;
  scroll-behavior: auto;
}
.month-scroll-body--fit {
  overflow-y: hidden;
  display: flex; flex-direction: column;
}
.month-scroll-body--fit .calendar-grid--fit {
  flex: 1; grid-auto-rows: 1fr;
}

.week-wrapper {
  position: relative; width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-surface);
  box-shadow: 0 2px 16px rgba(0,0,0,0.08);
  display: flex; flex-direction: column;
  flex: 1; min-height: 0;
  overflow: hidden;
}

.week-col-header {
  padding: 14px 12px 12px;
  text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  border-right: 1px solid var(--border);
}
.week-col-label {
  font-size: 10px; font-weight: 700;
  color: var(--text-faint);
  letter-spacing: 0.08em; text-transform: uppercase;
}
.week-col-date {
  font-size: 20px; font-weight: 800;
  color: var(--text-muted);
  font-family: 'Escoredream', sans-serif;
  line-height: 1;
  display: flex; align-items: center; gap: 5px;
}
.week-col-date.is-today { color: var(--text-primary); }
.week-today-tag {
  font-size: 8px; font-weight: 800;
  color: var(--text-muted);
  background: rgba(168, 162, 158, 0.12);
  border: 1px solid rgba(168, 162, 158, 0.3);
  border-radius: 4px;
  padding: 1px 4px;
  letter-spacing: 0.04em;
  line-height: 1.5;
}

.week-graph-zone {
  position: relative;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border);
  background: var(--bg-base);
  overflow: visible; 
}
.week-lane-labels {
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 90px;
  pointer-events: none;
  z-index: 3;
}
.week-lane-label {
  position: absolute;
  left: 8px;
  transform: translateY(-50%);
  display: flex; align-items: center; gap: 5px;
  font-size: 9px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  letter-spacing: 0.03em;
  background: var(--bg-surface);
  border: 1px solid;
  border-radius: 999px;
  padding: 2px 8px 2px 5px;
  white-space: nowrap;
  opacity: 0.85;
}
.lane-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.lane-hl-badge { font-size: 8px; margin-left: 1px; opacity: 0.8; }
.week-lane-label--highlight { border-style: dashed !important; opacity: 0.9; }

.week-lane-bg {
  position: absolute;
  left: 0; right: 0;
  height: 1px;
  transform: translateY(-50%);
  pointer-events: none;
  z-index: 0;
}

.week-graph-zone::before {
  content: '';
  position: absolute; inset: 0;
  background: repeating-linear-gradient(
    to right,
    transparent 0,
    transparent calc(100% / 7 - 1px),
    var(--border) calc(100% / 7 - 1px),
    var(--border) calc(100% / 7)
  );
  pointer-events: none;
  z-index: 0;
}
.week-graph-anchor { position: absolute; width: 1px; height: 1px; pointer-events: none; z-index: 0; }

.week-card-zone { display: grid; flex: 1; overflow: auto; scrollbar-width: thin; }

.week-cell {
  border-right: 1px solid var(--border);
  padding: 12px 10px 44px;
  background: var(--bg-surface);
  position: relative;
  display: flex; flex-direction: column; gap: 8px;
  transition: background 0.15s;
  min-height: 160px;
  cursor: pointer;
}
.week-cell:hover          { background: var(--bg-elevated); }
.week-cell--today         { background: var(--today-bg) !important; border-top: 2px solid rgba(168, 162, 158, 0.5); }
.week-cell--selected      { background: rgba(59,130,246,0.06) !important; outline: 2px solid var(--accent); outline-offset: -2px; }
.week-cell--sat, .week-cell--sun { background: color-mix(in srgb, var(--bg-surface), var(--bg-base) 30%); }

.week-cards { display: flex; flex-direction: column; gap: 8px; }

.btn-add-week {
  position: absolute; bottom: 10px; right: 10px;
  width: 26px; height: 26px; border-radius: 7px;
  background: var(--bg-hover); color: var(--text-muted);
  border: 1px solid var(--border);
  cursor: pointer; font-size: 10px;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: all 0.15s;
}
.week-cell:hover .btn-add-week { opacity: 1; }
.btn-add-week:hover { background: var(--accent); color: #fff; border-color: var(--accent); }

@keyframes slideInFromLeft { from { transform: translateX(-6%); opacity: 0.5; } to { transform: translateX(0); opacity: 1; } }
@keyframes slideInFromRight { from { transform: translateX(6%); opacity: 0.5; } to { transform: translateX(0); opacity: 1; } }
.slide-in-left  { animation: slideInFromLeft  0.32s cubic-bezier(0.25,0.46,0.45,0.94) both; }
.slide-in-right { animation: slideInFromRight 0.32s cubic-bezier(0.25,0.46,0.45,0.94) both; }

.prompt-view-layout { flex: 1; display: flex; overflow: hidden; gap: 0; }

.prompt-graph-panel {
  width: 380px; flex-shrink: 0;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  overflow: hidden;
}

.prompt-graph-title {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  background: var(--bg-elevated);
}
.ptl-header-tracks { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.ptl-header-track-badge {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
}
.ptl-head-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.ptl-count-badge {
  font-size: 10px; font-weight: 700;
  color: var(--text-faint);
  background: var(--bg-hover);
  padding: 2px 8px; border-radius: 999px;
  border: 1px solid var(--border);
  font-family: 'Escoredream', sans-serif;
  white-space: nowrap;
}

/* ── 프롬프트 스크롤 영역 (다이얼 스냅 추가) ── */
.prompt-graph-inner {
  flex: 1; overflow-y: auto; min-height: 0;
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) transparent;
  scroll-behavior: smooth;
  
  /* ★ 핵심: 다이얼처럼 탁탁 걸리게 만드는 스크롤 스냅 */
  scroll-snap-type: y mandatory;
}

.prompt-tl-empty {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 40px 20px; color: var(--text-faint); text-align: center;
}
.prompt-tl-empty i { font-size: 28px; opacity: 0.25; }
.prompt-tl-empty p { font-size: 12px; font-family: 'Escoredream', sans-serif; }

/* =========================================
   세로 Graph 타임라인 (프롬프트 뷰 전용)
   ========================================= */
.v-graph-timeline {
  display: flex; flex-direction: column;
  padding: 0 0 40px;
  position: relative;
}

/* ★ 연속되는 배경 선 (Spines) 컨테이너 */
.v-graph-spines-container {
  position: absolute;
  top: 0; bottom: 0;
  left: 69px; 
  display: flex; gap: 8px;
  z-index: 25; 
  pointer-events: none;
}
.v-graph-spine-line {
  width: 14px; 
  position: relative;
}
.v-graph-spine-inner {
  position: absolute; top: 0; bottom: 0; left: 50%;
  width: 2px; transform: translateX(-50%);
  opacity: 0.3;
}

/* ── 날짜 뱃지 ── */
.v-graph-date-row {
  display: flex; align-items: center; gap: 12px;
  margin: 16px 0 8px 0; 
  position: sticky; top: 12px; 
  z-index: 20; 
  pointer-events: none; 
  
  scroll-snap-align: start;
  scroll-snap-stop: always; /* ★ 추가: 휠 한 번에 여러 개 건너뛰기 금지 */
  scroll-margin-top: 12px;
}

.v-graph-date-badge {
  font-size: 11px; font-weight: 800; color: var(--text-primary);
  background: var(--bg-elevated); padding: 5px 12px;
  border-radius: 0 999px 999px 0; font-family: 'Escoredream', sans-serif;
  border: 1px solid var(--border-mid); border-left: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15); /* 떠있는 느낌 강조 */
  min-width: 50px; text-align: center;
  pointer-events: auto; /* 뱃지는 클릭 가능하게 */
}
/* 가로선 완전히 숨김 */
.v-graph-date-line {
  display: none; 
}

/* 일정이 없는 빈 날짜용 여백 */
.v-graph-empty-day {
  height: 28px;
  scroll-snap-align: start;
  scroll-snap-stop: always; /* ★ 추가 */
  scroll-margin-top: 50px;
}

/* ── 하나의 일정 행(Row) ── */
.v-graph-row {
  display: flex; align-items: stretch;
  padding: 0 20px 0 10px; cursor: pointer;
  position: relative; transition: background 0.15s;
  z-index: 10;
  
  scroll-snap-align: start;
  scroll-snap-stop: always; /* ★ 추가: 무조건 한 칸씩만 톱니바퀴처럼 걸리게 강제 */
  scroll-margin-top: 50px; 
}
.v-graph-row:hover { background: var(--bg-hover); }
.v-graph-row.is-selected { background: rgba(59, 130, 246, 0.05); }

.v-graph-time {
  width: 45px; flex-shrink: 0;
  font-size: 11px; color: var(--text-faint); font-family: monospace;
  padding-top: 18px; text-align: right; margin-right: 14px;
}

.v-graph-lanes {
  display: flex; align-items: stretch; gap: 8px;
  position: relative; 
}
.v-graph-lane {
  width: 14px; position: relative;
  display: flex; justify-content: center;
}

.v-graph-node {
  position: absolute; 
  top: 50%; left: 50%; 
  transform: translate(-50%, -50%);
  width: 14px; height: 14px; border-radius: 50%;
  border: 2.5px solid; 
  z-index: 30; 
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.v-graph-row:hover .v-graph-node { transform: translate(-50%, -50%) scale(1.3); }
.v-graph-row.is-selected .v-graph-node { transform: translate(-50%, -50%) scale(1.3); box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 25%, transparent); }
.v-graph-node-inner { width: 6px; height: 6px; border-radius: 50%; }

.v-graph-content {
  flex: 1; min-width: 0; padding-left: 18px; padding-top: 8px; padding-bottom: 8px;
  position: relative; z-index: 10;
}
.v-graph-card {
  position: relative;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-left: 4px solid;
  border-radius: 8px; padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: all 0.2s ease;
}
.v-graph-row:hover .v-graph-card {
  border-color: var(--border-mid); background: var(--bg-elevated);
  transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.v-graph-row.is-selected .v-graph-card {
  border-color: var(--accent); background: var(--bg-elevated);
  transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.v-graph-card-header {
  display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;
}
.v-graph-track-name {
  font-size: 10px; font-weight: 800; text-transform: uppercase;
  font-family: 'Escoredream', sans-serif; letter-spacing: 0.04em;
}
.v-graph-title {
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  font-family: 'Escoredream', sans-serif; line-height: 1.4;
}
.v-graph-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.v-graph-tag {
  font-size: 9px; color: var(--text-faint);
  background: var(--bg-base); border: 1px solid var(--border);
  padding: 2px 6px; border-radius: 4px; font-family: 'Escoredream', sans-serif;
}

.v-graph-cursor {
  position: absolute; right: 14px; top: 50%; transform: translateY(-50%);
  font-size: 14px; opacity: 0.8; animation: slideRight 1s infinite alternate ease-in-out;
}
@keyframes slideRight {
  from { transform: translate(0, -50%); }
  to { transform: translate(4px, -50%); }
}

.prompt-chat-panel {
  flex: 1; display: flex; flex-direction: column;
  background: var(--bg-base); overflow: hidden;
}
.prompt-chat-header {
  padding: 18px 24px 14px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
  flex-shrink: 0;
}
.prompt-chat-title {
  display: flex; align-items: center; gap: 9px;
  font-size: 15px; font-weight: 800;
  font-family: 'Escoredream', sans-serif;
  color: var(--text-primary);
}
.prompt-chat-title i { color: var(--accent); font-size: 16px; }
.prompt-chat-subtitle {
  font-size: 11px; color: var(--text-faint);
  font-family: 'Escoredream', sans-serif;
  margin-top: 3px; margin-left: 25px;
}
.prompt-messages-area {
  flex: 1; overflow-y: auto; padding: 24px;
  scrollbar-width: thin;
  display: flex; flex-direction: column; gap: 12px;
}
.prompt-welcome {
  display: flex; flex-direction: column; align-items: center;
  gap: 14px; padding: 40px 20px; text-align: center;
}
.prompt-welcome-icon {
  width: 60px; height: 60px; border-radius: 50%;
  background: var(--accent); opacity: 0.9;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #fff;
}
.prompt-welcome-text {
  font-size: 14px; color: var(--text-muted);
  font-family: 'Escoredream', sans-serif; line-height: 1.6;
}
.prompt-suggestions {
  display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;
  margin-top: 4px;
}
.prompt-suggest-chip {
  padding: 7px 14px; border: 1px solid var(--border);
  background: var(--bg-elevated); color: var(--text-secondary);
  border-radius: 999px; font-size: 11px; font-weight: 600;
  cursor: pointer; font-family: 'Escoredream', sans-serif;
  transition: all 0.15s;
}
.prompt-suggest-chip:hover {
  border-color: var(--accent); color: var(--accent);
  background: rgba(59,130,246,0.08);
}
.prompt-input-area {
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg-surface);
  display: flex; gap: 10px; align-items: flex-end;
  flex-shrink: 0;
}
.prompt-textarea {
  flex: 1; background: var(--bg-elevated);
  border: 1px solid var(--border); border-radius: 12px;
  color: var(--text-primary); font-size: 13px;
  font-family: 'Escoredream', sans-serif;
  padding: 12px 14px; outline: none; resize: none;
  line-height: 1.6; transition: border-color 0.15s;
}
.prompt-textarea:focus { border-color: var(--accent); }
.prompt-textarea::placeholder { color: var(--text-faint); }
.prompt-send-btn {
  width: 42px; height: 42px; border-radius: 10px;
  background: var(--accent); color: #fff; border: none;
  cursor: pointer; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.prompt-send-btn:hover { opacity: 0.85; transform: scale(1.05); }
.prompt-send-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.prompt-textarea:disabled { opacity: 0.45; cursor: not-allowed; }

.prompt-schedule-info { display: flex; align-items: center; gap: 12px; }
.prompt-schedule-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 6px currentColor; }
.prompt-welcome-icon-sm {
  width: 36px; height: 36px; border-radius: 50%; background: var(--accent); opacity: 0.85;
  display: flex; align-items: center; justify-content: center; font-size: 15px; color: #fff; flex-shrink: 0;
}
.prompt-track-badge {
  display: inline-block; font-size: 9px; font-weight: 700; padding: 1px 6px; border-radius: 4px;
  border: 1px solid; margin-left: 6px; font-family: 'Escoredream', sans-serif; letter-spacing: 0.03em;
}

.prompt-empty-msgs {
  display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 48px 20px; color: var(--text-faint); text-align: center;
}
.prompt-empty-msgs i { font-size: 32px; opacity: 0.2; }
.prompt-empty-msgs p { font-size: 13px; font-family: 'Escoredream', sans-serif; line-height: 1.6; }

.prompt-msg { display: flex; flex-direction: column; gap: 3px; }
.prompt-msg--user { align-items: flex-end; }
.prompt-msg--ai   { align-items: flex-start; }

.prompt-msg-bubble {
  max-width: 80%; padding: 10px 14px; border-radius: 14px; font-size: 13px; line-height: 1.55;
  font-family: 'Escoredream', sans-serif; white-space: pre-wrap; word-break: break-word;
}
.prompt-msg--user .prompt-msg-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 4px; }
.prompt-msg--ai .prompt-msg-bubble { background: var(--bg-elevated); border: 1px solid var(--border); color: var(--text-primary); border-bottom-left-radius: 4px; }
.prompt-msg-time { font-size: 9px; color: var(--text-faint); font-family: monospace; padding: 0 4px; }
</style>