<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-box">
      <!-- 헤더 -->
      <div class="modal-header">
        <div class="header-left">
          <div class="icon-circle"><i class="fas fa-brain" /></div>
          <div>
            <h2 class="modal-title">AI 추론 흐름 요약</h2>
            <p class="modal-subtitle">{{ data.kind === 'edge' ? '활동 연결 요약' : '활동 배치 요약' }}</p>
          </div>
        </div>
        <button class="btn-close" @click="handleClose"><i class="fas fa-times" /></button>
      </div>

      <!-- 바디 (스크롤) -->
      <div class="modal-body custom-scroll">
        <!-- 흐름 정보 카드 -->
        <div class="info-card">
          <div class="card-track"><i class="fas fa-code-branch" /> {{ data.flowName }}</div>
          <h4 class="card-subject">{{ data.subject }}</h4>
        </div>

        <!-- 한 줄 요약 -->
        <div class="info-card">
          <div class="card-label"><i class="fas fa-sparkles" /> 한 줄 요약</div>
          <p class="summary-text">{{ data.summary }}</p>
        </div>

        <!-- 3단계 확장 버튼 -->
        <button v-if="!showSteps" class="btn-expand" @click="showSteps = true; currentStep = 0">
          <i class="fas fa-brain" /> AI 사고과정 3단계 자세히 보기 <i class="fas fa-chevron-right" />
        </button>

        <!-- 3단계 상세 -->
        <div v-if="showSteps" class="info-card step-card">
          <!-- 단계 표시 -->
          <div class="step-indicator">
            <div class="step-dots">
              <template v-for="(sec, i) in sections" :key="sec.id">
                <button
                  class="step-dot"
                  :class="{ 'active': i === currentStep, 'past': i < currentStep }"
                  @click="currentStep = i"
                >{{ sec.id }}</button>
                <div v-if="i < sections.length - 1" class="step-line" :class="{ 'past': i < currentStep }" />
              </template>
            </div>
            <span class="step-count">{{ currentStep + 1 }} / 3단계</span>
          </div>

          <!-- 단계 컨텐츠 -->
          <div class="step-content">
            <div class="step-title-row">
              <i class="fas fa-sparkles" />
              <span class="step-title">{{ sections[currentStep].title }}</span>
            </div>
            <p class="step-desc">{{ sections[currentStep].description }}</p>
            <div class="step-items">
              <div v-for="(item, idx) in sections[currentStep].items" :key="idx" class="step-item">{{ item }}</div>
            </div>

            <!-- 이전/다음 네비게이션 -->
            <div class="step-nav">
              <button
                class="btn-step-nav"
                :class="{ 'disabled': currentStep === 0 }"
                :disabled="currentStep === 0"
                @click="currentStep = Math.max(0, currentStep - 1)"
              >
                <i class="fas fa-chevron-left" /> 이전
              </button>
              <button
                v-if="currentStep < sections.length - 1"
                class="btn-step-nav btn-step-next"
                @click="currentStep++"
              >
                다음 <i class="fas fa-chevron-right" />
              </button>
              <button
                v-else
                class="btn-step-nav btn-step-next"
                @click="showSteps = false"
              >
                완료
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <button class="btn-close-bottom" @click="handleClose">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['close'])

const showSteps = ref(false)
const currentStep = ref(0)

const sections = computed(() => [
  {
    id: '1',
    title: '먼저 확인한 맥락',
    description: 'AI가 커리큘럼을 만들기 전에 먼저 본 사용자 데이터와 학습 상황입니다.',
    items: props.data.observations || [],
  },
  {
    id: '2',
    title: 'AI가 이렇게 해석했어요',
    description: '관찰한 데이터를 바탕으로 학습 상태와 필요한 보완 지점을 이렇게 판단했습니다.',
    items: props.data.interpretations || [],
  },
  {
    id: '3',
    title: props.data.kind === 'edge'
      ? '그래서 이 순서를 이렇게 연결했어요'
      : '그래서 이 활동을 이렇게 넣었어요',
    description: props.data.kind === 'edge'
      ? '마지막으로 두 활동을 어떤 순서로 이어야 하는지 정리한 결과입니다.'
      : '마지막으로 이 활동을 어디에 어떻게 배치할지 정리한 결과입니다.',
    items: props.data.plan || [],
  },
])

function handleClose() {
  showSteps.value = false
  currentStep.value = 0
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(14px) saturate(180%);
  display: flex; align-items: center; justify-content: center;
  z-index: 5000; padding: 24px;
}

.modal-box {
  width: 100%; max-width: 520px; max-height: 90vh;
  background: var(--bg-base);
  border: 1px solid var(--border);
  overflow: hidden;
  display: flex; flex-direction: column;
}

/* 헤더 */
.modal-header {
  padding: 20px 24px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.icon-circle {
  width: 36px; height: 36px;
  background: var(--text-primary); color: var(--bg-base);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
}
.modal-title { font-size: 16px; font-weight: 900; color: var(--text-primary); margin: 0; letter-spacing: 0.03em; }
.modal-subtitle { font-size: 11px; font-weight: 700; color: var(--text-faint); margin-top: 2px; }
.btn-close {
  background: transparent; border: none; font-size: 18px; color: var(--text-muted);
  cursor: pointer; transition: color 0.2s;
}
.btn-close:hover { color: var(--text-primary); }

/* 바디 (스크롤) */
.modal-body {
  padding: 20px 24px;
  display: flex; flex-direction: column; gap: 16px;
  flex: 1; min-height: 0;
  overflow-y: auto;
}
.custom-scroll { scrollbar-width: thin; scrollbar-color: var(--border) transparent; }
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border); }

/* 카드 공통 */
.info-card {
  background: transparent;
  border: 1px solid var(--border);
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.card-track {
  font-size: 12px; font-weight: 700; color: var(--text-faint);
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}
.card-subject { font-size: 15px; font-weight: 900; color: var(--text-primary); margin: 0; }
.card-label {
  font-size: 13px; font-weight: 900; color: var(--text-primary);
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
}
.summary-text { font-size: 13px; line-height: 1.6; color: var(--text-secondary); font-weight: 600; margin: 0; }

/* 확장 버튼 */
.btn-expand {
  background: transparent; border: 1px solid var(--border);
  padding: 16px;
  font-size: 13px; font-weight: 900; color: var(--text-primary);
  display: flex; align-items: center; justify-content: center; gap: 10px;
  cursor: pointer; transition: all 0.3s; font-family: inherit;
}
.btn-expand:hover { background: var(--bg-hover); border-color: var(--text-primary); }

/* 3단계 상세 */
.step-card { border-color: var(--text-primary); padding: 0; overflow: hidden; }

.step-indicator {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
}
.step-dots { display: flex; align-items: center; gap: 8px; }
.step-dot {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--bg-surface); border: 2px solid var(--border);
  color: var(--text-muted); font-size: 12px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; font-family: inherit;
}
.step-dot.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.step-dot.past { background: var(--text-muted); color: var(--bg-base); border-color: var(--text-muted); }
.step-line { width: 20px; height: 1px; background: var(--border); }
.step-line.past { background: var(--text-muted); }
.step-count { margin-left: auto; font-size: 11px; font-weight: 700; color: var(--text-faint); }

.step-content { padding: 20px; }
.step-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.step-title { font-size: 13px; font-weight: 900; color: var(--text-primary); }
.step-title-row i { font-size: 12px; color: var(--text-muted); }
.step-desc { font-size: 12px; color: var(--text-muted); font-weight: 600; line-height: 1.5; margin-bottom: 16px; }

.step-items { display: flex; flex-direction: column; gap: 8px; }
.step-item {
  border: 1px solid var(--border);
  background: var(--bg-surface);
  padding: 12px 14px;
  font-size: 12px; line-height: 1.55; color: var(--text-secondary); font-weight: 600;
}

/* 단계 네비게이션 */
.step-nav {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 16px; padding-top: 14px;
  border-top: 1px solid var(--border);
}
.btn-step-nav {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--border);
  background: transparent; color: var(--text-muted);
  font-size: 12px; font-weight: 700; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
}
.btn-step-nav:hover:not(:disabled) { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }
.btn-step-nav.disabled { color: var(--text-faint); border-color: var(--border); cursor: not-allowed; }
.btn-step-next { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.btn-step-next:hover { opacity: 0.85; }

/* 푸터 */
.modal-footer { padding: 0 24px 24px; flex-shrink: 0; }
.btn-close-bottom {
  width: 100%; padding: 16px;
  background: var(--text-primary); color: var(--bg-base);
  border: none;
  font-size: 14px; font-weight: 900; cursor: pointer;
  transition: all 0.2s; font-family: inherit; letter-spacing: 0.05em;
}
.btn-close-bottom:hover { opacity: 0.85; }
</style>
