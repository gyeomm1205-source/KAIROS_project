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
          :style="{ color: trackColor(s.track), borderColor: trackColor(s.track)+'50', background: trackColor(s.track)+'18' }"
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

function trackColor(id) { return store.getTrackById(id)?.color || '#6b7280' }
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
  position: relative;
  border-bottom: 1px solid var(--border);
  border-right:  1px solid var(--border);
  display: flex; flex-direction: column;
  padding: 8px 8px 8px;
  background: var(--bg-surface);
  transition: background 0.15s;
  cursor: pointer;
  overflow: visible;
  /* ★ 핵심: z-index를 제거하여 배경이 캔버스를 덮지 못하게 함 */
}

.calendar-cell:hover { background: var(--bg-elevated); }
.calendar-cell:hover .btn-add-schedule { opacity: 1; }

.cell--today { background: rgba(168, 85, 247, 0.04) !important; }
.wrapper--today { display: flex; align-items: center; gap: 6px; }
.date--today {
  color: #fff !important; 
  background: linear-gradient(135deg, #a855f7, #7e22ce); 
  border-radius: 50%; width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800 !important; box-shadow: 0 2px 6px rgba(126, 34, 206, 0.25);
}
.today-tag {
  font-size: 8px; font-weight: 800; color: #9333ea; 
  background: rgba(147, 51, 234, 0.1); border: 1px solid rgba(147, 51, 234, 0.25);
  border-radius: 4px; padding: 2px 5px; letter-spacing: 0.05em;
}

.cell--selected {
  background: rgba(59,130,246,0.07) !important;
  outline: 2px solid var(--accent); outline-offset: -2px;
}
.cell--selected .date-label:not(.date--today) { color: var(--accent) !important; font-weight: 800 !important; }

.cell--sat .date-label { color: var(--sat-color) !important; font-weight: 700; }
.cell--sun .date-label { color: var(--sun-color) !important; font-weight: 700; }

.date--holiday { color: #ef4444 !important; font-weight: 700; }
.holiday-name {
  font-size: 10px; font-weight: 700; color: #ef4444;
  font-family: 'Escoredream', sans-serif;
  letter-spacing: -0.04em;
  margin-top: 1px;
}

.cell--other-month {
  background: color-mix(in srgb, var(--bg-base) 60%, var(--bg-surface)) !important; cursor: default;
}
.cell--other-month:hover { background: color-mix(in srgb, var(--bg-base) 60%, var(--bg-surface)) !important; }
.cell--other-month .date-label { color: var(--text-faint) !important; font-weight: 400; }
.cell--other-month .date--holiday { color: rgba(239, 68, 68, 0.45) !important; }
.cell--other-month .holiday-name { color: rgba(239, 68, 68, 0.45); }
.cell--other-month .btn-add-schedule { display: none; }

/* ★ z-index: 20을 유지해 날짜 글씨가 캔버스 선 위로 선명하게 올라오도록 보장 */
.cell-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 5px; flex-shrink: 0; z-index: 20; position: relative;
}
.cell-header-left {
  display: flex; align-items: baseline; gap: 6px;
}

.date-label {
  font-size: 12px; font-weight: 600; color: var(--text-muted);
  font-family: 'Escoredream', sans-serif; line-height: 1;
  min-width: 22px; text-align: center;
}

.btn-add-schedule {
  width: 18px; height: 18px; border-radius: 4px;
  background: var(--bg-hover); color: var(--text-muted);
  border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 8px; opacity: 0; transition: all 0.15s;
}
.btn-add-schedule:hover { background: var(--accent); color: #fff; }

.label-cluster {
  display: flex; flex-direction: column; gap: 4px;
  margin-top: 2px; margin-bottom: 4px; 
  z-index: 25; position: relative; flex-shrink: 0;
}

.schedule-label-chip {
  font-size: 10px; font-weight: 600; padding: 4px 8px; 
  border-radius: 6px; border: 1px solid transparent; cursor: pointer;
  white-space: nowrap; font-family: 'Escoredream', sans-serif;
  width: 100%; box-sizing: border-box; overflow: hidden; 
  text-overflow: ellipsis; text-align: left; 
  transition: transform 0.15s ease, opacity 0.15s ease; transform-origin: center; 
}
.schedule-label-chip:hover { opacity: 0.9; transform: scale(1.02); }

.chips-fade-enter-active { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.chips-fade-leave-active { transition: all 0.15s ease; }
.chips-fade-enter-from, .chips-fade-leave-to { opacity: 0; transform: translateY(-8px); }
</style>