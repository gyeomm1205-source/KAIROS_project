<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="modal-box">

          <div class="modal-header">
            <div class="header-left">
              <div class="icon-circle">
                <i :class="mode === 'create' ? 'fas fa-calendar-plus' : 'fas fa-edit'" />
              </div>
              <div class="header-text">
                <h3>{{ mode === 'create' ? 'ADD' : 'EDIT' }} SCHEDULE</h3>
                <p>{{ mode === 'create' ? '신규 일정 만들기' : '일정 내용 수정하기' }}</p>
              </div>
            </div>
            <button class="modal-close" @click="$emit('update:modelValue', false)">
              <i class="fas fa-times" />
            </button>
          </div>

          <div class="modal-body custom-scroll p-lg">
            <!-- 기본 정보 카드 -->
            <div class="info-card mb-md">
              <div class="flex-align gap-sm mb-sm text-muted">
                <i class="fas fa-info-circle text-xs" />
                <span class="text-xs font-bold">기본 정보</span>
              </div>
              
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">DATE</label>
                  <div class="input-wrapper">
                    <input v-model="localForm.day" type="date" :min="monthMin" :max="monthMax" class="form-input" />
                    <i class="fas fa-calendar-alt input-icon" />
                  </div>
                </div>
                
                <div class="form-group relative">
                  <label class="form-label">TRACK</label>
                  <div class="custom-select-container">
                    <div 
                      class="form-input custom-select-trigger" 
                      :class="{ 'is-open': isDropdownOpen }"
                      @click.stop="isDropdownOpen = !isDropdownOpen"
                    >
                      <template v-if="localForm.track">
                        <div class="selected-value">
                          <span class="track-color-indicator" :style="{ background: selectedTrackColor }"></span>
                          <span class="sv-text">{{ selectedTrackName }}</span>
                        </div>
                      </template>
                      <template v-else>
                        <div class="empty-placeholder">SELECT TRACK</div>
                      </template>
                      <i class="fas fa-chevron-down arrow-icon" />
                    </div>

                    <Transition name="dropdown-fade">
                      <div v-if="isDropdownOpen" class="custom-options-menu" @click.stop>
                        <div class="opt-group" v-if="store.activeTracks.length">
                          <div class="opt-group-label">USER TRACKS</div>
                          <div 
                            v-for="t in store.activeTracks" :key="t.id"
                            class="custom-option"
                            :class="{ 'is-selected': localForm.track === t.id }"
                            @click="selectTrack(t.id)"
                          >
                            <span class="track-color-indicator" :style="{ background: t.color }"></span>
                            {{ t.name }}
                          </div>
                        </div>

                        <div class="opt-group" v-if="store.HIGHLIGHT_TRACKS?.length">
                          <div class="opt-group-label">SYSTEM TRACKS</div>
                          <div 
                            v-for="t in store.HIGHLIGHT_TRACKS" :key="t.id"
                            class="custom-option"
                            :class="{ 'is-selected': localForm.track === t.id }"
                            @click="selectTrack(t.id)"
                          >
                            <span class="track-color-indicator" :style="{ background: t.color }"></span>
                            {{ t.name }}
                          </div>
                        </div>
                      </div>
                    </Transition>
                  </div>
                </div>
              </div>

              <div class="form-group mt-md">
                <label class="form-label">
                  TITLE <span class="badge-required">REQUIRED</span>
                </label>
                <input
                  v-model="localForm.title"
                  type="text"
                  placeholder="e.g., React Hooks Study"
                  class="form-input"
                  :class="{ 'form-input--error': titleError }"
                  @input="titleError = false"
                />
                <p v-if="titleError" class="form-error">제목은 필수 입력 항목입니다.</p>
              </div>
            </div>

            <!-- 연결 정보 카드 -->
            <div class="info-card mb-md">
              <div class="flex-align gap-sm mb-sm text-muted">
                <i class="fas fa-link text-xs" />
                <span class="text-xs font-bold">연결 정보</span>
              </div>
              <EdgeConnectorSection
                v-model:parentIds="localForm.parentIds"
                v-model:childIds="localForm.childIds"
                :parents="availableParents"
                :children="availableChildren"
                @jump="$emit('jump', $event)"
              />
            </div>

            <!-- 상세 정보 카드 -->
            <div class="info-card mb-md">
              <div class="flex-align gap-sm mb-sm text-muted">
                <i class="fas fa-ellipsis-h text-xs" />
                <span class="text-xs font-bold">기타 정보</span>
              </div>
              
              <div class="form-group">
                <label class="form-label">
                  LABEL <span class="badge-optional">OPTIONAL</span>
                </label>
                <input v-model="localForm.text" type="text" placeholder="Short description" class="form-input" />
              </div>

              <div class="form-row mt-md">
                <div class="form-group" style="flex:0 0 160px">
                  <label class="form-label">TIME</label>
                  <div class="input-wrapper">
                    <input v-model="localForm.time" type="time" class="form-input" />
                    <i class="fas fa-clock input-icon" />
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">
                    TAGS <span class="badge-optional">OPTIONAL</span>
                  </label>
                  <input v-model="localForm.tags" type="text" placeholder="#React, #Frontend" class="form-input" />
                </div>
              </div>
            </div>

            <!-- AI Reason Card (if editing) -->
            <div v-if="localForm.reasoning || mode === 'edit'" class="info-card reasoning-card">
              <div class="flex-align gap-sm mb-sm text-primary">
                <i class="fas fa-sparkles text-xs" />
                <span class="text-xs font-extra-bold">AI REASONING</span>
              </div>
              <p class="ai-reasoning-text">
                {{ localForm.reasoning || 'AI 추천 내역이 없는 일정입니다.' }}
              </p>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="$emit('update:modelValue', false)">CANCEL</button>
            <button class="btn-save" @click="handleSave">SAVE</button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import EdgeConnectorSection from '@/components/modal/EdgeConnectorSection.vue'

const props = defineProps({ modelValue: Boolean, mode: String, initialForm: Object, editNodeId: String })
const emit = defineEmits(['update:modelValue', 'save', 'jump'])
const store = useCalendarStore()

const localForm  = ref(defaultForm())
const titleError = ref(false)

const isDropdownOpen = ref(false)
const closeDropdown = () => { isDropdownOpen.value = false }

onMounted(() => { document.addEventListener('click', closeDropdown) })
onUnmounted(() => { document.removeEventListener('click', closeDropdown) })

function selectTrack(id) {
  localForm.value.track = id
  isDropdownOpen.value = false
}

function defaultForm() {
  const t = new Date()
  return { 
    day: `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')}`, 
    track: '', 
    title: '', 
    text: '', 
    time: '09:00', 
    tags: '', 
    parentIds: [], 
    childIds: [],
    reasoning: ''
  }
}

const selectedTrackName = computed(() => { const t = store.allTracks.find(x => x.id === localForm.value.track); return t ? t.name : '' })
const selectedTrackColor = computed(() => { const t = store.allTracks.find(x => x.id === localForm.value.track); return t ? t.color : 'transparent' })

const monthMin = computed(() => { if (!localForm.value.day) return ''; const [y, m] = localForm.value.day.split('-').map(Number); return `${y}-${String(m).padStart(2,'0')}-01` })
const monthMax = computed(() => { if (!localForm.value.day) return ''; const [y, m] = localForm.value.day.split('-').map(Number); const last = new Date(y, m, 0).getDate(); return `${y}-${String(m).padStart(2,'0')}-${String(last).padStart(2,'0')}` })

watch(() => props.initialForm, (val) => {
  const base = defaultForm()
  localForm.value = { 
    ...base, 
    ...val, 
    parentIds: Array.isArray(val?.parentIds) ? val.parentIds : (val?.parentId ? [val.parentId] : []), 
    childIds:  Array.isArray(val?.childIds)  ? val.childIds  : (val?.childId  ? [val.childId]  : []) 
  }
  titleError.value = false
}, { immediate: true })

const availableParents = computed(() => store.schedules.filter(s => s.day <= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => b.day.localeCompare(a.day)))
const availableChildren = computed(() => store.schedules.filter(s => s.day >= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => a.day.localeCompare(b.day)))

function handleSave() {
  if (!localForm.value.track) { alert('학습 트랙을 선택해주세요.'); return }
  if (!localForm.value.title.trim()) { titleError.value = true; return }
  const tags = localForm.value.tags.split(',').map(t => t.trim()).filter(Boolean)
  emit('save', { 
    day: localForm.value.day, 
    track: localForm.value.track, 
    text: localForm.value.text, 
    tooltip: { title: localForm.value.title.trim(), time: localForm.value.time, tags }, 
    parentIds: localForm.value.parentIds || [], 
    childIds: localForm.value.childIds || [],
    reasoning: localForm.value.reasoning 
  })
}
</script>

<style scoped>
.modal-overlay { 
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45); 
  backdrop-filter: blur(14px) saturate(180%); z-index: 2000; 
  display: flex; align-items: center; justify-content: center; padding: 20px; 
}
.modal-box { 
  width: 100%; max-width: 540px; max-height: 85vh; 
  display: flex; flex-direction: column; 
  background: rgba(var(--bg-surface-rgb, 255, 255, 255), 0.95); 
  border: 1px solid var(--text-primary); 
  border-radius: 0; 
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
.icon-circle { width: 44px; height: 44px; background: var(--text-primary); color: var(--bg-base); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
.header-text h3 { font-size: 18px; font-weight: 800; margin: 0; color: var(--text-primary); letter-spacing: -0.02em; font-family: 'Space Grotesk', sans-serif; }
.header-text p { font-size: 12px; font-weight: 600; margin: 2px 0 0 0; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.05em; }

.modal-close { background: transparent; border: none; color: var(--text-muted); font-size: 20px; cursor: pointer; transition: 0.3s cubic-bezier(0.16, 1, 0.3, 1); padding: 4px; display: flex; align-items: center; justify-content: center; }
.modal-close:hover { color: var(--text-primary); transform: rotate(90deg); }

.modal-body { padding: 0; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 0; }
.p-lg { padding: 24px 32px; }
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }

.info-card { 
  background: var(--bg-surface); border: 1px solid var(--border); border-radius: 16px; padding: 20px; 
  box-shadow: 0 4px 20px rgba(0,0,0,0.02); position: relative; overflow: hidden;
  transition: all 0.3s ease;
}
.info-card:hover { border-color: var(--text-primary); box-shadow: 0 8px 30px rgba(0,0,0,0.05); }

.mb-md { margin-bottom: 20px; }
.mt-md { margin-top: 16px; }

.flex-align { display: flex; align-items: center; }
.gap-sm { gap: 8px; }
.mb-sm { margin-bottom: 12px; }
.text-xs { font-size: 11px; }
.text-primary { color: var(--text-primary); }
.text-muted { color: var(--text-muted); }
.font-bold { font-weight: 700; }
.font-extra-bold { font-weight: 900; }

.form-row   { display: flex; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 8px; flex: 1; }
.form-label { display: flex; align-items: center; gap: 8px; font-size: 10px; font-weight: 900; letter-spacing: 0.12em; color: var(--text-faint); text-transform: uppercase; font-family: 'Space Grotesk', sans-serif; }

.badge-required { margin-left: auto; font-size: 8px; font-weight: 900; background: var(--text-primary); color: var(--bg-base); padding: 2px 6px; border-radius: 4px; letter-spacing: 0.05em; }
.badge-optional { margin-left: auto; font-size: 8px; font-weight: 900; border: 1px solid var(--border); color: var(--text-faint); padding: 2px 6px; border-radius: 4px; }

.input-wrapper { position: relative; width: 100%; }
.input-icon { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); font-size: 11px; color: var(--text-faint); pointer-events: none; transition: 0.2s; }

.form-input { 
  width: 100%; background: var(--bg-hover); border: 1px solid var(--border); 
  color: var(--text-primary); font-size: 14px; font-weight: 600; 
  border-radius: 12px; padding: 12px 14px; outline: none; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); 
}
.form-input:focus { border-color: var(--text-primary); background: var(--bg-surface); box-shadow: 0 0 0 4px rgba(0,0,0,0.03); }
.form-input:focus + .input-icon { color: var(--text-primary); }
.form-input--error { border-color: #ff4d4d; box-shadow: 0 0 0 4px rgba(255, 77, 77, 0.1); }
.form-error { font-size: 11px; font-weight: 700; color: #ff4d4d; margin-top: 4px; display: flex; align-items: center; gap: 4px; }

/* Custom Select */
.custom-select-container { position: relative; width: 100%; }
.custom-select-trigger { display: flex; align-items: center; justify-content: space-between; cursor: pointer; user-select: none; }
.custom-select-trigger.is-open { border-color: var(--text-primary); background: var(--bg-surface); }
.selected-value { display: flex; align-items: center; gap: 10px; font-weight: 800; font-size: 13px; }
.track-color-indicator { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 10px currentColor; }
.arrow-icon { font-size: 9px; transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); color: var(--text-faint); }
.custom-select-trigger.is-open .arrow-icon { transform: rotate(180deg); color: var(--text-primary); }
.empty-placeholder { font-size: 13px; font-weight: 600; color: var(--text-faint); }

.custom-options-menu { 
  position: absolute; top: calc(100% + 8px); left: 0; right: 0; 
  background: rgba(var(--bg-surface-rgb, 255, 255, 255), 0.9); backdrop-filter: blur(16px);
  border: 1px solid var(--text-primary); box-shadow: 0 20px 50px rgba(0,0,0,0.2); 
  z-index: 200; max-height: 240px; overflow-y: auto; padding: 10px; border-radius: 16px; 
}
.opt-group-label { font-size: 10px; font-weight: 900; letter-spacing: 0.15em; color: var(--text-faint); padding: 12px 12px 6px; text-transform: uppercase; }
.custom-option { display: flex; align-items: center; gap: 10px; padding: 12px 14px; font-size: 13px; font-weight: 700; cursor: pointer; transition: 0.2s; border-radius: 10px; }
.custom-option:hover { background: var(--bg-hover); transform: translateX(4px); }
.custom-option.is-selected { background: var(--text-primary); color: var(--bg-base); }

/* AI Reasoning Card */
.reasoning-card { background: var(--bg-surface); border-color: var(--text-primary); box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
.reasoning-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: var(--text-primary); }
.ai-reasoning-text { font-size: 13px; font-weight: 600; color: var(--text-secondary); line-height: 1.7; margin: 0; font-style: italic; }

.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 24px 32px 32px; border-top: 1px solid var(--border); background: rgba(255,255,255,0.02); }
.btn-cancel, .btn-save { padding: 16px 32px; font-size: 13px; font-weight: 900; border-radius: 14px; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border: 1px solid var(--border); letter-spacing: 0.05em; }
.btn-cancel { background: transparent; color: var(--text-muted); }
.btn-cancel:hover { background: var(--bg-hover); border-color: var(--text-primary); color: var(--text-primary); transform: translateY(-1px); }
.btn-save { background: var(--bg-dark); border-color: var(--bg-dark); color: var(--bg-base); box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
.btn-save:hover { opacity: 0.95; transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.15); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

.dropdown-fade-enter-active, .dropdown-fade-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.dropdown-fade-enter-from, .dropdown-fade-leave-to { opacity: 0; transform: translateY(-10px); }

/* Input type=date/time icon styling */
input::-webkit-calendar-picker-indicator {
  opacity: 0;
  position: absolute;
  inset: 0;
  width: 100%; height: 100%; cursor: pointer;
}
</style>