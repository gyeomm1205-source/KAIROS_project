<template>
  <div class="track-legend" @mouseleave="$emit('hover-track', null)">

    <button
      class="legend-btn legend-btn--all"
      :class="{ 'is-off': !isAllVisible }"
      @click="toggleAll"
      @mouseenter="$emit('hover-track', null)"
      title="전체 표시 / 숨김"
    >
      <span class="all-dots">
        <span
          v-for="t in displayTracks.slice(0, 4)"
          :key="t.id"
          class="all-dot"
          :style="{ background: t.color }"
        />
      </span>
      <span>ALL</span>
    </button>

    <div class="legend-sep" />

    <template v-for="(track, i) in displayTracks" :key="track.id">
      <div
        v-if="track.isHighlight && (i === 0 || !displayTracks[i-1].isHighlight)"
        class="legend-sep legend-sep--hl"
        title="형광펜 트랙"
      />
      <button
        class="legend-btn"
        :class="{
          'is-off':  hiddenSet.has(track.id),
          'is-sys':  track.isHighlight
        }"
        :title="(track.isHighlight ? '형광펜 · ' : '') + track.name"
        @click="toggle(track.id)"
        @mouseenter="$emit('hover-track', track.id)"
      >
        <span
          v-if="!track.isHighlight"
          class="legend-dot"
          :style="{
            background:  hiddenSet.has(track.id) ? 'transparent' : track.color,
            borderColor: track.color,
            boxShadow:   hiddenSet.has(track.id) ? 'none' : `0 0 6px ${track.color}55`
          }"
        />
        <span
          v-else
          class="legend-hl-bar"
          :style="{
            background: hiddenSet.has(track.id) ? 'transparent' : track.color,
            borderColor: track.color,
            boxShadow:   hiddenSet.has(track.id) ? 'none' : `0 0 4px ${track.color}66`
          }"
        />
        <span class="legend-name">{{ track.name }}</span>
      </button>
    </template>

  </div>
</template>

<script>
import { ref } from 'vue'
// 메모리 보존: 패널을 닫아도 선택했던 필터 상태가 초기화되지 않게 유지
const globalHiddenSet = ref(new Set())
</script>

<script setup>
import { computed, onMounted } from 'vue'

const props = defineProps({
  hiddenTracks: { type: Object, default: () => new Set() },
  visibleTracks: { type: Array, default: () => [] } // ★ 현재 화면/달에 활성화된 트랙 배열
})

const emit = defineEmits(['update:hiddenTracks', 'hover-track'])

const hiddenSet = globalHiddenSet

// 넘겨받은 동적 트랙 리스트를 화면 출력용으로 사용
const displayTracks = computed(() => props.visibleTracks)

onMounted(() => {
  emit('update:hiddenTracks', new Set(hiddenSet.value))
})

const isAllVisible = computed(() => hiddenSet.value.size === 0)

function toggle(id) {
  const next = new Set(hiddenSet.value)
  if (next.has(id)) next.delete(id)
  else              next.add(id)
  
  hiddenSet.value = next
  emit('update:hiddenTracks', new Set(next))
}

function toggleAll() {
  if (isAllVisible.value) {
    hiddenSet.value = new Set(displayTracks.value.map(t => t.id))
  } else {
    hiddenSet.value = new Set()
  }
  emit('update:hiddenTracks', new Set(hiddenSet.value))
}
</script>

<style scoped>
.track-legend {
  display: flex; align-items: center; gap: 3px; flex-wrap: wrap;
  min-width: 0;
}

.legend-sep {
  width: 1px; height: 16px;
  background: var(--border); flex-shrink: 0; margin: 0 3px;
}

.legend-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 9px; border-radius: 999px;
  background: none; border: 1px solid transparent;
  font-size: 11px; font-weight: 600; color: var(--text-muted);
  cursor: pointer; transition: all 0.15s;
  font-family: 'Escoredream', sans-serif; white-space: nowrap;
}
.legend-btn:hover {
  background: var(--bg-elevated);
  border-color: var(--border);
  color: var(--text-primary);
}
.legend-btn--all {
  background: var(--bg-elevated);
  border-color: var(--border);
  color: var(--text-secondary);
  font-weight: 800; letter-spacing: 0.04em; font-size: 10px;
}
.legend-btn--all:hover { border-color: var(--border-mid); }

.legend-btn.is-off {
  opacity: 0.38;
}
.legend-btn.is-off .legend-name { text-decoration: line-through; }

.all-dots { display: flex; align-items: center; }
.all-dot {
  width: 6px; height: 6px; border-radius: 50%;
  margin-right: -2px; border: 1px solid var(--bg-elevated);
  flex-shrink: 0;
}

.legend-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 2px solid; flex-shrink: 0;
  transition: background 0.15s, box-shadow 0.15s;
}

.legend-hl-bar {
  width: 18px; height: 4px; border-radius: 2px;
  border: 1px solid; flex-shrink: 0;
  transition: background 0.15s, box-shadow 0.15s;
}

.legend-sep--hl {
  width: 1px; height: 16px;
  background: var(--border); flex-shrink: 0; margin: 0 3px;
  opacity: 0.7;
}
</style>