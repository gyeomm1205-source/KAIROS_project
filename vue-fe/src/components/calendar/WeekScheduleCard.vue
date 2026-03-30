<template>
  <div class="week-card" :class="{ 'is-expanded': isExpanded }" :style="{ borderLeftColor: trackColor }"
    @mouseenter="onEnter" @mouseleave="onLeave">
    <div class="week-card-header">
      <span class="week-card-track" :style="{ color: trackColor }">
        <span class="track-dot" :style="{ background: trackColor }" />
        {{ trackName }}
      </span>
      <span class="week-card-time">{{ schedule.tooltip?.time }}</span>
    </div>
    <div ref="titleRef" class="week-card-title" :class="{ 'title-clamped': !isExpanded }">{{ schedule.tooltip?.title || schedule.text }}</div>
    <span v-if="isClamped && !isExpanded" class="clamp-hint">more</span>
    <div v-if="schedule.tooltip?.tags?.length" class="week-card-tags">
      <span v-for="(t, i) in schedule.tooltip.tags" :key="i" class="week-tag">{{ t }}</span>
    </div>
    <div class="week-card-actions">
      <button class="card-btn"              @click.stop="$emit('edit', schedule)"><i class="fas fa-edit" /></button>
      <button class="card-btn card-btn--del" @click.stop="$emit('delete', schedule.id)"><i class="fas fa-trash" /></button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({ schedule: { type: Object, required: true } })
defineEmits(['edit', 'delete'])

const store      = useCalendarStore()
const track      = computed(() => store.getTrackById(props.schedule.track))
const trackColor = computed(() => track.value?.color || '#6b7280')
const trackName  = computed(() => track.value?.name  || '')

const titleRef = ref(null)
const isClamped = ref(false)
const isExpanded = ref(false)

function checkClamp() {
  const el = titleRef.value
  if (el) isClamped.value = el.scrollHeight > el.clientHeight + 1
}
onMounted(() => nextTick(checkClamp))
watch(() => props.schedule, () => nextTick(checkClamp))

function onEnter() { if (isClamped.value) isExpanded.value = true }
function onLeave() { isExpanded.value = false }
</script>

<style scoped>
.week-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-left: 4px solid;
  border-radius: 10px;
  padding: 12px 14px 12px 16px;
  display: flex; flex-direction: column; gap: 7px;
  transition: background 0.15s, box-shadow 0.15s;
  position: relative;
  z-index: 2;
}
.week-card:hover {
  background: var(--bg-hover);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

/* 연결선 앵커 — 카드 상단 중앙, 크기를 키워서 눈에 잘 보이게 */
.week-card-header {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
}
.week-card-track {
  display: flex; align-items: center; gap: 6px;
  font-size: 10px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  letter-spacing: 0.03em;
}
.track-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.week-card-time {
  font-size: 10px; color: var(--text-faint);
  font-family: monospace;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 1px 6px; border-radius: 4px;
}

.week-card-title {
  font-size: 13px; font-weight: 700;
  color: var(--text-primary); line-height: 1.4;
  font-family: 'Escoredream', sans-serif;
  min-height: calc(13px * 1.4 * 3);
}
.title-clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.clamp-hint {
  font-size: 9px; font-weight: 700; color: var(--text-muted);
  letter-spacing: 0.05em; cursor: default; align-self: flex-end;
}
.week-card.is-expanded {
  position: relative; z-index: 10;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18);
}
.week-card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.week-tag {
  font-size: 9px; color: var(--text-faint);
  background: var(--bg-surface); border: 1px solid var(--border);
  padding: 2px 6px; border-radius: 4px;
}
.week-card-actions {
  display: flex; justify-content: flex-end; gap: 5px;
  margin-top: 3px;
  opacity: 0;
  transition: opacity 0.15s;
}
.week-card:hover .week-card-actions { opacity: 1; }

.card-btn {
  width: 26px; height: 26px; border: none; border-radius: 6px;
  background: var(--bg-hover); color: var(--text-muted);
  cursor: pointer; font-size: 10px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.card-btn:hover      { background: var(--accent); color: #fff; }
.card-btn--del:hover { background: var(--clr-danger); color: #fff; }
</style>
