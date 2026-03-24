<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-backdrop" @click.self="close">
        <div class="modal-box">

          <!-- 헤더 -->
          <div class="modal-header">
            <div class="modal-date-info">
              <span class="modal-day-num"
                :class="{ 'day--sat': dayOfWeek===6, 'day--sun': dayOfWeek===0 }">
                {{ displayDate }}
              </span>
              <span class="modal-day-label"
                :class="{ 'day--sat': dayOfWeek===6, 'day--sun': dayOfWeek===0 }">
                {{ dayLabel }}
              </span>
              <span v-if="isToday" class="today-badge">TODAY</span>
            </div>
            <div class="modal-header-actions">
              <button class="btn-add-modal" @click="$emit('add-schedule', dayStr)">
                <i class="fas fa-plus" /> 일정 추가
              </button>
              <button class="btn-close-modal" @click="close">
                <i class="fas fa-times" />
              </button>
            </div>
          </div>

          <!-- 타임라인 바디 -->
          <div class="modal-body">

            <!-- 일정 없을 때 -->
            <div v-if="!schedules.length" class="modal-empty">
              <i class="fas fa-calendar-plus" />
              <p>이 날의 일정이 없습니다.</p>
              <button class="btn-add-empty" @click="$emit('add-schedule', dayStr)">
                + 첫 일정 추가하기
              </button>
            </div>

            <!-- 타임라인 -->
            <div v-else class="timeline">
              <div
                v-for="(s, idx) in sortedSchedules"
                :key="s.id"
                class="tl-item"
              >
                <!-- 시간 축 -->
                <div class="tl-time">
                  <span class="tl-time-text">{{ s.tooltip?.time || '—' }}</span>
                </div>

                <!-- 선 + 점 -->
                <div class="tl-connector">
                  <div class="tl-line-top" :class="{ invisible: idx === 0 }" />
                  <div class="tl-dot" :style="{ background: trackColor(s.track), boxShadow: `0 0 0 3px ${trackColor(s.track)}33` }" />
                  <div class="tl-line-bottom" :class="{ invisible: idx === sortedSchedules.length - 1 }" />
                </div>

                <!-- 카드 -->
                <div class="tl-card" :style="{ borderLeftColor: trackColor(s.track) }">
                  <div class="tl-card-track" :style="{ color: trackColor(s.track) }">
                    <span class="tl-track-dot" :style="{ background: trackColor(s.track) }" />
                    {{ trackName(s.track) }}
                  </div>
                  <div class="tl-card-title">{{ s.tooltip?.title || s.text }}</div>
                  <div v-if="s.tooltip?.tags?.length" class="tl-card-tags">
                    <span v-for="(t, i) in s.tooltip.tags" :key="i" class="tl-tag">{{ t }}</span>
                  </div>
                  <textarea
                    v-model="memos[s.id]"
                    class="tl-memo"
                    placeholder="메모를 입력하세요..."
                    rows="2"
                  />
                  <div class="tl-card-actions">
                    <button class="tl-btn" @click="$emit('edit-schedule', s)">
                      <i class="fas fa-edit" /> 수정
                    </button>
                    <button class="tl-btn tl-btn--del" @click="$emit('delete-schedule', s.id)">
                      <i class="fas fa-trash" /> 삭제
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  dayStr:     { type: String,  required: true },
  schedules:  { type: Array,   default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'add-schedule', 'edit-schedule', 'delete-schedule'])

const store = useCalendarStore()
const memos = ref({})

function close() { emit('update:modelValue', false) }

// ESC 닫기
watch(() => props.modelValue, (v) => {
  if (v) {
    const handler = (e) => { if (e.key === 'Escape') { close(); window.removeEventListener('keydown', handler) } }
    window.addEventListener('keydown', handler)
  }
})

const today = new Date()
function toDateStr(d) {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
const todayStr = toDateStr(today)

const parsed = computed(() => {
  const [y, m, d] = props.dayStr.split('-').map(Number)
  return new Date(y, m - 1, d)
})
const dayOfWeek   = computed(() => parsed.value.getDay())
const isToday     = computed(() => props.dayStr === todayStr)
const displayDate = computed(() => {
  const d = parsed.value
  return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일`
})
const DAY_KOR = ['일요일','월요일','화요일','수요일','목요일','금요일','토요일']
const dayLabel = computed(() => DAY_KOR[dayOfWeek.value])

// 시간순 정렬
const sortedSchedules = computed(() => {
  return [...props.schedules].sort((a, b) => {
    const ta = a.tooltip?.time || '99:99'
    const tb = b.tooltip?.time || '99:99'
    return ta.localeCompare(tb)
  })
})

function trackColor(id) { return store.getTrackById(id)?.color || '#6b7280' }
function trackName(id)  { return store.getTrackById(id)?.name  || id }
</script>

<style scoped>
.modal-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.72);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

.modal-box {
  width: 100%; max-width: 680px;
  max-height: 88vh;
  background: var(--modal-bg);
  border: 2px solid var(--modal-border);
  border-radius: 14px;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255,255,255,0.04);
  display: flex; flex-direction: column;
  overflow: hidden;
}

/* 헤더 */
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.modal-date-info {
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
}
.modal-day-num {
  font-size: 22px; font-weight: 800;
  color: var(--text-primary);
  font-family: 'Escoredream', sans-serif;
}
.modal-day-label {
  font-size: 14px; font-weight: 500;
  color: var(--text-muted);
  font-family: 'Escoredream', sans-serif;
}
.day--sat { color: var(--sat-color) !important; }
.day--sun { color: var(--sun-color) !important; }
.today-badge {
  font-size: 9px; font-weight: 800;
  color: var(--accent);
  background: rgba(59,130,246,0.12);
  border: 1px solid rgba(59,130,246,0.35);
  border-radius: 4px; padding: 2px 6px;
  letter-spacing: 0.06em;
}
.modal-header-actions {
  display: flex; align-items: center; gap: 8px;
}
.btn-add-modal {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px; background: var(--accent); color: #fff;
  border: none; border-radius: 8px;
  font-size: 12px; font-weight: 700;
  font-family: 'Escoredream', sans-serif; cursor: pointer;
  transition: opacity 0.15s;
}
.btn-add-modal:hover { opacity: 0.85; }
.btn-close-modal {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); cursor: pointer; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.btn-close-modal:hover { background: var(--bg-hover); color: var(--text-primary); }

/* 바디 */
.modal-body {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) transparent;
}

/* 빈 상태 */
.modal-empty {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 48px 20px; color: var(--text-faint); text-align: center;
}
.modal-empty i { font-size: 40px; opacity: 0.25; }
.modal-empty p { font-size: 14px; font-family: 'Escoredream', sans-serif; }
.btn-add-empty {
  padding: 9px 20px; border: 1px dashed var(--border-mid);
  background: none; border-radius: 8px;
  color: var(--text-muted); font-size: 13px; font-weight: 600;
  cursor: pointer; font-family: 'Escoredream', sans-serif;
  transition: all 0.15s;
}
.btn-add-empty:hover { border-color: var(--accent); color: var(--accent); }

/* 타임라인 */
.timeline {
  display: flex; flex-direction: column; gap: 0;
}
.tl-item {
  display: flex; align-items: stretch; gap: 0; min-height: 60px;
}

/* 시간 축 */
.tl-time {
  width: 56px; flex-shrink: 0;
  display: flex; align-items: flex-start;
  padding-top: 18px;
}
.tl-time-text {
  font-size: 11px; font-weight: 600;
  font-family: monospace; color: var(--text-faint);
  white-space: nowrap;
}

/* 연결선 + 점 */
.tl-connector {
  width: 24px; flex-shrink: 0;
  display: flex; flex-direction: column; align-items: center;
  gap: 0;
}
.tl-line-top,
.tl-line-bottom {
  flex: 1; width: 2px;
  background: var(--border);
  min-height: 16px;
}
.tl-line-top { min-height: 18px; }
.tl-line-bottom { min-height: 10px; }
.invisible { background: transparent !important; }
.tl-dot {
  width: 12px; height: 12px; border-radius: 50%;
  flex-shrink: 0; position: relative; z-index: 1;
  border: 2.5px solid var(--bg-surface);
  transition: transform 0.15s;
}
.tl-item:hover .tl-dot { transform: scale(1.25); }

/* 카드 */
.tl-card {
  flex: 1; margin: 10px 0 10px 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-left: 3px solid;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex; flex-direction: column; gap: 7px;
  transition: background 0.15s;
}
.tl-card:hover { background: var(--bg-hover); }
.tl-card-track {
  display: flex; align-items: center; gap: 6px;
  font-size: 10px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  letter-spacing: 0.03em;
}
.tl-track-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.tl-card-title {
  font-size: 14px; font-weight: 700;
  color: var(--text-primary); line-height: 1.35;
  font-family: 'Escoredream', sans-serif;
}
.tl-card-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tl-tag {
  font-size: 10px; color: var(--text-muted);
  background: var(--bg-surface); border: 1px solid var(--border);
  padding: 2px 7px; border-radius: 5px;
  font-family: 'Escoredream', sans-serif;
}
.tl-memo {
  width: 100%; background: var(--bg-surface);
  border: 1px solid var(--border); border-radius: 7px;
  color: var(--text-secondary); font-size: 12px;
  font-family: 'Escoredream', sans-serif;
  padding: 8px 10px; outline: none; resize: none;
  transition: border-color 0.15s; line-height: 1.5;
}
.tl-memo:focus { border-color: var(--accent); }
.tl-memo::placeholder { color: var(--text-faint); }
.tl-card-actions {
  display: flex; gap: 6px; justify-content: flex-end;
}
.tl-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 10px; border: none; border-radius: 6px;
  font-size: 11px; font-weight: 600; cursor: pointer;
  background: var(--bg-hover); color: var(--text-muted);
  font-family: 'Escoredream', sans-serif; transition: all 0.15s;
}
.tl-btn:hover { background: var(--accent); color: #fff; }
.tl-btn--del:hover { background: #ef4444; }

/* 모달 트랜지션 */
.modal-fade-enter-active { transition: all 0.2s ease; }
.modal-fade-leave-active { transition: all 0.15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-enter-from .modal-box,
.modal-fade-leave-to .modal-box {
  transform: scale(0.95) translateY(10px);
}
.modal-fade-enter-active .modal-box,
.modal-fade-leave-active .modal-box {
  transition: transform 0.2s ease;
}
</style>
