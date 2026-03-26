<template>
  <div class="tech-ac-wrap" ref="wrapRef">
    <input
      v-model="inputVal"
      v-bind="$attrs"
      class="tech-ac-input"
      autocomplete="off"
      @input="onInput"
      @keydown.down.prevent="moveDown"
      @keydown.up.prevent="moveUp"
      @keydown.enter.prevent="confirmSelection"
      @keydown.escape="close"
      @focus="onInput"
    />
    <Transition name="ac-drop">
      <ul v-if="suggestions.length" class="ac-list custom-scroll">
        <li
          v-for="(s, i) in suggestions"
          :key="s"
          class="ac-item"
          :class="{ active: i === activeIdx }"
          @mousedown.prevent="select(s)"
        >
          <i v-if="hasTechIcon(s)" :class="getTechIcon(s)" class="ac-icon" />
          <span>{{ s }}</span>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getSuggestions, getTechIcon, hasTechIcon, normalizeTechName } from '@/utils/techIcons'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'select'])

const inputVal = ref(props.modelValue)
const suggestions = ref([])
const activeIdx = ref(-1)
const wrapRef = ref(null)

watch(() => props.modelValue, v => { if (v !== inputVal.value) inputVal.value = v })
watch(inputVal, v => emit('update:modelValue', v))

function onInput() {
  suggestions.value = getSuggestions(inputVal.value)
  activeIdx.value = -1
}

function moveDown() {
  if (!suggestions.value.length) return
  activeIdx.value = (activeIdx.value + 1) % suggestions.value.length
}

function moveUp() {
  if (!suggestions.value.length) return
  activeIdx.value = (activeIdx.value - 1 + suggestions.value.length) % suggestions.value.length
}

function confirmSelection() {
  if (activeIdx.value >= 0 && suggestions.value[activeIdx.value]) {
    select(suggestions.value[activeIdx.value])
  } else {
    // 직접 입력한 값 정규화 후 emit
    const normalized = normalizeTechName(inputVal.value)
    if (normalized) emit('select', normalized)
    close()
  }
}

function select(name) {
  emit('select', name)
  inputVal.value = ''
  emit('update:modelValue', '')
  close()
}

function close() {
  suggestions.value = []
  activeIdx.value = -1
}

// 외부 클릭 시 닫기
function onDocClick(e) {
  if (wrapRef.value && !wrapRef.value.contains(e.target)) close()
}
import { onMounted, onUnmounted } from 'vue'
onMounted(() => document.addEventListener('mousedown', onDocClick))
onUnmounted(() => document.removeEventListener('mousedown', onDocClick))
</script>

<style scoped>
.tech-ac-wrap { position: relative; flex: 1; min-width: 0; }

.tech-ac-input {
  width: 100%; box-sizing: border-box;
  padding: 10px 16px; border: 1px solid var(--border);
  background: transparent; color: var(--text-primary);
  font-size: 13px; font-weight: 700; outline: none;
  font-family: inherit; border-radius: 8px;
  transition: border-color 0.2s;
}
.tech-ac-input:focus { border-color: var(--text-primary); }

.ac-list {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0;
  background: var(--bg-base); border: 1px solid var(--text-primary);
  border-radius: 8px; z-index: 9999;
  max-height: 220px; overflow-y: auto;
  list-style: none; margin: 0; padding: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.custom-scroll { scrollbar-width: thin; scrollbar-color: var(--border) transparent; }
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); }

.ac-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 12px; border-radius: 6px;
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  cursor: pointer; transition: background 0.1s;
}
.ac-item:hover, .ac-item.active { background: var(--clr-primary); color: var(--bg-base); }
.ac-icon { font-size: 14px; flex-shrink: 0; width: 18px; text-align: center; }

.ac-drop-enter-active, .ac-drop-leave-active { transition: opacity 0.15s, transform 0.15s; }
.ac-drop-enter-from, .ac-drop-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
