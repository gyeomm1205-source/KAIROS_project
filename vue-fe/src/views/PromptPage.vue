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
                <button class="btn-pop-confirm" @click="applyCustomDate">해당 월로 이동</button>
              </div>
            </Transition>

            <div class="nav-controls">
              <button class="nav-btn" @click="navigate('prev')"><i class="fas fa-chevron-left" /></button>
              <button class="nav-btn nav-btn--text" @click="navigate('today')">오늘</button>
              <button class="nav-btn" @click="navigate('next')"><i class="fas fa-chevron-right" /></button>
            </div>
          </div>

          <div class="header-title-sep" />
          <h1 class="page-title-label"><i class="fas fa-robot" /> AI 어시스턴트</h1>
        </div>

        <div class="header-right">
          <div class="prompt-filters">
            <button class="p-filter-btn" :class="{ active: promptFilter === 'all' }" @click="setPromptFilter('all')">All</button>
            <button class="p-filter-btn" :class="{ active: promptFilter === 'prompt' }" @click="setPromptFilter('prompt')">
              <span class="p-dot" :style="{ background: headerPromptColor }"></span> 프롬프트
            </button>
            <button class="p-filter-btn" :class="{ active: promptFilter === 'blog' }" @click="setPromptFilter('blog')">
              <span class="p-dot" :style="{ background: headerBlogColor }"></span> 블로그
            </button>
          </div>
        </div>
      </header>

      <div class="prompt-view-layout">
         <div class="prompt-graph-panel">
          <div class="prompt-graph-title">
            <div class="ptl-header-tracks">
              <span v-for="track in promptTracks" :key="track.id" v-show="!hiddenTracks.has(track.id)" class="ptl-header-track-badge" :style="{ color: track.color }">
                <span class="ptl-head-dot" :style="{ background: track.color }" /> {{ track.name }}
              </span>
            </div>
            <span class="ptl-count-badge">{{ allPromptSchedules.length }}개</span>
          </div>

          <div class="prompt-graph-inner custom-scroll snap-container">
            <div v-if="!allPromptSchedules.length" class="prompt-tl-empty">
              <div class="empty-icon-wrap">
                <i class="fas fa-box-open" />
              </div>
              <p class="empty-title">선택한 월에 해당하는 기록이 없습니다.</p>
              <p class="empty-desc">새로운 AI 프롬프트나 블로그 포스팅을<br>진행하면 이곳에 타임라인이 그려집니다.</p>
            </div>

            <div v-else class="v-graph-timeline">
              <div class="v-graph-spines-container">
                <div v-for="track in promptTracks" :key="`bg-spine-${track.id}`" class="v-graph-spine-line" v-show="!hiddenTracks.has(track.id)">
                  <div class="v-graph-spine-inner" :style="{ background: track.color }"></div>
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
                        <div v-if="s.track === track.id" class="v-graph-node" :style="{ background: 'var(--bg-surface)', borderColor: track.color }">
                          <div class="v-graph-node-inner" :style="{ background: track.color }"></div>
                        </div>
                      </div>
                    </div>
                    <div class="v-graph-content">
                      <div class="v-graph-card" :style="{ borderLeftColor: getPromptTrackColor(s.track) }">
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
                <div class="prompt-welcome-icon-sm"><i class="fas fa-robot" /></div>
                <div class="prompt-chat-title">일정을 선택하세요</div>
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
            <textarea v-model="promptInput" class="prompt-textarea" placeholder="메시지 입력..." :disabled="!selectedPromptSchedule" rows="3" @keydown.enter.exact.prevent="sendPrompt" />
            <button class="prompt-send-btn" @click="sendPrompt" :disabled="!promptInput.trim() || !selectedPromptSchedule">
              <i class="fas fa-paper-plane" />
            </button>
          </div>
        </div>
      </div>
    </main>

    <Transition name="fade">
      <div v-if="edgeTooltip.visible" class="edge-tooltip-popup" :style="{ left: edgeTooltip.x + 'px', top: edgeTooltip.y + 'px' }">
        <div class="et-track" :style="{ color: edgeTooltip.edge.color }">
          {{ store.getTrackById(edgeTooltip.edge.track)?.name }}
        </div>
        <div class="et-nodes">
          <span>{{ edgeTooltip.edge.from.tooltip?.title || edgeTooltip.edge.from.text || '시작' }}</span>
          <i class="fas fa-arrow-right" />
          <span>{{ edgeTooltip.edge.to.tooltip?.title || edgeTooltip.edge.to.text || '종료' }}</span>
        </div>
      </div>
    </Transition>
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

const headerDateText = computed(() => `${currentYear.value}년 ${currentMonth.value}월`)
const yearOptions = computed(() => {
  const cur = new Date().getFullYear()
  return Array.from({ length: 11 }, (_, i) => cur - 5 + i)
})

function toggleDatePopover() {
  isPopoverOpen.value = !isPopoverOpen.value
  if (isPopoverOpen.value) {
    selYear.value = currentYear.value
    selMonth.value = currentMonth.value
    activeSelect.value = null
  }
}

function toggleSelect(type) { activeSelect.value = activeSelect.value === type ? null : type }
function pickYear(y) { selYear.value = y; activeSelect.value = null; }
function pickMonth(m) { selMonth.value = m; activeSelect.value = null; }

function applyCustomDate() {
  currentYear.value = selYear.value
  currentMonth.value = selMonth.value
  isPopoverOpen.value = false
}

function handleBackdropClick(e) {
  if (isPopoverOpen.value && dateWrapRef.value && !dateWrapRef.value.contains(e.target)) {
    isPopoverOpen.value = false
  }
}

function navigate(dir) {
  if (dir === 'prev') {
    if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value-- }
    else currentMonth.value--
  } else if (dir === 'next') {
    if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++ }
    else currentMonth.value++
  } else {
    currentYear.value = today.getFullYear(); currentMonth.value = today.getMonth() + 1
  }
}

const promptFilter = ref('all')
const hiddenTracks = ref(new Set())

const promptTrack = computed(() => allTracks.value.find(t => t.id?.toLowerCase().includes('prompt') || t.name?.toLowerCase().includes('prompt')))
const blogTrack = computed(() => allTracks.value.find(t => t.id?.toLowerCase().includes('blog') || t.name?.toLowerCase().includes('blog')))
const headerPromptColor = computed(() => promptTrack.value?.color || '#facc15')
const headerBlogColor = computed(() => blogTrack.value?.color || '#10b981')

function setPromptFilter(type) {
  promptFilter.value = type
  hiddenTracks.value = new Set()
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
  for (let d = 1; d <= lastDay; d++) {
    const ds = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    map.set(ds, [])
  }
  allPromptSchedules.value.forEach(s => { if (map.has(s.day)) map.get(s.day).push(s) })
  return [...map.entries()].map(([day, schedules]) => ({ day, schedules }))
})

// ── ★ 엣지 호버: 툴팁 잘림 방지 로직 ★ ──
const interactionState = ref({ hovered: null, clicked: null })
const edgeTooltip = ref({ visible: false, x: 0, y: 0, edge: null })

function onEdgeHover(edge, mouseEvent) {
  if (edge) {
    interactionState.value.hovered = { type: 'edge', data: edge }
    if (mouseEvent) {
      // 툴팁이 오른쪽 창 밖으로 나가지 않도록 Clamp(제한) 계산
      // 툴팁의 예상 최대 너비의 절반 정도를 여백으로 둡니다 (약 150px)
      const maxRightX = window.innerWidth - 150; 
      const safeX = Math.min(mouseEvent.clientX, maxRightX);

      edgeTooltip.value = { visible: true, x: Math.max(safeX, 150), y: mouseEvent.clientY - 30, edge }
    }
  } else {
    if (interactionState.value.hovered?.type === 'edge') interactionState.value.hovered = null;
    edgeTooltip.value.visible = false;
  }
}

// ── 채팅 로직 ──
const promptInput = ref('')
const promptMessagesRef = ref(null)
const selectedPromptId = ref(null)
const selectedPromptSchedule = computed(() => selectedPromptId.value ? schedules.value.find(s => s.id === selectedPromptId.value) || null : null)
const promptMessagesMap = ref({})
const currentMessages = computed(() => selectedPromptId.value ? promptMessagesMap.value[selectedPromptId.value] || [] : [])

function selectPromptSchedule(s) { selectedPromptId.value = s.id }
function getPromptTrackColor(id) { return store.getTrackById(id)?.color || '#6b7280' }
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
.app-layout { display: flex; width: 100%; height: 100vh; background: var(--bg-base); font-family: 'Escoredream', sans-serif; color: var(--text-primary); }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ── 헤더 (메인 캘린더 디자인 복구) ── */
.calendar-header {
  height: 64px; border-bottom: 1px solid var(--border); background: var(--bg-surface);
  display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0; z-index: 30;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.date-nav { display: flex; align-items: center; gap: 12px; position: relative; }
.date-text { font-size: 18px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 8px; transition: background 0.15s; }
.date-text:hover { background: var(--bg-hover); }

.header-title-sep { width: 1px; height: 16px; background: var(--border); margin: 0 4px; }
.page-title-label { font-size: 16px; font-weight: 800; display: flex; align-items: center; gap: 8px; color: var(--text-primary); }
.page-title-label i { color: var(--accent); }

/* 날짜 팝오버 */
.custom-date-popover { position: absolute; top: 100%; left: 0; margin-top: 8px; background: var(--bg-elevated); border: 1px solid var(--border-mid); border-radius: 14px; padding: 16px; box-shadow: 0 12px 32px rgba(0,0,0,0.4); z-index: 100; display: flex; flex-direction: column; gap: 16px; }
.popover-row { display: flex; gap: 8px; }
.custom-sel-wrap { position: relative; }
.custom-sel-display { background: var(--bg-surface); border: 1px solid var(--border); padding: 8px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: space-between; gap: 8px; min-width: 95px; transition: 0.15s; }
.custom-sel-display.active { border-color: var(--accent); }
.custom-sel-list { position: absolute; top: 100%; left: 0; width: 100%; max-height: 180px; overflow-y: auto; background: var(--bg-elevated); border: 1px solid var(--border-mid); border-radius: 10px; padding: 6px; z-index: 110; margin-top: 4px; box-shadow: 0 8px 24px rgba(0,0,0,0.2); }
.custom-sel-item { padding: 8px; font-size: 12px; border-radius: 6px; cursor: pointer; }
.custom-sel-item:hover { background: var(--bg-hover); }
.custom-sel-item.selected { color: var(--accent); font-weight: 700; background: rgba(59,130,246,0.1); }
.btn-pop-confirm { width: 100%; background: var(--accent); color: #fff; border: none; padding: 10px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; }

.nav-controls { display: flex; align-items: center; gap: 3px; }
.nav-btn { width: 30px; height: 30px; border: 1px solid var(--border); background: transparent; border-radius: 7px; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 10px; }
.nav-btn--text { width: auto; padding: 0 10px; font-size: 11px; font-weight: 600; }
.nav-btn:hover { background: var(--bg-hover); border-color: var(--border-mid); color: var(--text-primary); }

.header-right { display: flex; align-items: center; }
.prompt-filters { display: flex; gap: 4px; background: var(--bg-elevated); padding: 4px; border-radius: 8px; border: 1px solid var(--border); }
.p-filter-btn { padding: 6px 12px; border: none; border-radius: 6px; background: transparent; color: var(--text-muted); font-size: 12px; font-weight: 700; cursor: pointer; transition: 0.15s; }
.p-filter-btn.active { background: var(--bg-surface); color: var(--text-primary); box-shadow: 0 1px 4px rgba(0,0,0,0.1); border: 1px solid var(--border-mid); }
.p-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }

/* ── 타임라인 스냅 스크롤 ── */
.prompt-view-layout { flex: 1; display: flex; overflow: hidden; }

/* ★ 하루 단위 스냅 설정 */
.snap-container { scroll-snap-type: y mandatory; overflow-y: scroll; }
.snap-item-wrap { scroll-snap-align: start; scroll-margin-top: 12px; border-bottom: 1px solid var(--border-subtle); }

.prompt-graph-panel { width: 380px; flex-shrink: 0; background: var(--bg-surface); border-right: 1px solid var(--border); display: flex; flex-direction: column; }
.prompt-graph-title { padding: 12px 16px; border-bottom: 1px solid var(--border); background: var(--bg-elevated); display: flex; justify-content: space-between; }
.ptl-header-track-badge { display: flex; align-items: center; gap: 5px; font-size: 11px; font-weight: 700; }
.ptl-head-dot { width: 7px; height: 7px; border-radius: 50%; }
.ptl-count-badge { font-size: 10px; padding: 2px 8px; border-radius: 999px; border: 1px solid var(--border); }

.prompt-graph-inner { flex: 1; min-height: 0; padding-bottom: 150px; }

/* 데이터 없을 때 화면 */
.prompt-tl-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 400px; text-align: center; margin: 24px; background: var(--bg-elevated); border: 1.5px dashed var(--border-mid); border-radius: 16px; }
.empty-icon-wrap { width: 64px; height: 64px; border-radius: 50%; background: var(--bg-surface); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; margin-bottom: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.empty-icon-wrap i { font-size: 26px; color: var(--text-faint); }
.empty-title { font-size: 15px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; }
.empty-desc { font-size: 12px; font-weight: 500; color: var(--text-muted); line-height: 1.6; }

.v-graph-timeline { position: relative; }
.v-graph-spines-container { position: absolute; top: 0; bottom: 0; left: 69px; display: flex; gap: 8px; opacity: 0.2; }
.v-graph-spine-line { width: 14px; position: relative; }
.v-graph-spine-inner { position: absolute; top: 0; bottom: 0; left: 50%; width: 2px; transform: translateX(-50%); }

.v-graph-date-row { display: flex; align-items: center; margin: 20px 0 10px 0; position: sticky; top: 12px; z-index: 20; }
.v-graph-date-badge { font-size: 11px; font-weight: 800; background: var(--bg-elevated); padding: 5px 12px; border-radius: 0 999px 999px 0; border: 1px solid var(--border-mid); border-left: none; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.v-graph-empty-day { height: 20px; }

.v-graph-row { display: flex; padding: 6px 20px 6px 10px; cursor: pointer; transition: 0.15s; }
.v-graph-row:hover { background: var(--bg-hover); }
.v-graph-row.is-selected { background: rgba(59, 130, 246, 0.05); }

.v-graph-time { width: 45px; font-size: 11px; color: var(--text-faint); text-align: right; margin-right: 14px; padding-top: 14px; font-family: monospace; }
.v-graph-lanes { display: flex; gap: 8px; }
.v-graph-lane { width: 14px; position: relative; display: flex; justify-content: center; }
.v-graph-node { position: absolute; top: 18px; width: 14px; height: 14px; border-radius: 50%; border: 2.5px solid; z-index: 30; }
.v-graph-node-inner { width: 6px; height: 6px; border-radius: 50%; margin: 1.5px; }

.v-graph-content { flex: 1; padding-left: 18px; }
.v-graph-card { background: var(--bg-surface); border: 1px solid var(--border); border-left: 4px solid; border-radius: 8px; padding: 12px; transition: 0.2s; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.v-graph-row:hover .v-graph-card { transform: translateX(4px); }
.v-graph-track-name { font-size: 10px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.03em; }
.v-graph-title { font-size: 13px; font-weight: 700; margin-top: 4px; line-height: 1.4; }

/* 오른쪽 채팅 */
.prompt-chat-panel { flex: 1; display: flex; flex-direction: column; background: var(--bg-base); }
.prompt-chat-header { padding: 18px 24px; background: var(--bg-surface); border-bottom: 1px solid var(--border); height: 64px; display: flex; align-items: center; }
.prompt-welcome-info-placeholder { display: flex; align-items: center; gap: 12px; color: var(--text-faint); }
.prompt-chat-title { font-size: 15px; font-weight: 800; color: var(--text-primary); }
.prompt-chat-subtitle { font-size: 11px; color: var(--text-faint); margin-top: 2px; }

.prompt-schedule-info { display: flex; align-items: center; gap: 12px; }
.prompt-schedule-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 6px currentColor; }
.prompt-welcome-icon-sm { width: 36px; height: 36px; border-radius: 50%; background: var(--accent); display: flex; align-items: center; justify-content: center; font-size: 15px; color: #fff; opacity: 0.85; }

.prompt-messages-area { flex: 1; padding: 24px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; }
.prompt-msg { display: flex; flex-direction: column; gap: 4px; max-width: 80%; }
.prompt-msg--user { align-self: flex-end; align-items: flex-end; }
.prompt-msg--ai { align-self: flex-start; }
.prompt-msg-bubble { padding: 10px 14px; border-radius: 14px; font-size: 13px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
.prompt-msg--user .prompt-msg-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 4px; }
.prompt-msg--ai .prompt-msg-bubble { background: var(--bg-elevated); border: 1px solid var(--border); border-bottom-left-radius: 4px; color: var(--text-primary); }
.prompt-msg-time { font-size: 9px; color: var(--text-faint); font-family: monospace; }

.prompt-input-area { padding: 16px 20px; border-top: 1px solid var(--border); background: var(--bg-surface); display: flex; gap: 10px; }
.prompt-textarea { flex: 1; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 12px; padding: 12px; font-family: inherit; resize: none; outline: none; color: var(--text-primary); }
.prompt-send-btn { width: 42px; height: 42px; border-radius: 10px; background: var(--accent); color: #fff; border: none; cursor: pointer; transition: 0.15s; }
.prompt-send-btn:hover { opacity: 0.9; }

.custom-scroll::-webkit-scrollbar { width: 6px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border-mid); border-radius: 10px; }

/* ★ 툴팁 스타일: 찌그러짐 방지 (min-width, white-space) */
.edge-tooltip-popup {
  position: fixed; z-index: 9999; pointer-events: none; background: var(--bg-surface); 
  border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; 
  box-shadow: 0 4px 16px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 6px; 
  transform: translate(-50%, -100%); margin-top: -10px;
  
  /* 화면 끝에서 텍스트 줄바꿈/찌그러짐 방지 */
  min-width: max-content;
  white-space: nowrap;
}
.et-track { font-size: 11px; font-weight: 800; font-family: 'Escoredream', sans-serif; }
.et-nodes { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.et-nodes i { color: var(--text-faint); font-size: 11px; }

.fade-pop-enter-active, .fade-pop-leave-active { transition: 0.2s cubic-bezier(0.25, 0.8, 0.25, 1); }
.fade-pop-enter-from, .fade-pop-leave-to { opacity: 0; transform: translateY(-10px); }
.drop-anim-enter-active { transition: 0.2s; }
.drop-anim-enter-from { opacity: 0; transform: translateY(-5px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>