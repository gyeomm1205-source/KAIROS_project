<template>
  <div
    :id="`day-${dateStr}`"
    class="calendar-cell"
    :class="{
      'cell--today':         isToday,
      'cell--selected':      isSelected,
      'cell--sat':           dayOfWeek === 6,
      'cell--sun':           dayOfWeek === 0,
      'cell--holiday':       isHoliday,
      'cell--other-month':   !isCurrentMonth,
    }"
    @click.stop="handleCellClick"
    @dblclick.stop="$emit('day-detail', dateStr)"
  >
    <div class="cell-header">
      <div class="cell-header-left">
        <span class="date-label-wrapper" :class="{ 'wrapper--today': isToday }">
          <span class="date-label" :class="{ 'date--today': isToday, 'date--holiday': isHoliday }">{{ dayNum }}</span>
          <span v-if="isToday" class="today-tag">TODAY</span>
        </span>
        <span v-if="holidayName" class="holiday-name">{{ holidayName }}</span>
      </div>

      <button
        v-if="isCurrentMonth"
        class="btn-add-schedule"
        @click.stop="$emit('add-schedule', dateStr)"
      ><i class="fas fa-plus" /></button>
    </div>

    <Transition name="chips-fade">
      <div v-if="labelSchedules.length && isAllTracksHidden" class="label-cluster">
        <div
          v-for="s in labelSchedules" :key="s.id"
          class="schedule-label-chip"
          :style="{ color: trackColor(s.track), borderColor: trackColor(s.track), background: 'transparent' }"
          @click.stop="$emit('toggle-tooltip', s.id)"
          @mouseenter="$emit('hover-node', s)"
          @mouseleave="$emit('hover-node', null)"
        >{{ s.text }}</div>
      </div>
    </Transition>

    <CalendarNode
      v-for="s in schedules" :key="s.id"
      v-show="!hiddenTracks || !hiddenTracks.has(s.track)"
      :schedule="s"
      :is-active="activeTooltipId === s.id"
      :is-dimmed="dimmedNodeIds?.has(s.id)"
      @toggle-tooltip="$emit('toggle-tooltip', s.id)"
      @edit="$emit('edit-schedule', $event)"
      @delete="$emit('delete-schedule', $event)"
      @hover="$emit('hover-node', $event)"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import CalendarNode from './CalendarNode.vue'

const props = defineProps({
  dateStr:         { type: String,  required: true },
  currentMonth:    { type: Number,  required: true },
  schedules:       { type: Array,   default: () => [] },
  activeTooltipId: { type: String,  default: null },
  isToday:         { type: Boolean, default: false },
  isSelected:      { type: Boolean, default: false },
  hiddenTracks:    { type: Object,  default: () => new Set() },
  dimmedNodeIds:   { type: Object,  default: () => new Set() },
})

const emit = defineEmits(['cell-click', 'add-schedule', 'toggle-tooltip', 'edit-schedule', 'delete-schedule', 'day-detail', 'hover-node'])

const store = useCalendarStore()

const parsed = computed(() => { const [y, m, d] = props.dateStr.split('-').map(Number); return { y, m, d, date: new Date(y, m-1, d) } })
const dayNum         = computed(() => parsed.value.d)
const dayOfWeek      = computed(() => parsed.value.date.getDay())
const isCurrentMonth = computed(() => parsed.value.m === props.currentMonth)
const labelSchedules = computed(() => props.schedules.filter(s => s.text))

const holidayName = computed(() => store.getHoliday(props.dateStr))
const isHoliday = computed(() => !!holidayName.value)

function trackColor(id) { return store.getTrackById(id)?.color || 'var(--text-primary)' }
function handleCellClick() { emit('toggle-tooltip', null); emit('cell-click', props.dateStr) }

const totalTrackCount = computed(() => {
  const list = store.allTracks || []
  let count = list.length
  if (!list.some(t => t.id === 'hl_prompt' || t.name?.includes('프롬프트'))) count++
  if (!list.some(t => t.id === 'hl_blog' || t.name?.includes('블로그'))) count++
  return count
})
const isAllTracksHidden = computed(() => totalTrackCount.value > 0 && props.hiddenTracks.size >= totalTrackCount.value)
</script>

<style scoped>
.calendar-cell {
  position: relative; border-bottom: 1px solid var(--border); border-right:  1px solid var(--border);
  display: flex; flex-direction: column; padding: 8px; background: var(--bg-base);
  transition: background 0.1s; cursor: pointer; overflow: visible;
}
.calendar-cell:hover { background: var(--bg-hover); }
.calendar-cell:hover .btn-add-schedule { opacity: 1; }

/* Brutalism 투데이 강조 */
.cell--today { background: var(--bg-base) !important; border-top: 3px solid var(--text-primary); }
.wrapper--today { display: flex; align-items: center; gap: 6px; }
.date--today { 
  color: var(--bg-base) !important; 
  background: var(--text-primary) !important; 
  border-radius: 0; width: 24px; height: 24px; 
  display: flex; align-items: center; justify-content: center; 
  font-weight: 900 !important; 
}
.today-tag { 
  font-size: 9px; font-weight: 900; 
  color: var(--bg-base); background: var(--text-primary); 
  border: 1px solid var(--text-primary); 
  border-radius: 0; padding: 2px 6px; letter-spacing: 0.1em; 
}

.cell--selected { background: var(--bg-hover) !important; outline: 2px solid var(--text-primary); outline-offset: -2px; }
.cell--selected .date-label:not(.date--today) { color: var(--text-primary) !important; font-weight: 900 !important; }

/* ★ 토/일요일 색상 적용 (공휴일이 아닐 때만 파란색/빨간색 적용) */
.cell--sat .date-label:not(.date--holiday) { color: #2563eb !important; font-weight: 800; }
.cell--sun .date-label:not(.date--holiday) { color: #dc2626 !important; font-weight: 800; }

/* ★ 공휴일은 토/일 무관하게 무조건 빨간색 적용 */
.date--holiday { color: #dc2626 !important; font-weight: 900 !important; }
.holiday-name { font-size: 10px; font-weight: 800; color: #dc2626; font-family: 'Escoredream', sans-serif; letter-spacing: -0.04em; margin-top: 2px; }

.cell--other-month { background: var(--bg-surface) !important; cursor: default; }
.cell--other-month:hover { background: var(--bg-surface) !important; }
.cell--other-month .date-label { opacity: 0.3; font-weight: 600; }
.cell--other-month .holiday-name { opacity: 0.3; }
.cell--other-month .btn-add-schedule { display: none; }

.cell-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 5px; flex-shrink: 0; z-index: 20; position: relative; }
.cell-header-left { display: flex; align-items: baseline; gap: 6px; }

.date-label { font-size: 13px; font-weight: 700; color: var(--text-primary); font-family: 'Escoredream', sans-serif; line-height: 1; min-width: 22px; text-align: center; transition: color 0.1s; }

.btn-add-schedule { 
  width: 20px; height: 20px; border-radius: 0; 
  background: transparent; color: var(--text-primary); 
  border: 1px solid var(--text-primary); cursor: pointer; 
  display: flex; align-items: center; justify-content: center; 
  font-size: 10px; opacity: 0; transition: all 0.1s; 
}
.btn-add-schedule:hover { background: var(--text-primary); color: var(--bg-base); }

.label-cluster { display: flex; flex-direction: column; gap: 4px; margin-top: 2px; margin-bottom: 4px; z-index: 25; position: relative; flex-shrink: 0; }
.schedule-label-chip { 
  font-size: 10px; font-weight: 800; padding: 4px 8px; border-radius: 0; 
  border: 1px solid currentColor; cursor: pointer; white-space: nowrap; 
  font-family: 'Escoredream', sans-serif; width: 100%; box-sizing: border-box; 
  overflow: hidden; text-overflow: ellipsis; text-align: left; 
  transition: transform 0.1s ease, box-shadow 0.1s ease; 
}
.schedule-label-chip:hover { transform: translate(-2px, -2px); box-shadow: 2px 2px 0 currentColor; }

.chips-fade-enter-active { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.chips-fade-leave-active { transition: all 0.15s ease; }
.chips-fade-enter-from, .chips-fade-leave-to { opacity: 0; transform: translateY(-8px); }
</style>