<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="modal-box">

          <div class="modal-header">
            <div class="modal-title-wrap">
              <div class="modal-icon">
                <i :class="mode === 'create' ? 'fas fa-calendar-plus' : 'fas fa-edit'" />
              </div>
              <h3>{{ mode === 'create' ? '일정 추가' : '일정 수정' }}</h3>
            </div>
            <button class="modal-close" @click="$emit('update:modelValue', false)">
              <i class="fas fa-times" />
            </button>
          </div>

          <div class="modal-body">

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">
                  <i class="fas fa-calendar" />날짜
                </label>
                <input v-model="localForm.day" type="date" :min="monthMin" :max="monthMax" class="form-input" />
              </div>
              
              <div class="form-group relative">
                <label class="form-label">
                  <i class="fas fa-layer-group" />학습 트랙
                </label>
                
                <div class="custom-select-container" ref="dropdownRef">
                  <div 
                    class="form-input custom-select-trigger" 
                    :class="{ 'is-open': isDropdownOpen }"
                    @click="isDropdownOpen = !isDropdownOpen"
                  >
                    <template v-if="localForm.track">
                      <div class="selected-value">
                        <span class="track-color-indicator" :style="{ background: selectedTrackColor }"></span>
                        <span class="sv-text">{{ selectedTrackName }}</span>
                      </div>
                    </template>
                    
                    <template v-else>
                      <div class="empty-placeholder">
                        <span class="ph-line"></span>
                        <span class="ph-text">학습 트랙 선택</span>
                        <span class="ph-line"></span>
                      </div>
                    </template>
                    <i class="fas fa-chevron-down arrow-icon" />
                  </div>

                  <Transition name="dropdown-fade">
                    <div v-if="isDropdownOpen" class="custom-options-menu">
                      
                      <div class="opt-group" v-if="store.activeTracks.length">
                        <div class="opt-group-label">진행 중인 사용자 트랙</div>
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

                      <div class="opt-group" v-if="store.HIGHLIGHT_TRACKS.length">
                        <div class="opt-group-label">시스템 트랙</div>
                        <div 
                          v-for="t in store.HIGHLIGHT_TRACKS" :key="t.id"
                          class="custom-option"
                          :class="{ 'is-selected': localForm.track === t.id }"
                          @click="selectTrack(t.id)"
                        >
                          <span class="track-color-indicator" :style="{ background: t.color }"></span>
                          ✦ {{ t.name }}
                        </div>
                      </div>

                      <div class="opt-group" v-if="isEditingEndedTrack">
                        <div class="opt-group-label">종료된 트랙 (수정 불가)</div>
                        <div class="custom-option is-disabled">
                          <span class="track-color-indicator" :style="{ background: endedTrackColor }"></span>
                          {{ endedTrackName }}
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
                <i class="fas fa-heading" />일정 제목
                <span class="badge-required">필수</span>
              </label>
              <input
                v-model="localForm.title"
                type="text"
                placeholder="예: React Hooks 학습"
                class="form-input"
                :class="{ 'form-input--error': titleError }"
                @input="titleError = false"
              />
              <p v-if="titleError" class="form-error">
                <i class="fas fa-exclamation-circle" /> 제목을 입력해야 저장할 수 있어요.
              </p>
            </div>

            <div class="form-group">
              <label class="form-label">
                <i class="fas fa-tag" />캘린더 라벨
                <span class="badge-optional">선택</span>
              </label>
              <input v-model="localForm.text" type="text" placeholder="예: React 기초 (짧게)" class="form-input" />
            </div>

            <div class="form-row">
              <div class="form-group" style="flex:0 0 160px">
                <label class="form-label"><i class="fas fa-clock" />시간</label>
                <input v-model="localForm.time" type="time" class="form-input" />
              </div>
              <div class="form-group">
                <label class="form-label">
                  <i class="fas fa-hashtag" />태그
                  <span class="badge-optional">선택</span>
                </label>
                <input v-model="localForm.tags" type="text" placeholder="#React, #Frontend" class="form-input" />
              </div>
            </div>

          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="$emit('update:modelValue', false)">취소</button>
            <button class="btn-save" @click="handleSave">
              <i :class="mode === 'create' ? 'fas fa-plus' : 'fas fa-check'" />
              {{ mode === 'create' ? '일정 추가' : '저장하기' }}
            </button>
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

// ★ 드롭다운 상태 및 바깥 클릭 감지
const dropdownRef = ref(null)
const isDropdownOpen = ref(false)

function closeDropdown(e) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    isDropdownOpen.value = false
  }
}
onMounted(() => document.addEventListener('click', closeDropdown))
onUnmounted(() => document.removeEventListener('click', closeDropdown))

function selectTrack(id) {
  localForm.value.track = id
  isDropdownOpen.value = false
}

function defaultForm() {
  const t = new Date()
  return {
    day: `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')}`,
    track: '', 
    title: '', text: '', time: '09:00', tags: '', parentIds: [], childIds: []
  }
}

// 선택된 트랙 정보 가져오기
const selectedTrackName = computed(() => {
  const t = allTracks.value.find(x => x.id === localForm.value.track)
  return t ? (t.isHighlight ? `✦ ${t.name}` : t.name) : ''
})
const selectedTrackColor = computed(() => {
  const t = allTracks.value.find(x => x.id === localForm.value.track)
  return t ? t.color : 'transparent'
})

// 수정 모드일 때 종료된 트랙 처리
const isEditingEndedTrack = computed(() => {
  if (props.mode !== 'edit' || !localForm.value.track) return false
  return endedTracks.value.some(t => t.id === localForm.value.track)
})
const endedTrackName = computed(() => endedTracks.value.find(t => t.id === localForm.value.track)?.name || '')
const endedTrackColor = computed(() => endedTracks.value.find(t => t.id === localForm.value.track)?.color || '#ccc')

const monthMin = computed(() => {
  if (!localForm.value.day) return ''
  const [y, m] = localForm.value.day.split('-').map(Number)
  return `${y}-${String(m).padStart(2,'0')}-01`
})
const monthMax = computed(() => {
  if (!localForm.value.day) return ''
  const [y, m] = localForm.value.day.split('-').map(Number)
  const last = new Date(y, m, 0).getDate()
  return `${y}-${String(m).padStart(2,'0')}-${String(last).padStart(2,'0')}`
})

watch(() => props.initialForm, (val) => {
  const base = defaultForm()
  localForm.value = {
    ...base,
    ...val,
    parentIds: Array.isArray(val.parentIds) ? val.parentIds : (val.parentId ? [val.parentId] : []),
    childIds:  Array.isArray(val.childIds)  ? val.childIds  : (val.childId  ? [val.childId]  : []),
  }
  titleError.value = false
}, { immediate: true })

const availableParents = computed(() => schedules.value.filter(s => s.day <= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => b.day.localeCompare(a.day)))
const availableChildren = computed(() => schedules.value.filter(s => s.day >= localForm.value.day && s.id !== props.editNodeId).sort((a, b) => a.day.localeCompare(b.day)))

function handleSave() {
  if (!localForm.value.track) {
    alert('학습 트랙을 선택해주세요.')
    return
  }
  if (!localForm.value.title.trim()) { 
    titleError.value = true; 
    return 
  }
  const tags = localForm.value.tags.split(',').map(t => t.trim()).filter(Boolean)
  emit('save', {
    day: localForm.value.day, track: localForm.value.track, text: localForm.value.text,
    tooltip: { title: localForm.value.title.trim(), time: localForm.value.time, tags },
    parentIds: localForm.value.parentIds || [], childIds: localForm.value.childIds || []
  })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(4px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; }
.modal-box { width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; border-radius: 20px; background: var(--modal-bg); border: 2px solid var(--modal-border); box-shadow: var(--modal-shadow); scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 22px 16px; border-bottom: 1px solid var(--modal-divider); }
.modal-title-wrap { display: flex; align-items: center; gap: 10px; }
.modal-icon { width: 36px; height: 36px; border-radius: 10px; background: rgba(99,102,241,0.2); border: 1.5px solid rgba(99,102,241,0.4); color: #818cf8; display: flex; align-items: center; justify-content: center; font-size: 15px; flex-shrink: 0; }
.modal-header h3 { font-size: 17px; font-weight: 800; color: var(--text-primary); font-family: 'Escoredream', sans-serif; letter-spacing: -0.3px; }
.modal-close { width: 32px; height: 32px; border-radius: 8px; background: var(--modal-input-bg); border: 1.5px solid var(--modal-border); color: var(--text-muted); cursor: pointer; font-size: 13px; display: flex; align-items: center; justify-content: center; transition: all 0.15s; flex-shrink: 0; }
.modal-close:hover { background: var(--bg-hover); color: var(--text-primary); }

.modal-body { padding: 18px 22px; display: flex; flex-direction: column; gap: 16px; }
.form-row   { display: flex; gap: 12px; }
.form-group { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.form-label { display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 700; color: var(--text-muted); font-family: 'Escoredream', sans-serif; letter-spacing: 0.3px; }
.form-label i { font-size: 10px; color: var(--accent); }

.badge-required { margin-left: auto; font-size: 9px; font-weight: 700; color: #f87171; background: rgba(248,113,113,0.15); border: 1px solid rgba(248,113,113,0.3); padding: 1px 7px; border-radius: 999px; }
.badge-optional { margin-left: auto; font-size: 9px; font-weight: 500; color: var(--text-faint); background: var(--modal-input-bg); border: 1px solid var(--modal-border); padding: 1px 7px; border-radius: 999px; }

.form-input {
  width: 100%; box-sizing: border-box; background: var(--modal-input-bg); border: 1.5px solid var(--modal-border);
  color: var(--text-primary); font-size: 13px; font-weight: 500; font-family: 'Escoredream', sans-serif;
  border-radius: 10px; padding: 9px 13px; outline: none; transition: border-color 0.15s, background 0.15s;
}
:global(body.is-dark) .form-input[type="date"], :global(body.is-dark) .form-input[type="time"] { color-scheme: dark; }
:global(body.is-light) .form-input[type="date"], :global(body.is-light) .form-input[type="time"] { color-scheme: light; }
.form-input:hover  { border-color: var(--border-mid); }
.form-input:focus  { border-color: var(--accent); background: var(--modal-input-focus-bg); }
.form-input::placeholder { color: var(--text-faint); opacity: 1; }

/* ── ★ 커스텀 셀렉트 UI ── */
.custom-select-container { position: relative; width: 100%; }
.custom-select-trigger { display: flex; align-items: center; justify-content: space-between; cursor: pointer; user-select: none; }
.selected-value { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.track-color-indicator { width: 10px; height: 10px; border-radius: 50%; box-shadow: 0 0 4px rgba(0,0,0,0.2); flex-shrink: 0; }
.sv-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.arrow-icon { font-size: 12px; color: var(--text-muted); transition: transform 0.2s; flex-shrink: 0; margin-left: 8px; }
.custom-select-trigger.is-open .arrow-icon { transform: rotate(180deg); }

/* 양옆 대시라인이 들어간 플레이스홀더 */
.empty-placeholder { display: flex; align-items: center; gap: 10px; width: 100%; padding-right: 8px; }
.ph-line { flex: 1; height: 1px; background: repeating-linear-gradient(to right, var(--border-mid) 0, var(--border-mid) 4px, transparent 4px, transparent 8px); opacity: 0.5; }
.ph-text { font-size: 11px; color: var(--text-muted); font-weight: 600; flex-shrink: 0; }

.custom-options-menu {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0;
  background: var(--modal-bg); border: 1px solid var(--modal-border);
  border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  z-index: 200; max-height: 240px; overflow-y: auto; padding: 8px;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}
.opt-group { margin-bottom: 8px; }
.opt-group:last-child { margin-bottom: 0; }
.opt-group-label { font-size: 10px; font-weight: 700; color: var(--text-faint); padding: 4px 8px; margin-bottom: 2px; }
.custom-option {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px;
  border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: 600;
  color: var(--text-primary); transition: background 0.15s; font-family: 'Escoredream', sans-serif;
}
.custom-option:hover { background: var(--bg-hover); }
.custom-option.is-selected { background: rgba(99,102,241,0.1); color: #818cf8; }
.custom-option.is-disabled { cursor: not-allowed; opacity: 0.45; }
.custom-option.is-disabled:hover { background: transparent; }

.dropdown-fade-enter-active { transition: opacity 0.15s, transform 0.15s; transform-origin: top; }
.dropdown-fade-leave-active { transition: opacity 0.1s, transform 0.1s; transform-origin: top; }
.dropdown-fade-enter-from, .dropdown-fade-leave-to { opacity: 0; transform: scaleY(0.95); }

.form-error { font-size: 11px; color: #f87171; margin: 0; display: flex; align-items: center; gap: 5px; animation: shake 0.3s ease; }
@keyframes shake { 0%,100% { transform: translateX(0) } 25% { transform: translateX(-4px) } 75% { transform: translateX(4px) } }

.modal-footer { display: flex; justify-content: flex-end; align-items: center; gap: 10px; padding: 14px 22px 20px; border-top: 1px solid var(--modal-divider); }
.btn-cancel { padding: 8px 18px; background: var(--modal-input-bg); border: 1.5px solid var(--modal-border); color: var(--text-muted); font-size: 13px; font-weight: 600; font-family: 'Escoredream', sans-serif; border-radius: 10px; cursor: pointer; transition: all 0.15s; }
.btn-cancel:hover { background: var(--bg-hover); color: var(--text-primary); border-color: var(--border-mid); }
.btn-save { display: flex; align-items: center; gap: 7px; padding: 9px 22px; background: var(--accent); border: none; border-radius: 10px; color: #fff; font-size: 13px; font-weight: 700; font-family: 'Escoredream', sans-serif; cursor: pointer; box-shadow: 0 4px 16px var(--accent-glow); transition: all 0.15s; }
.btn-save:hover  { opacity: 0.88; transform: translateY(-1px); }
.btn-save:active { transform: translateY(0); }

.modal-fade-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-fade-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.modal-fade-enter-from   { opacity: 0; transform: scale(0.97) translateY(-8px); }
.modal-fade-leave-to     { opacity: 0; transform: scale(0.97) translateY(-4px); }
</style>