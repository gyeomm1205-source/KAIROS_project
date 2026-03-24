<template>
  <Transition name="tooltip-fade">
    <div
      v-if="visible"
      class="week-tooltip"
      :style="posStyle"
      @click.stop
    >
      <!-- 트랙 색상 상단 바 -->
      <div class="tooltip-track-bar" :style="{ background: trackColor }" />

      <div class="tooltip-body">
        <div class="tooltip-time">
          <i class="fas fa-clock" /> {{ tooltip.time }}
        </div>
        <div class="tooltip-title">{{ tooltip.title }}</div>
        <div v-if="tooltip.tags?.length" class="tooltip-tags">
          <span v-for="(tag, i) in tooltip.tags" :key="i" class="tooltip-tag">{{ tag }}</span>
        </div>
        <div class="tooltip-actions">
          <button class="tooltip-btn tooltip-btn--edit" @click.stop="$emit('edit')">
            <i class="fas fa-edit" /> 수정
          </button>
          <button class="tooltip-btn tooltip-btn--delete" @click.stop="$emit('delete')">
            <i class="fas fa-trash" /> 삭제
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible:     { type: Boolean, required: true },
  tooltip:     { type: Object,  required: true },
  trackColor:  { type: String,  default: '#6b7280' },
  colIdx:      { type: Number,  default: 3 },   // 0~6
  trackIdx:    { type: Number,  default: 0 },
  totalTracks: { type: Number,  default: 5 },
})
defineEmits(['edit', 'delete'])

const TW = 220
const TH = 160

const posStyle = computed(() => {
  // 요일 위치 파악 (0=일, 6=토)
  const dayCol = props.colIdx

  let horiz = {}
  if (dayCol <= 1) {
    horiz = { left: '14px', right: 'auto', transform: 'none' }
  } else if (dayCol >= 5) {
    horiz = { right: '14px', left: 'auto', transform: 'none' }
  } else {
    horiz = { left: '50%', right: 'auto', transform: 'translateX(-50%)' }
  }

  const midTrack = (props.totalTracks - 1) / 2
  let vert = {}
  if (props.trackIdx <= midTrack) {
    vert = { top: '20px', bottom: 'auto' }
  } else {
    vert = { bottom: '20px', top: 'auto' }
  }

  return { ...horiz, ...vert, position: 'absolute', width: TW + 'px', zIndex: 500 }
})
</script>

<style scoped>
.week-tooltip {
  background: var(--bg-surface);
  border: 1.5px solid var(--border-mid);
  border-radius: 14px;
  overflow: hidden;
  box-shadow:
    0 8px 28px rgba(0,0,0,0.22),
    0 2px 8px rgba(0,0,0,0.12);
  cursor: default;
}

.tooltip-track-bar {
  height: 3px;
  width: 100%;
}

.tooltip-body {
  padding: 12px 14px;
  display: flex; flex-direction: column; gap: 0;
}

.tooltip-time {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600;
  color: var(--text-muted); font-family: monospace;
  margin-bottom: 5px;
}
.tooltip-time i { font-size: 9px; }

.tooltip-title {
  font-weight: 700; color: var(--text-primary);
  font-size: 13px; line-height: 1.45;
  margin-bottom: 8px;
  font-family: 'Escoredream', sans-serif;
}

.tooltip-tags {
  display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 10px;
}
.tooltip-tag {
  background: var(--bg-elevated); color: var(--text-muted);
  font-size: 9px; font-weight: 600;
  padding: 2px 7px; border-radius: 999px;
  border: 1px solid var(--border);
}

.tooltip-actions {
  display: flex; gap: 6px;
  padding-top: 9px; border-top: 1px solid var(--border);
}
.tooltip-btn {
  flex: 1; font-size: 11px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  border: 1px solid; border-radius: 8px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  gap: 5px; padding: 6px 0; transition: all 0.15s;
}
.tooltip-btn--edit {
  background: rgba(59,130,246,0.1); color: #3b82f6;
  border-color: rgba(59,130,246,0.3);
}
.tooltip-btn--edit:hover   { background: #3b82f6; color: #fff; border-color: #3b82f6; }
.tooltip-btn--delete {
  background: rgba(239,68,68,0.08); color: #ef4444;
  border-color: rgba(239,68,68,0.25);
}
.tooltip-btn--delete:hover { background: #ef4444; color: #fff; border-color: #ef4444; }

.tooltip-fade-enter-active { transition: opacity 0.15s ease, transform 0.12s ease; }
.tooltip-fade-leave-active { transition: opacity 0.1s ease; }
.tooltip-fade-enter-from { opacity: 0; transform: scale(0.95); }
.tooltip-fade-leave-to   { opacity: 0; }
</style>
