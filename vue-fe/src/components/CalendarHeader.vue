<template>
  <header class="calendar-header">
    <div class="header-left">
      <div class="date-nav">
        <div class="date-text-wrap" ref="dateWrapRef">
          <h2
            class="date-text"
            title="날짜 선택"
            @click="toggleDatePopover"
          >
            {{ dateText }}
            <i class="fas" :class="isPopoverOpen ? 'fa-caret-up' : 'fa-caret-down'" style="margin-left: 4px; font-size: 14px;" />
          </h2>

          <Transition name="fade-pop">
            <div v-if="isPopoverOpen" class="custom-date-popover">
              <div class="popover-row">
                
                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('year')" :class="{ active: activeSelect === 'year' }">
                    {{ selYear }}년 <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'year'" class="custom-sel-list custom-scroll">
                      <li v-for="y in yearOptions" :key="y" class="custom-sel-item" :class="{ selected: selYear === y }" @click="pickYear(y)">{{ y }}년</li>
                    </ul>
                  </Transition>
                </div>

                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('month')" :class="{ active: activeSelect === 'month' }">
                    {{ selMonth }}월 <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'month'" class="custom-sel-list custom-scroll">
                      <li v-for="m in 12" :key="m" class="custom-sel-item" :class="{ selected: selMonth === m }" @click="pickMonth(m)">{{ m }}월</li>
                    </ul>
                  </Transition>
                </div>

                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('day')" :class="{ active: activeSelect === 'day' }">
                    {{ selDay }}일 <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'day'" class="custom-sel-list custom-scroll">
                      <li v-for="d in daysInSelectedMonth" :key="d" class="custom-sel-item" :class="{ selected: selDay === d }" @click="pickDay(d)">{{ d }}일</li>
                    </ul>
                  </Transition>
                </div>

              </div>
              <button class="btn-pop-confirm" @click="applyCustomDate">해당 날짜로 이동</button>
            </div>
          </Transition>
        </div>

        <div class="nav-controls">
          <button class="nav-btn" @click="$emit('navigate', 'prev')"><i class="fas fa-chevron-left" /></button>
          <button class="nav-btn nav-btn--text" @click="$emit('navigate', 'today')">오늘</button>
          <button class="nav-btn" @click="$emit('navigate', 'next')"><i class="fas fa-chevron-right" /></button>
        </div>
      </div>

      <div class="view-switcher">
        <button
          v-for="v in VIEW_OPTIONS" :key="v.key"
          class="view-btn"
          :class="{ 'view-btn--active': currentView === v.key }"
          @click="$emit('change-view', v.key)"
        >{{ v.label }}</button>
      </div>
    </div>

    <div class="header-right">
      <template v-if="currentView !== 'prompt'">
        <!-- 트랙 레전드 토글 -->
        <div ref="legendBtnRef" class="legend-collapse-wrap">
          <button
            class="btn-legend-toggle"
            :class="{ 'btn-legend-toggle--open': legendOpen }"
            @click="toggleLegend"
          >
            <i class="fas fa-layer-group" />
            <span>트랙 필터</span>
            <i class="fas" :class="legendOpen ? 'fa-chevron-up' : 'fa-chevron-down'" style="font-size:9px;" />
          </button>

          <Teleport to="body">
            <Transition name="legend-fade">
              <div
                v-if="legendOpen"
                ref="legendPanelRef"
                class="legend-panel-teleport"
                :style="legendPanelStyle"
              >
                <div class="legend-panel-title">
                  <i class="fas fa-layer-group" /> 트랙 필터
                  <button class="legend-panel-close" @click="legendOpen = false">
                    <i class="fas fa-times" />
                  </button>
                </div>
                <BranchLegend @update:hiddenTracks="$emit('update:hiddenTracks', $event)" />
              </div>
            </Transition>
          </Teleport>
        </div>
        <button class="btn-manage" @click="$emit('open-track-modal')">
          <i class="fas fa-layer-group" /><span>트랙 관리</span>
        </button>
        <!-- 학습 캘린더 전환 버튼 -->
        <RouterLink to="/study-calendar" class="btn-study-cal">
          <i class="fas fa-graduation-cap" />
          <span>학습 캘린더</span>
        </RouterLink>
      </template>
      <template v-else>
        <div class="prompt-filters">
          <button class="p-filter-btn" :class="{ active: promptFilter === 'all' }" @click="setPromptFilter('all')">
            All
          </button>
          <button class="p-filter-btn" :class="{ active: promptFilter === 'prompt' }" @click="setPromptFilter('prompt')">
            <span class="p-dot" :style="{ background: promptTrackColor }"></span> 프롬프트
          </button>
          <button class="p-filter-btn" :class="{ active: promptFilter === 'blog' }" @click="setPromptFilter('blog')">
            <span class="p-dot" :style="{ background: blogTrackColor }"></span> 블로그 어시스턴트
          </button>
        </div>
      </template>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import BranchLegend from '@/components/BranchLegend.vue'
import { RouterLink } from 'vue-router'
import { useCalendarStore } from '@/stores/useCalendarStore'

const legendOpen      = ref(false)
const legendBtnRef    = ref(null)
const legendPanelRef  = ref(null)
const legendPanelStyle = ref({})

function toggleLegend() {
  legendOpen.value = !legendOpen.value
  if (legendOpen.value && legendBtnRef.value) {
    const rect = legendBtnRef.value.getBoundingClientRect()
    legendPanelStyle.value = {
      top:   rect.bottom + 8 + 'px',
      right: window.innerWidth - rect.right + 'px',
    }
  }
}

function onDocClick(e) {
  // 토글 버튼 또는 패널 안 클릭이면 닫지 않음
  const inBtn   = legendBtnRef.value?.contains(e.target)
  const inPanel = legendPanelRef.value?.contains(e.target)
  if (!inBtn && !inPanel) {
    legendOpen.value = false
  }
}

const VIEW_OPTIONS = [
  { key: 'month', label: '월별' },
  { key: 'week',  label: '주별' },
  { key: 'prompt',label: '프롬프트' }
]

const props = defineProps({
  dateText:    { type: String, required: true },
  currentView: { type: String, required: true },
  currentDate: { type: Date,   required: true }
})

const emit = defineEmits(['navigate', 'change-view', 'open-track-modal', 'jump-to-date', 'update:hiddenTracks'])
const store = useCalendarStore()

// ── 날짜 팝오버 로직 ──
const isPopoverOpen = ref(false)
const dateWrapRef = ref(null)

// ── 커스텀 드롭다운 상태 ──
const activeSelect = ref(null) // 'year', 'month', 'day', null
const selYear = ref(new Date().getFullYear())
const selMonth = ref(new Date().getMonth() + 1)
const selDay = ref(new Date().getDate())

const yearOptions = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 11 }, (_, i) => current - 5 + i)
})
const daysInSelectedMonth = computed(() => new Date(selYear.value, selMonth.value, 0).getDate())

function toggleDatePopover() {
  if (!isPopoverOpen.value) {
    const d = props.currentDate || new Date()
    selYear.value = d.getFullYear()
    selMonth.value = d.getMonth() + 1
    selDay.value = d.getDate() || 1
    activeSelect.value = null // 열 때 드롭다운 초기화
  }
  isPopoverOpen.value = !isPopoverOpen.value
}

// ── 드롭다운 선택 함수 ──
function toggleSelect(type) {
  activeSelect.value = activeSelect.value === type ? null : type
}
function pickYear(y) { selYear.value = y; activeSelect.value = null; }
function pickMonth(m) { 
  selMonth.value = m; 
  if (selDay.value > daysInSelectedMonth.value) selDay.value = daysInSelectedMonth.value;
  activeSelect.value = null; 
}
function pickDay(d) { selDay.value = d; activeSelect.value = null; }

function applyCustomDate() {
  emit('jump-to-date', new Date(selYear.value, selMonth.value - 1, selDay.value))
  isPopoverOpen.value = false
  activeSelect.value = null
}

function handleClickOutside(e) {
  if (isPopoverOpen.value && dateWrapRef.value && !dateWrapRef.value.contains(e.target)) {
    isPopoverOpen.value = false
    activeSelect.value = null
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('click', onDocClick, true)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('click', onDocClick, true)
})


// ── ★ 프롬프트 전용 필터 로직 ★ ──
const promptFilter = ref('all') // 'all', 'prompt', 'blog'

const promptTrack = computed(() => store.allTracks.find(t => t.id?.toLowerCase().includes('prompt') || t.name?.toLowerCase().includes('prompt')))
const blogTrack = computed(() => store.allTracks.find(t => t.id?.toLowerCase().includes('blog') || t.name?.toLowerCase().includes('blog')))

const promptTrackColor = computed(() => promptTrack.value?.color || '#3b82f6')
const blogTrackColor = computed(() => blogTrack.value?.color || '#10b981')

function setPromptFilter(type) {
  promptFilter.value = type
  const newHidden = new Set()
  if (type === 'prompt' && blogTrack.value) newHidden.add(blogTrack.value.id)
  if (type === 'blog' && promptTrack.value) newHidden.add(promptTrack.value.id)
  emit('update:hiddenTracks', newHidden)
}

watch(() => props.currentView, (newView) => {
  if (newView === 'prompt') {
    promptFilter.value = 'all'
    emit('update:hiddenTracks', new Set())
  }
})
</script>

<style scoped>
.calendar-header {
  height: 64px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px 0 20px;
  flex-shrink: 0; z-index: 30;
  transition: background 0.3s;
  gap: 12px;
}

.header-left  { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.date-nav     { display: flex; align-items: center; gap: 8px; }

/* ── 우측 헤더 (가로 스크롤 적용 및 자식 요소 찌그러짐 방지) ── */
.header-right {
  display: flex; 
  align-items: center; 
  gap: 10px; 
  min-width: 0; 
  flex: 1; 
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 2px; /* 스크롤 여유 공간 */
}
.header-right > :first-child {
  margin-left: auto;
}
.header-right > * {
  flex-shrink: 0;
}
/* 스크롤바 숨기기 */
.header-right::-webkit-scrollbar { display: none; }
.header-right { -ms-overflow-style: none; scrollbar-width: none; }

/* ── 날짜 커스텀 팝오버 스타일 ── */
.date-text-wrap { position: relative; min-width: 150px; }

.date-text {
  font-size: 18px; font-weight: 700;
  color: var(--text-primary);
  cursor: pointer;
  display: flex; align-items: center; gap: 4px;
  border-radius: 8px; padding: 4px 8px;
  transition: background 0.15s; white-space: nowrap;
  font-family: 'Escoredream', sans-serif;
}
.date-text:hover { background: var(--bg-hover); }

.custom-date-popover {
  position: absolute;
  top: 100%; left: 0;
  margin-top: 8px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-mid);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 12px 32px rgba(0,0,0,0.4);
  z-index: 100;
  display: flex; flex-direction: column; gap: 16px;
}

.popover-row { display: flex; gap: 8px; }

/* ── ★ 커스텀 드롭다운 (네이티브 Select 대체) ★ ── */
.custom-sel-wrap { position: relative; }
.custom-sel-display {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  color: var(--text-primary);
  padding: 8px 12px;
  border-radius: 8px;
  font-family: 'Escoredream', sans-serif;
  font-size: 13px; font-weight: 600;
  cursor: pointer;
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  min-width: 95px; /* 연도 글자 잘림 방지 */
  white-space: nowrap; 
  transition: all 0.2s ease;
  user-select: none;
}
.custom-sel-display:hover, .custom-sel-display.active {
  border-color: var(--accent); background: var(--bg-hover);
}
.sel-icon {
  font-size: 10px; color: var(--text-faint); transition: transform 0.2s ease;
}
.custom-sel-display.active .sel-icon { transform: rotate(180deg); color: var(--accent); }

/* 드롭다운 목록 창 */
.custom-sel-list {
  position: absolute; top: calc(100% + 6px); left: 0; width: 100%;
  max-height: 200px; overflow-y: auto; overflow-x: hidden;
  background: var(--bg-elevated);
  border: 1px solid var(--border-mid);
  border-radius: 10px; padding: 6px; z-index: 110;
  box-shadow: 0 8px 24px rgba(0,0,0,0.25);
  list-style: none; margin: 0;
}
.custom-sel-item {
  padding: 8px 10px; font-size: 12px; border-radius: 6px; cursor: pointer;
  transition: all 0.15s; font-weight: 500; color: var(--text-primary);
  font-family: 'Escoredream', sans-serif;
  white-space: nowrap;
}
.custom-sel-item:hover { background: var(--bg-hover); }
.custom-sel-item.selected {
  background: rgba(59,130,246,0.15); color: var(--accent); font-weight: 700;
}

.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-track { background: transparent; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border-mid); border-radius: 10px; }

.drop-anim-enter-active, .drop-anim-leave-active { transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1); }
.drop-anim-enter-from, .drop-anim-leave-to { opacity: 0; transform: translateY(-6px); }

/* 이동 버튼 */
.btn-pop-confirm {
  width: 100%;
  background: var(--accent); color: #fff;
  border: none; padding: 10px; border-radius: 8px;
  font-size: 13px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  cursor: pointer; transition: all 0.15s;
}
.btn-pop-confirm:hover { opacity: 0.9; transform: scale(1.02); }

.fade-pop-enter-active, .fade-pop-leave-active { transition: all 0.2s ease; }
.fade-pop-enter-from, .fade-pop-leave-to { opacity: 0; transform: translateY(-10px); }

/* ── 나머지 네비게이션 스타일 ── */
.nav-controls { display: flex; align-items: center; gap: 3px; }

.nav-btn {
  width: 30px; height: 30px;
  border: 1px solid var(--border); background: transparent;
  border-radius: 7px; color: var(--text-muted);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  font-size: 10px; transition: all 0.15s;
}
.nav-btn--text { width: auto; padding: 0 10px; font-size: 11px; font-weight: 600; }
.nav-btn:hover { color: var(--text-primary); background: var(--bg-hover); border-color: var(--border-mid); }

.view-switcher {
  display: flex; gap: 2px;
  background: var(--bg-elevated); padding: 3px;
  border-radius: 9px; border: 1px solid var(--border);
}

.view-btn {
  padding: 5px 14px; font-size: 12px; font-weight: 600;
  border: none; border-radius: 6px;
  cursor: pointer; background: transparent;
  color: var(--text-muted); transition: all 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.view-btn:hover { color: var(--text-primary); background: var(--bg-hover); }
.view-btn--active {
  background: var(--accent); color: #fff;
  box-shadow: 0 0 10px var(--accent-glow);
}

.btn-manage {
  display: flex; align-items: center; gap: 7px;
  padding: 6px 14px;
  border: 1px solid var(--border); background: var(--bg-elevated);
  color: var(--text-secondary); border-radius: 8px;
  font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all 0.15s; white-space: nowrap;
  font-family: 'Escoredream', sans-serif;
  flex-shrink: 0;
}
.btn-manage:hover { color: var(--text-primary); border-color: var(--border-mid); background: var(--bg-hover); }

/* ── ★ 프롬프트 전용 필터 버튼 ── */
.prompt-filters {
  display: flex; gap: 4px;
  background: var(--bg-elevated); padding: 4px;
  border-radius: 8px; border: 1px solid var(--border);
  flex-shrink: 0;
}
.p-filter-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px; border: none; border-radius: 6px;
  background: transparent; color: var(--text-muted);
  font-size: 12px; font-weight: 700; cursor: pointer;
  font-family: 'Escoredream', sans-serif; transition: all 0.15s;
  white-space: nowrap; /* 글자 줄바꿈 방지 */
}
.p-filter-btn:hover { color: var(--text-primary); background: var(--bg-hover); }
.p-filter-btn.active {
  background: var(--bg-surface); color: var(--text-primary);
  box-shadow: 0 1px 4px rgba(0,0,0,0.1); border: 1px solid var(--border-mid);
}
.p-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

/* ── 트랙 레전드 토글 ── */
.legend-collapse-wrap { position: relative; flex-shrink: 0; }

.btn-legend-toggle {
  display: flex; align-items: center; gap: 7px;
  padding: 6px 12px; border-radius: 9px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-secondary); font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
  font-family: 'Escoredream', sans-serif;
}
.btn-legend-toggle:hover,
.btn-legend-toggle--open {
  background: var(--bg-hover); color: var(--text-primary); border-color: #818cf8;
}
.btn-legend-toggle i:first-child { color: #818cf8; }

/* ── 학습 캘린더 버튼 ── */
.btn-study-cal {
  display: flex; align-items: center; gap: 7px;
  padding: 6px 14px; border-radius: 9px;
  background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.3);
  color: #818cf8; font-size: 12px; font-weight: 700;
  text-decoration: none; transition: all 0.15s; white-space: nowrap;
  font-family: 'Escoredream', sans-serif;
}
.btn-study-cal:hover { background: rgba(129,140,248,0.2); border-color: #818cf8; }

</style>