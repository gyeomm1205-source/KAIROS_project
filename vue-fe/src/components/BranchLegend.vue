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
            :style="{ background: t.color, borderColor: t.color }"
          />
        </span>
        <span>ALL</span>
      </button>
      <div class="legend-sep" />
    </div>

    <!-- NEW: Global Toggles for Connections -->
    <div class="grid-cell" style="grid-column: 2; grid-row: 1;">
      <button 
        class="legend-btn toggle-btn" 
        :class="{ 'is-active': store.showConnections }"
        @click="store.showConnections = !store.showConnections"
      >
        <i class="fas" :class="store.showConnections ? 'fa-link' : 'fa-unlink'" />
        <span>LINES {{ store.showConnections ? 'ON' : 'OFF' }}</span>
      </button>
    </div>
    <div class="grid-cell" style="grid-column: 3; grid-row: 1;">
      <button 
        class="legend-btn toggle-btn" 
        :class="{ 'is-active': !store.lowIntensityLines }"
        @click="store.lowIntensityLines = !store.lowIntensityLines"
      >
        <i class="fas" :class="store.lowIntensityLines ? 'fa-eye-slash' : 'fa-eye'" />
        <span>DIM {{ store.lowIntensityLines ? 'ON' : 'OFF' }}</span>
      </button>
    </div>

    <template v-for="(track, index) in userTracks" :key="track.id">
      <div class="grid-cell" :style="{ gridColumn: (index % 2) + 2, gridRow: Math.floor(index / 2) + 2 }">
        <button
          class="legend-btn"
          :class="{ 'is-off': hiddenSet.has(track.id) }"
          @click="toggle(track.id)"
          @mouseenter="$emit('hover-track', track.id)"
        >
          <span class="legend-dot" :style="getDotStyle(track)" />
          <span class="legend-name">{{ track.name }}</span>
        </button>
      </div>
    </template>

    <div class="grid-cell col-all" :style="{ gridColumn: 1, gridRow: Math.ceil(userTracks.length / 2) + 2 }" v-if="sysTracks.length > 0">
      <div class="legend-sep legend-sep--hl" title="시스템 트랙" />
    </div>

    <template v-for="(track, index) in sysTracks" :key="track.id">
      <div class="grid-cell" :style="{ gridColumn: (index % 2) + 2, gridRow: Math.ceil(userTracks.length / 2) + 2 + Math.floor(index / 2) }">
        <button
          class="legend-btn is-sys"
          :class="{ 'is-off': hiddenSet.has(track.id) }"
          @click="toggle(track.id)"
          @mouseenter="$emit('hover-track', track.id)"
        >
          <span class="legend-hl-bar" :style="getHlStyle(track)" />
          <span class="legend-name">{{ track.name }}</span>
        </button>
      </div>
    </template>

  </div>
</template>

<script>
import { ref } from 'vue'
const globalHiddenSet = ref(new Set())
</script>

<script setup>
import { computed, onMounted } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  hiddenTracks: { type: Object, default: () => new Set() },
  visibleTracks: { type: Array, default: () => [] } 
})

const emit = defineEmits(['update:hiddenTracks', 'hover-track'])
const store = useCalendarStore()

const hiddenSet = globalHiddenSet
const displayTracks = computed(() => props.visibleTracks)

const userTracks = computed(() => displayTracks.value.filter(t => !t.isHighlight))
const sysTracks = computed(() => displayTracks.value.filter(t => t.isHighlight))

onMounted(() => { emit('update:hiddenTracks', new Set(hiddenSet.value)) })

const isAllVisible = computed(() => hiddenSet.value.size === 0)

function toggle(id) {
  const next = new Set(hiddenSet.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  hiddenSet.value = next
  emit('update:hiddenTracks', new Set(next))
}

function toggleAll() {
  if (isAllVisible.value) hiddenSet.value = new Set(displayTracks.value.map(t => t.id))
  else hiddenSet.value = new Set()
  emit('update:hiddenTracks', new Set(hiddenSet.value))
}

function getDotStyle(track) {
  const isOff = hiddenSet.value.has(track.id)
  return { background: isOff ? 'transparent' : track.color, borderColor: track.color }
}

function getHlStyle(track) {
  const isOff = hiddenSet.value.has(track.id)
  return { background: isOff ? 'transparent' : track.color, borderColor: track.color }
}
</script>

<style scoped>
.track-legend {
  display: grid;
  grid-template-columns: max-content max-content max-content;
  row-gap: 8px; column-gap: 8px; align-items: center; width: max-content;
}

.grid-cell { display: flex; align-items: center; }
.col-all { justify-content: flex-end; gap: 6px; height: 100%; }

.legend-sep { width: 2px; height: 20px; background: var(--border); flex-shrink: 0; margin: 0 4px; }

.legend-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 6px 12px; border-radius: 0;
  background: transparent; border: 1px solid var(--border);
  font-size: 11px; font-weight: 800; color: var(--text-primary);
  cursor: pointer; transition: all 0.1s; letter-spacing: 0.05em; text-transform: uppercase;
}
.legend-btn:hover { background: var(--text-primary); color: var(--bg-base); }
.legend-btn:hover .legend-dot, .legend-btn:hover .legend-hl-bar { border-color: var(--bg-base) !important; }

.legend-btn--all { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); }
.legend-btn--all:hover { background: transparent; color: var(--text-primary); }

.legend-btn.is-off { opacity: 0.4; border-style: dashed; }
.legend-btn.is-off .legend-name { text-decoration: line-through; }

.toggle-btn { gap: 6px; border-radius: 40px; border-color: var(--border); color: var(--text-muted); padding: 4px 10px; }
.toggle-btn.is-active { border-color: var(--text-primary); color: var(--text-primary); border-style: solid; font-weight: 900; }
.toggle-btn i { font-size: 10px; }

.all-dots { display: flex; align-items: center; }
.all-dot { width: 8px; height: 8px; border-radius: 0; margin-right: -2px; border: 1px solid var(--bg-base); flex-shrink: 0; }

.legend-dot { width: 10px; height: 10px; border-radius: 0; border: 2px solid; flex-shrink: 0; transition: all 0.1s; }
.legend-hl-bar { width: 16px; height: 6px; border-radius: 0; border: 2px solid; flex-shrink: 0; transition: all 0.1s; }
</style>