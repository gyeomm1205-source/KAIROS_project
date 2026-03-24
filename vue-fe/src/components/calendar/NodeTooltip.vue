<template>
  <Transition name="tooltip-fade">
    <div
      v-if="visible"
      ref="tooltipEl"
      class="schedule-tooltip"
      :style="posStyle"
      @click.stop
    >
      <div class="tooltip-time-row">
        <i class="fas fa-clock" />
        <span>{{ tooltip.time }}</span>
      </div>
      <div class="tooltip-title">{{ tooltip.title }}</div>
      <div v-if="tooltip.tags?.length" class="tooltip-tags">
        <span v-for="(tag, i) in tooltip.tags" :key="i" class="tooltip-tag">{{ tag }}</span>
      </div>
      <div class="tooltip-actions">
        <button class="tooltip-btn tooltip-btn--edit"   @click.stop="$emit('edit')">
          <i class="fas fa-edit" /> 수정
        </button>
        <button class="tooltip-btn tooltip-btn--delete" @click.stop="$emit('delete')">
          <i class="fas fa-trash" /> 삭제
        </button>
      </div>
    </div>
  </Transition>
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

  const nodeEl  = el.parentElement   // schedule-dot
  const cellEl  = nodeEl?.closest('.calendar-cell')
  const gridEl  = nodeEl?.closest('.calendar-grid')

  if (!cellEl || !gridEl) {
    // 기본값
    posStyle.value = { bottom: '28px', left: '50%', transform: 'translateX(-50%)' }
    return
  }

  const gridRect = gridEl.getBoundingClientRect()
  const cellRect = cellEl.getBoundingClientRect()

  // ── 가로 위치: 몇 번째 컬럼? (0~6) ──
  const colW   = gridRect.width / 7
  const colIdx = Math.round((cellRect.left - gridRect.left) / colW)

  // ── 세로 위치: 화면 기준 행 번호 ──
  // getBoundingClientRect는 스크롤과 무관하게 화면상 상대 위치를 반환
  // cellRect.top - gridRect.top: 둘 다 같은 스크롤 오프셋을 가지므로 상쇄됨
  const cellRelTop = cellRect.top - gridRect.top   // grid 내 현재 화면 Y
  const ROW_H      = cellRect.height || 150
  // 음수면 스크롤로 위로 올라간 경우 → 스크롤 보정
  const scrollBody = nodeEl?.closest('.month-scroll-body')
  const scrollTop  = scrollBody ? scrollBody.scrollTop : 0
  const cellAbsRow = Math.floor((cellRelTop + scrollTop) / ROW_H)

  // ── 방향 결정 ──
  // 3개월 뷰에서 절대 행 번호로 판단
  // 이전달 행(0~prevRows-1)과 현재달 1~2행 → 아래
  // 충분히 내려온 행(3행 이상) → 위
  // 단순하게: 화면에서 위쪽 절반에 있으면 → 아래
  const showBelow = cellRect.top < (window.innerHeight / 2)

  // 가로: 맨 좌측(col 0) 또는 맨 우측(col 6) → 옆으로 펼치기, 나머지 → 중앙
  let horizStyle = {}
  if (colIdx === 0) {
    // 왼쪽 끝: 오른쪽으로 펼치기
    horizStyle = { left: '0', right: 'auto', transform: 'none' }
  } else if (colIdx >= 6) {
    // 오른쪽 끝: 왼쪽으로 펼치기
    horizStyle = { right: '0', left: 'auto', transform: 'none' }
  } else {
    // 중앙 정렬
    horizStyle = { left: '50%', right: 'auto', transform: 'translateX(-50%)' }
  }

  // 세로 오프셋
  const vertStyle = showBelow
    ? { top: '18px', bottom: 'auto' }
    : { bottom: '18px', top: 'auto' }

  posStyle.value = { ...vertStyle, ...horizStyle }
})
</script>

<style scoped>
.schedule-tooltip {
  position: absolute;
  width: 218px;
  background: var(--bg-surface);
  border: 1.5px solid var(--border-mid);
  border-radius: 14px;
  padding: 13px 14px;
  box-shadow:
    0 8px 28px rgba(0,0,0,0.2),
    0 2px 8px rgba(0,0,0,0.1);
  z-index: 200;
  cursor: default;
}

.tooltip-time-row {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600;
  color: var(--text-muted); font-family: monospace;
  margin-bottom: 6px;
}
.tooltip-title {
  font-weight: 700; color: var(--text-primary);
  font-size: 13px; line-height: 1.45; margin-bottom: 8px;
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

.tooltip-fade-enter-active { transition: opacity 0.15s ease; }
.tooltip-fade-leave-active { transition: opacity 0.1s ease; }
.tooltip-fade-enter-from, .tooltip-fade-leave-to { opacity: 0; }
</style>
