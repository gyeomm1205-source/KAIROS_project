<template>
  <div class="track-legend">

    <button
      class="legend-btn legend-btn--all"
      :class="{ 'is-off': !isAllVisible }"
      @click="toggleAll"
      title="전체 표시 / 숨김"
    >
      <span class="all-dots">
        <span
          v-for="t in allTracks.slice(0, 4)"
          :key="t.id"
          class="all-dot"
          :style="{ background: t.color }"
        />
      </span>
      <span>ALL</span>
    </button>

    <div class="legend-sep" />

    <template v-for="(track, i) in allTracks" :key="track.id">
      <div
        v-if="track.isHighlight && (i === 0 || !allTracks[i-1].isHighlight)"
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
const globalHiddenSet = ref(new Set())
</script>

<script setup>
import { computed, onMounted } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const emit = defineEmits(['update:hiddenTracks'])
const store = useCalendarStore()

// 템플릿에서 쓸 수 있도록 전역 메모리를 로컬 변수에 연결
const hiddenSet = globalHiddenSet

const allTracks = computed(() => {
  const list = [...store.allTracks]
  
  const hasPrompt = list.some(t => t.id === 'hl_prompt' || t.name?.includes('프롬프트'))
  const hasBlog = list.some(t => t.id === 'hl_blog' || t.name?.includes('블로그'))
  
  if (!hasPrompt) {
    list.push({ id: 'hl_prompt', name: '프롬프트', color: '#facc15', index: 98, isHighlight: true })
  }
  if (!hasBlog) {
    list.push({ id: 'hl_blog', name: '블로그 어시스턴트', color: '#10b981', index: 99, isHighlight: true })
  }
  
  return list.sort((a, b) => a.index - b.index)
})

// ★ 창이 새로 열릴 때, 이전에 기억해둔 상태를 부모(캔버스)에게 다시 쏴주어 화면과 버튼 상태를 100% 동기화!
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
    hiddenSet.value = new Set(allTracks.value.map(t => t.id))
  } else {
    hiddenSet.value = new Set()
  }
  emit('update:hiddenTracks', new Set(hiddenSet.value))
}
</script>

<style scoped>
.track-legend {
  display: flex; align-items: center; gap: 3px; flex-wrap: nowrap;
  overflow-x: auto; min-width: 0;
  scrollbar-width: none;
}
.track-legend::-webkit-scrollbar { display: none; }

.legend-sep {
  width: 1px; height: 16px;
  background: var(--border); flex-shrink: 0; margin: 0 3px;
}

/* 공통 버튼 */
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
/* ALL 버튼 */
.legend-btn--all {
  background: var(--bg-elevated);
  border-color: var(--border);
  color: var(--text-secondary);
  font-weight: 800; letter-spacing: 0.04em; font-size: 10px;
}
.legend-btn--all:hover { border-color: var(--border-mid); }

/* 숨김 상태 */
.legend-btn.is-off {
  opacity: 0.38;
}
.legend-btn.is-off .legend-name { text-decoration: line-through; }

/* ALL의 미니 닷 무리 */
.all-dots {
  display: flex; align-items: center;
}
.all-dot {
  width: 6px; height: 6px; border-radius: 50%;
  margin-right: -2px; border: 1px solid var(--bg-elevated);
  flex-shrink: 0;
}

/* 트랙 색 원 */
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