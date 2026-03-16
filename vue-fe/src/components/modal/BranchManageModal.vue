<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-box">
          <div class="modal-header">
            <h3>TRACK MANAGE</h3>
            <button class="btn-close" @click="close"><i class="fas fa-times" /></button>
          </div>

          <div class="modal-body custom-scroll">
            
            <div class="track-section">
              <div class="section-header">
                <h4>ACTIVE TRACKS <span class="track-count">({{ store.activeTracks.length }}/4)</span></h4>
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
                <i class="fas fa-plus" /> NEW TRACK
              </button>
            </div>

            <div class="divider" />

            <div class="track-section">
              <div class="section-header">
                <h4>ARCHIVED TRACKS <span class="track-count">({{ store.endedTracks.length }})</span></h4>
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
    store.addTrack({ id: 't_' + Date.now(), name, color: '#897C9B' })
  }
}
</script>

<style scoped>
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

/* ★ 흐림 효과 제거, 강력한 불투명도 */
.modal-overlay { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,0.85); display: flex; align-items: center; justify-content: center; font-family: 'Space Grotesk', 'Escoredream', sans-serif; }

.modal-box { width: 100%; max-width: 480px; max-height: 80vh; background: var(--bg-base); border-radius: 0; border: 2px solid var(--text-primary); box-shadow: 12px 12px 0 var(--text-primary); display: flex; flex-direction: column; overflow: hidden; }

.modal-header { display: flex; justify-content: space-between; padding: 24px; border-bottom: 2px solid var(--text-primary); }
.modal-header h3 { font-size: 18px; font-weight: 900; letter-spacing: 0.05em; }
.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-primary); cursor: pointer; transition: 0.1s; }
.btn-close:hover { transform: scale(1.2); }

.modal-body { padding: 24px; overflow-y: auto; }

.track-section { display: flex; flex-direction: column; gap: 16px; }
.section-header h4 { font-size: 14px; font-weight: 900; color: var(--text-primary); letter-spacing: 0.05em; }
.track-count { color: var(--text-muted); font-weight: 800; margin-left: 6px; }
.section-desc { font-size: 12px; font-weight: 700; color: var(--text-faint); margin-top: 6px; line-height: 1.5; }

.track-list { display: flex; flex-direction: column; gap: 12px; }
.track-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--bg-surface); border: 2px solid var(--border); border-radius: 0; box-shadow: 4px 4px 0 var(--border); transition: 0.1s; }
.track-item:hover { border-color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--text-primary); }
.track-item.ended { opacity: 0.6; box-shadow: none; border-style: dashed; }
.track-item.ended:hover { opacity: 1; border-style: solid; }

.track-info { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; margin-right: 12px; }
.color-dot { width: 12px; height: 12px; border-radius: 0; flex-shrink: 0; border: 1px solid var(--bg-base); }

.track-name-input {
  flex: 1; background: transparent; border: none; color: var(--text-primary);
  font-size: 14px; font-weight: 800; font-family: 'Escoredream', sans-serif;
  padding: 8px 10px; border-radius: 0; outline: none; transition: all 0.1s; border: 2px solid transparent;
}
.track-name-input:focus { border-color: var(--text-primary); background: var(--bg-base); box-shadow: 4px 4px 0 var(--border); }

.btn-end-track, .btn-restore-track { 
  background: transparent; border: 2px solid var(--border); width: 36px; height: 36px; border-radius: 0; 
  cursor: pointer; transition: 0.1s; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 14px; 
}
.btn-end-track { color: #ef4444; }
.btn-end-track:hover { background: #ef4444; color: var(--bg-base); border-color: #ef4444; }
.btn-restore-track { color: var(--text-primary); }
.btn-restore-track:hover { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.empty-msg { font-size: 13px; font-weight: 700; color: var(--text-faint); text-align: center; padding: 24px 0; border: 2px dashed var(--border); }

.btn-add-track { width: 100%; padding: 16px; border-radius: 0; border: 2px dashed var(--text-primary); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 900; cursor: pointer; transition: 0.1s; letter-spacing: 0.05em; margin-top: 8px; }
.btn-add-track:not(:disabled):hover { background: var(--text-primary); color: var(--bg-base); border-style: solid; box-shadow: 4px 4px 0 var(--text-primary); transform: translate(-2px, -2px); }
.btn-add-track:disabled { opacity: 0.3; cursor: not-allowed; border-color: var(--border); color: var(--border); }

.divider { height: 2px; background: var(--border); margin: 32px 0; border-bottom: 2px dashed var(--bg-base); }
</style>