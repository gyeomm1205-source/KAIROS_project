<template>
  <div
    :id="`node-${schedule.id}`"
    class="schedule-dot"
    :style="dotStyle"
    @click.stop="$emit('toggle-tooltip')"
  >
    <NodeTooltip
      v-if="schedule.tooltip"
      :visible="isActive"
      :tooltip="schedule.tooltip"
      @edit="$emit('edit', schedule)"
      @delete="$emit('delete', schedule.id)"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import NodeTooltip from './NodeTooltip.vue'

// ★ 캔버스와 동일한 위치 & 간격 변수 설정
const BASE_BOTTOM = 3
const REGULAR_SP  = 13   // 일반 트랙 간격
const HL_SP       = 24   // 형광펜 트랙 간격

const props = defineProps({
  schedule: { type: Object,  required: true },
  isActive: { type: Boolean, default: false }
})
defineEmits(['toggle-tooltip', 'edit', 'delete'])

const store     = useCalendarStore()
const trackInfo = computed(() => store.getTrackById(props.schedule.track))

// 형광펜 트랙 판별 함수
const isHL = (t) => t && (t.isHighlight || t.id?.startsWith('hl_') || t.name?.includes('프롬프트') || t.name?.includes('블로그'))

// 누적 오프셋 계산
const trackOffset = computed(() => {
  const sortedTracks = [...store.allTracks].sort((a, b) => (a.index || 0) - (b.index || 0))
  let offset = 0
  for (const t of sortedTracks) {
    if (t.id === props.schedule.track) return offset
    offset += isHL(t) ? HL_SP : REGULAR_SP
  }
  return offset
})

function hexToRgba(hex, a) {
  if (!hex) return 'rgba(129,140,248,0.3)'
  const c = parseInt(hex.replace('#',''), 16)
  return `rgba(${(c>>16)&255},${(c>>8)&255},${c&255},${a})`
}

const dotStyle = computed(() => {
  const color = trackInfo.value?.color || '#818cf8'
  const isHighlight = trackInfo.value ? isHL(trackInfo.value) : false
  
  return {
    backgroundColor: color,
    // ★ 형광펜 트랙일 경우 3px 더 위로 올려줌!
    bottom: `${BASE_BOTTOM + trackOffset.value + (isHighlight ? 3 : 0)}px`,
    zIndex: props.isActive ? 100 : 20,
    boxShadow: props.isActive ? `0 0 0 4px ${hexToRgba(color, 0.3)}` : 'none'
  }
})
</script>

<style scoped>
.schedule-dot {
  position: absolute; left: 50%;
  transform: translateX(-50%);
  width: 14px; height: 14px;
  border-radius: 50%;
  border: 3px solid var(--node-border, var(--bg-surface));
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, bottom 0.3s ease;
  z-index: 50;
  overflow: visible;
}
.schedule-dot:hover { transform: translateX(-50%) scale(1.25); }
</style>