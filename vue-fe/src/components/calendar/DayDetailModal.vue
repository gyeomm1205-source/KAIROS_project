<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-box">

          <!-- 헤더 -->
          <div class="modal-header">
            <div class="header-left">
              <div class="icon-circle">
                <i class="far fa-calendar-check" />
              </div>
              <div class="header-text">
                <div class="date-flex">
                  <h3 class="modal-day-num" :class="{ 'day--sat': dayOfWeek===6, 'day--sun': dayOfWeek===0 }">
                    {{ displayDate }}
                  </h3>
                  <span v-if="isToday" class="today-badge">TODAY</span>
                </div>
                <p class="modal-day-label">{{ dayLabel }} 스케줄 요약</p>
              </div>
            </div>
            <div class="modal-header-actions">
              <button class="modal-close" @click="close">
                <i class="fas fa-times" />
              </button>
            </div>
          </div>

          <!-- 타임라인 바디 -->
          <div class="modal-body custom-scroll">

            <!-- 일정 없을 때 -->
            <div v-if="!schedules.length" class="modal-empty-card">
              <div class="empty-icon-box">
                <i class="fas fa-calendar-plus" />
              </div>
              <div class="empty-text">
                <h4>계획된 일정이 없습니다</h4>
                <p>새로운 학습 목표를 설정하고 성장을 시작해보세요.</p>
              </div>
              <button class="btn-add-primary" @click="$emit('add-schedule', dayStr)">
                <i class="fas fa-plus" /> 첫 일정 추가하기
              </button>
            </div>

            <!-- 타임라인 -->
            <div v-else class="timeline-container">
              <div
                v-for="s in sortedSchedules"
                :key="s.id"
                class="timeline-item"
              >
                <!-- 선 + 점 -->
                <div class="tl-connector">
                  <div class="tl-dot" :style="{ borderColor: trackColor(s.track) }" />
                </div>

                <!-- 카드 -->
                <div class="tl-card" :style="{ '--track-color': trackColor(s.track) }">
                  <div class="tl-header">
                    <div class="tl-time">{{ s.tooltip?.time || '—' }}</div>
                    <div class="tl-actions">
                      <button class="action-btn" title="편집" @click="$emit('edit-schedule', s)">
                        <i class="fas fa-edit" /> EDIT
                      </button>
                      <button class="action-btn" title="삭제" @click="$emit('delete-schedule', s.id)">
                        <i class="fas fa-trash" />
                      </button>
                    </div>
                  </div>
                  
                  <h4 class="tl-title">{{ s.tooltip?.title || s.text }}</h4>
                  <p class="tl-text">{{ trackName(s.track) }}</p>
                  
                  <div v-if="s.tooltip?.tags?.length" class="tl-card-tags">
                    <span v-for="(t, i) in s.tooltip.tags" :key="i" class="tl-tag">{{ t }}</span>
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
.modal-overlay { 
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); 
  backdrop-filter: blur(14px) saturate(180%); z-index: 2000; 
  display: flex; align-items: center; justify-content: center; padding: 20px; 
}
.modal-box { 
  width: 100%; max-width: 500px; max-height: 80vh; 
  display: flex; flex-direction: column; 
  background: rgba(var(--bg-surface-rgb, 255, 255, 255), 0.95); 
  border: 1px solid var(--text-primary); 
  border-radius: 8px; 
  box-shadow: 0 40px 100px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.1) inset; 
  overflow: hidden;
  animation: modal-pop 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modal-pop {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 32px; border-bottom: 1px solid var(--border); background: rgba(255,255,255,0.02); }
.header-left { display: flex; align-items: center; gap: 16px; }
.icon-circle { width: 44px; height: 44px; background: var(--text-primary); color: var(--bg-base); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
.header-text h3 { font-size: 18px; font-weight: 800; margin: 0; color: var(--text-primary); letter-spacing: -0.02em; font-family: 'Space Grotesk', sans-serif; }
.header-text p { font-size: 12px; font-weight: 600; margin: 2px 0 0 0; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.05em; }

.modal-header-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0;
  align-self: flex-start;
  margin-top: -8px;
  margin-right: -16px;
}
.modal-close { background: transparent; border: none; color: var(--text-muted); font-size: 20px; cursor: pointer; transition: 0.3s cubic-bezier(0.16, 1, 0.3, 1); padding: 4px; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { color: var(--text-primary); transform: rotate(90deg); }

.modal-body { padding: 0; flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }

/* Timeline Container */
.timeline-container { position: relative; padding: 24px 32px; }
.timeline-item { position: relative; margin-bottom: 24px; z-index: 2; cursor: pointer; }
.timeline-item:last-child { margin-bottom: 0; }

.tl-connector { position: absolute; left: 15px; top: 12px; width: 1px; height: calc(100% + 24px); background: var(--border); z-index: 1; }
.timeline-item:last-child .tl-connector { display: none; }

.tl-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--bg-surface); border: 2px solid var(--border); flex-shrink: 0; z-index: 2; position: relative; transition: 0.3s; }
.timeline-item:hover .tl-dot { transform: scale(1.4); border-color: var(--text-primary); }

.tl-card { 
  flex: 1; background: var(--bg-surface); border: 1px solid var(--border); border-radius: 8px; padding: 16px; margin-left: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.02); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); 
  position: relative; overflow: hidden;
}
.tl-card:hover { transform: translateX(4px); border-color: var(--text-primary); box-shadow: 0 8px 25px rgba(0,0,0,0.05); }
.tl-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--track-color, var(--text-primary)); opacity: 0.6; }

.tl-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.tl-time   { font-size: 11px; font-weight: 900; color: var(--text-faint); font-family: 'Space Grotesk', sans-serif; }
.tl-title  { font-size: 14px; font-weight: 800; color: var(--text-primary); margin: 0; line-height: 1.4; }
.tl-text   { font-size: 12px; color: var(--text-secondary); margin: 4px 0 0; line-height: 1.5; font-weight: 500; }

.tl-actions { display: flex; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border); opacity: 0; transition: 0.2s; }
.timeline-item:hover .tl-actions { opacity: 1; }
.action-btn { background: var(--bg-hover); border: none; color: var(--text-muted); padding: 6px 10px; border-radius: 8px; font-size: 10px; font-weight: 900; cursor: pointer; transition: 0.2s; }
.action-btn:hover { background: var(--text-primary); color: var(--bg-base); }

/* Empty State */
.modal-empty-card { 
  margin: 32px; padding: 48px 32px; border: 1px dashed var(--border); border-radius: 8px; 
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 16px;
}
.empty-icon-box { width: 64px; height: 64px; background: var(--bg-hover); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: var(--text-faint); }
.empty-text h4 { font-size: 16px; font-weight: 800; margin: 0; color: var(--text-primary); letter-spacing: -0.01em; }
.empty-text p  { font-size: 13px; font-weight: 500; color: var(--text-muted); margin: 6px 0 0; line-height: 1.5; }
.btn-add-primary { margin-top: 8px; padding: 14px 24px; font-size: 12px; font-weight: 900; background: var(--text-primary); color: var(--bg-base); border: none; border-radius: 8px; cursor: pointer; transition: 0.3s; }
.btn-add-primary:hover { transform: translateY(-2px); opacity: 0.9; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
