<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-box">
          <div class="modal-header">
            <h3>트랙 관리</h3>
            <button class="btn-close" @click="close"><i class="fas fa-times" /></button>
          </div>

          <div class="modal-body custom-scroll">
            
            <div class="track-section">
              <div class="section-header">
                <h4>진행 중인 트랙 <span class="track-count">({{ store.activeTracks.length }}/4)</span></h4>
              </div>
              <div class="track-list">
                <div v-for="track in store.activeTracks" :key="track.id" class="track-item">
                  <div class="track-info">
                    <span class="color-dot" :style="{ background: track.color }" />
                    <input 
                      type="text" 
                      class="track-name-input" 
                      :value="track.name" 
                      @change="store.updateTrackObj(track.id, { name: $event.target.value })" 
                      placeholder="트랙 이름 입력"
                    />
                  </div>
                  <button class="btn-end-track" @click="store.toggleTrackEnded(track.id)" title="트랙 종료">
                    <i class="fas fa-power-off" />
                  </button>
                </div>
              </div>
              
              <button 
                class="btn-add-track" 
                :disabled="store.activeTracks.length >= 4"
                @click="openAddForm"
              >
                <i class="fas fa-plus" /> 새 트랙 추가
              </button>
            </div>

            <div class="divider" />

            <div class="track-section">
              <div class="section-header">
                <h4>종료된 트랙 <span class="track-count">({{ store.endedTracks.length }})</span></h4>
                <p class="section-desc">과거의 기록은 캘린더에 남지만, 새 일정은 추가할 수 없습니다.</p>
              </div>
              <div class="track-list">
                <div v-if="store.endedTracks.length === 0" class="empty-msg">종료된 트랙이 없습니다.</div>
                <div v-for="track in store.endedTracks" :key="track.id" class="track-item ended">
                  <div class="track-info">
                    <span class="color-dot" :style="{ background: track.color }" />
                    <input 
                      type="text" 
                      class="track-name-input" 
                      :value="track.name" 
                      @change="store.updateTrackObj(track.id, { name: $event.target.value })" 
                      placeholder="트랙 이름 입력"
                    />
                  </div>
                  <button class="btn-restore-track" @click="store.toggleTrackEnded(track.id)" title="다시 진행하기">
                    <i class="fas fa-undo" />
                  </button>
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
import { useCalendarStore } from '@/stores/useCalendarStore'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const store = useCalendarStore()

function close() { emit('update:modelValue', false) }

function openAddForm() {
  if (store.activeTracks.length >= 4) return
  const name = prompt('새 트랙의 이름을 입력하세요:')
  if (name) {
    store.addTrack({ id: 't_' + Date.now(), name, color: '#3b82f6' })
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.modal-box { width: 440px; max-height: 80vh; background: var(--bg-surface); border-radius: 16px; border: 1px solid var(--border); box-shadow: 0 10px 40px rgba(0,0,0,0.2); display: flex; flex-direction: column; overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border); }
.modal-header h3 { font-size: 16px; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 16px; color: var(--text-faint); cursor: pointer; }
.modal-body { padding: 24px; overflow-y: auto; }

.track-section { display: flex; flex-direction: column; gap: 12px; }
.section-header h4 { font-size: 14px; font-weight: 700; color: var(--text-secondary); }
.track-count { color: var(--accent); font-weight: 800; margin-left: 4px; }
.section-desc { font-size: 11px; color: var(--text-faint); margin-top: 4px; }

.track-list { display: flex; flex-direction: column; gap: 8px; }
.track-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px 8px 16px; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 10px; }
.track-item.ended { opacity: 0.65; }
.track-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; margin-right: 12px; }
.color-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }

/* ★ 수정 가능하게 만든 Input 스타일 */
.track-name-input {
  flex: 1; background: transparent; border: none; color: var(--text-primary);
  font-size: 13px; font-weight: 600; font-family: 'Escoredream', sans-serif;
  padding: 6px 8px; border-radius: 6px; outline: none; transition: all 0.15s;
  min-width: 0;
}
.track-name-input:hover, .track-name-input:focus {
  background: var(--bg-base); box-shadow: inset 0 0 0 1px var(--border-mid);
}

.btn-end-track { background: rgba(239, 68, 68, 0.1); color: #ef4444; border: none; width: 32px; height: 32px; border-radius: 8px; cursor: pointer; transition: 0.15s; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.btn-end-track:hover { background: #ef4444; color: #fff; }

.btn-restore-track { background: var(--bg-hover); color: var(--text-muted); border: 1px solid var(--border); width: 32px; height: 32px; border-radius: 8px; cursor: pointer; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.btn-restore-track:hover { background: var(--accent); color: #fff; border-color: var(--accent); }

.empty-msg { font-size: 12px; color: var(--text-faint); text-align: center; padding: 20px 0; }

.btn-add-track { width: 100%; padding: 12px; border-radius: 10px; border: 1.5px dashed var(--border-mid); background: transparent; color: var(--text-muted); font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.15s; font-family: inherit; margin-top: 4px; }
.btn-add-track:not(:disabled):hover { border-color: var(--accent); color: var(--accent); background: rgba(59,130,246,0.05); }
.btn-add-track:disabled { opacity: 0.4; cursor: not-allowed; }

.divider { height: 1px; background: var(--border); margin: 24px 0; }
</style>