<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
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
                <span v-for="(ev, i) in cell.events.slice(0, 3)" :key="i" class="event-dot" :style="{ background: ev.color }" />
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
                style="padding: 12px; margin-bottom: 12px; width:100%; display:flex;"
                @click.stop="toggleDone(ev.id)"
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
                  :class="{ 'check--done': ev.done }"
                  style="width: 32px; height: 32px; padding: 0;"
                >
                  <i class="fas fa-check" v-if="ev.done"/>
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
import { ref, computed, onMounted, reactive } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useThemeStore } from '@/stores/useThemeStore'
import { storeToRefs } from 'pinia'

const calendarStore = useCalendarStore()
const themeStore = useThemeStore()
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

.page-header { height: 72px; border-bottom: 1px solid var(--border); background: var(--bg-base); display: flex; align-items: center; justify-content: space-between; padding: 0 32px; flex-shrink: 0; position: sticky; top:0; z-index:90;}
.header-left { display: flex; align-items: center; gap: 16px; }
.page-title { font-size: 18px; font-weight: 900; letter-spacing: 0.05em; }
.gcal-badge { display: flex; align-items: center; gap: 8px; font-size: 10px; font-weight: 800; color: #34C759; border: 1px solid rgba(52, 199, 89, 0.2); background: rgba(52, 199, 89, 0.05); padding: 4px 12px; border-radius: 40px; }

.header-actions { display: flex; align-items: center; gap: 12px; }
.btn-import, .btn-sync, .btn-flow-cal {
  display: flex; align-items: center; gap: 8px; padding: 8px 18px; border-radius: 40px;
  font-size: 11px; font-weight: 800; cursor: pointer; transition: all 0.2s; letter-spacing: 0.05em; text-decoration: none;
}
.btn-import { background: transparent; border: 1px solid var(--border); color: var(--text-primary); }
.btn-import:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.btn-sync { background: var(--text-primary); border: 1px solid var(--text-primary); color: var(--bg-base); }
.btn-sync:hover { background: transparent; color: var(--text-primary); }
.btn-flow-cal { background: transparent; border: 1px solid var(--text-primary); color: var(--text-primary); }
.btn-flow-cal:hover { background: var(--text-primary); color: var(--bg-base); }
.sync-time { font-size: 10px; font-weight: 700; color: var(--text-muted); margin-right: 8px; }

.body-grid { flex: 1; display: grid; grid-template-columns: 1fr 360px; overflow: hidden; }

/* 달력 */
.cal-panel { padding: 40px; border-right: 1px solid var(--border); }
.cal-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; padding: 0 10px; }
.cal-month-label { font-size: 22px; font-weight: 900; letter-spacing: 0.05em; }
.nav-btn { width: 32px; height: 32px; border-radius: 0; background: transparent; border: 1px solid var(--border); cursor: pointer; font-size: 12px; color: var(--text-primary); transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.nav-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.dow-row { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 12px; }
.dow { font-size: 11px; font-weight: 800; color: var(--text-muted); padding: 8px 0; letter-spacing: 0.1em; }

.date-grid { display: grid; grid-template-columns: repeat(7, 1fr); border: 1px solid var(--border); background: var(--bg-base); }
.date-cell { aspect-ratio: 1.1; padding: 12px; display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: all 0.2s; border-right: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.date-cell:nth-child(7n) { border-right: none; }
.date-cell:nth-last-child(-n+7) { border-bottom: none; }
.date-cell:hover { background: var(--bg-hover); }

.date-num { font-size: 13px; font-weight: 800; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.date-cell--today .date-num { background: var(--text-primary); color: var(--bg-base); }
.date-cell--selected .date-num { outline: 1px solid var(--text-primary); outline-offset: 2px; }
.date-cell--other .date-num { color: var(--text-faint); opacity: 0.5; }

.dots-row { display: flex; gap: 4px; justify-content: center; margin-top: 8px; }
.event-dot { width: 5px; height: 5px; border-radius: 50%; }

/* 오른쪽 패널 */
.day-panel { display: flex; flex-direction: column; gap: 32px; padding: 40px 24px; background: var(--bg-surface); overflow-y: auto; }
.day-panel-title { font-size: 18px; font-weight: 900; letter-spacing: 0.05em; border-bottom: 1px solid var(--border); padding-bottom: 16px; }

.today-card { background: var(--bg-base); border: 1px solid var(--border); padding: 24px; display: flex; flex-direction: column; gap: 20px; }
.today-card-left { display: flex; align-items: center; gap: 16px; }
.today-icon { width: 48px; height: 48px; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 18px; color: var(--text-primary); }
.today-label { font-size: 14px; font-weight: 900; letter-spacing: 0.05em; }
.today-status { font-size: 11px; font-weight: 700; color: var(--text-muted); margin-top: 2px; }

.today-progress-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 800; margin-bottom: 8px; }
.today-progress-label strong { font-size: 14px; color: var(--accent); }
.progress-bar { height: 2px; background: var(--border); overflow: hidden; }
.progress-fill { height: 100%; background: var(--text-primary); transition: width 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); }

.activities-label { font-size: 13px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-muted); margin-bottom: 16px; }
.activities-list { display: flex; flex-direction: column; gap: 12px; }
.activity-card { display: flex; align-items: center; gap: 14px; background: var(--bg-base); border: 1px solid var(--border); padding: 16px; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); }
.activity-card:hover { border-color: var(--text-primary); transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.activity-card--done { opacity: 0.5; filter: grayscale(1); }
.activity-icon { width: 40px; height: 40px; border: 1px solid rgba(0,0,0,0.1); display: flex; align-items: center; justify-content: center; font-size: 14px; }
.activity-name { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.activity-time { font-size: 11px; font-weight: 700; color: var(--text-muted); margin-top: 4px; font-family: 'Inter', sans-serif; }
.activity-check { width: 28px; height: 28px; border: 1px solid var(--border); background: transparent; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; color: transparent; padding: 0; flex-shrink: 0; }
.activity-check:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.activity-check.check--done { background: var(--text-primary); color: white !important; border-color: var(--text-primary); }

.day-empty { padding: 60px 0; text-align: center; color: var(--text-muted); }
.day-empty i { font-size: 48px; margin-bottom: 16px; opacity: 0.2; }
.day-empty p { font-size: 14px; font-weight: 800; letter-spacing: 0.05em; margin-bottom: 24px; }

.btn-add-sched { width: 100%; padding: 16px; border: 1px dashed var(--border); background: transparent; color: var(--text-muted); font-size: 12px; font-weight: 900; cursor: pointer; transition: all 0.2s; letter-spacing: 0.1em; }
.btn-add-sched:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); border-style: solid; }

/* 트랙 필터 */
.track-filter-wrap { margin-top: auto; border-top: 1px solid var(--border); padding-top: 24px; }
.track-filter-toggle { width: 100%; display: flex; align-items: center; gap: 8px; padding: 14px; border: 1px solid var(--border); background: var(--bg-base); color: var(--text-primary); font-size: 11px; font-weight: 900; cursor: pointer; transition: all 0.2s; letter-spacing: 0.05em; }
.track-filter-toggle:hover { border-color: var(--text-primary); transform: translateY(-1px); }

.track-filter-body { display: flex; flex-wrap: wrap; gap: 8px; padding: 16px 0 0; }
.tf-btn { display: inline-flex; align-items: center; gap: 8px; padding: 6px 14px; border: 1px solid var(--border); border-radius: 40px; background: var(--bg-base); font-size: 10px; font-weight: 800; color: var(--text-primary); cursor: pointer; text-transform: uppercase; transition: all 0.2s; }
.tf-btn:hover { border-color: var(--text-primary); transform: translateY(-1px); }
.tf-btn.tf-off { opacity: 0.3; filter: grayscale(1); }
.tf-dot { width: 6px; height: 6px; border-radius: 50%; }

/* 모달 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); backdrop-filter: blur(8px); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal-box { width: 440px; background: var(--bg-base); border: 1px solid var(--border); padding: 40px; }
.modal-header { display: flex; justify-content: space-between; margin-bottom: 32px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
.modal-header h3 { font-size: 18px; font-weight: 900; letter-spacing: 0.05em; }
.modal-close { background: transparent; border: none; font-size: 18px; color: var(--text-muted); cursor: pointer; transition: color 0.2s; }
.modal-close:hover { color: var(--text-primary); }
.modal-form { display: flex; flex-direction: column; gap: 20px; }
.form-row label { font-size: 11px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-muted); display: block; margin-bottom: 8px; text-transform: uppercase; }
.form-input { width: 100%; border: 1px solid var(--border); background: var(--bg-surface); color: var(--text-primary); padding: 14px; font-size: 14px; font-weight: 700; outline: none; transition: all 0.2s; }
.form-input:focus { border-color: var(--text-primary); background: var(--bg-base); box-shadow: 0 0 0 4px rgba(0,0,0,0.05); }
.modal-actions { display: flex; gap: 12px; margin-top: 40px; }
.modal-btn-cancel, .modal-btn-ok { flex: 1; padding: 16px; font-size: 14px; font-weight: 900; cursor: pointer; transition: all 0.2s; border-radius: 40px; letter-spacing: 0.05em; }
.modal-btn-cancel { background: transparent; border: 1px solid var(--border); color: var(--text-muted); }
.modal-btn-cancel:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }
.modal-btn-ok { background: var(--text-primary); color: var(--bg-base); border: 1px solid var(--text-primary); }
.modal-btn-ok:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.import-source { border: 1px solid var(--border); padding: 20px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 16px; transition: all 0.2s; background: var(--bg-base); }
.import-source--selected { border-color: var(--text-primary); background: var(--bg-hover); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
</style>