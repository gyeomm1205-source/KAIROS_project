<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="modal-box">

          <!-- 헤더 -->
          <div class="modal-header">
            <div class="modal-header-left">
              <div class="modal-icon"><i class="fas fa-layer-group" /></div>
              <div>
                <h3 class="modal-title">학습 트랙 관리</h3>
                <p class="modal-desc">드래그로 순서 조정 · 순서가 캘린더 노드 높이를 결정합니다</p>
              </div>
            </div>
            <button class="modal-close" @click="$emit('update:modelValue', false)"><i class="fas fa-times" /></button>
          </div>

          <div class="modal-body">

            <div class="section-header">
              <span class="section-label">사용자 트랙</span>
              <span class="section-count">{{ sortedUserTracks.length }}</span>
            </div>

            <div class="track-list" @dragover.prevent="onDragOver" @drop.prevent="onDrop">
              <div
                v-for="t in sortedUserTracks" :key="t.id"
                class="track-item"
                :class="{ 'track-item--dragging': draggingId === t.id, 'track-item--over': dragOverId === t.id && draggingId !== t.id }"
                draggable="true"
                @dragstart="onDragStart($event, t.id)"
                @dragend="onDragEnd"
                @dragenter.prevent="onDragEnter(t.id)"
                @dragleave="onDragLeave($event, t.id)"
              >
                <div class="drag-handle" title="드래그해서 순서 변경"><i class="fas fa-grip-vertical" /></div>

                <div class="color-wrap" v-click-outside="() => closePickerFor(t.id)">
                  <button class="color-btn" :style="{ background: t.color, boxShadow: `0 0 0 3px ${t.color}30` }" @click.stop="togglePicker(t.id)" title="색상 변경" />
                  <Transition name="palette-pop">
                    <div v-if="openPickerId === t.id" class="color-palette" @click.stop>
                      <button v-for="c in PRESET_COLORS" :key="c" class="palette-swatch" :style="{ background: c }" :class="{ active: t.color === c }" @click="pickColor(t.id, c)" />
                      <label class="palette-custom-wrap" title="직접 선택">
                        <i class="fas fa-eyedropper" />
                        <input type="color" :value="t.color" @input="pickColor(t.id, $event.target.value)" />
                      </label>
                    </div>
                  </Transition>
                </div>

                <input :value="t.name" type="text" class="track-name-input" placeholder="트랙 이름" @input="t.name = $event.target.value" />

                <button v-if="t.id !== 'main'" class="delete-btn" title="트랙 삭제" @click="handleDelete(t.id)"><i class="fas fa-trash-alt" /></button>
                <span v-else class="delete-placeholder" />
              </div>
            </div>

            <button class="btn-add-track" @click="store.addTrack()"><i class="fas fa-plus" /> 새 트랙 추가</button>

            <div class="sys-section-header">
              <div class="sys-section-line" />
              <span class="sys-section-label"><i class="fas fa-highlighter" /> 형광펜 트랙 <em>(시스템 고정)</em></span>
              <div class="sys-section-line" />
            </div>

            <div class="track-list track-list--sys">
              <div v-for="t in highlightTracks" :key="t.id" class="track-item track-item--sys">
                <div class="sys-lock-icon"><i class="fas fa-lock" /></div>
                <div class="color-btn color-btn--locked" :style="{ background: t.color }" />
                <span class="track-name-readonly">{{ t.name }}<span class="badge-sys">시스템</span></span>
                <span class="delete-placeholder" />
              </div>
            </div>

          </div>

          <div class="modal-footer">
            <button class="btn-done" @click="$emit('update:modelValue', false)"><i class="fas fa-check" /> 완료</button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'

defineProps({ modelValue: { type: Boolean, required: true } })
defineEmits(['update:modelValue'])

const store = useCalendarStore()
const { tracks } = storeToRefs(store)

const sortedUserTracks = computed(() => [...tracks.value].sort((a, b) => a.index - b.index))
const highlightTracks  = computed(() => store.HIGHLIGHT_TRACKS)

const openPickerId = ref(null)
const draggingId   = ref(null)
const dragOverId   = ref(null)

const PRESET_COLORS = [
  '#0ea5e9','#38bdf8','#06b6d4','#3b82f6','#6366f1',
  '#8b5cf6','#a855f7','#ec4899','#f43f5e','#ef4444',
  '#f97316','#eab308','#22c55e','#10b981','#14b8a6'
]

function togglePicker(id) { openPickerId.value = openPickerId.value === id ? null : id }
function closePickerFor(id) { if (openPickerId.value === id) openPickerId.value = null }
function pickColor(id, color) { const t = tracks.value.find(t => t.id === id); if (t) t.color = color; openPickerId.value = null }
function handleDelete(id) { if (!confirm('이 트랙을 삭제하면 관련된 모든 일정도 삭제됩니다.')) return; store.deleteTrack(id) }

function onDragStart(e, id) { draggingId.value = id; e.dataTransfer.effectAllowed = 'move'; e.dataTransfer.setData('text/plain', id) }
function onDragEnd() { draggingId.value = null; dragOverId.value = null }
function onDragEnter(id) { if (id !== draggingId.value) dragOverId.value = id }
function onDragLeave(e, id) { if (!e.currentTarget.contains(e.relatedTarget) && dragOverId.value === id) dragOverId.value = null }
function onDragOver(e) { e.preventDefault(); e.dataTransfer.dropEffect = 'move' }
function onDrop() {
  const fromId = draggingId.value, toId = dragOverId.value
  if (!fromId || !toId || fromId === toId) { draggingId.value = null; dragOverId.value = null; return }
  const sorted  = [...tracks.value].sort((a, b) => a.index - b.index)
  const fromIdx = sorted.findIndex(t => t.id === fromId)
  const toIdx   = sorted.findIndex(t => t.id === toId)
  if (fromIdx < 0 || toIdx < 0) { draggingId.value = null; dragOverId.value = null; return }
  const arr = [...sorted]
  const [moved] = arr.splice(fromIdx, 1)
  arr.splice(toIdx, 0, moved)
  arr.forEach((t, i) => { const track = tracks.value.find(x => x.id === t.id); if (track) track.index = i })
  draggingId.value = null; dragOverId.value = null
}

const vClickOutside = {
  mounted(el, binding) { el._co = (e) => { if (!el.contains(e.target)) binding.value() }; document.addEventListener('click', el._co) },
  unmounted(el) { document.removeEventListener('click', el._co) }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.55); backdrop-filter: blur(6px);
  z-index: 200; display: flex; align-items: center; justify-content: center; padding: 20px;
}
.modal-box {
  background: var(--modal-bg); border: 1px solid var(--modal-border); border-radius: 20px;
  width: 520px; max-width: 100%;
  max-height: 90vh; /* 내용이 많을때만 스크롤, 4개는 스크롤 없음 */
  display: flex; flex-direction: column; box-shadow: var(--modal-shadow); overflow: hidden;
}
.modal-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 22px 22px 16px; border-bottom: 1px solid var(--modal-divider); flex-shrink: 0;
}
.modal-header-left { display: flex; align-items: center; gap: 14px; }
.modal-icon {
  width: 40px; height: 40px; border-radius: 12px;
  background: rgba(59,130,246,0.12); border: 1px solid rgba(59,130,246,0.2);
  display: flex; align-items: center; justify-content: center; color: #60a5fa; font-size: 16px; flex-shrink: 0;
}
.modal-title { font-size: 15px; font-weight: 800; color: var(--text-primary); font-family: 'Escoredream', sans-serif; line-height: 1.2; }
.modal-desc { font-size: 11px; color: var(--text-faint); font-family: 'Escoredream', sans-serif; margin-top: 3px; }
.modal-close {
  background: none; border: none; width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: var(--text-muted);
  font-size: 13px; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.modal-close:hover { background: var(--bg-hover); color: var(--text-primary); }
.modal-body { flex: 1; padding: 20px 22px; overflow-y: auto; scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent; }
.section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.section-label { font-size: 10px; font-weight: 800; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.08em; font-family: 'Escoredream', sans-serif; }
.section-count { font-size: 10px; font-weight: 700; color: var(--text-faint); background: var(--bg-hover); border: 1px solid var(--border); padding: 1px 7px; border-radius: 999px; font-family: monospace; }
.track-list { display: flex; flex-direction: column; gap: 6px; }
.track-item {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 12px; padding: 12px 14px;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s, opacity 0.15s;
  cursor: default; user-select: none;
}
.track-item:hover:not(.track-item--sys) { border-color: var(--border-mid); }
.track-item--dragging { opacity: 0.35; border-color: var(--accent); box-shadow: 0 0 0 2px var(--accent-glow); }
.track-item--over {
  border-color: var(--accent); box-shadow: 0 -3px 0 var(--accent), 0 0 0 1px var(--accent-glow);
  background: color-mix(in srgb, var(--bg-elevated) 90%, var(--accent));
}
.track-item--sys { background: transparent; border: 1px dashed var(--border); opacity: 0.65; cursor: default; }
.track-item--sys:hover { border-color: var(--border-mid); opacity: 0.82; background: transparent; }
.drag-handle { color: var(--border-mid); cursor: grab; font-size: 13px; flex-shrink: 0; padding: 4px 3px; transition: color 0.15s; touch-action: none; }
.drag-handle:hover { color: var(--text-muted); }
.drag-handle:active { cursor: grabbing; color: var(--accent); }
.sys-lock-icon { color: var(--border-mid); font-size: 11px; flex-shrink: 0; }
.color-wrap { position: relative; flex-shrink: 0; }
.color-btn {
  width: 30px; height: 30px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.15); cursor: pointer; flex-shrink: 0;
  transition: transform 0.15s, border-color 0.15s; display: block;
}
.color-btn:hover { transform: scale(1.12); border-color: rgba(255,255,255,0.4); }
.color-btn--locked { cursor: default; opacity: 0.7; pointer-events: none; }
.color-palette {
  position: absolute; top: 38px; left: 0; z-index: 400;
  background: var(--modal-bg); border: 1px solid var(--modal-border); border-radius: 14px; padding: 12px;
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px;
  box-shadow: 0 16px 40px rgba(0,0,0,0.5); width: 176px;
}
.palette-swatch { width: 24px; height: 24px; border-radius: 50%; border: 2px solid transparent; cursor: pointer; transition: transform 0.12s; }
.palette-swatch:hover { transform: scale(1.2); }
.palette-swatch.active { border-color: var(--text-primary); transform: scale(1.15); }
.palette-custom-wrap { grid-column: span 5; display: flex; align-items: center; gap: 6px; margin-top: 4px; padding: 5px 8px; background: var(--bg-hover); border-radius: 8px; cursor: pointer; font-size: 10px; color: var(--text-muted); border: 1px solid var(--border); transition: background 0.15s; }
.palette-custom-wrap:hover { background: var(--border); color: var(--text-primary); }
.palette-custom-wrap input[type="color"] { width: 28px; height: 20px; border: none; background: none; cursor: pointer; flex: 1; -webkit-appearance: none; appearance: none; padding: 0; }
.palette-custom-wrap input::-webkit-color-swatch-wrapper { padding: 2px; }
.palette-custom-wrap input::-webkit-color-swatch { border-radius: 4px; border: none; }
.track-name-input { flex: 1; min-width: 0; background: transparent; border: none; border-bottom: 1px solid transparent; color: var(--text-primary); font-size: 13px; font-weight: 700; font-family: 'Escoredream', sans-serif; padding: 3px 6px; outline: none; transition: border-color 0.15s; }
.track-name-input:hover { border-color: var(--border-mid); }
.track-name-input:focus { border-color: var(--accent); }
.track-name-readonly { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 700; color: var(--text-muted); font-family: 'Escoredream', sans-serif; padding: 3px 6px; }
.badge-sys { font-size: 9px; font-weight: 800; padding: 2px 7px; border-radius: 999px; background: rgba(250,204,21,0.1); border: 1px solid rgba(250,204,21,0.25); color: #facc15; white-space: nowrap; flex-shrink: 0; }
.delete-btn { background: none; border: none; width: 30px; height: 30px; border-radius: 7px; display: flex; align-items: center; justify-content: center; color: var(--border-mid); cursor: pointer; font-size: 12px; transition: all 0.15s; flex-shrink: 0; }
.delete-btn:hover { color: #f87171; background: rgba(248,113,113,0.1); }
.delete-placeholder { width: 30px; flex-shrink: 0; }
.btn-add-track { width: 100%; margin-top: 10px; padding: 12px 16px; border: 1px dashed var(--border-mid); border-radius: 12px; background: transparent; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; font-weight: 700; color: var(--text-muted); font-family: 'Escoredream', sans-serif; transition: all 0.15s; }
.btn-add-track:hover { border-color: var(--accent); color: var(--accent); background: rgba(59,130,246,0.06); }
.sys-section-header { display: flex; align-items: center; gap: 10px; margin: 22px 0 12px; }
.sys-section-line { flex: 1; height: 1px; background: linear-gradient(90deg, transparent, rgba(250,204,21,0.2), transparent); }
.sys-section-label { display: flex; align-items: center; gap: 6px; font-size: 10px; font-weight: 700; color: #facc15; opacity: 0.7; font-family: 'Escoredream', sans-serif; white-space: nowrap; }
.sys-section-label em { font-style: normal; opacity: 0.6; font-weight: 500; }
.modal-footer { padding: 14px 22px 18px; border-top: 1px solid var(--modal-divider); display: flex; justify-content: flex-end; flex-shrink: 0; }
.btn-done { display: flex; align-items: center; gap: 7px; padding: 9px 26px; background: var(--accent); color: #fff; border: none; border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 800; font-family: 'Escoredream', sans-serif; box-shadow: 0 4px 14px var(--accent-glow); transition: all 0.15s; }
.btn-done:hover { opacity: 0.85; transform: translateY(-1px); }
.palette-pop-enter-active { animation: popIn 0.15s ease-out; }
.palette-pop-leave-active  { animation: popIn 0.1s ease-in reverse; }
@keyframes popIn { from { opacity: 0; transform: scale(0.9) translateY(-6px); } to { opacity: 1; transform: scale(1) translateY(0); } }
.modal-fade-enter-active { transition: opacity 0.2s, transform 0.2s; }
.modal-fade-leave-active  { transition: opacity 0.15s, transform 0.15s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.97) translateY(6px); }
</style>
