<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-backdrop" @click.self="close">
        <div class="modal-box">

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

          <div class="modal-body custom-scroll">

            <div v-if="!schedules.length" class="modal-empty">
              <i class="fas fa-calendar-plus" />
              <p>이 날의 일정이 없습니다.</p>
              <button class="btn-add-empty" @click="$emit('add-schedule', dayStr)">
                + 첫 일정 추가하기
              </button>
            </div>

            <div v-else class="timeline">
              <div
                v-for="(s, idx) in sortedSchedules"
                :key="s.id"
                class="tl-item"
              >
                <div class="tl-time">
                  <span class="tl-time-text">{{ s.tooltip?.time || '—' }}</span>
                </div>

                <div class="tl-connector">
                  <div class="tl-line-top" :class="{ invisible: idx === 0 }" />
                  <div class="tl-dot" :style="{ background: trackColor(s.track), borderColor: trackColor(s.track) }" />
                  <div class="tl-line-bottom" :class="{ invisible: idx === sortedSchedules.length - 1 }" />
                </div>

                <div class="tl-card" :style="{ borderLeftColor: trackColor(s.track) }">
                  <div class="tl-card-track" :style="{ color: trackColor(s.track) }">
                    <span class="tl-track-dot" :style="{ background: trackColor(s.track) }" />
                    {{ trackName(s.track) }}
                  </div>
                  
                  <div class="tl-card-title">{{ s.tooltip?.title || s.text }}</div>
                  
                  <div v-if="s.progress !== undefined" class="mt-2 mb-2">
                    <div class="flex justify-between text-[10px] font-bold text-gray-500 mb-1">
                      <span>PROGRESS</span>
                      <span>{{ s.progress }}%</span>
                    </div>
                    <div class="w-full h-1.5 bg-gray-200">
                      <div class="h-full bg-blue-600 transition-all duration-300" :style="{ width: s.progress + '%' }"></div>
                    </div>
                  </div>

                  <div v-if="s.tooltip?.tags?.length" class="tl-card-tags">
                    <span v-for="(t, i) in s.tooltip.tags" :key="i" class="tl-tag">{{ t }}</span>
                  </div>

                  <div v-if="s.reasoning" class="mt-2 p-2 border-2 border-dashed border-gray-400 bg-gray-50 text-[11px] font-bold text-gray-700">
                    <i class="fas fa-robot text-blue-600 mr-1"></i> AI 추천: {{ s.reasoning }}
                  </div>

                  <textarea
                    v-model="memos[s.id]"
                    class="tl-memo custom-scroll mt-2"
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

const sortedSchedules = computed(() => {
  return [...props.schedules].sort((a, b) => {
    const ta = a.tooltip?.time || '99:99'
    const tb = b.tooltip?.time || '99:99'
    return ta.localeCompare(tb)
  })
})

function trackColor(id) { return store.getTrackById(id)?.color || 'var(--text-primary)' }
function trackName(id)  { return store.getTrackById(id)?.name  || id }
</script>

<style scoped>
/* ── 스크롤바 숨김 (기능 유지) ── */
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.modal-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.85);
  display: flex; align-items: center; justify-content: center;
  padding: 20px; font-family: 'Space Grotesk', 'Escoredream', sans-serif;
}

.modal-box {
  width: 100%; max-width: 680px; max-height: 88vh;
  background: var(--bg-base);
  border: 2px solid var(--text-primary); border-radius: 0;
  box-shadow: 12px 12px 0 var(--text-primary);
  display: flex; flex-direction: column; overflow: hidden;
}

.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px; border-bottom: 2px solid var(--text-primary); flex-shrink: 0;
}
.modal-date-info { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.modal-day-num { font-size: 22px; font-weight: 900; color: var(--text-primary); letter-spacing: 0.05em; }
.modal-day-label { font-size: 14px; font-weight: 800; color: var(--text-muted); }
.today-badge { font-size: 10px; font-weight: 900; color: var(--bg-base); background: var(--text-primary); padding: 4px 8px; letter-spacing: 0.1em; border: 1px solid var(--text-primary); border-radius: 0; }

.modal-header-actions { display: flex; align-items: center; gap: 8px; }
.btn-add-modal { display: flex; align-items: center; gap: 6px; padding: 10px 16px; background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); border-radius: 0; font-size: 12px; font-weight: 900; cursor: pointer; transition: all 0.1s; letter-spacing: 0.05em; }
.btn-add-modal:hover { background: transparent; color: var(--text-primary); box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); }
.btn-close-modal { width: 36px; height: 36px; border-radius: 0; background: transparent; border: 2px solid transparent; color: var(--text-primary); cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center; transition: all 0.1s; }
.btn-close-modal:hover { transform: scale(1.2); }

.modal-body { flex: 1; overflow-y: auto; padding: 24px; }

.modal-empty { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 64px 20px; color: var(--text-faint); text-align: center; }
.modal-empty i { font-size: 48px; opacity: 0.5; color: var(--border); }
.modal-empty p { font-size: 14px; font-weight: 700; letter-spacing: 0.05em; }
.btn-add-empty { padding: 12px 24px; border: 2px dashed var(--text-primary); background: transparent; border-radius: 0; color: var(--text-primary); font-size: 13px; font-weight: 800; cursor: pointer; transition: all 0.1s; }
.btn-add-empty:hover { background: var(--text-primary); color: var(--bg-base); border-style: solid; }

.timeline { display: flex; flex-direction: column; gap: 12px; }
.tl-item { display: flex; align-items: stretch; gap: 0; min-height: 60px; background: transparent; border: none; padding: 4px 0; }

.tl-time { width: 56px; flex-shrink: 0; display: flex; align-items: flex-start; padding: 16px 0 0 10px; border-right: 2px solid var(--border); }
.tl-time-text { font-size: 12px; font-weight: 800; font-family: monospace; color: var(--text-muted); white-space: nowrap; }

.tl-connector { width: 32px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 0; padding: 0 4px; }
.tl-line-top, .tl-line-bottom { flex: 1; width: 2px; background: var(--border); min-height: 16px; }
.tl-line-top { min-height: 18px; }
.tl-line-bottom { min-height: 10px; }
.invisible { background: transparent !important; }
.tl-dot { width: 12px; height: 12px; border-radius: 0; flex-shrink: 0; position: relative; z-index: 1; border: 2px solid; }

.tl-card {
  flex: 1; margin: 6px 0 10px 12px;
  background: var(--bg-surface);
  border: 2px solid var(--text-primary);
  border-left-width: 6px; border-radius: 0;
  padding: 16px; display: flex; flex-direction: column; gap: 8px;
  box-shadow: 4px 4px 0 var(--border); transition: all 0.1s ease;
}
.tl-card:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--text-primary); background: var(--bg-base); }

.tl-card-track { display: flex; align-items: center; gap: 8px; font-size: 11px; font-weight: 900; letter-spacing: 0.05em; text-transform: uppercase; }
.tl-track-dot { width: 8px; height: 8px; border-radius: 0; flex-shrink: 0; border: 1px solid var(--bg-base); }
.tl-card-title { font-size: 15px; font-weight: 800; color: var(--text-primary); line-height: 1.4; }

.tl-card-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tl-tag { font-size: 10px; font-weight: 800; color: var(--text-primary); background: transparent; border: 1px solid var(--text-primary); padding: 4px 8px; border-radius: 0; }

.tl-memo { width: 100%; background: var(--bg-base); border: 2px solid var(--border); border-radius: 0; color: var(--text-primary); font-size: 13px; font-weight: 700; padding: 12px; outline: none; resize: none; transition: border-color 0.1s; line-height: 1.6; }
.tl-memo:focus { border-color: var(--text-primary); box-shadow: 4px 4px 0 var(--border); }
.tl-memo::placeholder { color: var(--text-faint); font-weight: 600; }

.tl-card-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 4px; }
.tl-btn { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border: 2px solid var(--border); border-radius: 0; font-size: 11px; font-weight: 800; cursor: pointer; background: transparent; color: var(--text-primary); transition: all 0.1s; letter-spacing: 0.05em; }
.tl-btn:hover { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.tl-btn--del:hover { background: #ef4444; color: #fff; border-color: #ef4444; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .modal-box, .modal-fade-leave-to .modal-box { transform: scale(0.98) translateY(10px); }
</style>