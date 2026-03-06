<template>
  <div
    :id="`node-${schedule.id}`"
    class="week-node"
    :class="{ 'week-node--active': isActive, 'is-dimmed': isDimmed }"
    :style="nodeStyle"
    @click.stop="$emit('toggle-tooltip', schedule.id)"
    @mouseenter="$emit('hover', schedule)"
    @mouseleave="$emit('hover', null)"
  >
    <div class="week-node-glow" :style="{ background: trackColor + '40' }" />
    <div class="week-node-dot"
      :style="{
        background: trackColor,
        borderColor: 'var(--node-border, var(--bg-surface))',
        boxShadow: isActive ? `0 0 0 3px ${trackColor}55` : `0 1px 4px ${trackColor}80`
      }"
    />

    <WeekNodeTooltip
      v-if="schedule.tooltip"
      :visible="isActive"
      :tooltip="schedule.tooltip"
      :track-color="trackColor"
      :col-idx="colIdx"
      :track-idx="trackIdx"
      :total-tracks="totalTracks"
      @edit="$emit('edit', schedule)"
      @delete="$emit('delete', schedule.id)"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { WEEK_TOP_MARGIN, WEEK_LANE_SPACING } from '@/composables/useCanvasLines'
import WeekNodeTooltip from './WeekNodeTooltip.vue'

const props = defineProps({
  schedule:    { type: Object,  required: true },
  colIdx:      { type: Number,  required: true },
  totalCols:   { type: Number,  default: 7 },    
  isActive:    { type: Boolean, default: false },
  totalTracks: { type: Number,  default: 5 },
  isDimmed:    { type: Boolean, default: false } // ★ 추가
})
defineEmits(['toggle-tooltip', 'edit', 'delete', 'hover'])

const store      = useCalendarStore()
const track      = computed(() => store.getTrackById(props.schedule.track))
const trackColor = computed(() => track.value?.color || '#6b7280')

const trackIdx = computed(() => {
  const sorted = [...store.allTracks].sort((a,b) => {
    if(a.isHighlight && !b.isHighlight) return 1;
    if(!a.isHighlight && b.isHighlight) return -1;
    return a.index - b.index
  })
  return sorted.findIndex(t => t.id === props.schedule.track)
})

const nodeStyle = computed(() => {
  const laneY   = WEEK_TOP_MARGIN + trackIdx.value * WEEK_LANE_SPACING
  const leftPct = ((props.colIdx + 0.5) / props.totalCols * 100).toFixed(3)
  return {
    position: 'absolute',
    left:     `${leftPct}%`,
    top:      `${laneY}px`,
    transform: 'translate(-50%, -50%)',
    zIndex:   props.isActive ? 100 : 10,
  }
})
</script>

<style scoped>
.week-node {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: opacity 0.2s; /* ★ 애니메이션 */
}

/* ★ 포커스 모드일 때 어둡게 처리 */
.week-node.is-dimmed {
  opacity: 0.15 !important;
}

.week-node-glow {
  position: absolute;
  width: 20px; height: 20px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}
.week-node:hover .week-node-glow,
.week-node--active .week-node-glow { opacity: 1; }

.week-node-dot {
  width: 14px; height: 14px;
  border-radius: 50%;
  border: 3.5px solid; /* 테두리 강화 */
  position: relative; z-index: 1;
  transition: transform 0.18s, box-shadow 0.18s;
}
.week-node:hover .week-node-dot { transform: scale(1.3); }
.week-node--active .week-node-dot { transform: scale(1.35); }
</style>