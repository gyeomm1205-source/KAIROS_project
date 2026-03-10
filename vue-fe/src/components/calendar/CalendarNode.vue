<template>
  <div
    :id="`node-${schedule.id}`"
    class="calendar-node"
    :style="nodeStyle"
    @click.stop="$emit('toggle-tooltip')"
    @mouseenter="$emit('hover', schedule)"
    @mouseleave="$emit('hover', null)"
  >
    <div class="schedule-dot" :class="{ 'is-active': isActive, 'is-dimmed': isDimmed }" :style="dotStyle" />
    
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

const props = defineProps({
  schedule: { type: Object, required: true },
  isActive: { type: Boolean, default: false },
  isDimmed: { type: Boolean, default: false }
})

defineEmits(['toggle-tooltip', 'edit', 'delete', 'hover'])

const store = useCalendarStore()

const trackObj = computed(() => store.getTrackById(props.schedule.track))
const trackColor = computed(() => trackObj.value?.color || '#6b7280')

const offsetBottom = computed(() => {
  const sorted = [...store.allTracks].sort((a,b) => {
    if(a.isHighlight && !b.isHighlight) return 1;
    if(!a.isHighlight && b.isHighlight) return -1;
    return a.index - b.index
  })
  
  let offset = 0;
  let currentIdx = null;
  for (const t of sorted) {
    if (currentIdx !== t.index) {
      if (currentIdx !== null) {
        const isHl = t.isHighlight || t.id?.startsWith('hl_') || t.name?.includes('프롬프트') || t.name?.includes('블로그')
        offset += isHl ? 24 : 13; 
      }
      currentIdx = t.index;
    }
    if (t.id === props.schedule.track) return offset;
  }
  return offset;
})

const nodeStyle = computed(() => {
  const isHl = trackObj.value?.isHighlight || trackObj.value?.name?.includes('프롬프트') || trackObj.value?.name?.includes('블로그')
  
  // ★ 캔버스 기준(10px)에서 달력 칸의 하단 테두리(1px) 두께를 뺀 9px이 정확한 DOM 좌표입니다.
  const targetY = 9 + offsetBottom.value + (isHl ? 3 : 0)
  
  return {
    position: 'absolute',
    left: '50%',
    // ★ X축은 가운데(-50%), Y축은 자기 높이의 절반(50%)을 내려서 히트박스의 정중앙이 선에 물리게 만듭니다.
    transform: 'translate(-50%, 50%)',
    bottom: targetY + 'px', 
    zIndex: props.isActive ? 100 : 50,
    width: '32px',
    height: '32px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    pointerEvents: 'auto'
  }
})

const dotStyle = computed(() => ({
  background: props.isDimmed ? 'transparent' : trackColor.value,
  borderColor: trackColor.value,
  borderWidth: '2.5px',
  borderStyle: 'solid',
  boxShadow: props.isDimmed ? 'none' : `0 0 0 2px var(--bg-surface)`
}))
</script>

<style scoped>
.calendar-node { 
  cursor: pointer; 
  border-radius: 50%;
}

.schedule-dot {
  width: 9px; height: 9px; 
  border-radius: 50%;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.2s;
  flex-shrink: 0;
}

/* ★ 확대 배율을 1.5에서 1.25로 줄여서 너무 커지는 현상 방지 */
.calendar-node:hover .schedule-dot, 
.schedule-dot.is-active { 
  transform: scale(1.25); 
}

.schedule-dot.is-dimmed { 
  border-color: var(--border-mid) !important; 
  opacity: 0.3; 
}
</style>