<template>
  <div class="edge-section">
    <h4 class="section-title">CONNECTIONS</h4>

    <div class="connector-row">
      <div class="connector-label">
        <span class="connector-badge">FROM</span>
        <span>어디서부터 이어질까요?</span>
      </div>
      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedParents.length">
          <span
            v-for="s in selectedParents" :key="s.id"
            class="tag-chip"
            :style="{ borderColor: trackColor(s.track), color: trackColor(s.track) }"
          >
            {{ s.tooltip?.title || s.text || '이름 없음' }}
            <button class="tag-remove" @click="removeParent(s.id)">×</button>
          </span>
        </div>
        <select class="connector-select" @change="addParent($event.target.value); $event.target.value = ''">
          <option value="">SELECT PREVIOUS NODE</option>
          <optgroup v-for="group in groupedParents" :key="group.trackId" :label="'── ' + group.trackName">
            <option
              v-for="s in group.schedules" :key="s.id" :value="s.id"
              :disabled="selectedParentIds.includes(s.id)"
            >
              {{ s.day }} │ {{ s.tooltip?.title || s.text || '이름 없음' }}
            </option>
          </optgroup>
        </select>
      </div>
    </div>

    <div class="connector-row mt-12">
      <div class="connector-label">
        <span class="connector-badge">TO</span>
        <span>어디로 이어질까요?</span>
      </div>
      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedChildren.length">
          <span
            v-for="s in selectedChildren" :key="s.id"
            class="tag-chip"
            :style="{ borderColor: trackColor(s.track), color: trackColor(s.track) }"
          >
            {{ s.tooltip?.title || s.text || '이름 없음' }}
            <button class="tag-remove" @click="removeChild(s.id)">×</button>
          </span>
        </div>
        <select class="connector-select" @change="addChild($event.target.value); $event.target.value = ''">
          <option value="">SELECT NEXT NODE</option>
          <optgroup v-for="group in groupedChildren" :key="group.trackId" :label="'── ' + group.trackName">
            <option
              v-for="s in group.schedules" :key="s.id" :value="s.id"
              :disabled="selectedChildIds.includes(s.id)"
            >
              {{ s.day }} │ {{ s.tooltip?.title || s.text || '이름 없음' }}
            </option>
          </optgroup>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  parentIds: { type: Array, default: () => [] },
  childIds:  { type: Array, default: () => [] },
  parents:   { type: Array, default: () => [] },
  children:  { type: Array, default: () => [] }
})
const emit = defineEmits(['update:parentIds', 'update:childIds'])

const store = useCalendarStore()
const trackColor = (id) => store.getTrackById(id)?.color || 'var(--text-primary)'

const selectedParents  = computed(() => props.parentIds.map(id => store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedChildren = computed(() => props.childIds.map(id =>  store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedParentIds = computed(() => props.parentIds)
const selectedChildIds  = computed(() => props.childIds)

function addParent(id) { if (!id || props.parentIds.includes(id)) return; emit('update:parentIds', [...props.parentIds, id]) }
function removeParent(id) { emit('update:parentIds', props.parentIds.filter(x => x !== id)) }
function addChild(id) { if (!id || props.childIds.includes(id)) return; emit('update:childIds', [...props.childIds, id]) }
function removeChild(id) { emit('update:childIds', props.childIds.filter(x => x !== id)) }

function groupByTrack(list) {
  const userTracks = [...store.tracks].sort((a, b) => a.index - b.index)
  const hlTracks   = store.HIGHLIGHT_TRACKS || []
  const allTracks  = [...userTracks, ...hlTracks]
  return allTracks
    .map(track => ({ trackId: track.id, trackName: track.name, schedules: list.filter(s => s.track === track.id).sort((a, b) => a.day.localeCompare(b.day)) }))
    .filter(g => g.schedules.length > 0)
}

const groupedParents  = computed(() => groupByTrack(props.parents))
const groupedChildren = computed(() => groupByTrack(props.children))
</script>

<style scoped>
.edge-section {
  padding: 20px;
  border: 2px solid var(--text-primary);
  border-radius: 0;
  background: var(--bg-surface);
  display: flex; flex-direction: column; gap: 16px;
  box-shadow: 4px 4px 0 var(--border);
}
.section-title {
  font-size: 14px; font-weight: 900; letter-spacing: 0.1em;
  color: var(--text-primary); padding-bottom: 12px;
  border-bottom: 2px solid var(--text-primary);
  font-family: 'Space Grotesk', 'Escoredream', sans-serif;
}
.mt-12 { margin-top: 12px; }
.connector-row   { display: flex; flex-direction: column; gap: 10px; }
.connector-label { display: flex; align-items: center; gap: 10px; font-size: 12px; font-weight: 700; color: var(--text-muted); }
.connector-badge { font-size: 10px; font-weight: 900; padding: 4px 8px; border-radius: 0; background: var(--text-primary); color: var(--bg-base); letter-spacing: 0.1em; }

.tag-select-wrap { display: flex; flex-direction: column; gap: 10px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 12px; font-weight: 800;
  padding: 6px 12px; border-radius: 0; border: 2px solid;
  background: transparent; white-space: nowrap;
}
.tag-remove { background: none; border: none; padding: 0; font-size: 14px; font-weight: 900; cursor: pointer; color: inherit; transition: 0.1s; }
.tag-remove:hover { transform: scale(1.3); }

/* 브루탈리즘 Select 디자인 */
.connector-select {
  width: 100%; background: var(--bg-surface); border: 2px solid var(--text-primary); border-radius: 0;
  color: var(--text-primary); font-size: 13px; font-weight: 800;
  padding: 12px 14px; outline: none; cursor: pointer; transition: 0.1s;
  box-shadow: 4px 4px 0 var(--text-primary);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23000' stroke-width='3'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  background-size: 12px;
}
:global(body.theme-dark) .connector-select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23fff' stroke-width='3'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
}
.connector-select:focus { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--text-primary); }
.connector-select:active { transform: translate(0, 0); box-shadow: 2px 2px 0 var(--text-primary); }

.connector-select option { background: var(--bg-base); color: var(--text-primary); font-weight: 700; }
.connector-select optgroup { background: var(--bg-surface); color: var(--text-muted); font-weight: 900; }
</style>