<template>
  <div class="tooltip-wrapper retro-modal" @click.stop>
    <div class="tooltip-header">
      <div class="header-track retro-badge" :style="{ color: trackColor, borderColor: trackColor }">
        <span class="track-dot" :style="{ background: trackColor }"></span>
        {{ trackName }}
      </div>
      <div class="tooltip-title">{{ tooltip.title }}</div>
      <div v-if="tooltip.tags?.length" class="tooltip-tags">
        <span v-for="(tag, i) in tooltip.tags" :key="i" class="tooltip-tag">{{ tag }}</span>
      </div>
      <div class="tooltip-actions">
        <button class="tooltip-btn tooltip-btn--edit" @click.stop="$emit('edit')">
          EDIT
        </button>
        <button class="tooltip-btn tooltip-btn--delete" @click.stop="$emit('delete')">
          DELETE
        </button>
      </div>
    </div>

    <div class="tooltip-body terminal-bg">
      <div class="detail-row">
        <i class="fas fa-calendar-day detail-icon" />
        <span class="detail-text">{{ schedule.day }}</span>
      </div>
      <div class="detail-row" v-if="schedule.tooltip?.time">
        <i class="fas fa-clock detail-icon" />
        <span class="detail-text">{{ schedule.tooltip.time }}</span>
      </div>
      
      <div class="title-wrap">
        <h4 class="tooltip-title">{{ schedule.tooltip?.title || schedule.text }}</h4>
      </div>

      <div class="tags-wrap" v-if="schedule.tooltip?.tags?.length">
        <span v-for="tag in schedule.tooltip.tags" :key="tag" class="tag-chip retro-badge text-accent-1">#{{ tag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  visible: { type: Boolean, required: true },
  tooltip: { type: Object,  required: true }
})
defineEmits(['edit', 'delete'])

const tooltipEl = ref(null)
const posStyle  = ref({})

watch(() => props.visible, async (val) => {
  if (!val) { posStyle.value = {}; return }
  await nextTick()

  const el = tooltipEl.value
  if (!el) return

  const nodeEl  = el.parentElement   
  const cellEl  = nodeEl?.closest('.calendar-cell')
  const gridEl  = nodeEl?.closest('.calendar-grid')
  const scrollBody = nodeEl?.closest('.month-scroll-body')

  if (!cellEl || !gridEl) {
    posStyle.value = { bottom: '28px', left: '50%', transform: 'translateX(-50%)' }
    return
  }

  const gridRect = gridEl.getBoundingClientRect()
  const cellRect = cellEl.getBoundingClientRect()
  
  // ★ 스크롤 컨테이너의 기준 좌표 가져오기 (없으면 body 기준)
  const scrollRect = scrollBody ? scrollBody.getBoundingClientRect() : { top: 0 }

  // 가로 위치 (0~6 컬럼)
  const colW   = gridRect.width / 7
  const colIdx = Math.round((cellRect.left - gridRect.left) / colW)

  // ★ 세로 방향 결정 로직:
  // 셀의 top 좌표가 스크롤 영역의 최상단에서 220px(약 1~2줄 높이) 이내라면 무조건 아래로 펼침
  const showBelow = (cellRect.top - scrollRect.top) < 220

  let horizStyle = {}
  if (colIdx === 0) {
    horizStyle = { left: '0', right: 'auto', transform: 'none' }
  } else if (colIdx >= 6) {
    horizStyle = { right: '0', left: 'auto', transform: 'none' }
  } else {
    horizStyle = { left: '50%', right: 'auto', transform: 'translateX(-50%)' }
  }

  const vertStyle = showBelow
    ? { top: '24px', bottom: 'auto' }
    : { bottom: '24px', top: 'auto' }

  posStyle.value = { ...vertStyle, ...horizStyle }
})
defineEmits(['edit', 'delete', 'close'])
</script>

<style scoped>
/* 💡 브루탈리즘 툴팁 팝업 스타일 */
.retro-modal {
  position: absolute;
  width: 220px;
  background: var(--bg-base);
  border: 2px solid var(--text-primary);
  border-radius: 0; /* 직각 형태 */
  padding: 16px;
  box-shadow: 6px 6px 0 var(--text-primary); /* 강력한 단색 그림자 */
  z-index: 200;
  cursor: default;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
}

.tooltip-time-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; font-weight: 800;
  color: var(--text-muted); font-family: monospace;
  margin-bottom: 10px; border-bottom: 1px solid var(--border); padding-bottom: 6px;
}
.tooltip-title {
  font-weight: 900; color: var(--text-primary);
  font-size: 14px; line-height: 1.4; margin-bottom: 12px;
  letter-spacing: 0.05em;
}
.tooltip-tags {
  display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px;
}
.tooltip-tag {
  background: transparent; color: var(--text-primary);
  font-size: 9px; font-weight: 800; letter-spacing: 0.05em;
  padding: 4px 8px; border-radius: 0;
  border: 1px solid var(--text-primary);
}
.tooltip-actions {
  display: flex; gap: 8px;
}
.tooltip-btn {
  flex: 1; font-size: 11px; font-weight: 900; letter-spacing: 0.1em;
  border: 2px solid var(--text-primary); border-radius: 0; cursor: pointer;
  background: transparent; color: var(--text-primary);
  display: flex; align-items: center; justify-content: center;
  gap: 6px; padding: 8px 0; transition: all 0.1s;
}
.tooltip-btn--edit:hover {
  background: var(--text-primary); color: var(--bg-base);
  transform: translate(-2px, -2px); box-shadow: 4px 4px 0 var(--text-primary);
}

/* 삭제 버튼 포인트 컬러 유지 */
.tooltip-btn--delete {
  border-color: #ef4444; color: #ef4444;
}
.tooltip-btn--delete:hover {
  background: #ef4444; color: var(--bg-base);
  transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #ef4444;
}

.tooltip-fade-enter-active, .tooltip-fade-leave-active { transition: all 0.1s ease; }
.tooltip-fade-enter-from, .tooltip-fade-leave-to { opacity: 0; }
</style>