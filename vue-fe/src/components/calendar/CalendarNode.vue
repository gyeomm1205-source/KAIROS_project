<template>
  <div
    :id="`node-${schedule.id}`"
    class="schedule-dot"
    :class="{ 'is-dimmed': isDimmed }"
    :style="dotStyle"
    @click.stop="$emit('toggle-tooltip')"
    @mouseenter="$emit('hover', schedule)"
    @mouseleave="$emit('hover', null)"
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

const BASE_BOTTOM = 3
const REGULAR_SP  = 13   
const HL_SP       = 24   

const props = defineProps({
  schedule: { type: Object,  required: true },
  isActive: { type: Boolean, default: false },
  isDimmed: { type: Boolean, default: false } // ★ 추가
})
defineEmits(['toggle-tooltip', 'edit', 'delete', 'hover'])

const store     = useCalendarStore()
const trackInfo = computed(() => store.getTrackById(props.schedule.track))
const isHL = (t) => t && (t.isHighlight || t.id?.startsWith('hl_') || t.name?.includes('프롬프트') || t.name?.includes('블로그'))

const trackOffset = computed(() => {
  const sortedTracks = [...store.allTracks].sort((a,b) => {
    if(a.isHighlight && !b.isHighlight) return 1;
    if(!a.isHighlight && b.isHighlight) return -1;
    return a.index - b.index
  })
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
  border: 3.5px solid var(--node-border, var(--bg-surface)); /* 테두리 강화 */
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s, box-shadow 0.2s, bottom 0.3s ease;
  z-index: 50;
  overflow: visible;
}
.schedule-dot:hover { transform: translateX(-50%) scale(1.25); }

/* ★ 포커스 모드일 때 어둡게 처리 */
.schedule-dot.is-dimmed {
  opacity: 0.15 !important;
}
</style>