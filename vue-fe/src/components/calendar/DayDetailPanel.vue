<template>
  <div class="day-panel">
    <div class="panel-header">
      <div class="panel-date">
        <span class="panel-day-num"
          :class="{ 'panel-day--sat': dayOfWeek===6, 'panel-day--sun': dayOfWeek===0 }">
          {{ displayDate }}
        </span>
        <span class="panel-day-label"
          :class="{ 'panel-day--sat': dayOfWeek===6, 'panel-day--sun': dayOfWeek===0 }">
          {{ dayLabel }}
        </span>
      </div>
      <button class="btn-add-day" @click="$emit('add-schedule', dayStr)">
        <i class="fas fa-plus" /> 일정 추가
      </button>
    </div>

    <div v-if="!schedules.length" class="panel-empty">
      <i class="fas fa-calendar-day" />
      <p>일정이 없어요.</p>
      <button class="btn-add-empty" @click="$emit('add-schedule', dayStr)">+ 일정 추가</button>
    </div>

    <div v-else class="panel-groups">
      <div v-for="group in groupedSchedules" :key="group.trackId" class="panel-group">
        <div class="group-label" :style="{ color: group.color }">
          <span class="group-dot" :style="{ background: group.color }" />
          {{ group.trackName }}
        </div>
        <div class="group-items">
          <div v-for="s in group.items" :key="s.id"
            class="panel-item" :style="{ borderLeftColor: group.color }">
            <div class="panel-item-time"><i class="fas fa-clock" /> {{ s.tooltip?.time || '시간 미정' }}</div>
            <div class="panel-item-title">{{ s.tooltip?.title || s.text }}</div>
            <div v-if="s.tooltip?.tags?.length" class="panel-item-tags">
              <span v-for="(t, i) in s.tooltip.tags" :key="i" class="panel-tag">{{ t }}</span>
            </div>
            <textarea v-model="memos[s.id]" class="panel-memo" placeholder="메모를 입력하세요..." rows="3" />
            <div class="panel-item-actions">
              <button class="panel-btn" @click.stop="$emit('edit-schedule', s)"><i class="fas fa-edit" /> 수정</button>
              <button class="panel-btn panel-btn--del" @click.stop="$emit('delete-schedule', s.id)"><i class="fas fa-trash" /> 삭제</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({
  dayStr:    { type: String, required: true },  // 'YYYY-MM-DD'
  schedules: { type: Array,  default: () => [] }
})
defineEmits(['add-schedule', 'edit-schedule', 'delete-schedule'])

const store = useCalendarStore()
const memos = ref({})

const parsed = computed(() => {
  const [y,m,d] = props.dayStr.split('-').map(Number)
  return new Date(y, m-1, d)
})

const dayOfWeek   = computed(() => parsed.value.getDay())
const displayDate = computed(() => {
  const d = parsed.value
  return `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일`
})
const DAY_KOR = ['일요일','월요일','화요일','수요일','목요일','금요일','토요일']
const dayLabel = computed(() => DAY_KOR[dayOfWeek.value])

const groupedSchedules = computed(() => {
  const map = new Map()
  props.schedules.forEach(s => {
    const track = store.getTrackById(s.track)
    if (!map.has(s.track)) map.set(s.track, { trackId: s.track, trackName: track?.name||s.track, color: track?.color||'#6b7280', items: [] })
    map.get(s.track).items.push(s)
  })
  return [...map.values()].sort((a,b) => (store.getTrackById(a.trackId)?.index??99) - (store.getTrackById(b.trackId)?.index??99))
})
</script>

<style scoped>
.day-panel { flex: 1; display: flex; flex-direction: column; background: var(--bg-surface); border-left: 1px solid var(--border); overflow-y: auto; scrollbar-width: thin; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px 16px; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.panel-date { display: flex; align-items: baseline; gap: 10px; }
.panel-day-num { font-size: 20px; font-weight: 800; color: var(--text-primary); font-family: 'Escoredream', sans-serif; }
.panel-day-label { font-size: 13px; font-weight: 500; color: var(--text-muted); font-family: 'Escoredream', sans-serif; }
.panel-day--sat { color: var(--sat-color) !important; }
.panel-day--sun { color: var(--sun-color) !important; }
.btn-add-day { display: flex; align-items: center; gap: 6px; padding: 7px 14px; background: var(--accent); color: #fff; border: none; border-radius: 8px; font-size: 12px; font-weight: 700; font-family: 'Escoredream', sans-serif; cursor: pointer; }
.btn-add-day:hover { opacity: 0.85; }
.panel-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text-faint); padding: 40px; }
.panel-empty i { font-size: 36px; opacity: 0.3; }
.panel-empty p { font-size: 14px; font-family: 'Escoredream', sans-serif; }
.btn-add-empty { padding: 8px 18px; border: 1px dashed var(--border-mid); background: none; border-radius: 8px; color: var(--text-muted); font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Escoredream', sans-serif; }
.btn-add-empty:hover { border-color: var(--accent); color: var(--accent); }
.panel-groups { padding: 16px 20px; display: flex; flex-direction: column; gap: 20px; }
.panel-group  { display: flex; flex-direction: column; gap: 8px; }
.group-label  { display: flex; align-items: center; gap: 7px; font-size: 11px; font-weight: 700; font-family: 'Escoredream', sans-serif; }
.group-dot    { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.group-items  { display: flex; flex-direction: column; gap: 8px; }
.panel-item   { background: var(--bg-elevated); border: 1px solid var(--border); border-left: 3px solid; border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 8px; }
.panel-item:hover { background: var(--bg-hover); }
.panel-item-time  { font-size: 11px; color: var(--text-faint); font-family: monospace; display: flex; align-items: center; gap: 4px; }
.panel-item-title { font-size: 15px; font-weight: 700; color: var(--text-primary); line-height: 1.3; font-family: 'Escoredream', sans-serif; }
.panel-item-tags  { display: flex; flex-wrap: wrap; gap: 4px; }
.panel-tag        { font-size: 10px; color: var(--text-muted); background: var(--bg-surface); border: 1px solid var(--border); padding: 2px 7px; border-radius: 5px; }
.panel-memo { width: 100%; background: var(--bg-surface); border: 1px solid var(--border); border-radius: 8px; color: var(--text-secondary); font-size: 12px; font-family: 'Escoredream', sans-serif; padding: 10px 12px; outline: none; resize: vertical; transition: border-color 0.15s; line-height: 1.6; }
.panel-memo:focus { border-color: var(--accent); }
.panel-memo::placeholder { color: var(--text-faint); }
.panel-item-actions { display: flex; gap: 8px; justify-content: flex-end; }
.panel-btn { display: flex; align-items: center; gap: 4px; padding: 5px 10px; border: none; border-radius: 6px; font-size: 11px; font-weight: 600; cursor: pointer; background: var(--bg-hover); color: var(--text-muted); font-family: 'Escoredream', sans-serif; }
.panel-btn:hover { background: var(--accent); color: #fff; }
.panel-btn--del:hover { background: #ef4444; }
</style>
