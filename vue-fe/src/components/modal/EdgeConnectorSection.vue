<template>
  <div class="edge-section">
    <h4 class="section-title">
      <i class="fas fa-project-diagram" /> 일정 연결 설정
    </h4>

    <!-- 이전 연결 -->
    <div class="connector-row">
      <div class="connector-label">
        <span class="connector-badge connector-badge--in">이전</span>
        <span>어디서부터 이어질까요?</span>
      </div>
      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedParents.length">
          <span
            v-for="s in selectedParents" :key="s.id"
            class="tag-chip"
            :style="{ borderColor: trackColor(s.track) + '80', background: trackColor(s.track) + '18', color: trackColor(s.track) }"
          >
            {{ s.tooltip?.title || s.text || '이름 없음' }}
            <button class="tag-remove" @click="removeParent(s.id)">×</button>
          </span>
        </div>
        <select class="connector-select" @change="addParent($event.target.value); $event.target.value = ''">
          <option value="">── 추가하기 ──</option>
          <optgroup v-for="group in groupedParents" :key="group.trackId" :label="'📌 ' + group.trackName">
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

    <!-- 다음 연결 -->
    <div class="connector-row">
      <div class="connector-label">
        <span class="connector-badge connector-badge--out">다음</span>
        <span>어디로 이어질까요?</span>
      </div>
      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedChildren.length">
          <span
            v-for="s in selectedChildren" :key="s.id"
            class="tag-chip"
            :style="{ borderColor: trackColor(s.track) + '80', background: trackColor(s.track) + '18', color: trackColor(s.track) }"
          >
            {{ s.tooltip?.title || s.text || '이름 없음' }}
            <button class="tag-remove" @click="removeChild(s.id)">×</button>
          </span>
        </div>
        <select class="connector-select" @change="addChild($event.target.value); $event.target.value = ''">
          <option value="">── 추가하기 ──</option>
          <optgroup v-for="group in groupedChildren" :key="group.trackId" :label="'📌 ' + group.trackName">
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

const trackColor = (id) => store.getTrackById(id)?.color || '#6b7280'

// 선택된 일정 객체 목록
const selectedParents  = computed(() => props.parentIds.map(id => store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedChildren = computed(() => props.childIds.map(id =>  store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedParentIds = computed(() => props.parentIds)
const selectedChildIds  = computed(() => props.childIds)

function addParent(id) {
  if (!id || props.parentIds.includes(id)) return
  emit('update:parentIds', [...props.parentIds, id])
}
function removeParent(id) {
  emit('update:parentIds', props.parentIds.filter(x => x !== id))
}
function addChild(id) {
  if (!id || props.childIds.includes(id)) return
  emit('update:childIds', [...props.childIds, id])
}
function removeChild(id) {
  emit('update:childIds', props.childIds.filter(x => x !== id))
}

function groupByTrack(list) {
  // 일반 트랙 + 형광펜 트랙 모두 포함
  const userTracks = [...store.tracks].sort((a, b) => a.index - b.index)
  const hlTracks   = store.HIGHLIGHT_TRACKS || []
  const allTracks  = [...userTracks, ...hlTracks]
  return allTracks
    .map(track => ({
      trackId:   track.id,
      trackName: track.isHighlight ? `✦ ${track.name}` : track.name,
      schedules: list.filter(s => s.track === track.id).sort((a, b) => a.day.localeCompare(b.day))
    }))
    .filter(g => g.schedules.length > 0)
}

const groupedParents  = computed(() => groupByTrack(props.parents))
const groupedChildren = computed(() => groupByTrack(props.children))
</script>

<style scoped>
.edge-section {
  padding: 14px;
  border: 1.5px solid var(--modal-border, var(--border));
  border-radius: 12px;
  background: var(--modal-input-bg, var(--bg-elevated));
  display: flex; flex-direction: column; gap: 14px;
}
.section-title {
  font-size: 12px; font-weight: 700;
  color: var(--accent);
  padding-bottom: 10px;
  border-bottom: 1px solid var(--modal-divider, var(--border));
  display: flex; align-items: center; gap: 6px;
  font-family: 'Escoredream', sans-serif;
}
.connector-row   { display: flex; flex-direction: column; gap: 8px; }
.connector-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 11px; color: var(--text-muted);
  font-family: 'Escoredream', sans-serif;
}
.connector-badge {
  font-size: 9px; font-weight: 700;
  padding: 2px 7px; border-radius: 999px; flex-shrink: 0;
}
.connector-badge--in  { background: rgba(52,211,153,0.15); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.connector-badge--out { background: rgba(244,114,182,0.15); color: #f472b6; border: 1px solid rgba(244,114,182,0.3); }

/* 태그 + 셀렉트 묶음 */
.tag-select-wrap { display: flex; flex-direction: column; gap: 6px; }

.tag-list {
  display: flex; flex-wrap: wrap; gap: 5px;
}
.tag-chip {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 600;
  padding: 3px 8px 3px 10px; border-radius: 999px;
  border: 1.5px solid;
  font-family: 'Escoredream', sans-serif;
  white-space: nowrap;
}
.tag-remove {
  background: none; border: none; padding: 0;
  font-size: 13px; line-height: 1;
  cursor: pointer; color: inherit; opacity: 0.7;
}
.tag-remove:hover { opacity: 1; }

.connector-select {
  width: 100%;
  background: var(--modal-input-focus-bg, var(--bg-surface));
  border: 1.5px solid var(--modal-border, var(--border));
  color: var(--text-primary);
  font-size: 12px; font-weight: 500;
  font-family: 'Escoredream', sans-serif;
  border-radius: 9px; padding: 8px 28px 8px 10px;
  outline: none; cursor: pointer;
  transition: border-color 0.15s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 12px;
}
.connector-select:focus { border-color: var(--accent); }
.connector-select option   { background: var(--modal-bg, var(--bg-surface)); color: var(--text-primary); }
.connector-select optgroup { background: var(--modal-input-bg, var(--bg-elevated)); color: var(--text-muted); }
</style>
