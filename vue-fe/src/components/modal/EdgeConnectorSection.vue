<template>
  <div class="edge-section">

    <div class="connector-row">
      <div class="connector-label">
        <span class="connector-badge">FROM</span>
        <span>이전 일정</span>
      </div>
      
      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedParents.length">
          <span
            v-for="s in selectedParents" :key="s.id"
            class="tag-chip"
          >
            <span class="tag-color" :style="{ background: trackColor(s.track) }" />
            <span class="tag-text">{{ s.tooltip?.title || s.text || '이름 없음' }}</span>
            <div class="tag-actions">
              <button class="tag-action-btn jump" @click="$emit('jump', s.day)" title="GO TO DATE">
                <i class="fas fa-external-link-alt" />
              </button>
              <button class="tag-action-btn remove" @click="removeParent(s.id)" title="REMOVE">
                <i class="fas fa-times" />
              </button>
            </div>
          </span>
        </div>

        <div class="searchable-select">
          <div class="search-input-wrapper">
            <i class="fas fa-search search-icon" />
            <input 
              v-model="parentSearch" 
              type="text" 
              placeholder="SEARCH PREVIOUS NODES..." 
              class="search-input"
            />
          </div>
          <div class="search-results custom-scroll" v-if="parentSearch">
            <template v-for="group in filteredParents" :key="group.trackId">
              <div class="search-group-label">{{ group.trackName }}</div>
              <div
                v-for="s in group.schedules" :key="s.id"
                class="search-item"
                :class="{ 'is-added': selectedParentIds.includes(s.id) }"
                @click="addParent(s.id)"
              >
                <div class="item-info">
                  <span class="item-date">{{ s.day }}</span>
                  <span class="item-title">{{ s.tooltip?.title || s.text || '이름 없음' }}</span>
                </div>
                <i v-if="selectedParentIds.includes(s.id)" class="fas fa-check check-icon" />
              </div>
            </template>
            <div v-if="!filteredParents.length" class="no-result">결과가 없습니다.</div>
          </div>
          <select 
            v-else
            class="connector-select" 
            @change="addParent($event.target.value); $event.target.value = ''"
          >
            <option value="">SELECT PREVIOUS NODE</option>
            <optgroup v-for="group in groupedParents" :key="group.trackId" :label="group.trackName">
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
    </div>

    <div class="connector-row mt-12">
      <div class="connector-label">
        <span class="connector-badge">TO</span>
        <span>다음 일정</span>
      </div>

      <div class="tag-select-wrap">
        <div class="tag-list" v-if="selectedChildren.length">
          <span
            v-for="s in selectedChildren" :key="s.id"
            class="tag-chip"
          >
            <span class="tag-color" :style="{ background: trackColor(s.track) }" />
            <span class="tag-text">{{ s.tooltip?.title || s.text || '이름 없음' }}</span>
            <div class="tag-actions">
              <button class="tag-action-btn jump" @click="$emit('jump', s.day)" title="GO TO DATE">
                <i class="fas fa-external-link-alt" />
              </button>
              <button class="tag-action-btn remove" @click="removeChild(s.id)" title="REMOVE">
                <i class="fas fa-times" />
              </button>
            </div>
          </span>
        </div>

        <div class="searchable-select">
          <div class="search-input-wrapper">
            <i class="fas fa-search search-icon" />
            <input 
              v-model="childSearch" 
              type="text" 
              placeholder="SEARCH NEXT NODES..." 
              class="search-input"
            />
          </div>
          <div class="search-results custom-scroll" v-if="childSearch">
            <template v-for="group in filteredChildren" :key="group.trackId">
              <div class="search-group-label">{{ group.trackName }}</div>
              <div
                v-for="s in group.schedules" :key="s.id"
                class="search-item"
                :class="{ 'is-added': selectedChildIds.includes(s.id) }"
                @click="addChild(s.id)"
              >
                <div class="item-info">
                  <span class="item-date">{{ s.day }}</span>
                  <span class="item-title">{{ s.tooltip?.title || s.text || '이름 없음' }}</span>
                </div>
                <i v-if="selectedChildIds.includes(s.id)" class="fas fa-check check-icon" />
              </div>
            </template>
            <div v-if="!filteredChildren.length" class="no-result">결과가 없습니다.</div>
          </div>
          <select 
            v-else
            class="connector-select" 
            @change="addChild($event.target.value); $event.target.value = ''"
          >
            <option value="">SELECT NEXT NODE</option>
            <optgroup v-for="group in groupedChildren" :key="group.trackId" :label="group.trackName">
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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  parentIds: { type: Array, default: () => [] },
  childIds:  { type: Array, default: () => [] },
  parents:   { type: Array, default: () => [] },
  children:  { type: Array, default: () => [] }
})
const emit = defineEmits(['update:parentIds', 'update:childIds', 'jump'])

const store = useCalendarStore()
const trackColor = (id) => store.getTrackById(id)?.color || 'var(--text-muted)'

const parentSearch = ref('')
const childSearch  = ref('')

const selectedParents  = computed(() => props.parentIds.map(id => store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedChildren = computed(() => props.childIds.map(id =>  store.schedules.find(s => s.id === id)).filter(Boolean))
const selectedParentIds = computed(() => props.parentIds)
const selectedChildIds  = computed(() => props.childIds)

function addParent(id) { if (!id || props.parentIds.includes(id)) return; emit('update:parentIds', [...props.parentIds, id]) }
function removeParent(id) { emit('update:parentIds', props.parentIds.filter(x => x !== id)) }
function addChild(id) { if (!id || props.childIds.includes(id)) return; emit('update:childIds', [...props.childIds, id]) }
function removeChild(id) { emit('update:childIds', props.childIds.filter(x => x !== id)) }

function groupByTrack(list, query = '') {
  const q = query.toLowerCase().trim()
  const userTracks = [...store.tracks].sort((a, b) => a.index - b.index)
  const hlTracks   = store.HIGHLIGHT_TRACKS || []
  const allTracks  = [...userTracks, ...hlTracks]
  
  return allTracks
    .map(track => {
      const schedules = list.filter(s => {
        if (s.track !== track.id) return false
        if (!q) return true
        const title = (s.tooltip?.title || s.text || '').toLowerCase()
        return title.includes(q) || s.day.includes(q)
      }).sort((a, b) => a.day.localeCompare(b.day))
      
      return { trackId: track.id, trackName: track.name, schedules }
    })
    .filter(g => g.schedules.length > 0)
}

const groupedParents  = computed(() => groupByTrack(props.parents))
const groupedChildren = computed(() => groupByTrack(props.children))

const filteredParents  = computed(() => groupByTrack(props.parents, parentSearch.value))
const filteredChildren = computed(() => groupByTrack(props.children, childSearch.value))
</script>

<style scoped>
.edge-section { display: flex; flex-direction: column; gap: 20px; }

.connector-row   { display: flex; flex-direction: column; gap: 10px; }
.connector-label { display: flex; align-items: center; gap: 8px; font-size: 10px; font-weight: 800; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.05em; }
.connector-badge { font-size: 9px; font-weight: 900; padding: 2px 6px; border-radius: 4px; background: var(--bg-dark); color: var(--bg-base); letter-spacing: 0.05em; }

.tag-select-wrap { display: flex; flex-direction: column; gap: 10px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 12px; font-weight: 700;
  padding: 8px 12px; border-radius: 10px; border: 1px solid var(--border);
  background: var(--bg-surface); white-space: nowrap;
  transition: all 0.2s;
}
.tag-chip:hover { border-color: var(--text-primary); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.tag-text { overflow: hidden; text-overflow: ellipsis; max-width: 140px; color: var(--text-primary); }
.tag-color { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.tag-actions { display: flex; align-items: center; gap: 6px; margin-left: 6px; }
.tag-action-btn { background: var(--bg-hover); border: 1px solid var(--border); padding: 5px; font-size: 10px; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; border-radius: 6px; transition: 0.2s; }
.tag-action-btn:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-surface); }
.tag-action-btn.remove:hover { color: #f44336; border-color: #f44336; }

/* Searchable Select */
.searchable-select { display: flex; flex-direction: column; gap: 0; position: relative; }
.search-input-wrapper { position: relative; width: 100%; }
.search-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); font-size: 12px; color: var(--text-faint); }
.search-input { 
  width: 100%; background: var(--bg-hover); border: 1px solid var(--border); 
  border-radius: 10px; color: var(--text-primary); font-size: 13px; font-weight: 600; 
  padding: 12px 14px 12px 38px; outline: none; transition: 0.2s; 
}
.search-input:focus { border-color: var(--text-primary); background: var(--bg-surface); }

.search-results { 
  position: absolute; top: calc(100% + 8px); left: 0; right: 0; 
  max-height: 200px; overflow-y: auto; background: rgba(255, 255, 255, 0.9); 
  backdrop-filter: blur(12px); border: 1px solid var(--text-primary); 
  border-radius: 12px; z-index: 100; box-shadow: 0 12px 40px rgba(0,0,0,0.15); 
}
.search-group-label { font-size: 9px; font-weight: 900; color: var(--text-faint); padding: 10px 14px 4px; text-transform: uppercase; letter-spacing: 0.05em; border-top: 1px solid var(--border); }
.search-group-label:first-child { border-top: none; }
.search-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; cursor: pointer; transition: 0.2s; }
.search-item:hover { background: var(--bg-hover); }
.search-item.is-added { background: var(--text-primary); color: var(--bg-base); }

.item-info { display: flex; align-items: center; gap: 10px; }
.item-date { font-size: 10px; font-weight: 700; opacity: 0.6; font-family: 'Space Grotesk', sans-serif; }
.item-title { font-size: 13px; font-weight: 700; }

.no-result { padding: 20px; font-size: 13px; color: var(--text-faint); text-align: center; font-weight: 600; }

.connector-select {
  width: 100%; background: var(--bg-hover); border: 1px solid var(--border); border-radius: 10px;
  color: var(--text-primary); font-size: 13px; font-weight: 600;
  padding: 12px 14px; outline: none; cursor: pointer; transition: 0.2s; appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='3'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 14px center; background-size: 10px;
}
.connector-select:focus { border-color: var(--text-primary); background: var(--bg-surface); }

.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
</style>