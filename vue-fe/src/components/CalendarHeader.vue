<template>
  <header class="calendar-header">
    <div class="header-left">
      <div class="date-nav">
        <div class="date-text-wrap" ref="dateWrapRef">
          <h2 class="date-text" title="CHANGE DATE" @click="toggleDatePopover" style="padding: 6px 12px; margin: 0; font-size: 16px;">
            {{ dateText }}
            <i class="fas" :class="isPopoverOpen ? 'fa-caret-up' : 'fa-caret-down'" style="margin-left: 8px; font-size: 14px;" />
          </h2>

          <Transition name="fade-pop">
            <div v-if="isPopoverOpen" class="custom-date-popover">
              <div class="popover-row">
                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('year')" :class="{ active: activeSelect === 'year' }">
                    {{ selYear }} <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'year'" class="custom-sel-list custom-scroll">
                      <li v-for="y in yearOptions" :key="y" class="custom-sel-item" :class="{ selected: selYear === y }" @click="pickYear(y)">{{ y }}</li>
                    </ul>
                  </Transition>
                </div>
                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('month')" :class="{ active: activeSelect === 'month' }">
                    {{ selMonth }} <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'month'" class="custom-sel-list custom-scroll">
                      <li v-for="m in 12" :key="m" class="custom-sel-item" :class="{ selected: selMonth === m }" @click="pickMonth(m)">{{ m }}</li>
                    </ul>
                  </Transition>
                </div>
                <div class="custom-sel-wrap">
                  <div class="custom-sel-display" @click="toggleSelect('day')" :class="{ active: activeSelect === 'day' }">
                    {{ selDay }} <i class="fas fa-chevron-down sel-icon"></i>
                  </div>
                  <Transition name="drop-anim">
                    <ul v-if="activeSelect === 'day'" class="custom-sel-list custom-scroll">
                      <li v-for="d in daysInSelectedMonth" :key="d" class="custom-sel-item" :class="{ selected: selDay === d }" @click="pickDay(d)">{{ d }}</li>
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
          <button class="nav-btn" @click="$emit('navigate', 'next')"><i class="fas fa-chevron-right" /></button>
        </div>
      </div>

      <div class="view-switcher">
        <button class="view-btn" :class="{ 'view-btn--active': currentView === 'month' }" @click="$emit('change-view', 'month')">MONTH</button>
        <button class="view-btn" :class="{ 'view-btn--active': currentView === 'week' }" @click="$emit('change-view', 'week')">WEEK</button>
      </div>
    </div>

    <div class="header-right">
      <button class="btn-legend-toggle" :class="{ active: isLegendVisible }" @click="$emit('toggle-legend')">
        <i class="fas fa-layer-group" /> <span>FLOW LEGEND</span>
      </button>

      <div class="legend-divider" />

      <button class="btn-manage" @click="$emit('import-external')">
        <i class="fas fa-download" /><span>외부 일정 가져오기</span>
      </button>
      <button class="btn-study-cal" @click="$emit('sync-schedule')">
        <i class="fas fa-upload" /><span>내 일정 반영하기</span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({ dateText: String, currentView: String, currentDate: Date, isLegendVisible: Boolean })
const emit = defineEmits(['navigate', 'change-view', 'jump-to-date', 'import-external', 'sync-schedule', 'toggle-legend'])

const isPopoverOpen = ref(false); const dateWrapRef = ref(null); const activeSelect = ref(null); const selYear = ref(new Date().getFullYear()); const selMonth = ref(new Date().getMonth() + 1); const selDay = ref(new Date().getDate());
const yearOptions = computed(() => { const current = new Date().getFullYear(); return Array.from({ length: 21 }, (_, i) => current - 10 + i) })
const daysInSelectedMonth = computed(() => new Date(selYear.value, selMonth.value, 0).getDate())

function toggleDatePopover() { if (!isPopoverOpen.value) { const d = props.currentDate || new Date(); selYear.value = d.getFullYear(); selMonth.value = d.getMonth() + 1; selDay.value = d.getDate() || 1; activeSelect.value = null; } isPopoverOpen.value = !isPopoverOpen.value }
function toggleSelect(type) { activeSelect.value = activeSelect.value === type ? null : type }
function pickYear(y) { selYear.value = y; activeSelect.value = null; }
function pickMonth(m) { selMonth.value = m; if (selDay.value > daysInSelectedMonth.value) selDay.value = daysInSelectedMonth.value; activeSelect.value = null; }
function pickDay(d) { selDay.value = d; activeSelect.value = null; }
function applyCustomDate() { emit('jump-to-date', new Date(selYear.value, selMonth.value - 1, selDay.value)); isPopoverOpen.value = false; activeSelect.value = null; }

const legendPanelStyle = computed(() => {
  if (!legendBtnRef.value) return {};
  const rect = legendBtnRef.value.getBoundingClientRect();
  return {
    top: `${rect.bottom + window.scrollY + 8}px`,
    left: `${rect.left + window.scrollX - 220}px`, // Shift left to avoid being cut off
    zIndex: 9999
  };
});

function handleClickOutside(e) { if (isPopoverOpen.value && dateWrapRef.value && !dateWrapRef.value.contains(e.target)) { isPopoverOpen.value = false; activeSelect.value = null; } }

onMounted(() => { document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })
</script>

<style scoped>
.calendar-header {
  height: 64px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-base);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0; z-index: 200;
  gap: 12px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
}
.custom-sel-display:hover, .custom-sel-display.active { border-color: var(--accent); }
.sel-icon { font-size: 10px; transition: transform 0.2s ease; }
.custom-sel-display.active .sel-icon { transform: rotate(180deg); }

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
  border-radius: 4px; padding: 4px 8px; letter-spacing: 0.05em;
  transition: background 0.15s; white-space: nowrap;
}
.date-text:hover { background: var(--text-primary); color: var(--bg-base); }

.custom-date-popover {
  position: absolute; top: 100%; left: 0; margin-top: 8px;
  background: var(--bg-base); border: 1px solid var(--border);
  padding: 20px; box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  z-index: 100; display: flex; flex-direction: column; gap: 16px;
}

.popover-row { display: flex; gap: 12px; }

.custom-sel-wrap { position: relative; }
.custom-sel-display {
  background: transparent; border: 1px solid var(--border); color: var(--text-primary);
  padding: 10px 14px; border-radius: 4px;
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
  background: var(--bg-base); border: 1px solid var(--border);
  padding: 6px; z-index: 110;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1); list-style: none; margin: 0;
}
.custom-sel-item {
  padding: 8px 10px; font-size: 12px; border-radius: 4px; cursor: pointer;
  transition: all 0.1s; font-weight: 700; color: var(--text-primary); white-space: nowrap;
}
.custom-sel-item:hover { background: var(--text-primary); color: var(--bg-base); }
.custom-sel-item.selected { background: var(--text-primary); color: var(--bg-base); }

.btn-pop-confirm { width: 100%; background: var(--text-primary); color: var(--bg-base); border: none; padding: 12px; border-radius: 4px; font-size: 13px; font-weight: 900; letter-spacing: 0.1em; cursor: pointer; transition: all 0.15s; }
.btn-pop-confirm:hover { background: var(--bg-hover); color: var(--text-primary); border: 1px solid var(--text-primary); }

.nav-controls { display: flex; align-items: center; gap: 6px; }
.nav-btn { width: 34px; height: 34px; border: 1px solid var(--border); background: transparent; border-radius: 4px; color: var(--text-primary); cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 12px; transition: all 0.1s; }
.nav-btn--text { width: auto; padding: 0 14px; font-size: 12px; font-weight: 800; letter-spacing: 0.05em; }
.nav-btn:hover { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.view-switcher { display: flex; gap: 4px; background: transparent; border: 1px solid var(--border); padding: 4px; border-radius: 4px; }
.view-btn { padding: 6px 16px; font-size: 12px; font-weight: 800; letter-spacing: 0.05em; border: 1px solid transparent; border-radius: 4px; cursor: pointer; background: transparent; color: var(--text-muted); transition: all 0.1s; }
.view-btn:hover { color: var(--text-primary); border-color: var(--border); }
.view-btn--active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.btn-manage, .btn-study-cal {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border: 1px solid var(--border); background: transparent;
  color: var(--text-primary); border-radius: 4px;
  font-size: 12px; font-weight: 800; cursor: pointer;
  transition: all 0.1s; white-space: nowrap; letter-spacing: 0.05em;
}
.btn-manage:hover { background: var(--text-primary); color: var(--bg-base); }
.btn-study-cal { border: 1px solid var(--text-primary); }
.btn-study-cal:hover { background: var(--text-primary); color: var(--bg-base); }

/* 인라인 범례 */
.legend-inline { display: flex; align-items: center; gap: 12px; margin-right: 8px; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 11px; font-weight: 700; color: var(--text-muted); white-space: nowrap; }
.legend-item i { font-size: 10px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }
.legend-divider { width: 1px; height: 16px; background: var(--border); margin: 0 4px; }

.btn-legend-toggle {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border: 1px solid var(--border); background: transparent;
  color: var(--text-muted); border-radius: 4px;
  font-size: 12px; font-weight: 800; cursor: pointer;
  transition: all 0.2s; white-space: nowrap; letter-spacing: 0.05em;
}
.btn-legend-toggle:hover { color: var(--text-primary); border-color: var(--text-primary); background: var(--bg-hover); }
.btn-legend-toggle.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

</style>