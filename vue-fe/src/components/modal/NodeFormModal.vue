<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-box" @click.stop>

          <!-- ── HEADER ── -->
          <div class="modal-header">
            <div class="header-left">
              <span class="mode-tag">{{ mode === 'create' ? 'NEW' : 'EDIT' }}</span>
              <span class="header-title">{{ mode === 'create' ? 'ADD SCHEDULE' : 'EDIT SCHEDULE' }}</span>
            </div>
            <button class="btn-icon" @click="close"><i class="fas fa-times" /></button>
          </div>

          <!-- ── BODY ── -->
          <div class="modal-body custom-scroll">

            <!-- 섹션 1: 날짜 + 트랙 + 제목 -->
            <div class="form-section">
              <div class="section-label">기본 정보</div>

              <div class="field-row">
                <!-- 날짜 -->
                <div class="field" style="flex: 0 0 160px">
                  <label class="field-label">DATE</label>
                  <div class="input-wrap">
                    <i class="fas fa-calendar-alt icon-left" />
                    <input v-model="localForm.day" type="date" class="field-input" />
                  </div>
                </div>

                <!-- 시간 -->
                <div class="field" style="flex: 0 0 130px">
                  <label class="field-label">TIME</label>
                  <div class="input-wrap">
                    <i class="fas fa-clock icon-left" />
                    <input v-model="localForm.time" type="time" class="field-input" />
                  </div>
                </div>

                <!-- 트랙 -->
                <div class="field" ref="trackFieldRef">
                  <label class="field-label">TRACK <span class="req-dot">*</span></label>
                  <div
                    class="track-trigger"
                    :class="{ open: dropdownOpen, error: trackError }"
                    @click.stop="dropdownOpen = !dropdownOpen"
                  >
                    <span v-if="localForm.track" class="track-selected">
                      <span class="track-dot" :style="{ background: selectedTrackColor }" />
                      {{ selectedTrackName }}
                    </span>
                    <span v-else class="track-placeholder">트랙 선택</span>
                    <i class="fas fa-chevron-down chevron" />
                  </div>

                  <!-- 드롭다운 -->
                  <Transition name="dd-fade">
                    <div v-if="dropdownOpen" class="track-dropdown" @click.stop>
                      <div v-if="store.activeTracks.length" class="dd-group">
                        <div class="dd-group-label">USER TRACKS</div>
                        <div
                          v-for="t in store.activeTracks" :key="t.id"
                          class="dd-item"
                          :class="{ active: localForm.track === t.id }"
                          @click="selectTrack(t.id)"
                        >
                          <span class="track-dot" :style="{ background: t.color }" />
                          <span>{{ t.name }}</span>
                          <i v-if="localForm.track === t.id" class="fas fa-check dd-check" />
                        </div>
                      </div>
                      <div v-if="store.HIGHLIGHT_TRACKS?.length" class="dd-group">
                        <div class="dd-group-label">SYSTEM TRACKS</div>
                        <div
                          v-for="t in store.HIGHLIGHT_TRACKS" :key="t.id"
                          class="dd-item"
                          :class="{ active: localForm.track === t.id }"
                          @click="selectTrack(t.id)"
                        >
                          <span class="track-dot" :style="{ background: t.color }" />
                          <span>{{ t.name }}</span>
                          <i v-if="localForm.track === t.id" class="fas fa-check dd-check" />
                        </div>
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>

              <!-- 제목 -->
              <div class="field mt-14">
                <label class="field-label">TITLE <span class="req-dot">*</span></label>
                <input
                  v-model="localForm.title"
                  type="text"
                  placeholder="일정 제목을 입력하세요"
                  class="field-input"
                  :class="{ error: titleError }"
                  @input="titleError = false"
                />
                <p v-if="titleError" class="error-msg"><i class="fas fa-exclamation-circle" /> 제목은 필수입니다.</p>
              </div>

              <!-- 메모 -->
              <div class="field mt-14">
                <label class="field-label">MEMO <span class="opt-badge">선택</span></label>
                <input
                  v-model="localForm.text"
                  type="text"
                  placeholder="짧은 메모 (캘린더에 표시됩니다)"
                  class="field-input"
                />
              </div>

              <!-- 태그 -->
              <div class="field mt-14">
                <label class="field-label">TAGS <span class="opt-badge">선택</span></label>
                <input
                  v-model="localForm.tags"
                  type="text"
                  placeholder="React, Frontend, TypeScript"
                  class="field-input"
                />
                <p class="field-hint">쉼표(,)로 구분</p>
              </div>
            </div>

            <!-- 구분선 -->
            <div class="divider" />

            <!-- 섹션 2: 연결 -->
            <div class="form-section">
              <div class="section-label">노드 연결 <span class="opt-badge">선택</span></div>
              <EdgeConnectorSection
                v-model:parentIds="localForm.parentIds"
                v-model:childIds="localForm.childIds"
                :parents="availableParents"
                :children="availableChildren"
                @jump="$emit('jump', $event)"
              />
            </div>

          </div>

          <!-- ── FOOTER ── -->
          <div class="modal-footer">
            <!-- 편집 모드일 때만 삭제 버튼 -->
            <button v-if="mode === 'edit'" class="btn-delete" @click="handleDelete">
              <i class="fas fa-trash-alt" /> 삭제
            </button>
            <button class="btn-save" @click="handleSave">
              <i :class="mode === 'create' ? 'fas fa-plus' : 'fas fa-check'" />
              {{ mode === 'create' ? '추가' : '저장' }}
            </button>
            <button class="btn-cancel" @click="close">취소</button>
          </div>

        </div>
      </div>
    </Transition>

    <!-- 삭제 확인 모달 -->
    <Transition name="modal-fade">
      <div v-if="showDeleteConfirm" class="modal-overlay confirm-overlay" @click.self="showDeleteConfirm = false">
        <div class="confirm-box">
          <div class="confirm-icon"><i class="fas fa-exclamation-triangle" /></div>
          <h3 class="confirm-title">일정을 삭제할까요?</h3>
          <p class="confirm-desc">삭제된 일정과 연결 정보는 복구할 수 없습니다.</p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="showDeleteConfirm = false">취소</button>
            <button class="btn-confirm-delete" @click="confirmDelete">
              <i class="fas fa-trash-alt" /> 삭제
            </button>
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

const props = defineProps({
  modelValue:  Boolean,
  mode:        String,
  initialForm: Object,
  editNodeId:  String
})
const emit = defineEmits(['update:modelValue', 'save', 'jump', 'delete'])

const store = useCalendarStore()

const localForm  = ref(defaultForm())
const titleError = ref(false)
const trackError = ref(false)
const dropdownOpen = ref(false)
const showDeleteConfirm = ref(false)
const trackFieldRef = ref(null)

function defaultForm() {
  const t = new Date()
  return {
    day: `${t.getFullYear()}-${String(t.getMonth()+1).padStart(2,'0')}-${String(t.getDate()).padStart(2,'0')}`,
    track: '', title: '', text: '', time: '09:00', tags: '',
    parentIds: [], childIds: [], reasoning: ''
  }
}

watch(() => props.initialForm, (val) => {
  const base = defaultForm()
  localForm.value = {
    ...base, ...val,
    parentIds: Array.isArray(val?.parentIds) ? val.parentIds : (val?.parentId ? [val.parentId] : []),
    childIds:  Array.isArray(val?.childIds)  ? val.childIds  : (val?.childId  ? [val.childId]  : [])
  }
  titleError.value = false
  trackError.value = false
  dropdownOpen.value = false
  showDeleteConfirm.value = false
}, { immediate: true })

// 드롭다운 외부 클릭 닫기
function onDocClick(e) {
  if (trackFieldRef.value && !trackFieldRef.value.contains(e.target)) {
    dropdownOpen.value = false
  }
}
onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))

function selectTrack(id) {
  localForm.value.track = id
  dropdownOpen.value = false
  trackError.value = false
}

const selectedTrackName  = computed(() => store.allTracks.find(x => x.id === localForm.value.track)?.name  ?? '')
const selectedTrackColor = computed(() => store.allTracks.find(x => x.id === localForm.value.track)?.color ?? 'transparent')

const availableParents  = computed(() =>
  store.schedules.filter(s => s.day <= localForm.value.day && s.id !== props.editNodeId)
    .sort((a, b) => b.day.localeCompare(a.day))
)
const availableChildren = computed(() =>
  store.schedules.filter(s => s.day >= localForm.value.day && s.id !== props.editNodeId)
    .sort((a, b) => a.day.localeCompare(b.day))
)

function close() { emit('update:modelValue', false) }

function handleSave() {
  let valid = true
  if (!localForm.value.track)       { trackError.value = true; valid = false }
  if (!localForm.value.title.trim()) { titleError.value = true; valid = false }
  if (!valid) return

  const tags = (typeof localForm.value.tags === 'string')
    ? localForm.value.tags.split(',').map(t => t.trim()).filter(Boolean)
    : []

  emit('save', {
    day: localForm.value.day,
    track: localForm.value.track,
    text: localForm.value.text,
    tooltip: { title: localForm.value.title.trim(), time: localForm.value.time, tags },
    parentIds: localForm.value.parentIds || [],
    childIds:  localForm.value.childIds  || [],
    reasoning: localForm.value.reasoning
  })
}

function handleDelete() {
  showDeleteConfirm.value = true
}

function confirmDelete() {
  showDeleteConfirm.value = false
  emit('delete', props.editNodeId)
  close()
}
</script>

<style scoped>
/* ── OVERLAY ── */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

/* ── BOX ── */
.modal-box {
  width: 100%; max-width: 600px; max-height: 88vh;
  display: flex; flex-direction: column;
  background: var(--bg-base);
  border: 1px solid var(--border);
  overflow: hidden;
  animation: pop-in 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes pop-in {
  from { opacity: 0; transform: translateY(16px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0)    scale(1); }
}

/* ── HEADER ── */
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.mode-tag {
  font-size: 9px; font-weight: 900; letter-spacing: 0.15em;
  padding: 3px 8px; border-radius: 8px;
  background: var(--text-primary); color: var(--bg-base);
}
.header-title {
  font-size: 15px; font-weight: 900; letter-spacing: 0.08em;
  color: var(--text-primary);
}
.btn-icon {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border);
  color: var(--text-muted); font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-icon:hover { border-color: var(--text-primary); color: var(--text-primary); }

/* ── BODY ── */
.modal-body {
  flex: 1; overflow-y: auto;
  padding: 0;
}
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); }

.form-section { padding: 24px; }

.section-label {
  font-size: 10px; font-weight: 900; letter-spacing: 0.15em;
  color: var(--text-faint); text-transform: uppercase;
  margin-bottom: 18px;
  display: flex; align-items: center; gap: 8px;
}

.divider {
  height: 1px; background: var(--border); margin: 0;
}

.mt-14 { margin-top: 14px; }

/* ── FIELDS ── */
.field-row { display: flex; gap: 12px; flex-wrap: wrap; }
.field { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 0; }

.field-label {
  font-size: 9px; font-weight: 900; letter-spacing: 0.14em;
  color: var(--text-faint); text-transform: uppercase;
  display: flex; align-items: center; gap: 6px;
}
.req-dot { color: var(--text-primary); font-size: 11px; line-height: 1; }
.opt-badge {
  font-size: 8px; font-weight: 700; letter-spacing: 0.05em;
  padding: 1px 6px; border: 1px solid var(--border); border-radius: 8px;
  color: var(--text-faint);
}

.input-wrap { position: relative; }
.icon-left {
  position: absolute; left: 12px; top: 50%; transform: translateY(-50%);
  font-size: 11px; color: var(--text-faint); pointer-events: none;
  transition: color 0.2s;
}

.field-input {
  width: 100%;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  color: var(--text-primary);
  font-size: 13px; font-weight: 600;
  padding: 10px 12px; border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}
.field-input:focus { border-color: var(--text-primary); }
.field-input:focus + .icon-left,
.input-wrap:focus-within .icon-left { color: var(--text-primary); }
.field-input.error { border-color: #ef4444; }

/* date/time 아이콘 있을 때 왼쪽 패딩 */
.input-wrap .field-input { padding-left: 34px; }

.error-msg {
  font-size: 11px; font-weight: 700; color: #ef4444;
  display: flex; align-items: center; gap: 4px;
}
.field-hint { font-size: 10px; color: var(--text-faint); font-weight: 600; }

/* date/time picker 숨김 */
input::-webkit-calendar-picker-indicator {
  opacity: 0; position: absolute; inset: 0; width: 100%; height: 100%; cursor: pointer;
}

/* ── TRACK DROPDOWN ── */
.track-trigger {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 10px 12px; border-radius: 8px;
  cursor: pointer; user-select: none;
  transition: border-color 0.2s;
  min-height: 41px;
}
.track-trigger:hover, .track-trigger.open { border-color: var(--text-primary); }
.track-trigger.error { border-color: #ef4444; }

.track-selected { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 700; color: var(--text-primary); }
.track-placeholder { font-size: 13px; font-weight: 600; color: var(--text-faint); }
.track-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.chevron { font-size: 9px; color: var(--text-faint); transition: transform 0.25s; }
.track-trigger.open .chevron { transform: rotate(180deg); }

.track-dropdown {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0;
  background: var(--bg-base);
  border: 1px solid var(--text-primary); border-radius: 8px;
  z-index: 300;
  max-height: 220px; overflow-y: auto;
}
.dd-group { padding: 6px 0; }
.dd-group + .dd-group { border-top: 1px solid var(--border); }
.dd-group-label {
  font-size: 9px; font-weight: 900; letter-spacing: 0.12em;
  color: var(--text-faint); padding: 8px 14px 4px;
  text-transform: uppercase;
}
.dd-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  cursor: pointer; transition: background 0.15s;
}
.dd-item:hover { background: var(--bg-hover); }
.dd-item.active { background: var(--text-primary); color: var(--bg-base); }
.dd-item.active .track-dot { box-shadow: 0 0 0 1px var(--bg-base); }
.dd-check { margin-left: auto; font-size: 11px; }

/* ── FOOTER ── */
.modal-footer {
  display: flex; align-items: center; justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
  gap: 8px;
}

.btn-delete {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 16px;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 12px; font-weight: 800; letter-spacing: 0.04em;
  cursor: pointer; transition: all 0.2s;
  font-family: inherit;
}
.btn-delete:hover { border-color: #ef4444; color: #ef4444; background: rgba(239,68,68,0.05); }

.btn-cancel {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 12px; font-weight: 800; letter-spacing: 0.06em;
  cursor: pointer; transition: all 0.2s;
  font-family: inherit;
}
.btn-cancel:hover { border-color: var(--text-primary); color: var(--text-primary); }

.btn-save {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 24px;
  background: var(--text-primary);
  border: 1px solid var(--text-primary);
  color: var(--bg-base);
  font-size: 12px; font-weight: 900; letter-spacing: 0.06em;
  cursor: pointer; transition: all 0.2s;
  font-family: inherit;
}
.btn-save:hover { opacity: 0.85; transform: translateY(-1px); }

/* ── DELETE CONFIRM ── */
.confirm-overlay { z-index: 2100; }
.confirm-box {
  width: 100%; max-width: 360px;
  background: var(--bg-base);
  border: 1px solid var(--border); border-radius: 8px;
  padding: 36px 32px;
  text-align: center;
  animation: pop-in 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.confirm-icon {
  width: 48px; height: 48px; margin: 0 auto 16px;
  border: 1px solid #ef4444; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #ef4444;
}
.confirm-title {
  font-size: 16px; font-weight: 900; letter-spacing: 0.02em;
  color: var(--text-primary); margin-bottom: 8px;
}
.confirm-desc {
  font-size: 12px; font-weight: 600; color: var(--text-muted); line-height: 1.6; margin-bottom: 28px;
}
.confirm-actions { display: flex; gap: 8px; justify-content: center; }
.btn-confirm-delete {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 24px;
  background: #ef4444;
  border: 1px solid #ef4444; border-radius: 8px;
  color: #fff;
  font-size: 12px; font-weight: 900; letter-spacing: 0.06em;
  cursor: pointer; transition: all 0.2s;
  font-family: inherit;
}
.btn-confirm-delete:hover { opacity: 0.85; }

/* ── TRANSITIONS ── */
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.25s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }

.dd-fade-enter-active,
.dd-fade-leave-active { transition: opacity 0.18s, transform 0.18s; }
.dd-fade-enter-from,
.dd-fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
