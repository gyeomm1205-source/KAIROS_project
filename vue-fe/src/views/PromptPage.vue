<template>
  <div class="app-layout" @click="handleBackdropClick">
    <AppSidebar />

    <main class="main-content">
      <header class="calendar-header">
        <div class="header-left">
          <div class="date-nav" ref="dateWrapRef">
            <h2 class="date-text" @click="toggleDatePopover">
              {{ headerDateText }}
              <i class="fas" :class="isPopoverOpen ? 'fa-caret-up' : 'fa-caret-down'" style="margin-left: 4px; font-size: 14px;" />
            </h2>

            <Transition name="fade-pop">
              <div v-if="isPopoverOpen" class="custom-date-popover">
                <div class="popover-row">
                  <div class="custom-sel-wrap">
                    <div class="custom-sel-display" @click.stop="toggleSelect('year')" :class="{ active: activeSelect === 'year' }">
                      {{ selYear }}년 <i class="fas fa-chevron-down sel-icon"></i>
                    </div>
                    <Transition name="drop-anim">
                      <ul v-if="activeSelect === 'year'" class="custom-sel-list custom-scroll">
                        <li v-for="y in yearOptions" :key="y" class="custom-sel-item" :class="{ selected: selYear === y }" @click.stop="pickYear(y)">{{ y }}년</li>
                      </ul>
                    </Transition>
                  </div>
                  <div class="custom-sel-wrap">
                    <div class="custom-sel-display" @click.stop="toggleSelect('month')" :class="{ active: activeSelect === 'month' }">
                      {{ selMonth }}월 <i class="fas fa-chevron-down sel-icon"></i>
                    </div>
                    <Transition name="drop-anim">
                      <ul v-if="activeSelect === 'month'" class="custom-sel-list custom-scroll">
                        <li v-for="m in 12" :key="m" class="custom-sel-item" :class="{ selected: selMonth === m }" @click.stop="pickMonth(m)">{{ m }}월</li>
                      </ul>
                    </Transition>
                  </div>
                </div>
                <button class="btn-pop-confirm" @click="applyCustomDate">JUMP TO DATE</button>
              </div>
            </Transition>

            <div class="nav-controls">
              <button class="nav-btn" @click="navigate('prev')"><i class="fas fa-chevron-left" /></button>
              <button class="nav-btn nav-btn--text" @click="navigate('today')">TODAY</button>
              <button class="nav-btn" @click="navigate('next')"><i class="fas fa-chevron-right" /></button>
            </div>
          </div>

          <div class="header-title-sep" />
          <h1 class="page-title-label"><i class="fas fa-terminal" /> AI ASSISTANT</h1>
        </div>

        <div class="header-right">
          <div class="prompt-filters">
            <button class="p-filter-btn" :class="{ active: promptFilter === 'all' }" @click="setPromptFilter('all')">ALL</button>
            <button class="p-filter-btn" :class="{ active: promptFilter === 'prompt' }" @click="setPromptFilter('prompt')">
              <span class="p-dot" :style="{ background: headerPromptColor }"></span> PROMPT
            </button>
            <button class="p-filter-btn" :class="{ active: promptFilter === 'blog' }" @click="setPromptFilter('blog')">
              <span class="p-dot" :style="{ background: headerBlogColor }"></span> BLOG
            </button>
          </div>
        </div>
      </header>

      <div class="prompt-view-layout">
         <div class="prompt-graph-panel">
          <div class="prompt-graph-title">
            <div class="ptl-header-tracks custom-scroll">
              <span v-for="track in promptTracks" :key="track.id" v-show="!hiddenTracks.has(track.id)" class="ptl-header-track-badge" :style="{ color: track.color, borderColor: track.color }">
                <span class="ptl-head-dot" :style="{ background: track.color }" /> {{ track.name }}
              </span>
            </div>
          </div>

          <div class="prompt-graph-inner custom-scroll snap-container">
            <div v-if="!allPromptSchedules.length" class="prompt-tl-empty">
              <div class="empty-icon-wrap">
                <i class="fas fa-box-open" />
              </div>
              <p class="empty-title">NO RECORDS FOUND</p>
              <p class="empty-desc">선택한 월에 해당하는 AI 기록이 없습니다.</p>
            </div>

            <div v-else class="v-graph-timeline">
              <div class="v-graph-spines-container">
                <div v-for="track in promptTracks" :key="`bg-spine-${track.id}`" class="v-graph-spine-line" v-show="!hiddenTracks.has(track.id)">
                  <div class="v-graph-spine-inner" :style="{ borderLeftColor: track.color }"></div>
                </div>
              </div>

              <template v-for="group in groupedPromptSchedules" :key="group.day">
                <div class="snap-item-wrap">
                  <div class="v-graph-date-row" :id="`prompt-day-${group.day}`">
                    <div class="v-graph-date-badge">{{ formatShortDate(group.day) }} {{ getDayOfWeek(group.day) }}</div>
                  </div>

                  <div v-if="!group.schedules.length" class="v-graph-empty-day"></div>

                  <div v-for="s in group.schedules" :key="s.id" class="v-graph-row" :class="{ 'is-selected': selectedPromptId === s.id }" @click="selectPromptSchedule(s)">
                    <div class="v-graph-time">{{ s.tooltip?.time || '—' }}</div>
                    <div class="v-graph-lanes">
                      <div v-for="track in promptTracks" :key="track.id" class="v-graph-lane" v-show="!hiddenTracks.has(track.id)">
                        <div v-if="s.track === track.id" class="v-graph-node" :style="{ background: track.color }"></div>
                      </div>
                    </div>
                    <div class="v-graph-content">
                      <div class="v-graph-card" :style="{ borderColor: getPromptTrackColor(s.track) }">
                        <div class="v-graph-card-header">
                          <span class="v-graph-track-name" :style="{ color: getPromptTrackColor(s.track) }">{{ getPromptTrackName(s.track) }}</span>
                        </div>
                        <div class="v-graph-title">{{ s.tooltip?.title || s.text }}</div>
                      </div>
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
                <span class="prompt-schedule-dot" :style="{ background: getPromptTrackColor(selectedPromptSchedule.track) }"/>
                <div>
                  <div class="prompt-chat-title">{{ selectedPromptSchedule.tooltip?.title || selectedPromptSchedule.text }}</div>
                  <div class="prompt-chat-subtitle">{{ formatShortDate(selectedPromptSchedule.day) }} · {{ selectedPromptSchedule.tooltip?.time || '' }}</div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="prompt-welcome-info-placeholder">
                <div class="prompt-welcome-icon-sm"><i class="fas fa-terminal" /></div>
                <div class="prompt-chat-title">SELECT A RECORD TO VIEW CHAT</div>
              </div>
            </template>
          </div>

          <div class="prompt-messages-area custom-scroll" ref="promptMessagesRef">
            <template v-if="selectedPromptSchedule">
              <div v-for="msg in currentMessages" :key="msg.id" class="prompt-msg" :class="msg.role === 'user' ? 'prompt-msg--user' : 'prompt-msg--ai'">
                <div class="prompt-msg-bubble">{{ msg.content }}</div>
                <div class="prompt-msg-time">{{ msg.time }}</div>
              </div>
            </template>
          </div>

          <div class="prompt-input-area">
            <textarea v-model="promptInput" class="prompt-textarea custom-scroll" placeholder="SEND MESSAGE..." :disabled="!selectedPromptSchedule" rows="3" @keydown.enter.exact.prevent="sendPrompt" />
            <button class="prompt-send-btn" @click="sendPrompt" :disabled="!promptInput.trim() || !selectedPromptSchedule">
              SEND
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const store = useCalendarStore()
const { schedules, allTracks } = storeToRefs(store)

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)

const isPopoverOpen = ref(false)
const dateWrapRef = ref(null)
const activeSelect = ref(null)
const selYear = ref(today.getFullYear())
const selMonth = ref(today.getMonth() + 1)

const headerDateText = computed(() => `${currentYear.value}.${String(currentMonth.value).padStart(2,'0')}`)
const yearOptions = computed(() => { const cur = new Date().getFullYear(); return Array.from({ length: 11 }, (_, i) => cur - 5 + i) })

function toggleDatePopover() { isPopoverOpen.value = !isPopoverOpen.value; if (isPopoverOpen.value) { selYear.value = currentYear.value; selMonth.value = currentMonth.value; activeSelect.value = null } }
function toggleSelect(type) { activeSelect.value = activeSelect.value === type ? null : type }
function pickYear(y) { selYear.value = y; activeSelect.value = null; }
function pickMonth(m) { selMonth.value = m; activeSelect.value = null; }
function applyCustomDate() { currentYear.value = selYear.value; currentMonth.value = selMonth.value; isPopoverOpen.value = false }
function handleBackdropClick(e) { if (isPopoverOpen.value && dateWrapRef.value && !dateWrapRef.value.contains(e.target)) isPopoverOpen.value = false }

function navigate(dir) {
  if (dir === 'prev') { if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value-- } else currentMonth.value-- }
  else if (dir === 'next') { if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++ } else currentMonth.value++ }
  else { currentYear.value = today.getFullYear(); currentMonth.value = today.getMonth() + 1 }
}

const promptFilter = ref('all')
const hiddenTracks = ref(new Set())
const promptTrack = computed(() => allTracks.value.find(t => t.id?.toLowerCase().includes('prompt') || t.name?.toLowerCase().includes('prompt')))
const blogTrack = computed(() => allTracks.value.find(t => t.id?.toLowerCase().includes('blog') || t.name?.toLowerCase().includes('blog')))
const headerPromptColor = computed(() => promptTrack.value?.color || '#facc15')
const headerBlogColor = computed(() => blogTrack.value?.color || '#10b981')

function setPromptFilter(type) {
  promptFilter.value = type; hiddenTracks.value = new Set();
  if (type === 'prompt' && blogTrack.value) hiddenTracks.value.add(blogTrack.value.id)
  if (type === 'blog' && promptTrack.value) hiddenTracks.value.add(promptTrack.value.id)
}

const PROMPT_TRACK_IDS = ['prompt', 'blog']
const promptTracks = computed(() => allTracks.value.filter(t => PROMPT_TRACK_IDS.some(id => t.id?.toLowerCase().includes(id) || t.name?.toLowerCase().includes(id))).sort((a, b) => a.index - b.index))

const allPromptSchedules = computed(() => {
  const ids = promptTracks.value.map(t => t.id)
  const targetPrefix = `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}`
  return schedules.value.filter(s => {
    if (!ids.includes(s.track) || hiddenTracks.value.has(s.track)) return false
    return s.day.startsWith(targetPrefix)
  }).sort((a, b) => (a.day + (a.tooltip?.time || '99:99')).localeCompare(b.day + (b.tooltip?.time || '99:99')))
})

function formatShortDate(str) { const [, m, d] = str.split('-'); return `${parseInt(m)}/${parseInt(d)}` }
function getDayOfWeek(str) { return ['일', '월', '화', '수', '목', '금', '토'][new Date(str).getDay()] }

const groupedPromptSchedules = computed(() => {
  const map = new Map()
  const lastDay = new Date(currentYear.value, currentMonth.value, 0).getDate()
  for (let d = 1; d <= lastDay; d++) { const ds = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`; map.set(ds, []) }
  allPromptSchedules.value.forEach(s => { if (map.has(s.day)) map.get(s.day).push(s) })
  return [...map.entries()].map(([day, schedules]) => ({ day, schedules }))
})

const promptInput = ref('')
const promptMessagesRef = ref(null)
const selectedPromptId = ref(null)
const selectedPromptSchedule = computed(() => selectedPromptId.value ? schedules.value.find(s => s.id === selectedPromptId.value) || null : null)
const promptMessagesMap = ref({})
const currentMessages = computed(() => selectedPromptId.value ? promptMessagesMap.value[selectedPromptId.value] || [] : [])

function selectPromptSchedule(s) { selectedPromptId.value = s.id }
function getPromptTrackColor(id) { return store.getTrackById(id)?.color || 'var(--text-primary)' }
function getPromptTrackName(id) { return store.getTrackById(id)?.name || id }

function sendPrompt() {
  if (!promptInput.value.trim() || !selectedPromptId.value) return
  const sid = selectedPromptId.value
  if (!promptMessagesMap.value[sid]) promptMessagesMap.value[sid] = []
  const now = new Date()
  const time = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
  promptMessagesMap.value[sid].push({ id: Date.now(), role: 'user', content: promptInput.value.trim(), time })
  promptInput.value = ''
  nextTick(() => { if (promptMessagesRef.value) promptMessagesRef.value.scrollTop = promptMessagesRef.value.scrollHeight })
}
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', sans-serif; color: var(--text-primary); }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ★ 스크롤바 완전히 숨김 */
.custom-scroll { overflow-y: auto; -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.calendar-header { height: 64px; border-bottom: 2px solid var(--border); background: var(--bg-surface); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0; z-index: 30; }
.header-left { display: flex; align-items: center; gap: 16px; }
.date-nav { display: flex; align-items: center; gap: 8px; position: relative; }
.date-text { font-size: 20px; font-weight: 900; cursor: pointer; display: flex; align-items: center; gap: 4px; transition: 0.1s; letter-spacing: 0.05em; padding: 4px; }
.date-text:hover { background: var(--text-primary); color: var(--bg-base); }

.header-title-sep { width: 2px; height: 20px; background: var(--border); margin: 0 8px; }
.page-title-label { font-size: 16px; font-weight: 900; display: flex; align-items: center; gap: 8px; letter-spacing: 0.05em; }

.custom-date-popover { position: absolute; top: 100%; left: 0; margin-top: 8px; background: var(--bg-base); border: 2px solid var(--text-primary); padding: 20px; box-shadow: 6px 6px 0 var(--text-primary); z-index: 100; display: flex; flex-direction: column; gap: 16px; }
.popover-row { display: flex; gap: 12px; }
.custom-sel-display { background: transparent; border: 1px solid var(--border); padding: 10px 14px; font-size: 13px; font-weight: 800; cursor: pointer; display: flex; justify-content: space-between; gap: 8px; min-width: 95px; }
.custom-sel-display.active, .custom-sel-display:hover { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); }
.custom-sel-list { position: absolute; top: 100%; left: 0; width: 100%; max-height: 180px; background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 4px 4px 0 var(--text-primary); padding: 6px; z-index: 110; margin-top: 4px; }
.custom-sel-item { padding: 8px 10px; font-size: 12px; font-weight: 700; cursor: pointer; }
.custom-sel-item:hover, .custom-sel-item.selected { background: var(--text-primary); color: var(--bg-base); }
.btn-pop-confirm { width: 100%; background: var(--text-primary); color: var(--bg-base); border: none; padding: 12px; font-size: 13px; font-weight: 900; letter-spacing: 0.1em; cursor: pointer; }
.btn-pop-confirm:hover { background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); }

.nav-controls { display: flex; align-items: center; gap: 6px; }
.nav-btn { width: 34px; height: 34px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.nav-btn--text { width: auto; padding: 0 14px; font-size: 12px; font-weight: 800; letter-spacing: 0.05em; }
.nav-btn:hover { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.prompt-filters { display: flex; gap: 6px; }
.p-filter-btn { padding: 8px 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 12px; font-weight: 800; cursor: pointer; transition: 0.1s; display: flex; align-items: center; letter-spacing: 0.05em; }
.p-filter-btn.active, .p-filter-btn:hover { border-color: var(--text-primary); box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); background: var(--bg-surface); }
.p-dot { display: inline-block; width: 10px; height: 10px; border: 2px solid var(--border); margin-right: 6px; }

/* 레이아웃 */
.prompt-view-layout { flex: 1; display: flex; overflow: hidden; }
.prompt-graph-panel { width: 380px; flex-shrink: 0; background: var(--bg-surface); border-right: 2px solid var(--border); display: flex; flex-direction: column; }

.prompt-graph-title { padding: 20px; border-bottom: 2px solid var(--border); display: flex; align-items: center; }
.ptl-header-tracks { display: flex; gap: 12px; width: 100%; overflow-x: auto; white-space: nowrap; }
.ptl-header-track-badge { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 900; padding: 6px 12px; border: 2px solid; box-shadow: 4px 4px 0 currentColor; }
.ptl-head-dot { width: 10px; height: 10px; border: 1px solid var(--bg-base); }

.prompt-graph-inner { flex: 1; padding-bottom: 20px; }

.prompt-tl-empty { text-align: center; margin: 40px 20px; padding: 40px 0; border: 2px dashed var(--border); }
.empty-icon-wrap { font-size: 40px; margin-bottom: 16px; color: var(--text-faint); }
.empty-title { font-size: 16px; font-weight: 900; letter-spacing: 0.05em; }
.empty-desc { font-size: 12px; font-weight: 700; color: var(--text-muted); margin-top: 8px; }

.v-graph-timeline { position: relative; }
.v-graph-spines-container { position: absolute; top: 0; bottom: 0; left: 74px; display: flex; gap: 12px; opacity: 0.3; }
.v-graph-spine-line { width: 16px; position: relative; }
.v-graph-spine-inner { position: absolute; top: 0; bottom: 0; left: 50%; border-left: 2px dashed; transform: translateX(-50%); }

.snap-container { scroll-snap-type: y mandatory; }
.snap-item-wrap { scroll-snap-align: start; scroll-margin-top: 20px; border-bottom: 1px solid var(--border); padding-bottom: 10px; }

.v-graph-date-row { display: flex; align-items: center; margin: 24px 0 16px 0; position: sticky; top: 0; z-index: 20; }
.v-graph-date-badge { font-size: 12px; font-weight: 900; background: var(--text-primary); color: var(--bg-base); padding: 6px 16px; letter-spacing: 0.05em; border-right: 2px solid var(--bg-base); }
.v-graph-empty-day { height: 20px; }

.v-graph-row { display: flex; padding: 12px 20px 12px 10px; cursor: pointer; transition: 0.1s; }
.v-graph-row:hover { background: var(--bg-hover); }
.v-graph-row.is-selected { background: rgba(0,0,0,0.05); }
:global(body.theme-dark) .v-graph-row.is-selected { background: rgba(255,255,255,0.05); }

.v-graph-time { width: 50px; font-size: 12px; font-weight: 800; color: var(--text-muted); text-align: right; margin-right: 14px; padding-top: 14px; }
.v-graph-lanes { display: flex; gap: 12px; }
.v-graph-lane { width: 16px; position: relative; display: flex; justify-content: center; }
.v-graph-node { position: absolute; top: 16px; width: 14px; height: 14px; border: 2px solid var(--bg-base); z-index: 30; } /* 점을 네모 반듯하게 변경 */

.v-graph-content { flex: 1; padding-left: 20px; }
.v-graph-card { background: var(--bg-base); border: 2px solid; padding: 14px; box-shadow: 4px 4px 0 currentColor; transition: 0.1s; }
.v-graph-row:hover .v-graph-card { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 currentColor; }
.v-graph-track-name { font-size: 11px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid currentColor; padding-bottom: 2px; }
.v-graph-title { font-size: 14px; font-weight: 800; margin-top: 8px; line-height: 1.4; color: var(--text-primary); }

/* 채팅 창 */
.prompt-chat-panel { flex: 1; display: flex; flex-direction: column; background: var(--bg-base); }
.prompt-chat-header { padding: 20px 32px; background: var(--bg-surface); border-bottom: 2px solid var(--border); height: 80px; display: flex; align-items: center; }
.prompt-welcome-info-placeholder { display: flex; align-items: center; gap: 16px; color: var(--text-faint); }
.prompt-welcome-icon-sm { width: 40px; height: 40px; background: var(--text-primary); color: var(--bg-base); display: flex; align-items: center; justify-content: center; font-size: 18px; }
.prompt-chat-title { font-size: 18px; font-weight: 900; color: var(--text-primary); letter-spacing: 0.05em; }
.prompt-chat-subtitle { font-size: 12px; font-weight: 700; color: var(--text-muted); margin-top: 4px; }
.prompt-schedule-info { display: flex; align-items: center; gap: 16px; }
.prompt-schedule-dot { width: 16px; height: 16px; border: 2px solid var(--bg-surface); outline: 2px solid currentColor; }

.prompt-messages-area { flex: 1; padding: 32px; display: flex; flex-direction: column; gap: 20px; }
.prompt-msg { display: flex; flex-direction: column; gap: 6px; max-width: 75%; }
.prompt-msg--user { align-self: flex-end; align-items: flex-end; }
.prompt-msg--ai { align-self: flex-start; }
.prompt-msg-bubble { padding: 14px 20px; font-size: 14px; font-weight: 600; line-height: 1.6; white-space: pre-wrap; word-break: break-word; border: 2px solid var(--text-primary); box-shadow: 4px 4px 0 var(--text-primary); }
.prompt-msg--user .prompt-msg-bubble { background: var(--text-primary); color: var(--bg-base); }
.prompt-msg--ai .prompt-msg-bubble { background: var(--bg-surface); color: var(--text-primary); }
.prompt-msg-time { font-size: 11px; font-weight: 800; color: var(--text-muted); }

.prompt-input-area { padding: 24px 32px; border-top: 2px solid var(--border); background: var(--bg-surface); display: flex; gap: 16px; }
.prompt-textarea { flex: 1; background: var(--bg-base); border: 2px solid var(--text-primary); padding: 16px; font-size: 14px; font-weight: 600; resize: none; outline: none; color: var(--text-primary); transition: 0.1s; }
.prompt-textarea:focus { box-shadow: 4px 4px 0 var(--text-primary); }
.prompt-send-btn { padding: 0 32px; background: var(--text-primary); color: var(--bg-base); font-size: 16px; font-weight: 900; letter-spacing: 0.1em; border: 2px solid var(--text-primary); cursor: pointer; transition: 0.1s; }
.prompt-send-btn:hover:not(:disabled) { background: transparent; color: var(--text-primary); box-shadow: 6px 6px 0 var(--text-primary); transform: translate(-2px, -2px); }
.prompt-send-btn:disabled { opacity: 0.3; cursor: not-allowed; }
</style>