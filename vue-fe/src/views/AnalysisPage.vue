<template>
  <div class="page-root custom-scroll">
    
    <div class="global-stepper-wrap">
      <div class="page-stepper">
        <div class="step done">1. 계정 연동</div>
        <div class="step done">2. 사전 설문</div>
        <div class="step done">3. 데이터 분석</div>
        <div class="step active">4. 결과 확인</div>
      </div>
    </div>

    <main class="dashboard-content">
      
      <div class="header-top">
        <button class="btn-back" @click="router.push('/setup')">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>

      <div class="title-section">
        <h2>현재 학습 상태를 이렇게 이해했어요</h2>
        <p>최근 GitHub, Velog, 사전설문 데이터를 종합 분석한 결과입니다.</p>
      </div>

      <div class="grid-layout">
        <div class="brutal-panel">
          <div class="panel-top">
            <i class="fas fa-code panel-icon" />
            <h3>최근 활동 기술</h3>
          </div>
          <div class="pill-group">
            <span v-for="t in recentTechs" :key="t" class="data-pill">
              {{ t }}
            </span>
          </div>
        </div>

        <div class="brutal-panel">
          <div class="panel-top">
            <i class="fas fa-chart-bar panel-icon" />
            <h3>전반적인 기술 숙련도</h3>
          </div>
          <div class="skill-bars">
            <div class="skill-row" v-for="s in skillLevels" :key="s.name">
              <span class="skill-name">{{ s.name }}</span>
              <div class="bar-bg">
                <div class="bar-fill" :style="{ width: s.level + '%' }" />
              </div>
              <span class="skill-pct">{{ s.level }}%</span>
            </div>
          </div>
        </div>

        <div class="brutal-panel">
          <div class="panel-top">
            <i class="fas fa-redo panel-icon" />
            <h3>반복적으로 다룬 기술</h3>
          </div>
          <div class="list-group">
            <div v-for="t in repeatedTechs" :key="t.name" class="list-item">
              <span class="item-name">{{ t.name }}</span>
              <span class="item-count">{{ t.count }}회 등장</span>
            </div>
          </div>
        </div>

        <div class="brutal-panel">
          <div class="panel-top">
            <i class="fas fa-compass panel-icon" />
            <h3>추천 포지션</h3>
          </div>
          <div class="position-group">
            <div v-for="pos in recommendedPositions" :key="pos.title" class="position-row">
              <span class="pos-title">{{ pos.title }}</span>
              <span class="pos-badge" :class="{ highlight: pos.isHighMatch }">
                {{ pos.isHighMatch ? '적합도 높음' : '가능성 있음' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="summary-panel">
        <h3><i class="fas fa-lightbulb text-blue-600 mr-2" /> 요약</h3>
        <p>
          최근 3개월간 React와 TypeScript 중심의 프론트엔드 학습을 꾸준히 진행하고 있습니다. 
          Velog에서는 주로 상태 관리와 성능 최적화 관련 글을 작성했고, GitHub에서는 Next.js 기반 프로젝트에 활발히 커밋하고 있습니다. 
          백엔드 관련 활동은 상대적으로 적어, 프론트엔드 전문성 강화를 추천합니다.
        </p>
      </div>

      <div class="action-section">
        <h3>분석 결과가 실제와 잘 맞나요?</h3>
        <div class="btn-group">
          <button class="btn-primary" @click="router.push('/curriculum/suggest')">
            <i class="fas fa-check" /> 결과가 맞아요
          </button>
          <button class="btn-outline" @click="showFeedback = true">
            <i class="fas fa-pen" /> 수정할게요
          </button>
        </div>
      </div>
    </main>

    <div v-if="showFeedback" class="modal-overlay" @click.self="showFeedback = false">
      <div class="brutal-modal">
        <div class="modal-header">
          <h3>어떤 부분을 수정하면 좋을까요?</h3>
          <button class="btn-close" @click="showFeedback = false"><i class="fas fa-times" /></button>
        </div>
        <p class="modal-desc">기술 숙련도, 관심 분야, 추천 포지션 등 수정이 필요한 내용을 자유롭게 적어주세요.</p>
        
        <textarea 
          v-model="feedbackText" 
          class="brutal-textarea" 
          placeholder="예: TypeScript 숙련도가 실제보다 낮게 나온 것 같아요."
          rows="4"
        ></textarea>

        <div class="modal-actions">
          <button class="btn-outline-small" @click="showFeedback = false">취소</button>
          <button 
            class="btn-primary-small" 
            :disabled="!feedbackText.trim()"
            @click="submitFeedback"
          >
            <i class="fas fa-paper-plane" /> 피드백 제출
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showFeedback = ref(false)
const feedbackText = ref('')

const recentTechs = ['React', 'TypeScript', 'Next.js', 'TailwindCSS']
const skillLevels = [
  { name: 'JavaScript', level: 78 },
  { name: 'React', level: 72 },
  { name: 'TypeScript', level: 55 },
  { name: 'Node.js', level: 40 },
  { name: 'Python', level: 30 },
]
const repeatedTechs = [
  { name: 'React', count: 23 },
  { name: 'TypeScript', count: 18 },
  { name: 'CSS', count: 14 },
  { name: 'REST API', count: 9 },
]
const recommendedPositions = [
  { title: '프론트엔드 개발자', isHighMatch: true },
  { title: '풀스택 개발자', isHighMatch: false }
]

const submitFeedback = () => {
  if (feedbackText.value.trim()) {
    showFeedback.value = false
    router.push('/curriculum/suggest')
  }
}
</script>

<style scoped>
.page-root { height: 100vh; overflow-y: auto; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', sans-serif; display: block; padding-bottom: 60px; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.global-stepper-wrap { position: absolute; top: 32px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 10px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 12px; font-weight: 700; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); white-space: nowrap;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.page-stepper .step.done { color: var(--text-primary); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

.dashboard-content { max-width: 900px; margin: 0 auto; padding: 110px 24px 40px; }

.header-top { margin-bottom: 24px; }
.btn-back { background: transparent; border: none; font-weight: 700; font-size: 14px; color: var(--text-muted); cursor: pointer; transition: color 0.3s, transform 0.3s; letter-spacing: 0.1em; padding: 0; display: inline-flex; align-items: center; gap: 8px;}
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px);}

.title-section { text-align: center; margin-bottom: 40px; }
.title-section h2 { font-size: 26px; font-weight: 900; color: var(--text-primary); margin-bottom: 12px; }
.title-section p { font-size: 14px; font-weight: 700; color: var(--text-muted); }

.grid-layout { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; margin-bottom: 24px; }

.brutal-panel { background: transparent; border: 1px solid var(--border); padding: 32px; display: flex; flex-direction: column; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
.brutal-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.panel-top { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
.panel-icon { font-size: 18px; color: var(--text-primary); }
.panel-top h3 { font-size: 15px; font-weight: 800; letter-spacing: 0.05em; color: var(--text-primary); margin: 0; }

.pill-group { display: flex; flex-wrap: wrap; gap: 8px; }
.data-pill { padding: 6px 14px; border: 1px solid var(--text-primary); background: transparent; font-size: 13px; font-weight: 700; color: var(--text-primary); border-radius: 40px;}

.skill-bars { display: flex; flex-direction: column; gap: 16px; }
.skill-row { display: flex; align-items: center; gap: 16px; color: var(--text-primary); }
.skill-name { width: 85px; font-size: 12px; font-weight: 700; }
.bar-bg { flex: 1; height: 4px; background: var(--border); }
.bar-fill { height: 100%; background: var(--text-primary); }
.skill-pct { width: 35px; text-align: right; font-size: 12px; font-weight: 800; }

.list-group { display: flex; flex-direction: column; gap: 12px; }
.list-item { display: flex; justify-content: space-between; align-items: center; padding-bottom: 12px; border-bottom: 1px solid var(--border); }
.list-item:last-child { border-bottom: none; padding-bottom: 0; }
.item-name { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.item-count { font-size: 12px; font-weight: 700; color: var(--text-muted); }

.position-group { display: flex; flex-direction: column; gap: 16px; }
.position-row { display: flex; justify-content: space-between; align-items: center; }
.pos-title { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.pos-badge { padding: 6px 12px; border: 1px solid var(--text-primary); font-size: 11px; font-weight: 800; background: transparent; color: var(--text-primary); border-radius: 40px;}
.pos-badge.highlight { background: var(--text-primary); color: var(--bg-base); }

.summary-panel { background: transparent; border: 1px solid var(--border); padding: 32px; margin-bottom: 40px; }
.summary-panel h3 { font-size: 16px; font-weight: 900; color: var(--text-primary); margin-bottom: 16px; display: flex; align-items: center; }
.summary-panel p { font-size: 14px; font-weight: 600; line-height: 1.8; color: var(--text-muted); margin: 0; }

.action-section { border-top: 1px solid var(--border); padding-top: 48px; text-align: center; }
.action-section h3 { font-size: 20px; font-weight: 900; color: var(--text-primary); margin-bottom: 32px; letter-spacing: 0.05em;}
.btn-group { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; }
.btn-primary, .btn-outline { padding: 16px 24px; font-size: 13px; font-weight: 800; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: flex; align-items: center; justify-content: center; gap: 8px; border: 1px solid var(--text-primary); font-family: inherit; width: 220px; letter-spacing: 0.1em; border-radius: 40px;}
.btn-primary { background: var(--text-primary); color: var(--bg-base); }
.btn-primary:hover { background: transparent; color: var(--text-primary); }
.btn-outline { background: transparent; color: var(--text-primary); border-color: var(--border); }
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 24px; }
.brutal-modal { width: 100%; max-width: 500px; background: var(--bg-surface); border: 1px solid var(--border); padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.modal-header h3 { font-size: 18px; font-weight: 900; color: var(--text-primary); letter-spacing: 0.05em; margin: 0; }
.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-muted); cursor: pointer; transition: color 0.3s; padding: 0; }
.btn-close:hover { color: var(--text-primary); }
.modal-desc { font-size: 13px; font-weight: 600; color: var(--text-muted); margin-bottom: 24px; line-height: 1.6; }

.brutal-textarea { width: 100%; padding: 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 14px; font-weight: 700; outline: none; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; resize: none; margin-bottom: 32px; border-radius: 0;}
.brutal-textarea:focus { border-color: var(--text-primary); background: var(--bg-hover);}

.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-outline-small, .btn-primary-small { padding: 12px 24px; font-size: 13px; font-weight: 800; cursor: pointer; border: 1px solid var(--text-primary); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; display: flex; align-items: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em; }
.btn-outline-small { background: transparent; color: var(--text-primary); border-color: var(--border); }
.btn-outline-small:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-primary-small { background: var(--text-primary); color: var(--bg-base); }
.btn-primary-small:hover:not(:disabled) { background: transparent; color: var(--text-primary); }
.btn-primary-small:disabled { background: transparent; border-color: var(--border); color: var(--text-faint); cursor: not-allowed; }

@media (max-width: 768px) {
  .grid-layout { grid-template-columns: 1fr; }
}
</style>