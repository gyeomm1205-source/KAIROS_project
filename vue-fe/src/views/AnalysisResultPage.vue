<template>
  <div class="setup-root">
    <!-- ONBOARDING STEPPER -->
    <div class="global-stepper-wrap">
      <div class="page-stepper">
        <div class="step done">1. 계정 연동</div>
        <div class="step done">2. 사전 설문</div>
        <div class="step done">3. 데이터 분석</div>
        <div class="step active">4. 결과 확인</div>
      </div>
    </div>

    <div class="setup-card custom-scroll">
      <div class="header-top">
        <button @click="router.back()" class="btn-back">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>

      <div class="setup-header">
        <h2>현재 학습 상태 분석 결과</h2>
        <p>최근 GitHub, Velog, Google Calendar 활동을 종합 분석한 결과입니다.</p>
      </div>

      <!-- LOADING STATE -->
      <div v-if="isLoadingAI" class="loading-container">
        <div class="spinner"></div>
        <p class="loading-text">AI가 활동을 분석 중입니다...</p>
      </div>

      <!-- ANALYSIS CONTENT -->
      <div v-else-if="analysisResult" class="analysis-body">
        <div class="analysis-grid mb-lg">
          <!-- Recent Techs -->
          <div class="base-panel p-md">
            <div class="panel-header mb-md">
              <i class="fas fa-code"></i>
              <h3>RECENT TECHS</h3>
            </div>
            <div class="tech-tags">
              <span v-for="t in analysisResult.recentTechs" :key="t" class="outline-badge">
                <i :class="getTechIcon(t)" class="tech-icon" />{{ t }}
              </span>
            </div>
          </div>

          <!-- Skill Levels -->
          <div class="base-panel p-md">
            <div class="panel-header mb-md">
              <i class="fas fa-chart-bar"></i>
              <h3>SKILL LEVELS</h3>
            </div>
            <div class="skill-list">
              <div v-for="s in analysisResult.skillLevels" :key="s.name" class="skill-item">
                <div class="skill-info">
                  <span class="skill-name">{{ s.name }}</span>
                  <span class="skill-percent">{{ s.level }}%</span>
                </div>
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill" :style="{ width: s.level + '%' }"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Repeated Techs -->
          <div class="base-panel p-md">
            <div class="panel-header mb-md">
              <i class="fas fa-redo"></i>
              <h3>REPEATED TECHS</h3>
            </div>
            <div class="repeated-list">
              <div v-for="t in analysisResult.repeatedTechs" :key="t.name" class="repeated-item">
                <span class="tech-name">{{ t.name }}</span>
                <span class="tech-count">{{ t.count }}회 등장</span>
              </div>
            </div>
          </div>

          <!-- Recommended Position -->
          <div class="base-panel p-md">
            <div class="panel-header mb-md">
              <i class="fas fa-compass"></i>
              <h3>RECOMMENDED POSITION</h3>
            </div>
            <div class="position-list">
              <div v-for="pos in analysisResult.recommendedPositions" :key="pos.title" class="position-item">
                <span class="pos-title">{{ pos.title }}</span>
                <span class="match-badge" :class="{ 'high': pos.isHighMatch }">
                  {{ pos.isHighMatch ? 'HIGH MATCH' : 'POTENTIAL' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Summary Panel -->
        <div class="base-panel summary-panel p-lg mb-lg">
          <div class="panel-header mb-sm">
            <h3 class="text-xs font-black letter-spacing-wide">SUMMARY</h3>
          </div>
          <p class="summary-text">
            {{ analysisResult.summary }}
          </p>
        </div>

        <!-- Action Section -->
        <div class="action-section">
          <p class="action-desc">분석 결과가 실제와 잘 맞나요?</p>
          <div class="action-buttons">
            <button @click="router.push('/curriculum/suggest')" class="btn-primary px-xl">
              결과가 맞아요
            </button>
            <button @click="showFeedback = true" class="btn-outline px-xl">
              수정할게요
            </button>
          </div>
        </div>
      </div>

      <!-- ERROR STATE -->
      <div v-else class="error-container">
        <i class="fas fa-exclamation-triangle text-2xl mb-md"></i>
        <p>분석 데이터를 불러오지 못했습니다.</p>
        <button @click="store.loadAnalysisResult('user-123')" class="btn-outline-small mt-md">재시도</button>
      </div>
    </div>

    <!-- FEEDBACK MODAL -->
    <Transition name="fade">
      <div v-if="showFeedback" class="modal-overlay" @click.self="closeFeedback">
        <div class="modal-content base-panel">
          <div class="modal-header">
            <div class="flex-column">
              <h3 class="modal-title">EDIT FEEDBACK</h3>
              <p class="modal-subtitle">수정이 필요한 내용을 적어주세요</p>
            </div>
            <button @click="closeFeedback" class="btn-close">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <div class="modal-body">
            <textarea
              v-model="feedbackText"
              placeholder="예: TypeScript 숙련도가 실제보다 낮게 나온 것 같아요."
              class="base-textarea"
              rows="5"
            ></textarea>
          </div>

          <div class="modal-footer">
            <button @click="closeFeedback" class="btn-outline-small px-lg">취소</button>
            <button
              @click="submitFeedback"
              :disabled="!feedbackText.trim() || isSubmitting"
              class="btn-primary-small"
            >
              <i class="fas fa-spinner fa-spin mr-2" v-if="isSubmitting"></i>
              제출하기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { postProfileFeedback } from '@/api/aiApi'
import { getTechIcon } from '@/utils/techIcons'

const router = useRouter()
const store = useCalendarStore()

// Store 데이터 연동
const { analysisResult, isLoadingAI } = storeToRefs(store)

const showFeedback = ref(false)
const feedbackText = ref('')
const isSubmitting = ref(false)

onMounted(async () => {
  // 실제 백엔드 연동: 데이터가 없으면 API 호출
  if (!analysisResult.value) {
    await store.loadAnalysisResult('user-123') // 실제 유저 ID 매핑 필요
  }
})

const closeFeedback = () => {
  showFeedback.value = false
  feedbackText.value = ''
}

const submitFeedback = async () => {
  if (feedbackText.value.trim()) {
    isSubmitting.value = true
    try {
      const previousAnalysis = {
        summary: analysisResult.value?.summary || '',
        techDetails: (analysisResult.value?.skillLevels || []).map(s => ({
          techName: s.name,
          proficiencyPercentage: s.level,
          usageCount: (analysisResult.value?.repeatedTechs || []).find(t => t.name === s.name)?.count || 0
        })),
        recommendedPositions: (analysisResult.value?.recommendedPositions || []).map(p => ({
          positionName: p.title,
          fitLevel: p.isHighMatch ? 'HIGH' : 'MEDIUM'
        }))
      }

      const { data } = await postProfileFeedback({
        userId: 1,
        previousAnalysis,
        userFeedback: feedbackText.value
      })

      // 응답으로 분석 결과 갱신 + localStorage 덮어쓰기
      localStorage.setItem('analysisResult', JSON.stringify(data))
      await store.loadAnalysisResult()
      closeFeedback()
    } catch (e) {
      console.error('피드백 제출 실패:', e)
    } finally {
      isSubmitting.value = false
    }
  }
}
</script>

<style scoped>
.setup-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 72px 24px 24px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative;
}
.setup-card {
  width: 100%; max-width: 800px; max-height: calc(100vh - 100px);
  background: var(--bg-surface); border: 1px solid var(--border);
  padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; overflow-y: auto;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }

.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

/* ── Stepper (Matched) ── */
.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 10px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 12px; font-weight: 700; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); white-space: nowrap;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--clr-primary); color: var(--bg-base); border-color: var(--clr-primary); }
.page-stepper .step.done { color: var(--clr-success); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

/* ── Header ── */
.header-top { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 20px; }
.btn-back {
  background: transparent; border: 1px solid var(--border);
  font-weight: 800; font-size: 11px; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s; letter-spacing: 0.1em;
  padding: 8px 14px; display: inline-flex; align-items: center; gap: 8px;
}
.btn-back:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

.setup-header { border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 28px; }
.setup-header h2 { font-size: 24px; font-weight: 900; color: var(--text-primary); margin-bottom: 6px; letter-spacing: -0.02em; }
.setup-header p { font-size: 13px; font-weight: 600; color: var(--text-muted); }

/* Analysis Content */
.analysis-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
@media (max-width: 768px) { .analysis-grid { grid-template-columns: 1fr; } }

.base-panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 8px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.analysis-grid .base-panel:hover, .summary-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.panel-header { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }
.panel-header i { font-size: 14px; color: var(--text-primary); margin-right: 4px; }
.panel-header .fa-code { color: var(--clr-icon-github); }
.panel-header .fa-compass { color: var(--clr-icon-ai); }
.panel-header .fa-chart-bar { color: var(--clr-icon-growth); }
.panel-header h3 { font-size: 11px; font-weight: 900; letter-spacing: 0.15em; color: var(--text-muted); text-transform: uppercase; }

/* Tech Tags */
.tech-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.outline-badge { padding: 4px 12px; border: 1px solid var(--border); border-radius: 40px; font-size: 11px; font-weight: 700; color: var(--text-secondary); transition: all 0.2s; }
.outline-badge:hover { border-color: var(--text-primary); color: var(--text-primary); }

/* Skill List */
.skill-list { display: flex; flex-direction: column; gap: 14px; }
.skill-item { display: flex; flex-direction: column; gap: 4px; }
.skill-info { display: flex; justify-content: space-between; align-items: center; }
.skill-icon-wrap i { font-size: 18px; color: var(--text-primary); margin-right: 12px; }
.skill-name { font-size: 13px; font-weight: 800; color: var(--text-primary); letter-spacing: 0.02em; }
.skill-percent { font-size: 11px; font-weight: 800; color: var(--text-muted); font-family: 'Space Grotesk', monospace; }
.progress-bar-bg { height: 2px; background: var(--border); overflow: hidden; }
.progress-bar-fill { height: 100%; background: var(--clr-primary); transition: width 1s ease-out; }

/* Repeated List */
.repeated-list { display: flex; flex-direction: column; gap: 8px; }
.repeated-item { display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-weight: 600; }
.tech-count { font-size: 10px; color: var(--text-muted); font-weight: 700; }

/* Position List */
.position-list { display: flex; flex-direction: column; gap: 10px; }
.position-item { display: flex; justify-content: space-between; align-items: center; }
.pos-title { font-size: 12px; font-weight: 700; color: var(--text-primary); }
.match-badge { font-size: 9px; font-weight: 900; padding: 3px 8px; border: 1px solid var(--border); border-radius: 40px; color: var(--text-muted); }
.match-badge.high { background: var(--clr-primary); color: var(--bg-base); border-color: var(--clr-primary); }

/* Summary */
.summary-text { font-size: 13px; line-height: 1.7; color: var(--text-secondary); font-weight: 500; }
.letter-spacing-wide { letter-spacing: 0.2em; }

/* Action Sections */
.action-section { text-align: center; border-top: 1px solid var(--border); padding-top: 32px; margin-top: 16px; }
.action-desc { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 20px; }
.action-buttons { display: flex; gap: 12px; justify-content: center; }

.btn-primary { padding: 16px; border: 1px solid var(--clr-primary); background: var(--clr-primary); color: var(--bg-base); font-weight: 900; font-size: 14px; letter-spacing: 0.1em; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; }
.btn-primary:hover { background: transparent; color: var(--clr-primary); }

.btn-outline { padding: 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-weight: 800; font-size: 14px; letter-spacing: 0.1em; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.btn-outline:hover { border-color: var(--text-primary); background: var(--bg-hover); }

/* Loading & Error */
.loading-container, .error-container { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; text-align: center; color: var(--text-muted); }
.spinner { width: 32px; height: 32px; border: 2px solid var(--border); border-top-color: var(--text-primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Modal Styles */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); padding: 20px; }
.modal-content { width: 100%; max-width: 480px; padding: 32px; background: var(--modal-bg); border: 1px solid var(--modal-border); box-shadow: var(--modal-shadow); }
.modal-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.modal-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); margin-bottom: 2px; }
.modal-subtitle { font-size: 11px; color: var(--text-muted); font-weight: 600; }
.btn-close { background: transparent; border: none; color: var(--text-muted); cursor: pointer; transition: color 0.2s; font-size: 18px; }
.btn-close:hover { color: var(--text-primary); }
.base-textarea { width: 100%; background: transparent; border: 1px solid var(--border); padding: 16px; color: var(--text-primary); font-family: inherit; font-size: 13px; outline: none; transition: border-color 0.3s; resize: none; border-radius: 0; }
.base-textarea:focus { border-color: var(--text-primary); background: var(--bg-hover); }
.modal-footer { margin-top: 20px; display: flex; justify-content: flex-end; gap: 10px; }

.btn-primary-small { padding: 10px 20px; border: 1px solid var(--clr-primary); background: var(--clr-primary); color: var(--bg-base); font-weight: 800; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.btn-primary-small:hover:not(:disabled) { background: transparent; color: var(--clr-primary); }
.btn-outline-small { padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-weight: 700; font-size: 12px; cursor: pointer; transition: all 0.2s; }

/* Utils & Transitions */
.p-md { padding: 20px; }
.p-lg { padding: 32px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 40px; }
.px-lg { padding-left: 24px; padding-right: 24px; }
.px-xl { padding-left: 40px; padding-right: 40px; }
.mr-2 { margin-right: 8px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
```