<template>
  <div
    class="calendar-cell"
    :class="{
      'cell--today': isToday,
      'cell--other-month': !isCurrentMonth,
      'cell--selected': isSelected,
      'cell--sat': dayOfWeek === 6,
      'cell--sun': dayOfWeek === 0,
    }"
    @click="handleCellClick"
    @dblclick="handleDblClick"
  >
    <div class="cell-header">
      <div class="wrapper--today" v-if="isToday">
        <span class="date-label date--today">{{ dayNum }}</span>
      </div>
      <span v-else class="date-label" :class="{ 'date--holiday': isHoliday }">{{ dayNum }}</span>
      <button class="btn-add-schedule" @click.stop="$emit('add-schedule', dateStr)">
        <i class="fas fa-plus"></i>
      </button>
    </div>

    <div v-if="holidayName" class="holiday-name">{{ holidayName }}</div>

    <Transition name="chips-fade">
      <div v-if="labelSchedules.length" class="label-cluster">
        <div
          v-for="s in visibleSchedules" :key="s.id"
          :id="'node-' + s.id"
          class="schedule-label-chip"
          :class="{ 'is-dimmed': dimmedNodeIds.has(s.id) }"
          :style="{ color: trackColor(s.track), borderColor: trackColor(s.track) }"
          @click.stop="$emit('toggle-tooltip', s.id)"
          @dblclick.stop="$emit('open-ai-modal', s)"
          @mouseenter="$emit('hover-node', s)"
          @mouseleave="$emit('hover-node', null)"
        >{{ s.tooltip?.title || s.text }}</div>
        <div v-if="hiddenCount > 0" class="hidden-count">+{{ hiddenCount }} more</div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const store = useCalendarStore()

const props = defineProps({
  dateStr: String,
  currentMonth: Number,
  schedules: { type: Array, default: () => [] },
  activeTooltipId: String,
  isToday: Boolean,
  isSelected: Boolean,
  hiddenTracks: { type: Object, default: () => new Set() },
  dimmedNodeIds: { type: Object, default: () => new Set() },
})

const emit = defineEmits(['cell-click', 'add-schedule', 'toggle-tooltip', 'edit-schedule', 'delete-schedule', 'day-detail', 'hover-node', 'open-ai-modal'])

const parsed = computed(() => {
  const [y, m, d] = props.dateStr.split('-').map(Number)
  return { y, m, d, date: new Date(y, m - 1, d) }
})
const dayNum         = computed(() => parsed.value.d)
const dayOfWeek      = computed(() => parsed.value.date.getDay())
const isCurrentMonth = computed(() => parsed.value.m === props.currentMonth)

const holidayName = computed(() => store.getHoliday?.(props.dateStr) || null)
const isHoliday   = computed(() => !!holidayName.value)

const filteredSchedules = computed(() =>
  props.schedules.filter(s => !props.hiddenTracks?.has?.(s.track))
)
const labelSchedules = computed(() => filteredSchedules.value.filter(s => s.text || s.tooltip?.title))

const MAX_VISIBLE = 3
const visibleSchedules = computed(() => labelSchedules.value.slice(0, MAX_VISIBLE))
const hiddenCount      = computed(() => Math.max(0, labelSchedules.value.length - MAX_VISIBLE))

function trackColor(id) { return store.getTrackById?.(id)?.color || 'var(--text-primary)' }
function handleCellClick() { emit('toggle-tooltip', null); emit('cell-click', props.dateStr); }
function handleDblClick() { emit('day-detail', props.dateStr) }
</script>

<style scoped>
.calendar-cell {
  position: relative; border-bottom: 1px solid var(--border); border-right: 1px solid var(--border);
  display: flex; flex-direction: column; padding: 8px; background: var(--bg-base);
  transition: background 0.1s; cursor: pointer; overflow: visible; min-height: 80px;
}
.calendar-cell:nth-child(7n) { border-right: none; }
.calendar-cell:hover { background: rgba(0,0,0,0.015); }
.theme-dark .calendar-cell:hover { background: rgba(255,255,255,0.02); }
.calendar-cell:hover .btn-add-schedule { opacity: 1; }

.cell--today { background: var(--bg-base) !important; outline: 2px solid var(--text-primary); outline-offset: -2px; z-index: 5; }
.wrapper--today { display: flex; align-items: center; gap: 6px; }
.date--today {
  color: var(--bg-base) !important; background: var(--text-primary) !important;
  border-radius: 4px; width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center; font-weight: 900 !important;
}
.today-tag {
  font-size: 9px; font-weight: 900;
  color: var(--bg-base); background: var(--text-primary);
  border: 1px solid var(--text-primary); border-radius: 4px; padding: 2px 6px; letter-spacing: 0.1em;
}

.cell--selected { background: rgba(0,0,0,0.03) !important; outline: 2px solid var(--text-primary); outline-offset: -2px; }
.theme-dark .cell--selected { background: rgba(255,255,255,0.04) !important; }
.cell--sat .date-label:not(.date--holiday) { color: #2563eb !important; font-weight: 800; }
.cell--sun .date-label:not(.date--holiday) { color: #dc2626 !important; font-weight: 800; }
.date--holiday { color: #dc2626 !important; font-weight: 900 !important; }
.holiday-name { font-size: 10px; font-weight: 800; color: #dc2626; font-family: 'Escoredream', sans-serif; margin-top: 2px; }
.cell--other-month { background: var(--bg-surface) !important; cursor: default; }
.cell--other-month:hover { background: var(--bg-surface) !important; }
.cell--other-month .date-label { opacity: 0.3; font-weight: 600; }
.cell--other-month .btn-add-schedule { display: none; }

.cell-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.date-label { font-size: 13px; font-weight: 700; color: var(--text-primary); font-family: 'Escoredream', sans-serif; line-height: 1; min-width: 22px; text-align: center; }

.btn-add-schedule {
  width: 20px; height: 20px; border-radius: 4px;
  background: transparent; color: var(--text-primary);
  border: 1px solid var(--text-primary); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; opacity: 0; transition: all 0.1s;
}
.btn-add-schedule:hover { background: var(--text-primary); color: var(--bg-base); }

.label-cluster { display: flex; flex-direction: column; gap: 4px; margin-top: 2px; z-index: 25; position: relative; }
.schedule-label-chip {
  font-size: 10px; font-weight: 800; padding: 4px 8px; border-radius: 4px;
  border: 1px solid var(--border); cursor: pointer; white-space: nowrap;
  font-family: 'Inter', sans-serif; width: 100%; box-sizing: border-box;
  overflow: hidden; text-overflow: ellipsis; text-align: left;
  background: var(--bg-elevated);
  transition: all 0.2s cubic-bezier(0.16,1,0.3,1);
}
.schedule-label-chip:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateY(-1px); }
.schedule-label-chip.is-dimmed { opacity: 0.15 !important; border-color: var(--border) !important; color: var(--text-muted) !important; filter: grayscale(1); pointer-events: none; }
.hidden-count { font-size: 10px; font-weight: 700; color: var(--text-muted); padding: 2px 8px; }

.chips-fade-enter-active { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.chips-fade-leave-active { transition: all 0.15s ease; }
.chips-fade-enter-from, .chips-fade-leave-to { opacity: 0; transform: translateY(-8px); }

@keyframes targetFlash {
  0% { background-color: var(--today-bg); box-shadow: inset 0 0 0 4px var(--accent); }
  100% { background-color: transparent; box-shadow: inset 0 0 0 0px transparent; }
}
:deep(.flash-target) { animation: targetFlash 1.2s ease-out; }
</style>