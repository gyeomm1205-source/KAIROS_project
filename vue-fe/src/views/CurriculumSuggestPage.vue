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
        <div class="icon-wrap"><i class="fas fa-lightbulb" /></div>
        <h2>맞춤형 커리큘럼이 준비됐어요</h2>
        <p>분석 결과를 기반으로 최적의 학습 계획을 구성했습니다.<br>확인 후 캘린더에 반영해보세요.</p>
      </div>

      <!-- LOADING STATE -->
      <div v-if="isLoading" class="loading-container">
        <div class="spinner"></div>
        <p class="loading-text">AI가 맞춤 커리큘럼을 생성 중입니다...</p>
      </div>

      <div v-else class="curriculum-body">
        <!-- Timeline Section -->
        <section class="base-panel p-lg mb-lg">
          <div class="panel-header mb-md">
            <i class="fas fa-calendar-alt"></i>
            <h3>PLANNED SCHEDULE</h3>
          </div>
          
          <div class="timeline-container">
            <div class="timeline-line"></div>
            <div v-for="(item, idx) in scheduleItems" :key="idx" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-date">{{ item.date }}</div>
              <div class="timeline-card base-panel">
                <span class="tc-title">{{ item.title }}</span>
                <span class="tc-duration"><i class="fas fa-clock" /> {{ item.duration }}</span>
              </div>
            </div>
          </div>

          <div class="summary-badge-wrap mt-md">
            <div class="outline-badge">
              <i class="fas fa-flag-checkered mr-2" /> 총 6개 학습 · 약 12시간 소요 예상
            </div>
          </div>
        </section>

        <!-- Reason Section -->
        <section class="base-panel p-lg mb-lg">
          <div class="panel-header mb-md">
            <i class="fas fa-magic"></i>
            <h3>WHY THIS PLAN?</h3>
          </div>
          
          <div class="reason-list">
            <div v-for="(section, idx) in reasonSections" :key="idx" class="reason-box base-panel p-md">
              <div class="reason-header mb-sm">
                <div class="reason-title-wrap">
                  <i :class="section.icon" class="text-muted" />
                  <span class="reason-title">{{ section.title }}</span>
                </div>
                <span class="reason-meta">{{ section.meta }}</span>
              </div>
              <div class="reason-body">
                <div v-for="(bullet, bIdx) in section.bullets" :key="bIdx" class="bullet-item">
                  <div class="bullet-dot"></div>
                  <div class="bullet-text">
                    {{ bullet.text }}
                    <span class="bullet-hint">{{ bullet.hint }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="final-summary-box p-md">
              <i class="fas fa-quote-left text-muted mb-sm" />
              <p class="final-summary-text">
                <strong class="text-primary">종합:</strong> 
                실제 코드·글에서 드러난 약점 보강 → 실전 적용 → 스택 확장 순서로 배치했습니다. 일정은 캘린더 여유 시간과 희망 학습량을 기준으로 산정했습니다.
              </p>
            </div>
          </div>
        </section>

        <!-- Action Panel -->
        <section class="action-section">
          <p class="action-desc">이 커리큘럼을 캘린더에 반영할까요?</p>
          <div class="action-buttons">
            <button class="btn-outline px-xl" @click="router.push('/curriculum/alternative')">
              다른 커리큘럼 추천받기
            </button>
            <button class="btn-primary px-xl" @click="openCoachmark">
              <i class="fas fa-calendar-plus" /> 캘린더에 추가하기
            </button>
          </div>
        </section>
      </div>
    </div>

    <!-- MODAL -->
    <Transition name="fade">
      <div v-if="showCoachmark" class="modal-overlay" @click.self="closeCoachmark">
        <div class="modal-content base-panel">
          <div class="modal-header">
            <div class="flex-column">
              <h3 class="modal-title">ADD TO CALENDAR</h3>
              <p class="modal-subtitle">추천된 일정을 반영하시겠습니까?</p>
            </div>
            <button @click="closeCoachmark" class="btn-close-modal">
               <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body mb-lg">
            <p class="text-sm text-secondary">
              확인 버튼을 누르면 캘린더 페이지로 이동하며,<br>추천된 항목들이 새로운 노드로 자동 생성됩니다.
            </p>
          </div>
          <div class="modal-actions">
            <button class="btn-outline-small" @click="closeCoachmark">취소</button>
            <button class="btn-primary-small" @click="confirmAndGoCalendar">확인하고 이동</button>
          </div>
        </div>
      </div>
    </Transition>

    <Teleport to="body">
      <div v-if="isAnimating" class="fly-overlay">
        <div class="flying-curriculum-part1">
          <i class="fas fa-calendar-check" />
          <span>일정 생성 중...</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postCurriculumGenerate, postCurriculumPreview, postCurriculaConfirm } from '@/api/aiApi'
import { useCalendarStore } from '@/stores/useCalendarStore'

const router = useRouter()
const store = useCalendarStore()
const showCoachmark = ref(false)
const isAnimating = ref(false)
const isLoading = ref(true)

const scheduleItems = ref([
  { date: "3월 11일 (수)", title: "React 기초 및 렌더링 최적화", duration: "2시간 예상" },
  { date: "3월 12일 (목)", title: "상태 관리 심화 (Zustand/Pinia)", duration: "1시간 30분 예상" },
  { date: "3월 13일 (금)", title: "TypeScript 고급 타입 활용", duration: "2시간 예상" },
  { date: "3월 14일 (토)", title: "번들 최적화 및 Lighthouse 분석", duration: "3시간 예상" },
  { date: "3월 16일 (월)", title: "Next.js App Router 심화", duration: "2시간 예상" },
  { date: "3월 17일 (화)", title: "API 설계 패턴 및 데이터 패칭 전략", duration: "1시간 30분 예상" },
])

const reasonSections = ref([
  {
    icon: "fab fa-github",
    title: "GitHub",
    meta: "· 최근 90일 · 커밋 247건",
    bullets: [
      { text: "React 커밋 68% 차지하나, useEffect 의존성 오류·리렌더링 패턴 반복", hint: "→ 1일차 렌더링 최적화" },
      { text: "Next.js 레포 존재하나 App Router 미사용, 번들 경고 커밋 2건", hint: "→ 4~5일차 번들·App Router 심화" },
    ],
  },
  {
    icon: "fas fa-file-alt",
    title: "Velog",
    meta: "· 최근 6개월 · 포스트 12편",
    bullets: [
      { text: "상태 관리 비교글 4편 작성했으나 결론 없이 종료 반복", hint: "→ 2일차 상태 관리 심화" },
      { text: "TS 제네릭 관련 질문글 3편, 타입 추론 이해 부족 확인", hint: "→ 3일차 TypeScript 고급 타입" },
    ],
  },
  {
    icon: "fas fa-calendar",
    title: "Google Calendar + 사전설문",
    meta: "· 최근 30일",
    bullets: [
      { text: "평일 19~22시 빈 슬롯 확인, 기존 학습 이벤트 평균 1.5h", hint: "→ 일 1.5~3h, 일요일 제외 배치" },
      { text: "사전설문 응답: 하루 2시간 이내 희망 · 실습 위주 선호", hint: "→ 학습량 상한 기준 반영" },
    ],
  },
])

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${days[d.getDay()]})`
}

onMounted(async () => {
  try {
    // Spring Boot 경유 시도 (preview API)
    const analysisRaw = localStorage.getItem('analysisResult')
    const analysisData = analysisRaw ? JSON.parse(analysisRaw) : {
      summary: '',
      techDetails: [],
      recommendedPositions: []
    }

    let data
    try {
      const res = await postCurriculumPreview({ analysisData })
      data = res.data
      if (data.curriculumPreviewKey) {
        store.curriculumPreviewKey = data.curriculumPreviewKey
      }
    } catch (previewErr) {
      console.error('curricula/preview 실패, FastAPI 직접 호출 폴백:', previewErr)
      const res = await postCurriculumGenerate({
        userId: 1,
        curriculumType: 'ONBOARDING',
        considerPersonalSchedule: false
      })
      data = res.data
    }

    if (data.nodes && data.nodes.length > 0) {
      store.curriculumResult = data
      scheduleItems.value = data.nodes.map(n => ({
        date: formatDate(n.scheduledDate),
        title: n.title,
        duration: `${n.expectedMinutes}분 예상`
      }))
    }

    if (data.recommendationReason) {
      const r = data.recommendationReason
      reasonSections.value = [{
        icon: "fas fa-robot",
        title: "AI 분석",
        meta: r.summaryLine || '',
        bullets: [
          { text: r.userContext || '', hint: '' },
          { text: r.aiInterpretation || '', hint: '' },
        ].filter(b => b.text)
      }]
    }
  } catch (e) {
    console.error('커리큘럼 생성 실패:', e)
  } finally {
    isLoading.value = false
  }
})

const openCoachmark = () => { showCoachmark.value = true }
const closeCoachmark = () => { showCoachmark.value = false }

const confirmAndGoCalendar = async () => {
  showCoachmark.value = false
  isAnimating.value = true

  try {
    if (store.curriculumResult) {
      await postCurriculaConfirm({ curriculumPreviewKey: store.curriculumPreviewKey || 'curriculumPreview:1:temp' })
    }
  } catch (e) {
    console.error('curricula/confirm 호출 실패 (BE 미구현):', e)
  }

  setTimeout(() => {
    sessionStorage.setItem('playCalendarEntryAnim', 'true')
    router.push('/calendar')
  }, 600)
}
</script>

<style scoped>
.setup-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 80px 24px 40px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative;
}
.setup-card {
  width: 100%; max-width: 800px; max-height: calc(100vh - 120px);
  background: var(--bg-surface); border: 1px solid var(--border);
  padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; overflow-y: auto;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }

.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

/* Stepper */
.global-stepper-wrap { position: fixed; top: 24px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 12px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 11px; font-weight: 700; transition: all 0.3s;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--text-primary); color: var(--bg-base); }
.page-stepper .step.done { color: var(--text-primary); }

.header-top { margin-bottom: 24px; }
.btn-back {
  background: transparent; border: 1px solid var(--border);
  font-weight: 800; font-size: 11px; color: var(--text-muted); padding: 8px 14px;
  cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px;
}
.btn-back:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

.setup-header { text-align: center; margin-bottom: 40px; border-bottom: 1px solid var(--border); padding-bottom: 32px; }
.icon-wrap { width: 48px; height: 48px; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--text-primary); margin: 0 auto 16px; }
.setup-header h2 { font-size: 24px; font-weight: 900; color: var(--text-primary); margin-bottom: 12px; }
.setup-header p { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; }

.base-panel { background: var(--bg-surface); border: 1px solid var(--border); transition: all 0.3s; }
.panel-header { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }
.panel-header i { font-size: 18px; color: var(--text-primary); margin-right: 4px; }
.panel-header h3 { font-size: 11px; font-weight: 900; letter-spacing: 0.15em; color: var(--text-muted); }

/* Timeline */
.timeline-container { position: relative; padding-left: 20px; }
.timeline-line { position: absolute; left: 23px; top: 10px; bottom: 10px; width: 2px; background: var(--border); }
.timeline-item { position: relative; padding-bottom: 24px; padding-left: 24px; }
.timeline-dot { position: absolute; left: -2px; top: 6px; width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; z-index: 2; }
.timeline-date { font-size: 11px; font-weight: 800; color: var(--text-muted); margin-bottom: 6px; }
.timeline-card { border: 1px solid var(--border); padding: 14px; display: flex; justify-content: space-between; align-items: center; }
.timeline-card:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.tc-title { font-size: 13px; font-weight: 700; color: var(--text-primary); }
.tc-duration { font-size: 11px; color: var(--text-muted); font-weight: 600; display: flex; align-items: center; gap: 4px; }

.outline-badge { display: inline-flex; align-items: center; padding: 6px 16px; border: 1px solid var(--border); border-radius: 40px; font-size: 11px; font-weight: 700; color: var(--text-secondary); }

/* Reasons */
.reason-box:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.reason-header { display: flex; justify-content: space-between; align-items: center; }
.reason-title-wrap { display: flex; align-items: center; gap: 10px; }
.reason-title-wrap i { font-size: 14px; margin-right: 2px; }
.reason-title { font-size: 13px; font-weight: 800; color: var(--text-primary); }
.reason-meta { font-size: 11px; color: var(--text-muted); font-weight: 600; }
.bullet-item { display: flex; align-items: flex-start; gap: 10px; margin-top: 12px; }
.bullet-dot { margin-top: 8px; width: 4px; height: 4px; border-radius: 50%; background: var(--text-primary); }
.bullet-text { font-size: 12.5px; font-weight: 500; color: var(--text-primary); line-height: 1.7; }
.bullet-hint { font-weight: 700; color: var(--text-muted); margin-left: 6px; display: inline-block; }

.final-summary-box { border: 1px dashed var(--border); margin-top: 16px; }
.final-summary-text { font-size: 12px; line-height: 1.6; color: var(--text-secondary); }

/* Actions */
.action-section { text-align: center; border-top: 1px solid var(--border); padding-top: 32px; margin-top: 16px; }
.action-desc { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 20px; }
.action-buttons { display: flex; gap: 12px; justify-content: center; }

.btn-primary { padding: 16px 32px; border: 1px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-weight: 900; font-size: 14px; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.btn-primary:hover { background: transparent; color: var(--text-primary); }
.btn-outline { padding: 16px 32px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-weight: 800; font-size: 14px; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.btn-outline:hover { border-color: var(--text-primary); background: var(--bg-hover); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); padding: 20px; }
.modal-content { width: 100%; max-width: 480px; padding: 32px; background: var(--modal-bg); border: 1px solid var(--modal-border); box-shadow: var(--modal-shadow); }
.modal-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; border-bottom: none; padding-bottom: 0; }
.btn-close-modal { background: transparent; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; font-size: 16px; transition: color 0.2s; margin-top: -4px; margin-right: -4px; }
.btn-close-modal:hover { color: var(--text-primary); }
.modal-title { font-size: 15px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); margin-bottom: 6px; }
.modal-subtitle { font-size: 11px; color: var(--text-muted); font-weight: 600; margin-bottom: 0; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }

.btn-primary-small { padding: 10px 20px; border: 1px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-weight: 800; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.btn-outline-small { padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-weight: 700; font-size: 12px; cursor: pointer; transition: all 0.2s; }

/* Fly Anim */
.fly-overlay { position: fixed; inset: 0; z-index: 9999; pointer-events: none; display: flex; align-items: center; justify-content: center; }
.flying-curriculum-part1 { 
  display: flex; align-items: center; gap: 12px; padding: 20px 32px; 
  background: var(--text-primary); color: var(--bg-base); font-size: 16px; font-weight: 800; 
  border-radius: 40px; animation: packToCenter 0.6s forwards; 
}
@keyframes packToCenter { 0% { transform: scale(1); opacity: 0; } 30% { transform: scale(1.1); opacity: 1; } 100% { transform: scale(0.45); opacity: 1; } }

/* Helpers */
.p-md { padding: 16px; }
.p-lg { padding: 32px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 40px; }
.mt-md { margin-top: 16px; }
.px-xl { padding-left: 40px; padding-right: 40px; }
.mr-2 { margin-right: 8px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 640px) {
  .setup-card { padding: 24px; }
  .action-buttons { flex-direction: column; }
  .btn-outline, .btn-primary { width: 100%; }
}

.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; text-align: center; color: var(--text-muted); }
.spinner { width: 32px; height: 32px; border: 2px solid var(--border); border-top-color: var(--text-primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { font-size: 13px; font-weight: 600; color: var(--text-muted); }
</style>