<template>
  <div
    class="track-item"
    :class="{ 'track-item--highlight': track.isHighlight }"
    v-click-outside="closePicker"
  >
    <!-- 드래그 핸들 / 형광펜 고정 아이콘 -->
    <div class="drag-handle" :title="track.isHighlight ? '시스템 트랙 (고정)' : '드래그해서 순서 변경'">
      <i :class="track.isHighlight ? 'fas fa-lock' : 'fas fa-grip-vertical'" />
    </div>

    <!-- 컬러 프리뷰 (형광펜은 색상 변경 불가) -->
    <div class="color-wrap">
      <div
        class="color-preview"
        :style="{ backgroundColor: track.color }"
        :class="{ 'color-preview--locked': track.isHighlight }"
        @click.stop="!track.isHighlight && (pickerOpen = !pickerOpen)"
        :title="track.isHighlight ? '시스템 트랙 색상은 고정입니다' : '색상 변경'"
      />
      <Transition name="palette-pop">
        <div v-if="pickerOpen && !track.isHighlight" class="color-palette" @click.stop>
          <button
            v-for="c in PRESET_COLORS"
            :key="c"
            class="palette-swatch"
            :style="{ backgroundColor: c }"
            :class="{ 'palette-swatch--active': track.color === c }"
            @click="pickColor(c)"
          />
          <input
            :value="track.color"
            type="color"
            class="palette-custom"
            title="직접 선택"
            @input="pickColor($event.target.value)"
          />
        </div>
      </Transition>
    </div>

    <!-- 트랙 이름 (형광펜은 읽기 전용) -->
    <div v-if="track.isHighlight" class="track-name-readonly">
      {{ track.name }}
      <span class="badge-system">시스템</span>
    </div>
    <input
      v-else
      :value="track.name"
      type="text"
      class="track-name-input"
      placeholder="트랙 이름"
      @input="$emit('update:name', $event.target.value)"
    />

    <!-- 위/아래 이동 (형광펜은 disabled) -->
    <div class="index-control">
      <button
        class="index-btn"
        title="위로"
        :disabled="track.isHighlight || track.index === 0"
        @click="$emit('move-up', track.id)"
      >
        <i class="fas fa-chevron-up" />
      </button>
      <span class="index-label">{{ track.index }}</span>
      <button
        class="index-btn"
        title="아래로"
        :disabled="track.isHighlight"
        @click="$emit('move-down', track.id)"
      >
        <i class="fas fa-chevron-down" />
      </button>
    </div>

    <!-- 삭제 (형광펜 및 main은 불가) -->
    <button
      v-if="!track.isHighlight && track.id !== 'main'"
      class="btn-delete"
      title="트랙 삭제"
      @click="$emit('delete', track.id)"
    >
      <i class="fas fa-trash" />
    </button>
    <span v-else class="btn-placeholder" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({ track: { type: Object, required: true } })
const emit = defineEmits(['update:color', 'update:name', 'delete', 'move-up', 'move-down'])

const pickerOpen = ref(false)

const PRESET_COLORS = [
  '#0ea5e9', '#38bdf8', '#06b6d4', '#3b82f6', '#6366f1',
  '#8b5cf6', '#a855f7', '#ec4899', '#f43f5e', '#ef4444',
  '#f97316', '#eab308', '#22c55e', '#10b981', '#14b8a6'
]

function pickColor(c) {
  emit('update:color', c)
  pickerOpen.value = false
}

function closePicker() {
  pickerOpen.value = false
}

const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value() }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('click', el._clickOutside)
  }
}
</script>

<style scoped>
.track-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #17171d;
  border: 1px solid #2d2d38;
  padding: 10px 12px;
  border-radius: 12px;
  transition: border-color 0.15s;
  position: relative;
}
.track-item:hover { border-color: #374151; }

/* 형광펜 트랙: 구분되는 스타일 */
.track-item--highlight {
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed #374151;
}
.track-item--highlight:hover { border-color: #4b5563; }

.drag-handle {
  color: #2d2d38;
  cursor: grab;
  font-size: 12px;
  padding: 0 2px;
  flex-shrink: 0;
  transition: color 0.15s;
}
.drag-handle:hover { color: #6b7280; }
.track-item--highlight .drag-handle { cursor: default; color: #374151; }

/* 컬러 */
.color-wrap { position: relative; flex-shrink: 0; }

.color-preview {
  width: 26px; height: 26px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid rgba(255,255,255,0.12);
  transition: transform 0.15s, border-color 0.15s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.5);
}
.color-preview:hover:not(.color-preview--locked) { transform: scale(1.12); border-color: rgba(255,255,255,0.3); }
.color-preview--locked { cursor: default; opacity: 0.8; }

.color-palette {
  position: absolute; top: 36px; left: 0; z-index: 300;
  background: #1e1e24; border: 1px solid #374151;
  border-radius: 14px; padding: 12px;
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px;
  box-shadow: 0 16px 40px rgba(0,0,0,0.8); width: 168px;
}
.palette-swatch {
  width: 22px; height: 22px; border-radius: 50%;
  border: 2px solid transparent; cursor: pointer;
  transition: transform 0.12s, border-color 0.12s;
}
.palette-swatch:hover { transform: scale(1.2); }
.palette-swatch--active { border-color: #fff; transform: scale(1.15); }

.palette-custom {
  grid-column: span 5; width: 100%; height: 26px;
  border-radius: 8px; border: 1px solid #374151;
  background: #17171d; cursor: pointer; padding: 0;
  -webkit-appearance: none; appearance: none; margin-top: 2px;
}
.palette-custom::-webkit-color-swatch-wrapper { padding: 3px; }
.palette-custom::-webkit-color-swatch { border-radius: 5px; border: none; }

/* 이름 */
.track-name-input {
  flex: 1; background: transparent; border: none;
  border-bottom: 1px solid transparent; color: #fff;
  font-size: 13px; font-weight: 600; padding: 4px 6px;
  outline: none; transition: border-color 0.15s; min-width: 0;
}
.track-name-input:hover { border-color: #4b5563; }
.track-name-input:focus { border-color: #3b82f6; }

.track-name-readonly {
  flex: 1; display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #9ca3af;
  padding: 4px 6px; min-width: 0;
}
.badge-system {
  font-size: 9px; font-weight: 700; padding: 2px 6px;
  border-radius: 999px; background: rgba(250, 204, 21, 0.12);
  border: 1px solid rgba(250, 204, 21, 0.3); color: #facc15;
  white-space: nowrap; flex-shrink: 0;
}

/* 인덱스 조절 */
.index-control {
  display: flex; flex-direction: column; align-items: center; gap: 1px; flex-shrink: 0;
}
.index-btn {
  width: 18px; height: 16px; background: #2d2d38; border: none;
  border-radius: 4px; color: #6b7280; cursor: pointer; font-size: 8px;
  display: flex; align-items: center; justify-content: center; transition: all 0.12s;
}
.index-btn:hover:not(:disabled) { background: #3b82f6; color: #fff; }
.index-btn:disabled { opacity: 0.25; cursor: not-allowed; }
.index-label { font-size: 10px; color: #4b5563; font-family: monospace; line-height: 1; }

/* 삭제 */
.btn-delete {
  background: none; border: none; color: #374151; cursor: pointer;
  padding: 5px 7px; font-size: 12px; border-radius: 6px;
  transition: all 0.15s; flex-shrink: 0;
}
.btn-delete:hover { color: #f87171; background: rgba(248, 113, 113, 0.1); }
.btn-placeholder { width: 28px; flex-shrink: 0; }

/* 팔레트 애니메이션 */
.palette-pop-enter-active { animation: popIn 0.15s ease-out; }
.palette-pop-leave-active { animation: popIn 0.1s ease-in reverse; }
@keyframes popIn {
  from { opacity: 0; transform: scale(0.9) translateY(-4px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
