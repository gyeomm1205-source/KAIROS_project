<template>
  <div
    ref="cellRef"
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

    <Teleport to="body">
      <div v-if="hoverTooltip.visible" class="chip-hover-tooltip"
        :style="{
          left: hoverTooltip.x + 'px',
          top: hoverTooltip.y + 'px',
          borderColor: hoverTooltip.color,
          color: hoverTooltip.color,
          boxShadow: hoverTooltip.color.startsWith('#') ? `0 4px 16px ${hoverTooltip.color}40` : '0 4px 16px rgba(0,0,0,0.15)'
        }">
        {{ hoverTooltip.text }}
      </div>
    </Teleport>

    <Transition name="chips-fade">
      <div v-if="labelSchedules.length" class="label-cluster">
        <div
          v-for="s in visibleSchedules" :key="s.id"
          :id="'node-' + s.id"
          class="schedule-label-chip"
          :class="{ 'is-dimmed': dimmedNodeIds.has(s.id) }"
          :style="{ color: trackColor(s.track), borderColor: trackColor(s.track) }"
          @click.stop="onChipClick(s)"
          @dblclick.stop="onChipDblClick(s)"
          @mouseenter="onChipEnter(s, $event)"
          @mousemove="onChipMove($event)"
          @mouseleave="onChipLeave"
        >{{ s.tooltip?.title || s.text }}</div>
        <div v-if="hiddenCount > 0" class="hidden-count" @click.stop="openHiddenPopover">
          +{{ hiddenCount }} more
        </div>
      </div>
    </Transition>

    <Teleport to="body">
      <div v-if="hiddenPopover.visible" class="hidden-schedules-popover" :style="hiddenPopover.style" @click.stop>
        <div class="popover-header">
          <span class="popover-date">{{ dateStr }}</span>
          <button class="popover-close" @click="hiddenPopover.visible = false"><i class="fas fa-times" /></button>
        </div>
        <div class="popover-chips">
          <div
            v-for="s in hiddenSchedules" :key="s.id"
            class="schedule-label-chip popover-chip"
            :style="{ color: trackColor(s.track), borderColor: trackColor(s.track) }"
            @click="onPopoverChipClick(s)"
          >{{ s.tooltip?.title || s.text }}</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, inject, onBeforeUnmount } from 'vue'
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
const visibleSchedules  = computed(() => labelSchedules.value.slice(0, MAX_VISIBLE))
const hiddenCount       = computed(() => Math.max(0, labelSchedules.value.length - MAX_VISIBLE))
const hiddenSchedules   = computed(() => labelSchedules.value.slice(MAX_VISIBLE))

const cellRef = ref(null)
const popoverCloseSignal = inject('popoverCloseSignal', ref(0))

function trackColor(id) { return store.getTrackById?.(id)?.color || 'var(--text-primary)' }
function handleCellClick() { emit('toggle-tooltip', null); emit('cell-click', props.dateStr); }
function handleDblClick() { emit('day-detail', props.dateStr) }

// ── hidden-count 팝오버 ──
const POPOVER_WIDTH = 210
const hiddenPopover = ref({ visible: false, style: {} })

// 모달이 열릴 때 팝오버 닫기 (선언 후에 watch)
watch(popoverCloseSignal, () => { hiddenPopover.value.visible = false })
let closePopoverListener = null

function openHiddenPopover() {
  const rect = cellRef.value?.getBoundingClientRect()
  if (!rect) return

  const spaceRight = window.innerWidth - rect.right
  const spaceLeft  = rect.left
  const openRight  = spaceRight >= POPOVER_WIDTH || spaceRight >= spaceLeft

  const style = {
    position: 'fixed',
    top: rect.top + 'px',
    width: POPOVER_WIDTH + 'px',
    ...(openRight
      ? { left: rect.right + 4 + 'px' }
      : { left: rect.left - POPOVER_WIDTH - 4 + 'px' }),
  }
  hiddenPopover.value = { visible: true, style }

  nextTick(() => {
    closePopoverListener = () => { hiddenPopover.value.visible = false; closePopoverListener = null }
    document.addEventListener('click', closePopoverListener, { once: true })
  })
}

function onPopoverChipClick(s) {
  hiddenPopover.value.visible = false
  emit('edit-schedule', s)
}

let clickTimer = null
function onChipClick(s) {
  hiddenPopover.value.visible = false
  clickTimer = setTimeout(() => {
    clickTimer = null
    emit('edit-schedule', s)
  }, 220)
}
function onChipDblClick(s) {
  hiddenPopover.value.visible = false
  clearTimeout(clickTimer)
  clickTimer = null
  emit('open-ai-modal', s)
}

const hoverTooltip = ref({ visible: false, text: '', color: '', x: 0, y: 0 })
let hoverTimer = null

function onChipEnter(s, e) {
  emit('hover-node', s)
  const text = s.tooltip?.title || s.text
  const color = trackColor(s.track)
  const rect = e.currentTarget.getBoundingClientRect()
  hoverTimer = setTimeout(() => {
    hoverTooltip.value = { visible: true, text, color, x: e.clientX, y: rect.top }
  }, 1200)
}
function onChipMove(e) {
  hoverTooltip.value.x = e.clientX
  // y는 chip 상단에 고정
}
function onChipLeave() {
  clearTimeout(hoverTimer)
  hoverTimer = null
  hoverTooltip.value.visible = false
  emit('hover-node', null)
}
onBeforeUnmount(() => {
  clearTimeout(hoverTimer)
  clearTimeout(clickTimer)
  if (closePopoverListener) document.removeEventListener('click', closePopoverListener)
})
</script>

<style scoped>
.calendar-cell {
  position: relative; border-bottom: 1px solid var(--border); border-right: 1px solid var(--border);
  display: flex; flex-direction: column; padding: 8px; background: var(--bg-base);
  transition: background 0.1s; cursor: pointer; overflow: visible; min-height: 80px; min-width: 0;
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
.cell--sat .date-label:not(.date--holiday) { color: var(--clr-primary) !important; font-weight: 800; }
.cell--sun .date-label:not(.date--holiday) { color: var(--clr-danger) !important; font-weight: 800; }
.date--holiday { color: var(--clr-danger) !important; font-weight: 900 !important; }
.holiday-name { font-size: 10px; font-weight: 800; color: var(--clr-danger); font-family: 'Escoredream', sans-serif; margin-top: 2px; }
.cell--other-month { background: var(--bg-surface) !important; cursor: default; }
.cell--other-month:hover { background: var(--bg-surface) !important; }
.cell--other-month .date-label { opacity: 0.3; font-weight: 600; }
.cell--other-month .btn-add-schedule { display: none; }
.cell--other-month .schedule-label-chip { opacity: 0.5; filter: grayscale(0.2); }

.cell-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; height: 24px; }
.date-label { font-size: 13px; font-weight: 700; color: var(--text-primary); font-family: 'Escoredream', sans-serif; line-height: 1; min-width: 22px; text-align: center; }

.btn-add-schedule {
  width: 20px; height: 20px; border-radius: 6px;
  background: transparent; color: var(--text-primary);
  border: 1px solid var(--text-primary); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; opacity: 0; transition: all 0.1s;
}
.btn-add-schedule:hover { background: var(--text-primary); color: var(--bg-base); }

.label-cluster { display: flex; flex-direction: column; align-items: center; gap: 4px; margin-top: 2px; z-index: 25; position: relative; overflow: visible; width: 100%; }
.schedule-label-chip {
  font-size: 10px; font-weight: 800; padding: 4px 6px; border-radius: 4px;
  border: 1px solid var(--border); cursor: pointer; white-space: nowrap;
  font-family: 'Inter', sans-serif; width: 90%; max-width: 100%; box-sizing: border-box;
  overflow: hidden; text-overflow: ellipsis; text-align: left;
  background: var(--bg-elevated); min-width: 0;
  transition: all 0.2s cubic-bezier(0.16,1,0.3,1);
}
.schedule-label-chip:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateY(-1px); }
.schedule-label-chip.is-dimmed { opacity: 0.15 !important; border-color: var(--border) !important; color: var(--text-muted) !important; filter: grayscale(1); pointer-events: none; }
.hidden-count {
  font-size: 10px; font-weight: 700; color: var(--text-muted);
  padding: 2px 8px; cursor: pointer; border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}
.hidden-count:hover { color: var(--text-primary); background: var(--bg-hover); }

.chips-fade-enter-active { transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.chips-fade-leave-active { transition: all 0.15s ease; }
.chips-fade-enter-from, .chips-fade-leave-to { opacity: 0; transform: translateY(-8px); }

:global(.hidden-schedules-popover) {
  position: fixed; z-index: 9990; pointer-events: all;
  background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18); overflow: hidden;
  animation: popover-in 0.15s cubic-bezier(0.16,1,0.3,1);
}
@keyframes popover-in {
  from { opacity: 0; transform: scale(0.96) translateY(-4px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}
:global(.popover-header) {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px 6px; border-bottom: 1px solid var(--border);
}
:global(.popover-date) {
  font-size: 10px; font-weight: 900; letter-spacing: 0.08em;
  color: var(--text-muted); font-family: 'Inter', sans-serif;
}
:global(.popover-close) {
  background: transparent; border: none; color: var(--text-muted);
  font-size: 11px; cursor: pointer; padding: 2px 4px; line-height: 1;
  border-radius: 4px; transition: color 0.15s;
}
:global(.popover-close:hover) { color: var(--text-primary); }
:global(.popover-chips) {
  display: flex; flex-direction: column; align-items: center;
  gap: 4px; padding: 8px 0;
}
:global(.popover-chip) {
  width: 90% !important; max-width: 90% !important;
  cursor: pointer;
}

:global(.chip-hover-tooltip) {
  position: fixed; z-index: 9999; pointer-events: none;
  background: var(--bg-elevated); color: var(--text-primary);
  border: 1px solid var(--border); border-radius: 6px;
  padding: 6px 10px; font-size: 11px; font-weight: 800;
  font-family: 'Inter', sans-serif; line-height: 1.5;
  white-space: normal; word-break: break-word; max-width: 220px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  transform: translate(-50%, calc(-100% - 10px));
  animation: tooltip-in 0.15s cubic-bezier(0.16,1,0.3,1);
}
@keyframes tooltip-in {
  from { opacity: 0; transform: translate(-50%, calc(-100% - 4px)); }
  to   { opacity: 1; transform: translate(-50%, calc(-100% - 10px)); }
}

@keyframes targetFlash {
  0% { background-color: var(--today-bg); box-shadow: inset 0 0 0 4px var(--accent); }
  100% { background-color: transparent; box-shadow: inset 0 0 0 0px transparent; }
}
:deep(.flash-target) { animation: targetFlash 1.2s ease-out; }
</style>