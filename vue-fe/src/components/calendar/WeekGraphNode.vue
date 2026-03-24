<template>
  <div
    :id="`node-${schedule.id}`"
    class="week-node"
    :style="nodeStyle"
    @click.stop="$emit('select', schedule)"
    @dblclick.stop="$emit('open-ai-modal', schedule)"
    @mouseenter="$emit('hover', schedule)"
    @mouseleave="$emit('hover', null)"
  >
    <div class="week-node-dot" :class="{ 'is-dimmed': isDimmed }" :style="dotStyle" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  schedule: { type: Object, required: true },
  colIdx: { type: Number, required: true },
  laneY: { type: Number, required: true },
  isDimmed: { type: Boolean, default: false }
})

defineEmits(['hover', 'edit', 'select'])
const store = useCalendarStore()

const trackObj = computed(() => store.getTrackById(props.schedule.track))
const trackColor = computed(() => trackObj.value?.color || '#6b7280')

// ★ 7등분 동일 비율에 맞춘 수평 중앙 위치 계산
const leftPct = computed(() => {
  return ((props.colIdx + 0.5) / 7) * 100 + '%';
})

const nodeStyle = computed(() => ({
  position: 'absolute',
  top: props.laneY + 'px',
  left: leftPct.value,
  transform: 'translate(-50%, -50%)',
  zIndex: 50,
  width: '32px',
  height: '32px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer'
}))

const dotStyle = computed(() => ({
  background: props.isDimmed ? 'transparent' : trackColor.value,
  borderColor: trackColor.value,
  borderWidth: '2.5px',
  borderStyle: 'solid',
  width: '10px', 
  height: '10px', 
  borderRadius: '50%',
  boxShadow: props.isDimmed ? 'none' : '0 0 0 2px var(--bg-surface)',
  transition: 'transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
  flexShrink: 0
}))
</script>

<style scoped>
.week-node:hover .week-node-dot { transform: scale(1.35); }
.week-node-dot.is-dimmed { border-color: var(--border-mid) !important; opacity: 0.3; }
</style>