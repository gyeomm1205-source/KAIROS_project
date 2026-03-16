<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">

      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">STUDY LOG</h1>
          <div class="gcal-badge">
            <i class="fas fa-check" />
            GOOGLE CALENDAR CONNECTED
          </div>
        </div>
        <div class="header-actions">
          <div v-if="lastSync" class="sync-time">LAST SYNC: {{ lastSync }}</div>
          <button class="btn-import" @click="showImportModal = true">
            <i class="fas fa-download" /> IMPORT
          </button>
          <button class="btn-sync" @click="handleSync">
            <i :class="syncing ? 'fas fa-spinner fa-spin' : 'fas fa-sync-alt'" />
            SYNC NOW
          </button>

          <RouterLink to="/calendar" class="btn-flow-cal">
            <i class="fas fa-code-branch" />
            FLOW CALENDAR
          </RouterLink>
        </div>
      </div>

      <div class="body-grid">

        <div class="cal-panel custom-scroll">
          <div class="cal-top">
            <button class="nav-btn" @click="prevMonth"><i class="fas fa-chevron-left"/></button>
            <span class="cal-month-label">{{ monthLabel }}</span>
            <button class="nav-btn" @click="nextMonth"><i class="fas fa-chevron-right"/></button>
          </div>

          <div class="dow-row">
            <span v-for="d in ['SUN','MON','TUE','WED','THU','FRI','SAT']" :key="d" class="dow">{{ d }}</span>
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

        <div class="day-panel custom-scroll">
          <div class="day-panel-title">{{ dayPanelTitle }}</div>

          <div class="today-card" v-if="selectedDayEvents.length">
            <div class="today-card-left">
              <div class="today-icon"><i class="fas fa-star" /></div>
              <div>
                <div class="today-label">TODAY'S MISSION</div>
                <div class="today-status">IN PROGRESS</div>
              </div>
            </div>
            <div class="today-progress-wrap">
              <div class="today-progress-label">
                COMPLETION <strong>{{ completionPct }}%</strong>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: completionPct + '%' }" />
              </div>
            </div>
          </div>

          <div v-if="!selectedDayEvents.length" class="day-empty">
            <i class="fas fa-calendar-times" />
            <p>NO SCHEDULES FOUND</p>
            <button class="btn-add-sched" @click="openAddModal">
              <i class="fas fa-plus" /> ADD SCHEDULE
            </button>
          </div>

          <div v-else>
            <div class="activities-label">ACTIVITIES</div>
            <div class="activities-list">
              <div
                v-for="ev in selectedDayEvents" :key="ev.id"
                class="activity-card"
                :class="{ 'activity-card--done': ev.done }"
              >
                <div class="activity-icon" :style="{ color: ev.color, borderColor: ev.color }">
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
              <i class="fas fa-plus" /> ADD SCHEDULE
            </button>
          </div>

          <div class="track-filter-wrap">
            <button class="track-filter-toggle" @click="showTrackFilter = !showTrackFilter">
              <i class="fas fa-filter" />
              TRACK FILTER
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
                    :style="{ background: hiddenTracks.has(t.id) ? 'transparent' : t.color, borderColor: t.color }"
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
              <h3>ADD SCHEDULE</h3>
              <button class="modal-close" @click="showAddModal = false"><i class="fas fa-times" /></button>
            </div>
            <div class="modal-form">
              <div class="form-row">
                <label>TITLE</label>
                <input v-model="newEvent.title" type="text" class="form-input" placeholder="Enter study topic" />
              </div>
              <div class="form-row">
                <label>DATE</label>
                <input v-model="newEvent.date" type="date" class="form-input" />
              </div>
              <div class="form-row two-col">
                <div>
                  <label>START</label>
                  <input v-model="newEvent.start" type="time" class="form-input" />
                </div>
                <div>
                  <label>END</label>
                  <input v-model="newEvent.end" type="time" class="form-input" />
                </div>
              </div>
              <div class="form-row">
                <label>TRACK</label>
                <select v-model="newEvent.track" class="form-input">
                  <option v-for="t in calendarStore.allTracks" :key="t.id" :value="t.id">{{ t.name }}</option>
                </select>
              </div>
            </div>
            <div class="modal-actions">
              <button class="modal-btn-cancel" @click="showAddModal = false">CANCEL</button>
              <button class="modal-btn-ok" @click="addEvent">ADD</button>
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
              <h3>IMPORT DATA</h3>
              <button class="modal-close" @click="showImportModal = false"><i class="fas fa-times" /></button>
            </div>
            <p class="modal-desc">Google Calendar 등 외부 소스에서 일정을 가져옵니다.</p>
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
              <button class="modal-btn-cancel" @click="showImportModal = false">CANCEL</button>
              <button class="modal-btn-ok" @click="doImport">IMPORT</button>
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

const today = new Date()
const viewYear  = ref(today.getFullYear())
const viewMonth = ref(today.getMonth())
const todayStr  = today.toISOString().slice(0, 10)
const selectedDay = ref(todayStr)

const monthLabel = computed(() => `${viewYear.value}.${String(viewMonth.value + 1).padStart(2,'0')}`)

function prevMonth() { if (viewMonth.value === 0) { viewMonth.value = 11; viewYear.value-- } else viewMonth.value-- }
function nextMonth() { if (viewMonth.value === 11) { viewMonth.value = 0; viewYear.value++ } else viewMonth.value++ }

const calendarCells = computed(() => {
  const y = viewYear.value, m = viewMonth.value
  const firstDay = new Date(y, m, 1).getDay()
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const cells = []

  const prevDays = new Date(y, m, 0).getDate()
  for (let i = firstDay - 1; i >= 0; i--) {
    const d = prevDays - i
    const pm = m === 0 ? 12 : m; const py = m === 0 ? y - 1 : y
    const ds = `${py}-${String(pm).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ key: `p${i}`, day: d, dateStr: ds, current: false, isToday: false, events: eventsFor(ds) })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const ds = `${y}-${String(m+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    cells.push({ key: ds, day: d, dateStr: ds, current: true, isToday: ds === todayStr, events: eventsFor(ds) })
  }
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    const nm = m === 11 ? 1 : m + 2; const ny = m === 11 ? y + 1 : y
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
        color: track?.color || '#818cf8', icon: trackIcon(s.track), done: s.done || false,
      }
    })
}

function trackIcon(trackId) { const map = { main: 'fas fa-book', algo: 'fas fa-code-branch', react: 'fas fa-code', portfolio: 'fas fa-briefcase' }; return map[trackId] || 'fas fa-stop' }
function selectDay(ds) { selectedDay.value = ds }

const dayPanelTitle = computed(() => {
  const d = new Date(selectedDay.value + 'T00:00:00')
  return `${d.getMonth() + 1}/${d.getDate()} ${['SUN','MON','TUE','WED','THU','FRI','SAT'][d.getDay()]}`
})

const selectedDayEvents = computed(() => eventsFor(selectedDay.value))
const completionPct = computed(() => { const evs = selectedDayEvents.value; if (!evs.length) return 0; return Math.round(evs.filter(e => e.done).length / evs.length * 100) })

function toggleDone(id) { const s = schedules.value.find(x => x.id === id); if (s) s.done = !s.done }

const showTrackFilter = ref(false)
const hiddenTracks = ref(new Set())
const visibleTrackList = computed(() => calendarStore.allTracks)

function toggleTrack(id) { const next = new Set(hiddenTracks.value); if (next.has(id)) next.delete(id); else next.add(id); hiddenTracks.value = next }
function toggleAllTracks() { if (hiddenTracks.value.size === 0) hiddenTracks.value = new Set(calendarStore.allTracks.map(t => t.id)); else hiddenTracks.value = new Set() }

const showAddModal = ref(false)
const newEvent = reactive({ title: '', date: selectedDay.value, start: '09:00', end: '10:00', track: 'main' })

function openAddModal() { newEvent.date = selectedDay.value; newEvent.title = ''; newEvent.start = '09:00'; newEvent.end = '10:00'; showAddModal.value = true }
function addEvent() {
  if (!newEvent.title.trim()) return
  calendarStore.createSchedule({ day: newEvent.date, track: newEvent.track, text: newEvent.title, done: false, tooltip: { title: newEvent.title, time: newEvent.start, endTime: newEvent.end, tags: ['#학습'] }})
  showAddModal.value = false
}

const syncing  = ref(false)
const lastSync = ref('2026.03.04 14:32')
function handleSync() { syncing.value = true; setTimeout(() => { syncing.value = false; const now = new Date(); lastSync.value = `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}` }, 1800) }

const showImportModal = ref(false)
const selectedImport  = ref('Google Calendar')
const importSources = [ { label: 'Google Calendar', icon: 'fab fa-google', color: '#ea4335' }, { label: 'Notion', icon: 'fas fa-file-alt', color: '#000' }, { label: 'CSV File', icon: 'fas fa-file-csv', color: '#10b981' } ]
function doImport() { showImportModal.value = false }
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); color: var(--text-primary); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ★ 스크롤바 완전히 숨김 (기능은 유지) */
.custom-scroll {
  overflow-y: auto;
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
.custom-scroll::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.page-header {
  height: 64px; background: var(--bg-surface); border-bottom: 2px solid var(--border);
  display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.page-title { font-size: 20px; font-weight: 900; letter-spacing: 0.05em; }
.gcal-badge { display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 800; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 4px 10px; border-radius: 0; }

.header-actions { display: flex; align-items: center; gap: 8px; }
.btn-import, .btn-sync, .btn-flow-cal {
  display: flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 0;
  font-size: 12px; font-weight: 800; cursor: pointer; transition: 0.1s; letter-spacing: 0.05em; text-decoration: none;
}
.btn-import { background: transparent; border: 1px solid var(--border); color: var(--text-primary); }
.btn-import:hover { border-color: var(--text-primary); }
.btn-sync { background: var(--text-primary); border: 1px solid var(--text-primary); color: var(--bg-base); }
.btn-sync:hover { background: transparent; color: var(--text-primary); }
.btn-flow-cal { background: transparent; border: 2px solid var(--text-primary); color: var(--text-primary); box-shadow: 2px 2px 0 var(--text-primary); }
.btn-flow-cal:hover { transform: translate(1px, 1px); box-shadow: 0 0 0 var(--text-primary); }
.sync-time { font-size: 10px; font-weight: 700; color: var(--text-muted); margin-right: 8px; }

.body-grid { flex: 1; display: grid; grid-template-columns: 1fr 340px; overflow: hidden; }

/* 달력 */
.cal-panel { padding: 32px; border-right: 2px solid var(--border); }
.cal-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding: 0 10px; }
.cal-month-label { font-size: 24px; font-weight: 900; letter-spacing: 0.1em; }
.nav-btn { width: 36px; height: 36px; border-radius: 0; background: transparent; border: 2px solid var(--border); cursor: pointer; font-size: 14px; color: var(--text-primary); transition: 0.1s; }
.nav-btn:hover { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); }

.dow-row { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8px; }
.dow { font-size: 12px; font-weight: 800; color: var(--text-muted); padding: 8px 0; letter-spacing: 0.1em; }

.date-grid { display: grid; grid-template-columns: repeat(7, 1fr); border: 2px solid var(--text-primary); background: var(--bg-surface); }
.date-cell { aspect-ratio: 1.1; padding: 8px; display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: 0.1s; border-right: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.date-cell:nth-child(7n) { border-right: none; }
.date-cell:nth-last-child(-n+7) { border-bottom: none; }
.date-cell:hover { background: var(--bg-hover); }

.date-num { font-size: 14px; font-weight: 800; width: 28px; height: 28px; border-radius: 0; display: flex; align-items: center; justify-content: center; }
.date-cell--today .date-num { background: var(--text-primary); color: var(--bg-base); }
.date-cell--selected .date-num { outline: 2px solid var(--text-primary); }
.date-cell--other .date-num { color: var(--border-mid); }

.dots-row { display: flex; gap: 4px; justify-content: center; margin-top: 8px; }
.event-dot { width: 8px; height: 8px; border-radius: 0; border: 1px solid rgba(0,0,0,0.1); }

/* 오른쪽 패널 */
.day-panel { display: flex; flex-direction: column; gap: 24px; padding: 32px 24px; background: var(--bg-surface); }
.day-panel-title { font-size: 20px; font-weight: 900; letter-spacing: 0.1em; border-bottom: 2px solid var(--text-primary); padding-bottom: 12px; }

.today-card { background: transparent; border: 2px solid var(--text-primary); border-radius: 0; padding: 20px; box-shadow: 6px 6px 0 var(--text-primary); display: flex; flex-direction: column; gap: 16px; }
.today-card-left { display: flex; align-items: center; gap: 12px; }
.today-icon { width: 44px; height: 44px; border: 2px solid var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 18px; color: var(--text-primary); }
.today-label { font-size: 16px; font-weight: 900; letter-spacing: 0.05em; }
.today-status { font-size: 11px; font-weight: 700; color: var(--text-muted); margin-top: 2px; }

.today-progress-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 800; margin-bottom: 8px; }
.today-progress-label strong { font-size: 14px; }
.progress-bar { height: 12px; border: 1px solid var(--text-primary); background: transparent; }
.progress-fill { height: 100%; background: var(--text-primary); transition: width 0.3s; }

.activities-label { font-size: 14px; font-weight: 900; letter-spacing: 0.1em; margin-bottom: 12px; }
.activities-list { display: flex; flex-direction: column; gap: 12px; }
.activity-card { display: flex; align-items: center; gap: 14px; background: transparent; border: 2px solid var(--border); padding: 14px; transition: 0.1s; }
.activity-card:hover { border-color: var(--text-primary); transform: translateX(2px); }
.activity-card--done { opacity: 0.4; border-style: dashed; }
.activity-icon { width: 36px; height: 36px; border: 2px solid; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.activity-name { font-size: 14px; font-weight: 800; }
.activity-time { font-size: 11px; font-weight: 700; color: var(--text-muted); margin-top: 4px; font-family: monospace; }
.activity-check { width: 28px; height: 28px; border: 2px solid var(--text-primary); background: transparent; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.1s; color: transparent; border-radius: 0; }
.activity-check:hover { background: var(--bg-hover); }
.activity-check--done { color: var(--bg-base) !important; border-color: var(--text-primary) !important; }

.day-empty { padding: 40px 0; text-align: center; color: var(--text-muted); }
.day-empty i { font-size: 40px; margin-bottom: 12px; }
.day-empty p { font-size: 14px; font-weight: 800; letter-spacing: 0.05em; }

.btn-add-sched { width: 100%; padding: 14px; border: 2px dashed var(--text-primary); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 900; cursor: pointer; transition: 0.1s; letter-spacing: 0.1em; margin-top: 12px; }
.btn-add-sched:hover { background: var(--text-primary); color: var(--bg-base); border-style: solid; }

/* 트랙 필터 */
.track-filter-wrap { margin-top: auto; border-top: 2px solid var(--border); padding-top: 16px; }
.track-filter-toggle { width: 100%; display: flex; align-items: center; gap: 8px; padding: 12px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 12px; font-weight: 900; cursor: pointer; transition: 0.1s; letter-spacing: 0.05em; }
.track-filter-toggle:hover { border-color: var(--text-primary); }

.track-filter-body { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 0 0; }
.tf-btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border: 1px solid var(--border); background: transparent; font-size: 11px; font-weight: 800; color: var(--text-primary); cursor: pointer; text-transform: uppercase; }
.tf-btn:hover { border-color: var(--text-primary); }
.tf-btn.tf-off { opacity: 0.3; text-decoration: line-through; }
.tf-dot { width: 10px; height: 10px; border: 2px solid; }

/* 모달 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal-box { width: 440px; background: var(--bg-base); border: 3px solid var(--text-primary); padding: 32px; box-shadow: 12px 12px 0 var(--text-primary); }
.modal-header { display: flex; justify-content: space-between; margin-bottom: 24px; border-bottom: 2px solid var(--text-primary); padding-bottom: 12px; }
.modal-header h3 { font-size: 20px; font-weight: 900; letter-spacing: 0.05em; }
.modal-close { background: transparent; border: none; font-size: 20px; color: var(--text-primary); cursor: pointer; }
.modal-form { display: flex; flex-direction: column; gap: 16px; }
.form-row label { font-size: 12px; font-weight: 800; letter-spacing: 0.1em; display: block; margin-bottom: 6px; }
.form-input { width: 100%; border: 2px solid var(--border); background: transparent; color: var(--text-primary); padding: 12px; font-size: 14px; font-weight: 700; outline: none; }
.form-input:focus { border-color: var(--text-primary); box-shadow: 4px 4px 0 var(--border); }
.modal-actions { display: flex; gap: 12px; margin-top: 32px; }
.modal-btn-cancel, .modal-btn-ok { flex: 1; padding: 14px; font-size: 14px; font-weight: 900; cursor: pointer; transition: 0.1s; border: 2px solid var(--text-primary); letter-spacing: 0.1em; }
.modal-btn-cancel { background: transparent; color: var(--text-primary); }
.modal-btn-cancel:hover { box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); }
.modal-btn-ok { background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 var(--text-primary); }
.modal-btn-ok:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--text-primary); }

.import-source { border: 2px solid var(--border); padding: 16px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: 0.1s; }
.import-source--selected { border-color: var(--text-primary); box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); }
</style>