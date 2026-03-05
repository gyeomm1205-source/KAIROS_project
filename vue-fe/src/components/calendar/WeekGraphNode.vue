<template>
  <!-- 트랙 레인 위의 클릭 가능한 노드 점 -->
  <div
    :id="`node-${schedule.id}`"
    class="week-node"
    :class="{ 'week-node--active': isActive }"
    :style="nodeStyle"
    @click.stop="$emit('toggle-tooltip', schedule.id)"
  >
    <!-- 글로우 링 -->
    <div class="week-node-glow" :style="{ background: trackColor + '40' }" />
    <!-- 실제 점 -->
    <div class="week-node-dot"
      :style="{
        background: trackColor,
        borderColor: 'var(--node-border, var(--bg-surface))',
        boxShadow: isActive ? `0 0 0 3px ${trackColor}55` : `0 1px 4px ${trackColor}80`
      }"
    />

    <!-- 툴팁 -->
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
  colIdx:      { type: Number,  required: true }, // 0~(totalCols-1)
  totalCols:   { type: Number,  default: 7 },     // 7 or 21
  isActive:    { type: Boolean, default: false },
  totalTracks: { type: Number,  default: 5 },
})
defineEmits(['toggle-tooltip', 'edit', 'delete'])

const store     = useCalendarStore()
const track     = computed(() => store.getTrackById(props.schedule.track))
const trackColor = computed(() => track.value?.color || '#6b7280')

// allTracks(사용자+시스템) 정렬에서 이 스케줄의 레인 인덱스
const trackIdx = computed(() => {
  const sorted = [...store.allTracks].sort((a, b) => a.index - b.index)
  return sorted.findIndex(t => t.id === props.schedule.track)
})

// graph zone 안에서의 absolute 위치
const nodeStyle = computed(() => {
  const laneY  = WEEK_TOP_MARGIN + trackIdx.value * WEEK_LANE_SPACING
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
  width: 13px; height: 13px;
  border-radius: 50%;
  border: 2.5px solid;
  position: relative; z-index: 1;
  transition: transform 0.18s, box-shadow 0.18s;
}
.week-node:hover .week-node-dot { transform: scale(1.3); }
.week-node--active .week-node-dot { transform: scale(1.35); }
</style>
