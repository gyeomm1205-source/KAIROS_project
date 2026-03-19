<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <Transition name="modal-pop">
      <div class="modal-box reasoning-modal">
        
        <!-- 헤더 -->
        <div class="modal-header">
          <div class="header-left">
            <div class="icon-circle">
              <i class="fas fa-brain" />
            </div>
            <div class="header-text">
              <h3 class="modal-title">AI 추론 흐름 요약</h3>
              <p class="modal-subtitle">활동 배치 요약</p>
            </div>
          </div>
          <button class="btn-close" @click="$emit('close')">
            <i class="fas fa-times" />
          </button>
        </div>

        <!-- 바디 -->
        <div class="modal-body custom-scroll">
          
          <!-- 상단 맥락 카드 -->
          <div class="info-card context-card">
            <div class="card-track">
              <i class="fas fa-code-branch" /> {{ node.track || '학습 흐름' }}
            </div>
            <h4 class="card-node-info">
              {{ formatNodeDate(node.day) }} · {{ node.tooltip?.title || node.text }}
            </h4>
          </div>

          <!-- 한 줄 요약 카드 -->
          <div class="info-card summary-card">
            <div class="card-label">
              <i class="fas fa-sparkles" /> 한 줄 요약
            </div>
            <p class="summary-text">{{ node.summary || '최근 학습 패턴을 분석하여 효율적인 복습 및 실전 적용 주기를 설계했습니다.' }}</p>
          </div>

          <!-- 확장 영역 (사고과정) -->
          <Transition name="fade-slide">
            <div v-if="detailedView" class="info-card detail-card">
              <div class="detail-header">
                <div class="stepper">
                  <div 
                    v-for="step in 3" 
                    :key="step" 
                    class="step-dot" 
                    :class="{ 'active': activeStep === step, 'past': activeStep > step }"
                    @click="activeStep = step"
                  />
                  <div class="stepper-line" />
                </div>
                <div class="step-label">{{ activeStep }} / 3단계</div>
              </div>

              <div class="detail-content">
                <div class="card-label">
                  <i class="fas fa-sparkles" /> AI가 이렇게 해석했어요
                </div>
                <p class="interpret-subtitle">관찰한 데이터를 바탕으로 학습 상태와 필요한 보완 지점을 이렇게 판단했습니다.</p>
                
                <div class="interpret-box">
                  {{ interpretationTexts[activeStep - 1] }}
                </div>
              </div>
            </div>
          </Transition>

          <!-- 자세히 보기 버튼 (상세 아닐 때만) -->
          <button v-if="!detailedView" class="btn-expand" @click="detailedView = true">
            <i class="fas fa-brain" /> AI 사고과정 3단계 자세히 보기 <i class="fas fa-chevron-right" />
          </button>

        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <button class="btn-close-bottom" @click="$emit('close')">닫기</button>
        </div>

      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  node: { type: Object, required: true }
})

const emit = defineEmits(['close'])

const activeStep = ref(1)
const detailedView = ref(false)

const interpretationTexts = [
  "개념을 접한 경험은 있지만, 실제 코드에서 언제 어떤 패턴을 써야 하는지 판단하는 단계가 아직 약하다고 봤습니다.",
  "실전 프로젝트로의 전환이 필요한 시점입니다. 단순 이론 공부보다 구현 경험이 더 중요하게 작용할 것입니다.",
  "이후 일정은 프로젝트 완성도를 높이는 방향으로 활동을 배치했습니다. 배운 내용을 즉시 적용하는 선순환 구조입니다."
]

function formatNodeDate(dateStr) {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-').map(Number);
  return `${m}월 ${d}일`;
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; 
  background: rgba(0, 0, 0, 0.4); 
  backdrop-filter: blur(14px) saturate(180%);
  display: flex; align-items: center; justify-content: center;
  z-index: 5000;
}

.modal-box {
  width: 100%; max-width: 520px;
  background: var(--bg-base);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.4), inset 0 0 20px rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  overflow: hidden;
  display: flex; flex-direction: column;
}

.modal-header {
  padding: 24px 32px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--border);
}

.header-left { display: flex; align-items: center; gap: 20px; }

.icon-circle {
  width: 44px; height: 44px;
  background: var(--text-primary);
  color: var(--bg-base);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}

.modal-title { font-size: 20px; font-weight: 900; color: var(--text-primary); letter-spacing: -0.02em; margin: 0; }
.modal-subtitle { font-size: 13px; font-weight: 600; color: var(--text-faint); margin-top: 2px; }

.btn-close {
  background: transparent; border: none; font-size: 20px; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s;
}
.btn-close:hover { color: var(--text-primary); transform: rotate(90deg); }

.modal-body {
  padding: 24px 32px;
  display: flex; flex-direction: column; gap: 16px;
  max-height: 60vh; overflow-y: auto;
}

.info-card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.card-track {
  font-family: 'Space Grotesk', sans-serif;
  font-size: 13px; font-weight: 700; color: var(--text-faint);
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}

.card-node-info {
  font-size: 18px; font-weight: 900; color: var(--text-primary);
  letter-spacing: -0.01em; margin: 0;
}

.card-label {
  font-size: 14px; font-weight: 900; color: var(--text-primary);
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
  letter-spacing: 0.05em;
}

.summary-text { font-size: 15px; line-height: 1.6; color: var(--text-secondary); font-weight: 600; margin: 0; }

.btn-expand {
  background: transparent; border: 1px solid var(--border);
  padding: 18px; border-radius: 14px;
  font-size: 14px; font-weight: 900; color: var(--text-primary);
  display: flex; align-items: center; justify-content: center; gap: 12px;
  cursor: pointer; transition: all 0.3s;
}
.btn-expand:hover { background: var(--bg-hover); border-color: var(--text-primary); }

/* 상세 영역 */
.detail-card { background: var(--bg-elevated); border-color: var(--text-primary); }

.detail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }

.stepper { display: flex; align-items: center; gap: 12px; position: relative; }
.step-dot {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--bg-surface); border: 2px solid var(--border);
  z-index: 2; cursor: pointer; transition: all 0.3s;
}
.step-dot.active { background: var(--text-primary); border-color: var(--text-primary); box-shadow: 0 0 12px var(--text-primary); }
.step-dot.past { border-color: var(--text-primary); background: transparent; }

.stepper-line {
  position: absolute; left: 0; right: 0; top: 11px; height: 1px;
  background: var(--border); z-index: 1;
}

.step-label { font-family: 'Space Grotesk', sans-serif; font-size: 12px; font-weight: 700; color: var(--text-faint); }

.interpret-subtitle { font-size: 14px; font-weight: 700; color: var(--text-muted); margin-bottom: 16px; }

.interpret-box {
  background: var(--bg-hover); border-radius: 12px; padding: 16px;
  font-size: 14px; line-height: 1.6; color: var(--text-primary); font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.modal-footer { padding: 0 32px 32px; }

.btn-close-bottom {
  width: 100%; padding: 18px;
  background: var(--bg-dark); color: var(--bg-base);
  border: none; border-radius: 14px;
  font-size: 16px; font-weight: 900; cursor: pointer;
  transition: all 0.2s;
}
.btn-close-bottom:hover { opacity: 0.9; transform: translateY(-1px); }

/* 애니메이션 */
.modal-pop-enter-active { animation: pop-in 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes pop-in { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

.fade-slide-enter-active { transition: all 0.4s ease-out; }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }

.custom-scroll::-webkit-scrollbar { width: 0; }
</style>
