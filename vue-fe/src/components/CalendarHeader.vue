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
              <button class="btn-pop-confirm" @click="applyCustomDate">JUMP TO DATE</button>
            </div>
          </Transition>
        </div>

        <div class="nav-controls">
          <button class="nav-btn" @click="$emit('navigate', 'prev')"><i class="fas fa-chevron-left" /></button>
          <button class="nav-btn nav-btn--text" @click="$emit('navigate', 'today')">TODAY</button>
          <button class="nav-btn" @click="$emit('navigate', 'next')"><i class="fas fa-chevron-right" /></button>
        </div>
      </div>

      <div class="view-switcher">
        <button class="view-btn" :class="{ 'view-btn--active': currentView === 'month' }" @click="$emit('change-view', 'month')">MONTH</button>
        <button class="view-btn" :class="{ 'view-btn--active': currentView === 'week' }" @click="$emit('change-view', 'week')">WEEK</button>
      </div>
    </div>

    <div class="header-right">
      <div ref="legendBtnRef" class="legend-collapse-wrap">
        <button
          class="btn-legend-toggle"
          :class="{ 'btn-legend-toggle--open': legendOpen }"
          @click="toggleLegend"
        >
          <i class="fas fa-filter" />
          <span>FILTER</span>
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
                <span>TRACK FILTER</span>
                <button class="legend-panel-close" @click="legendOpen = false">
                  <i class="fas fa-times" />
                </button>
              </div>
              <BranchLegend 
                :hidden-tracks="hiddenTracks"
                :visible-tracks="visibleTracks" 
                @update:hiddenTracks="$emit('update:hiddenTracks', $event)" 
                @hover-track="$emit('hover-track', $event)" 
              />
            </div>
          </Transition>
        </Teleport>
      </div>

      <button class="btn-manage" @click="$emit('open-track-modal')">
        <i class="fas fa-layer-group" /><span>MANAGE</span>
      </button>

      <RouterLink to="/study-calendar" class="btn-study-cal">
        <i class="fas fa-bolt" />
        <span>STUDY LOG</span>
      </RouterLink>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import BranchLegend from '@/components/BranchLegend.vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  dateText:      { type: String, required: true },
  currentView:   { type: String, required: true },
  currentDate:   { type: Date,   required: true },
  visibleTracks: { type: Array,  default: () => [] }
})

const emit = defineEmits(['navigate', 'change-view', 'open-track-modal', 'jump-to-date', 'update:hiddenTracks', 'hover-track'])

const legendOpen      = ref(false)
const legendBtnRef    = ref(null)
const legendPanelRef  = ref(null)
const legendPanelStyle = ref({})

const hiddenTracks = ref(new Set())

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
  const inBtn   = legendBtnRef.value?.contains(e.target)
  const inPanel = legendPanelRef.value?.contains(e.target)
  if (!inBtn && !inPanel) legendOpen.value = false
}

const isPopoverOpen = ref(false)
const dateWrapRef = ref(null)

const activeSelect = ref(null)
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
    activeSelect.value = null
  }
  isPopoverOpen.value = !isPopoverOpen.value
}

function toggleSelect(type) { activeSelect.value = activeSelect.value === type ? null : type }
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
</script>

<style scoped>
.calendar-header {
  height: 64px;
  border-bottom: 2px solid var(--border);
  background: var(--bg-surface);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0; z-index: 30;
  gap: 12px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
}

.header-left  { display: flex; align-items: center; gap: 20px; flex-shrink: 0; }
.date-nav     { display: flex; align-items: center; gap: 12px; }

.header-right {
  display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; 
  overflow-x: auto; padding-bottom: 2px;
}
.header-right > :first-child { margin-left: auto; }
.header-right > * { flex-shrink: 0; }
.header-right::-webkit-scrollbar { display: none; }

.date-text-wrap { position: relative; min-width: 150px; }

.date-text {
  font-size: 20px; font-weight: 900; color: var(--text-primary);
  cursor: pointer; display: flex; align-items: center; gap: 4px;
  border-radius: 0; padding: 4px 8px; letter-spacing: 0.05em;
  transition: background 0.15s; white-space: nowrap;
}
.date-text:hover { background: var(--text-primary); color: var(--bg-base); }

.custom-date-popover {
  position: absolute; top: 100%; left: 0; margin-top: 8px;
  background: var(--bg-base); border: 2px solid var(--text-primary);
  border-radius: 0; padding: 20px; box-shadow: 6px 6px 0 var(--text-primary);
  z-index: 100; display: flex; flex-direction: column; gap: 16px;
}

.popover-row { display: flex; gap: 12px; }

.custom-sel-wrap { position: relative; }
.custom-sel-display {
  background: transparent; border: 1px solid var(--border); color: var(--text-primary);
  padding: 10px 14px; border-radius: 0;
  font-size: 13px; font-weight: 800; cursor: pointer;
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  min-width: 95px; white-space: nowrap; transition: all 0.1s ease;
}
.custom-sel-display:hover, .custom-sel-display.active { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); }
.sel-icon { font-size: 10px; transition: transform 0.2s ease; }
.custom-sel-display.active .sel-icon { transform: rotate(180deg); }

.custom-sel-list {
  position: absolute; top: calc(100% + 6px); left: 0; width: 100%;
  max-height: 200px; overflow-y: auto; overflow-x: hidden;
  background: var(--bg-base); border: 2px solid var(--text-primary);
  border-radius: 0; padding: 6px; z-index: 110;
  box-shadow: 4px 4px 0 var(--text-primary); list-style: none; margin: 0;
}
.custom-sel-item {
  padding: 8px 10px; font-size: 12px; border-radius: 0; cursor: pointer;
  transition: all 0.1s; font-weight: 700; color: var(--text-primary); white-space: nowrap;
}
.custom-sel-item:hover { background: var(--text-primary); color: var(--bg-base); }
.custom-sel-item.selected { background: var(--text-primary); color: var(--bg-base); }

.btn-pop-confirm { width: 100%; background: var(--text-primary); color: var(--bg-base); border: none; padding: 12px; border-radius: 0; font-size: 13px; font-weight: 900; letter-spacing: 0.1em; cursor: pointer; transition: all 0.15s; }
.btn-pop-confirm:hover { background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); }

.nav-controls { display: flex; align-items: center; gap: 6px; }
.nav-btn { width: 34px; height: 34px; border: 1px solid var(--border); background: transparent; border-radius: 0; color: var(--text-primary); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 12px; transition: all 0.1s; }
.nav-btn--text { width: auto; padding: 0 14px; font-size: 12px; font-weight: 800; letter-spacing: 0.05em; }
.nav-btn:hover { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.view-switcher { display: flex; gap: 4px; background: transparent; border: 1px solid var(--border); padding: 4px; border-radius: 0; }
.view-btn { padding: 6px 16px; font-size: 12px; font-weight: 800; letter-spacing: 0.05em; border: 1px solid transparent; border-radius: 0; cursor: pointer; background: transparent; color: var(--text-muted); transition: all 0.1s; }
.view-btn:hover { color: var(--text-primary); border-color: var(--border); }
.view-btn--active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.btn-manage, .btn-legend-toggle, .btn-study-cal {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border: 1px solid var(--border); background: transparent;
  color: var(--text-primary); border-radius: 0;
  font-size: 12px; font-weight: 800; cursor: pointer;
  transition: all 0.1s; white-space: nowrap; letter-spacing: 0.05em;
}
.btn-manage:hover, .btn-legend-toggle:hover, .btn-legend-toggle--open { background: var(--text-primary); color: var(--bg-base); }

.btn-study-cal { border: 2px solid var(--text-primary); box-shadow: 3px 3px 0 var(--text-primary); }
.btn-study-cal:hover { transform: translate(1px, 1px); box-shadow: 2px 2px 0 var(--text-primary); background: transparent; color: var(--text-primary); }

/* 모달 */
.legend-panel-teleport {
  position: absolute; z-index: 9999;
  background: var(--bg-base); border: 2px solid var(--text-primary);
  border-radius: 0; padding: 20px; 
  width: max-content; min-width: 320px;
  box-shadow: 6px 6px 0 var(--text-primary);
  display: flex; flex-direction: column; gap: 16px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
}
.legend-panel-title {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary);
  border-bottom: 2px solid var(--text-primary); padding-bottom: 12px;
}
.legend-panel-close { background: none; border: none; color: var(--text-primary); font-size: 16px; cursor: pointer; transition: 0.1s; }
.legend-panel-close:hover { transform: scale(1.1); }
</style>