<template>
  <div class="track-legend" @mouseleave="$emit('hover-track', null)">

    <div class="grid-cell col-all" style="grid-column: 1; grid-row: 1;">
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
    </div>

    <div class="grid-cell" style="grid-column: 2; grid-row: 1;" v-if="userTracks[0]">
      <button
        class="legend-btn"
        :class="{ 'is-off': hiddenSet.has(userTracks[0].id) }"
        @click="toggle(userTracks[0].id)"
        @mouseenter="$emit('hover-track', userTracks[0].id)"
      >
        <span class="legend-dot" :style="getDotStyle(userTracks[0])" />
        <span class="legend-name">{{ userTracks[0].name }}</span>
      </button>
    </div>

    <div class="grid-cell" style="grid-column: 3; grid-row: 1;" v-if="userTracks[1]">
      <button
        class="legend-btn"
        :class="{ 'is-off': hiddenSet.has(userTracks[1].id) }"
        @click="toggle(userTracks[1].id)"
        @mouseenter="$emit('hover-track', userTracks[1].id)"
      >
        <span class="legend-dot" :style="getDotStyle(userTracks[1])" />
        <span class="legend-name">{{ userTracks[1].name }}</span>
      </button>
    </div>


    <div class="grid-cell" style="grid-column: 2; grid-row: 2;" v-if="userTracks[2]">
      <button
        class="legend-btn"
        :class="{ 'is-off': hiddenSet.has(userTracks[2].id) }"
        @click="toggle(userTracks[2].id)"
        @mouseenter="$emit('hover-track', userTracks[2].id)"
      >
        <span class="legend-dot" :style="getDotStyle(userTracks[2])" />
        <span class="legend-name">{{ userTracks[2].name }}</span>
      </button>
    </div>

    <div class="grid-cell" style="grid-column: 3; grid-row: 2;" v-if="userTracks[3]">
      <button
        class="legend-btn"
        :class="{ 'is-off': hiddenSet.has(userTracks[3].id) }"
        @click="toggle(userTracks[3].id)"
        @mouseenter="$emit('hover-track', userTracks[3].id)"
      >
        <span class="legend-dot" :style="getDotStyle(userTracks[3])" />
        <span class="legend-name">{{ userTracks[3].name }}</span>
      </button>
    </div>


    <div class="grid-cell col-all" style="grid-column: 1; grid-row: 3;" v-if="sysTracks.length > 0">
      <div class="legend-sep legend-sep--hl" title="형광펜 트랙" />
    </div>

    <div class="grid-cell" style="grid-column: 2; grid-row: 3;" v-if="sysTracks[0]">
      <button
        class="legend-btn is-sys"
        :class="{ 'is-off': hiddenSet.has(sysTracks[0].id) }"
        @click="toggle(sysTracks[0].id)"
        @mouseenter="$emit('hover-track', sysTracks[0].id)"
      >
        <span class="legend-hl-bar" :style="getHlStyle(sysTracks[0])" />
        <span class="legend-name">{{ sysTracks[0].name }}</span>
      </button>
    </div>

    <div class="grid-cell" style="grid-column: 3; grid-row: 3;" v-if="sysTracks[1]">
      <button
        class="legend-btn is-sys"
        :class="{ 'is-off': hiddenSet.has(sysTracks[1].id) }"
        @click="toggle(sysTracks[1].id)"
        @mouseenter="$emit('hover-track', sysTracks[1].id)"
      >
        <span class="legend-hl-bar" :style="getHlStyle(sysTracks[1])" />
        <span class="legend-name">{{ sysTracks[1].name }}</span>
      </button>
    </div>

  </div>
</template>

<script>
import { ref } from 'vue'
const globalHiddenSet = ref(new Set())
</script>

<script setup>
import { computed, onMounted } from 'vue'

const props = defineProps({
  hiddenTracks: { type: Object, default: () => new Set() },
  visibleTracks: { type: Array, default: () => [] } 
})

const emit = defineEmits(['update:hiddenTracks', 'hover-track'])

const hiddenSet = globalHiddenSet
const displayTracks = computed(() => props.visibleTracks)

// 트랙을 종류별로 분리하여 그리드 배치를 용이하게 함
const userTracks = computed(() => displayTracks.value.filter(t => !t.isHighlight))
const sysTracks = computed(() => displayTracks.value.filter(t => t.isHighlight))

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

function getDotStyle(track) {
  const isOff = hiddenSet.value.has(track.id)
  return {
    background: isOff ? 'transparent' : track.color,
    borderColor: track.color,
    boxShadow: isOff ? 'none' : `0 0 6px ${track.color}55`
  }
}

function getHlStyle(track) {
  const isOff = hiddenSet.value.has(track.id)
  return {
    background: isOff ? 'transparent' : track.color,
    borderColor: track.color,
    boxShadow: isOff ? 'none' : `0 0 4px ${track.color}66`
  }
}
</script>

<style scoped>
/* ★ 핵심: 트랙 이름 길이에 따라 동적으로 창이 넓어지는 3단 Grid 레이아웃 */
.track-legend {
  display: grid;
  grid-template-columns: max-content max-content max-content;
  row-gap: 6px;
  column-gap: 6px;
  align-items: center;
  width: max-content; /* 내부 내용 길이에 맞춰 전체 컨테이너 너비 확장 */
}

.grid-cell {
  display: flex;
  align-items: center;
}

.col-all {
  justify-content: flex-end;
  gap: 4px;
  height: 100%;
}

.legend-sep {
  width: 1px; height: 16px;
  background: var(--border); flex-shrink: 0; margin: 0 4px;
}
.legend-sep--hl {
  opacity: 0.7;
}

.legend-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 999px;
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
</style>