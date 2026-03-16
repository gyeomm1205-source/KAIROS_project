<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="modal-box">

          <div class="modal-header">
            <h3 class="modal-title">{{ mode === 'create' ? 'ADD SCHEDULE' : 'EDIT SCHEDULE' }}</h3>
            <button class="modal-close" @click="$emit('update:modelValue', false)">
              <i class="fas fa-times" />
            </button>
          </div>

          <div class="modal-body">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">DATE</label>
                <input v-model="localForm.day" type="date" :min="monthMin" :max="monthMax" class="form-input" />
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

            <EdgeConnectorSection
              v-model:parentIds="localForm.parentIds"
              v-model:childIds="localForm.childIds"
              :parents="availableParents"
              :children="availableChildren"
            />

            <div class="form-group">
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

            <div class="form-group">
              <label class="form-label">
                LABEL <span class="badge-optional">OPTIONAL</span>
              </label>
              <input v-model="localForm.text" type="text" placeholder="Short description" class="form-input" />
            </div>

            <div class="form-row">
              <div class="form-group" style="flex:0 0 160px">
                <label class="form-label">TIME</label>
                <input v-model="localForm.time" type="time" class="form-input" />
              </div>
              <div class="form-group">
                <label class="form-label">
                  TAGS <span class="badge-optional">OPTIONAL</span>
                </label>
                <input v-model="localForm.tags" type="text" placeholder="#React, #Frontend" class="form-input" />
              </div>
            </div>

            <div class="form-group mt-2 p-4 border-2 border-gray-800 bg-gray-100">
              <label class="form-label text-gray-800 mb-2">
                <i class="fas fa-magic text-blue-600"></i> AI REASONING 
                <span class="badge-optional border-gray-800 text-gray-800">READ-ONLY</span>
              </label>
              <p class="text-[13px] font-bold text-gray-600 leading-relaxed">
                {{ localForm.reasoning || '사용자가 직접 생성한 일정입니다. (AI 추천 내역 없음)' }}
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'
import EdgeConnectorSection from './EdgeConnectorSection.vue'

const props = defineProps({
  modelValue:  { type: Boolean, required: true },
  mode:        { type: String,  default: 'create' },
  initialForm: { type: Object,  default: () => ({}) },
  editNodeId:  { type: String,  default: null }
})
const emit = defineEmits(['update:modelValue', 'save'])

const store = useCalendarStore()
const { schedules, allTracks, endedTracks } = storeToRefs(store)

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

// 폼 초기화 시 reasoning 필드 추가
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
    reasoning: '' // 신규 필드 추가
  }
}

const selectedTrackName = computed(() => { const t = allTracks.value.find(x => x.id === localForm.value.track); return t ? t.name : '' })
const selectedTrackColor = computed(() => { const t = allTracks.value.find(x => x.id === localForm.value.track); return t ? t.color : 'transparent' })

const monthMin = computed(() => { if (!localForm.value.day) return ''; const [y, m] = localForm.value.day.split('-').map(Number); return `${y}-${String(m).padStart(2,'0')}-01` })
const monthMax = computed(() => { if (!localForm.value.day) return ''; const [y, m] = localForm.value.day.split('-').map(Number); const last = new Date(y, m, 0).getDate(); return `${y}-${String(m).padStart(2,'0')}-${String(last).padStart(2,'0')}` })

watch(() => props.initialForm, (val) => {
  const base = defaultForm()
  localForm.value = { 
    ...base, 
    ...val, 
    parentIds: Array.isArray(val.parentIds) ? val.parentIds : (val.parentId ? [val.parentId] : []), 
    childIds:  Array.isArray(val.childIds)  ? val.childIds  : (val.childId  ? [val.childId]  : []) 
  }
  titleError.value = false
}, { immediate: true })

const availableParents = computed(() => schedules.value.filter(s => s.day <= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => b.day.localeCompare(a.day)))
const availableChildren = computed(() => schedules.value.filter(s => s.day >= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => a.day.localeCompare(b.day)))

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
    reasoning: localForm.value.reasoning // 저장 시 데이터 유지
  })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(2px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.modal-box { width: 100%; max-width: 520px; max-height: 90vh; overflow-y: auto; border-radius: 0; background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 12px 12px 0 var(--text-primary); scrollbar-width: none; }
.modal-box::-webkit-scrollbar { display: none; }

.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 32px 20px; border-bottom: 2px solid var(--text-primary); }
.modal-title { font-size: 20px; font-weight: 900; letter-spacing: 0.05em; color: var(--text-primary); }
.modal-close { background: transparent; border: none; color: var(--text-primary); font-size: 20px; cursor: pointer; transition: 0.1s; }
.modal-close:hover { transform: scale(1.2); }

.modal-body { padding: 24px 32px; display: flex; flex-direction: column; gap: 24px; }
.form-row   { display: flex; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 8px; flex: 1; }
.form-label { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); }

.badge-required { margin-left: auto; font-size: 10px; font-weight: 800; background: var(--text-primary); color: var(--bg-base); padding: 2px 6px; }
.badge-optional { margin-left: auto; font-size: 10px; font-weight: 800; border: 1px solid var(--text-primary); color: var(--text-primary); padding: 2px 6px; }

.form-input { width: 100%; background: transparent; border: 2px solid var(--border); color: var(--text-primary); font-size: 14px; font-weight: 700; border-radius: 0; padding: 12px 16px; outline: none; transition: 0.1s; font-family: 'Escoredream', sans-serif; }
.form-input:focus { border-color: var(--text-primary); box-shadow: 4px 4px 0 var(--border); }
.form-input--error { border-color: #ef4444; }
.form-error { font-size: 12px; font-weight: 800; color: #ef4444; margin: 0; }

.custom-select-container { position: relative; width: 100%; }
.custom-select-trigger { display: flex; align-items: center; justify-content: space-between; cursor: pointer; user-select: none; }
.selected-value { display: flex; align-items: center; gap: 10px; font-weight: 800; }
.track-color-indicator { width: 12px; height: 12px; border-radius: 0; border: 2px solid var(--text-primary); flex-shrink: 0; }
.arrow-icon { font-size: 12px; transition: transform 0.2s; }
.custom-select-trigger.is-open .arrow-icon { transform: rotate(180deg); }
.empty-placeholder { font-size: 13px; font-weight: 700; color: var(--text-muted); }

.custom-options-menu { position: absolute; top: calc(100% + 4px); left: 0; right: 0; background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 6px 6px 0 var(--text-primary); z-index: 200; max-height: 240px; overflow-y: auto; padding: 8px; scrollbar-width: none; }
.opt-group-label { font-size: 11px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-muted); padding: 8px; border-bottom: 1px solid var(--border); margin-bottom: 4px; }
.custom-option { display: flex; align-items: center; gap: 10px; padding: 12px 10px; font-size: 13px; font-weight: 800; cursor: pointer; transition: 0.1s; }
.custom-option:hover, .custom-option.is-selected { background: var(--text-primary); color: var(--bg-base); }
.custom-option:hover .track-color-indicator { border-color: var(--bg-base); }

.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 24px 32px; border-top: 2px solid var(--text-primary); background: var(--bg-surface); }
.btn-cancel, .btn-save { padding: 14px 28px; font-size: 14px; font-weight: 900; letter-spacing: 0.1em; border-radius: 0; border: 2px solid var(--text-primary); cursor: pointer; transition: 0.1s; font-family: 'Space Grotesk', 'Escoredream', sans-serif; }
.btn-cancel { background: transparent; color: var(--text-primary); }
.btn-cancel:hover { box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); }
.btn-save { background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 var(--text-primary); }
.btn-save:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--text-primary); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.97) translateY(-10px); }
</style>