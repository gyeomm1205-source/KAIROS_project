<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">

      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">학습 캘린더</h1>
          <div class="gcal-badge">
            <i class="fas fa-check-circle" />
            Google Calendar 연결됨 ✓
          </div>
        </div>
        <div class="header-actions">
          <div v-if="lastSync" class="sync-time">마지막 동기화: {{ lastSync }}</div>
          <button class="btn-import" @click="showImportModal = true">
            <i class="fas fa-download" /> 일정 불러오기
          </button>
          <button class="btn-sync" @click="handleSync">
            <i :class="syncing ? 'fas fa-spinner fa-spin' : 'fas fa-sync-alt'" />
            일정 반영하기
          </button>

          <RouterLink to="/calendar" class="btn-flow-cal">
            <i class="fas fa-code-branch" />
            캘린더 플로우
          </RouterLink>
        </div>
      </div>

      <div class="body-grid">

        <div class="cal-panel">

          <div class="cal-top">
            <button class="nav-btn" @click="prevMonth"><i class="fas fa-chevron-left"/></button>
            <span class="cal-month-label">{{ monthLabel }}</span>
            <button class="nav-btn" @click="nextMonth"><i class="fas fa-chevron-right"/></button>
          </div>

          <div class="dow-row">
            <span v-for="d in ['일','월','화','수','목','금','토']" :key="d" class="dow">{{ d }}</span>
          </div>

          <div class="date-grid">
            <div
              v-for="cell in calendarCells" :key="cell.key"
              class="date-cell"
              :class="{
                'date-cell--other':    !cell.current,
                'date-cell--today':     cell.isToday,
                'date-cell--selected':  cell.dateStr === selectedDay,
                'date-cell--has-event': cell.events.length > 0,
              }"
              @click="selectDay(cell.dateStr)"
            >
              <span class="date-num">{{ cell.day }}</span>
              <div class="dots-row">
                <span
                  v-for="(ev, i) in cell.events.slice(0, 3)" :key="i"
                  class="event-dot"
                  :style="{ background: ev.color }"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="day-panel">
          <div class="day-panel-title">{{ dayPanelTitle }}</div>

          <div class="today-card" v-if="selectedDayEvents.length">
            <div class="today-card-left">
              <div class="today-icon"><i class="fas fa-star" /></div>
              <div>
                <div class="today-label">오늘의 학습</div>
                <div class="today-status">현재 진행 중</div>
              </div>
            </div>
            <div class="today-progress-wrap">
              <div class="today-progress-label">
                완료율 <strong>{{ completionPct }}%</strong>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: completionPct + '%' }" />
              </div>
            </div>
          </div>

          <div v-if="!selectedDayEvents.length" class="day-empty">
            <i class="fas fa-calendar-day" />
            <p>이 날은 학습 일정이 없어요</p>
            <button class="btn-add-sched" @click="openAddModal">
              <i class="fas fa-plus" /> 일정 추가
            </button>
          </div>

          <div v-else>
            <div class="activities-label">학습 활동</div>
            <div class="activities-list">
              <div
                v-for="ev in selectedDayEvents" :key="ev.id"
                class="activity-card"
                :class="{ 'activity-card--done': ev.done }"
              >
                <div class="activity-icon" :style="{ background: ev.color + '22', color: ev.color }">
                  <i :class="ev.icon" />
                </div>
                <div class="activity-info">
                  <div class="activity-name">{{ ev.title }}</div>
                  <div class="activity-time">{{ ev.time }}</div>
                </div>
                <button
                  class="activity-check"
                  :class="{ 'activity-check--done': ev.done }"
                  :style="ev.done ? { background: ev.color, borderColor: ev.color } : {}"
                  @click.stop="toggleDone(ev.id)"
                >
                  <i class="fas fa-check" />
                </button>
              </div>
            </div>

            <button class="btn-add-sched btn-add-sched--ghost" @click="openAddModal">
              <i class="fas fa-plus" /> 일정 추가
            </button>
          </div>

          <div class="track-filter-wrap">
            <button class="track-filter-toggle" @click="showTrackFilter = !showTrackFilter">
              <i class="fas fa-layer-group" />
              트랙 필터
              <i class="fas" :class="showTrackFilter ? 'fa-chevron-up' : 'fa-chevron-down'" style="margin-left:auto; font-size:10px;" />
            </button>

            <Transition name="filter-expand">
              <div v-if="showTrackFilter" class="track-filter-body">
                <button
                  class="tf-btn tf-btn--all"
                  :class="{ 'tf-off': hiddenTracks.size > 0 }"
                  @click="toggleAllTracks"
                >
                  <span class="tf-all-dots">
                    <span v-for="t in visibleTrackList.slice(0,4)" :key="t.id" class="tf-dot" :style="{ background: t.color }" />
                  </span>
                  ALL
                </button>
                <div class="tf-sep" />
                <button
                  v-for="t in visibleTrackList" :key="t.id"
                  class="tf-btn"
                  :class="{ 'tf-off': hiddenTracks.has(t.id) }"
                  @click="toggleTrack(t.id)"
                >
                  <span
                    class="tf-dot"
                    :style="{
                      background: hiddenTracks.has(t.id) ? 'transparent' : t.color,
                      borderColor: t.color,
                      boxShadow: hiddenTracks.has(t.id) ? 'none' : `0 0 5px ${t.color}55`
                    }"
                  />
                  {{ t.name }}
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </main>

    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>학습 일정 추가</h3>
              <button class="modal-close" @click="showAddModal = false"><i class="fas fa-times" /></button>
            </div>
            <div class="modal-form">
              <div class="form-row">
                <label>제목</label>
                <input v-model="newEvent.title" type="text" class="form-input" placeholder="학습 내용 입력" />
              </div>
              <div class="form-row">
                <label>날짜</label>
                <input v-model="newEvent.date" type="date" class="form-input" />
              </div>
              <div class="form-row two-col">
                <div>
                  <label>시작</label>
                  <input v-model="newEvent.start" type="time" class="form-input" />
                </div>
                <div>
                  <label>종료</label>
                  <input v-model="newEvent.end" type="time" class="form-input" />
                </div>
              </div>
              <div class="form-row">
                <label>트랙</label>
                <select v-model="newEvent.track" class="form-input">
                  <option v-for="t in calendarStore.allTracks" :key="t.id" :value="t.id">{{ t.name }}</option>
                </select>
              </div>
            </div>
            <div class="modal-actions">
              <button class="modal-btn-cancel" @click="showAddModal = false">취소</button>
              <button class="modal-btn-ok" @click="addEvent">추가</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showImportModal" class="modal-overlay" @click.self="showImportModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>일정 불러오기</h3>
              <button class="modal-close" @click="showImportModal = false"><i class="fas fa-times" /></button>
            </div>
            <p class="modal-desc">Google Calendar에서 학습 일정을 불러옵니다.</p>
            <div class="import-source-list">
              <div v-for="src in importSources" :key="src.label" class="import-source"
                :class="{ 'import-source--selected': selectedImport === src.label }"
                @click="selectedImport = src.label">
                <i :class="src.icon" :style="{ color: src.color }" />
                <span>{{ src.label }}</span>
                <i v-if="selectedImport === src.label" class="fas fa-check import-check" />
              </div>
            </div>
            <div class="modal-actions">
              <button class="modal-btn-cancel" @click="showImportModal = false">취소</button>
              <button class="modal-btn-ok" @click="doImport">불러오기</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { storeToRefs } from 'pinia'

const calendarStore = useCalendarStore()
const { schedules } = storeToRefs(calendarStore)

// ── 날짜 상태 ──
const today = new Date()
const viewYear  = ref(today.getFullYear())
const viewMonth = ref(today.getMonth()) // 0-based
const todayStr  = today.toISOString().slice(0, 10)
const selectedDay = ref(todayStr)

const monthLabel = computed(() =>
  `${viewYear.value}년 ${viewMonth.value + 1}월`
)

function prevMonth() {
  if (viewMonth.value === 0) { viewMonth.value = 11; viewYear.value-- }
  else viewMonth.value--
}
function nextMonth() {
  if (viewMonth.value === 11) { viewMonth.value = 0; viewYear.value++ }
  else viewMonth.value++
}

// ── 달력 셀 생성 ──
const calendarCells = computed(() => {
  const y = viewYear.value, m = viewMonth.value
  const firstDay = new Date(y, m, 1).getDay()
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const cells = []

  // 이전 달 채우기
  const prevDays = new Date(y, m, 0).getDate()
  for (let i = firstDay - 1; i >= 0; i--) {
    const d = prevDays - i
    const pm = m === 0 ? 12 : m
    const py = m === 0 ? y - 1 : y
    const ds = `${py}-${String(pm).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ key: `p${i}`, day: d, dateStr: ds, current: false, isToday: false, events: eventsFor(ds) })
  }
  // 이번 달
  for (let d = 1; d <= daysInMonth; d++) {
    const ds = `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ key: ds, day: d, dateStr: ds, current: true, isToday: ds === todayStr, events: eventsFor(ds) })
  }
  // 다음 달 채우기 (6주)
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    const nm = m === 11 ? 1 : m + 2
    const ny = m === 11 ? y + 1 : y
    const ds = `${ny}-${String(nm).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ key: `n${d}`, day: d, dateStr: ds, current: false, isToday: false, events: eventsFor(ds) })
  }
  return cells
})

function eventsFor(dateStr) {
  return schedules.value
    .filter(s => s.day === dateStr && !hiddenTracks.value.has(s.track))
    .map(s => {
      const track = calendarStore.getTrackById(s.track)
      return {
        id: s.id, title: s.tooltip?.title || s.text || '일정',
        time: s.tooltip?.time ? `${s.tooltip.time} - ${s.tooltip.endTime || ''}` : '',
        color: track?.color || '#818cf8',
        icon: trackIcon(s.track),
        done: s.done || false,
      }
    })
}

function trackIcon(trackId) {
  const map = { main: 'fas fa-book', algo: 'fas fa-code-branch', react: 'fas fa-code', portfolio: 'fas fa-briefcase' }
  return map[trackId] || 'fas fa-circle'
}

function selectDay(ds) { selectedDay.value = ds }

// ── 오른쪽 패널 ──
const dayPanelTitle = computed(() => {
  const d = new Date(selectedDay.value + 'T00:00:00')
  const mon = d.getMonth() + 1
  const day = d.getDate()
  const dow = ['일','월','화','수','목','금','토'][d.getDay()]
  return `${mon}월 ${day}일 (${dow})`
})

const selectedDayEvents = computed(() => eventsFor(selectedDay.value))

const completionPct = computed(() => {
  const evs = selectedDayEvents.value
  if (!evs.length) return 0
  return Math.round(evs.filter(e => e.done).length / evs.length * 100)
})

function toggleDone(id) {
  const s = schedules.value.find(x => x.id === id)
  if (s) s.done = !s.done
}

// ── 트랙 필터 ──
const showTrackFilter = ref(false)
const hiddenTracks = ref(new Set())
const visibleTrackList = computed(() => calendarStore.allTracks)

function toggleTrack(id) {
  const next = new Set(hiddenTracks.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  hiddenTracks.value = next
}
function toggleAllTracks() {
  if (hiddenTracks.value.size === 0) {
    hiddenTracks.value = new Set(calendarStore.allTracks.map(t => t.id))
  } else {
    hiddenTracks.value = new Set()
  }
}

// ── 일정 추가 ──
const showAddModal = ref(false)
const newEvent = reactive({ title: '', date: selectedDay.value, start: '09:00', end: '10:00', track: 'main' })

function openAddModal() {
  newEvent.date = selectedDay.value
  newEvent.title = ''
  newEvent.start = '09:00'
  newEvent.end = '10:00'
  showAddModal.value = true
}

function addEvent() {
  if (!newEvent.title.trim()) return
  calendarStore.createSchedule({
    day:   newEvent.date,
    track: newEvent.track,
    text:  newEvent.title,
    done:  false,
    tooltip: {
      title: newEvent.title,
      time:  newEvent.start,
      endTime: newEvent.end,
      tags: ['#학습'],
    },
  })
  showAddModal.value = false
}

// ── Google Calendar 동기화 ──
const syncing  = ref(false)
const lastSync = ref('2026.03.04 14:32')

function handleSync() {
  syncing.value = true
  setTimeout(() => {
    syncing.value = false
    const now = new Date()
    lastSync.value = `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
  }, 1800)
}

// ── 불러오기 ──
const showImportModal = ref(false)
const selectedImport  = ref('Google Calendar')
const importSources = [
  { label: 'Google Calendar', icon: 'fab fa-google', color: '#ea4335' },
  { label: 'Notion',          icon: 'fas fa-file-alt', color: '#000' },
  { label: 'CSV 파일',        icon: 'fas fa-file-csv', color: '#10b981' },
]
function doImport() {
  showImportModal.value = false
}
</script>

<style scoped>
.app-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }

/* ── 헤더 ── */
.page-header {
  height: 64px; background: var(--bg-surface); border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; /* ★ 양쪽 화면 정렬 통일 */
  flex-shrink: 0; gap: 16px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.page-title { font-size: 18px; font-weight: 800; color: var(--text-primary); white-space: nowrap; }

.gcal-badge {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: #10b981;
  background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.25);
  border-radius: 8px; padding: 4px 12px;
}
.gcal-badge i { font-size: 11px; }

.header-actions { display: flex; align-items: center; gap: 10px; }

.btn-import, .btn-sync {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 16px; border-radius: 10px; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-import {
  background: var(--bg-elevated); border: 1px solid var(--border); color: var(--text-secondary);
}
.btn-import:hover { background: var(--bg-hover); color: var(--text-primary); }
.btn-sync {
  background: #1a1a1a; border: none; color: #fff;
}
.btn-sync:hover { opacity: 0.88; }

.sync-time { font-size: 11px; color: var(--text-faint); white-space: nowrap; margin-right: 4px; }

/* ── 바디 2컬럼 ── */
.body-grid {
  flex: 1; display: grid; grid-template-columns: 1fr 320px;
  overflow: hidden;
}

/* ── 왼쪽 캘린더 패널 ── */
.cal-panel {
  padding: 24px 28px;
  overflow-y: auto;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}

.cal-top {
  display: flex; align-items: center; justify-content: center; gap: 20px;
  margin-bottom: 20px;
}
.cal-month-label { font-size: 17px; font-weight: 800; color: var(--text-primary); }
.nav-btn {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); cursor: pointer; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.nav-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

.dow-row {
  display: grid; grid-template-columns: repeat(7, 1fr);
  text-align: center; margin-bottom: 8px;
}
.dow { font-size: 11px; font-weight: 700; color: var(--text-faint); padding: 6px 0; }

.date-grid {
  display: grid; grid-template-columns: repeat(7, 1fr);
  border-radius: 14px; overflow: hidden;
  border: 1px solid var(--border);
  background: var(--bg-surface);
}
.date-cell {
  aspect-ratio: 1.15;
  padding: 8px;
  display: flex; flex-direction: column; align-items: center;
  cursor: pointer; transition: background 0.12s;
  border-right: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  position: relative;
  min-height: 64px;
}
.date-cell:nth-child(7n) { border-right: none; }
.date-cell:nth-last-child(-n+7) { border-bottom: none; }
.date-cell:hover { background: var(--bg-hover); }

.date-num {
  font-size: 13px; font-weight: 600; color: var(--text-secondary);
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.12s; flex-shrink: 0;
}
.date-cell--today .date-num {
  background: #1a1a1a; color: #fff; font-weight: 800;
}
.theme-light .date-cell--today .date-num { background: #1a1a1a; }
.date-cell--selected .date-num {
  background: rgba(129,140,248,0.2); color: #818cf8;
}
.date-cell--today.date-cell--selected .date-num {
  background: #818cf8; color: #fff;
}
.date-cell--other .date-num { color: var(--text-faint); }

.dots-row {
  display: flex; gap: 3px; justify-content: center; margin-top: 4px; flex-wrap: wrap;
}
.event-dot {
  width: 6px; height: 6px; border-radius: 50%;
  flex-shrink: 0;
}

/* ── 오른쪽 패널 ── */
.day-panel {
  border-left: 1px solid var(--border);
  background: var(--bg-surface);
  display: flex; flex-direction: column; gap: 16px;
  padding: 24px 20px;
  overflow-y: auto;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}

.day-panel-title { font-size: 17px; font-weight: 800; color: var(--text-primary); flex-shrink: 0; }

/* 오늘의 학습 진행 카드 */
.today-card {
  background: #1a1a1a; border-radius: 14px; padding: 18px 20px;
  display: flex; flex-direction: column; gap: 14px;
}
.theme-light .today-card { background: #111; }
.today-card-left { display: flex; align-items: center; gap: 12px; }
.today-icon {
  width: 40px; height: 40px; border-radius: 50%;
  background: rgba(255,255,255,0.15);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; color: #facc15; flex-shrink: 0;
}
.today-label  { font-size: 14px; font-weight: 700; color: #fff; }
.today-status { font-size: 11px; color: rgba(255,255,255,0.55); margin-top: 2px; }

.today-progress-wrap {}
.today-progress-label {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: rgba(255,255,255,0.6); margin-bottom: 8px;
}
.today-progress-label strong { color: #fff; font-size: 14px; }

.progress-bar {
  height: 8px; background: rgba(255,255,255,0.15); border-radius: 4px; overflow: hidden;
}
.progress-fill {
  height: 100%; background: #fff; border-radius: 4px; transition: width 0.6s ease;
}

/* 활동 목록 */
.activities-label {
  font-size: 12px; font-weight: 700; color: var(--text-muted); letter-spacing: 0.06em;
}
.activities-list { display: flex; flex-direction: column; gap: 10px; }

.activity-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 12px; padding: 13px 14px;
  transition: all 0.15s; cursor: default;
}
.activity-card--done { opacity: 0.55; }
.activity-card--done .activity-name { text-decoration: line-through; }

.activity-icon {
  width: 38px; height: 38px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 15px;
}
.activity-info { flex: 1; min-width: 0; }
.activity-name { font-size: 13px; font-weight: 700; color: var(--text-primary); }
.activity-time { font-size: 11px; color: var(--text-faint); margin-top: 2px; }

.activity-check {
  width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0;
  background: transparent; border: 2px solid var(--border-mid);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.15s; font-size: 11px; color: transparent;
}
.activity-check:hover { border-color: var(--text-muted); color: var(--text-faint); }
.activity-check--done { color: #fff !important; }

/* 빈 날 */
.day-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 36px 0; color: var(--text-faint); text-align: center;
}
.day-empty i { font-size: 36px; }
.day-empty p { font-size: 13px; }

.btn-add-sched {
  width: 100%; padding: 10px; border-radius: 10px; cursor: pointer;
  background: rgba(129,140,248,0.1); border: 1.5px dashed rgba(129,140,248,0.35);
  color: #818cf8; font-size: 13px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; gap: 7px;
  transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-add-sched:hover { background: rgba(129,140,248,0.18); border-style: solid; }
.btn-add-sched--ghost { margin-top: 8px; }

/* ── 트랙 필터 ── */
.track-filter-wrap {
  margin-top: auto; border-top: 1px solid var(--border); padding-top: 14px;
}
.track-filter-toggle {
  width: 100%; display: flex; align-items: center; gap: 8px;
  padding: 9px 12px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-secondary); font-size: 12px; font-weight: 700;
  cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.track-filter-toggle:hover { background: var(--bg-hover); color: var(--text-primary); }
.track-filter-toggle i:first-child { color: #818cf8; }

.track-filter-body {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding: 12px 4px 4px;
}

.tf-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 10px; border-radius: 999px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  font-size: 11px; font-weight: 600; color: var(--text-muted);
  cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.tf-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.tf-btn.tf-off { opacity: 0.38; }
.tf-btn.tf-off .tf-name { text-decoration: line-through; }

.tf-btn--all { font-weight: 800; font-size: 10px; }
.tf-all-dots { display: flex; }
.tf-sep { width: 1px; height: 16px; background: var(--border); margin: 0 2px; align-self: center; }

.tf-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 2px solid; flex-shrink: 0;
  transition: background 0.15s;
}
.tf-btn--all .tf-dot { margin-right: -2px; border-color: var(--bg-elevated); width: 7px; height: 7px; }

.filter-expand-enter-active { transition: all 0.22s ease; }
.filter-expand-leave-active { transition: all 0.15s ease; }
.filter-expand-enter-from, .filter-expand-leave-to {
  opacity: 0; transform: translateY(-6px); max-height: 0;
}

/* ── 모달 ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.45); backdrop-filter: blur(5px);
  display: flex; align-items: center; justify-content: center; padding: 24px;
}
.modal-box {
  width: 100%; max-width: 420px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 18px; padding: 28px 28px 24px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  animation: popIn 0.22s cubic-bezier(0.34,1.56,0.64,1) both;
}
@keyframes popIn { from{opacity:0;transform:scale(0.94)} to{opacity:1;transform:scale(1)} }

.modal-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.modal-header h3 { font-size: 16px; font-weight: 800; color: var(--text-primary); }
.modal-close {
  width: 28px; height: 28px; border-radius: 7px; background: var(--bg-hover);
  border: 1px solid var(--border); color: var(--text-faint); cursor: pointer; font-size: 11px;
  display: flex; align-items: center; justify-content: center;
}
.modal-desc { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; }

.modal-form { display: flex; flex-direction: column; gap: 14px; }
.form-row { display: flex; flex-direction: column; gap: 5px; }
.form-row.two-col { flex-direction: row; gap: 12px; }
.form-row.two-col > div { flex: 1; display: flex; flex-direction: column; gap: 5px; }
.form-row label { font-size: 11px; font-weight: 700; color: var(--text-muted); }
.form-input {
  padding: 10px 13px; border-radius: 9px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-primary); font-size: 13px; outline: none;
  font-family: 'Escoredream', sans-serif; transition: border-color 0.15s; width: 100%;
}
.form-input:focus { border-color: #818cf8; }

.modal-actions { display: flex; gap: 10px; margin-top: 22px; }
.modal-btn-cancel {
  flex: 1; padding: 11px; border-radius: 10px; border: 1px solid var(--border);
  background: var(--bg-elevated); color: var(--text-muted); font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.modal-btn-cancel:hover { background: var(--bg-hover); color: var(--text-primary); }
.modal-btn-ok {
  flex: 1; padding: 11px; border-radius: 10px; border: none;
  background: #1a1a1a; color: #fff; font-size: 14px; font-weight: 700;
  cursor: pointer; transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.modal-btn-ok:hover { opacity: 0.87; }

/* 불러오기 소스 */
.import-source-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 4px; }
.import-source {
  display: flex; align-items: center; gap: 12px;
  padding: 13px 14px; border-radius: 11px;
  border: 1.5px solid var(--border); background: var(--bg-elevated);
  cursor: pointer; transition: all 0.15s; font-size: 14px; font-weight: 600;
  color: var(--text-secondary);
}
.import-source:hover { border-color: rgba(129,140,248,0.4); background: var(--bg-hover); }
.import-source--selected { border-color: #818cf8; background: rgba(129,140,248,0.08); color: var(--text-primary); }
.import-check { margin-left: auto; color: #818cf8; font-size: 13px; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

/* ★ 캘린더 플로우 버튼 (학습 캘린더 버튼과 완벽하게 크기/디자인 동기화) */
.btn-flow-cal {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 16px; border-radius: 10px;
  background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.3);
  color: #818cf8; font-size: 13px; font-weight: 700;
  text-decoration: none; transition: all 0.15s; white-space: nowrap;
  font-family: 'Escoredream', sans-serif;
}
.btn-flow-cal:hover { background: rgba(129,140,248,0.2); border-color: #818cf8; }

</style>