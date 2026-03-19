<template>
  <div class="page-root custom-scroll">
    <header class="page-header">
      <div class="header-left">
        <button class="btn-icon" @click="$router.push('/analyze')">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>
      <div class="step-indicator">CURRICULUM RECOMMENDATION</div>
    </header>

    <main class="dashboard-content">
      <div class="title-section">
        <div class="icon-wrap"><i class="fas fa-lightbulb" /></div>
        <h2>맞춤형 커리큘럼이 준비됐어요</h2>
        <p>분석 결과를 기반으로 최적의 학습 계획을 구성했습니다.<br>확인 후 캘린더에 반영해보세요.</p>
      </div>

      <section class="brutal-panel">
        <div class="panel-top">
          <i class="fas fa-calendar-alt panel-icon" />
          <h3>생성될 커리큘럼 일정</h3>
        </div>
        
        <div class="timeline-container">
          <div class="timeline-line"></div>
          <div v-for="(item, idx) in scheduleItems" :key="idx" class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-date">{{ item.date }}</div>
            <div class="timeline-card">
              <span class="tc-title">{{ item.title }}</span>
              <span class="tc-duration"><i class="fas fa-clock" /> {{ item.duration }}</span>
            </div>
          </div>
        </div>

        <div class="summary-badge">
          <i class="fas fa-flag-checkered" /> 총 6개 학습 · 약 12시간 소요 예상
        </div>
      </section>

      <section class="brutal-panel mt-10">
        <div class="panel-top">
          <i class="fas fa-magic panel-icon" />
          <h3>AI 추천 이유 요약</h3>
        </div>
        
        <div class="reason-list">
          <div v-for="(section, idx) in reasonSections" :key="idx" class="reason-box">
            <div class="reason-header">
              <div class="reason-title-wrap">
                <i :class="section.icon" class="text-blue-600" />
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
          
          <div class="final-summary-box">
            <i class="fas fa-arrow-right final-summary-icon" />
            <div class="final-summary-text">
              <strong style="color: var(--text-primary); font-weight: 900;">종합:</strong> 
              실제 코드·글에서 드러난 약점 보강 → 실전 적용 → 스택 확장 순서로 배치했습니다. 일정은 캘린더 여유 시간과 희망 학습량을 기준으로 산정했습니다.
            </div>
          </div>
        </div>
      </section>

      <section class="action-panel mt-10">
        <div class="action-text">
          <h3>이 커리큘럼을 캘린더에 반영할까요?</h3>
          <p>일정이 캘린더에 자동으로 추가됩니다.</p>
        </div>
        <div class="btn-group">
          <button class="btn-outline" @click="$router.push('/curriculum/alternative')">
            다른 커리큘럼 추천받기
          </button>
          <button class="btn-primary" @click="openCoachmark">
            <i class="fas fa-calendar-plus" /> 캘린더에 추가하기
          </button>
        </div>
      </section>
    </main>

    <div v-if="showCoachmark" class="modal-overlay" @click.self="closeCoachmark">
      <div class="brutal-modal">
        <div class="modal-icon"><i class="fas fa-magic" style="color: #ffca28" /></div>
        <h3>캘린더에 커리큘럼 기반으로 일정을 반영할게요</h3>
        <p>확인 버튼을 누르면 캘린더 페이지로 이동하며,<br>추천된 항목들이 새로운 노드로 자동 생성됩니다.</p>
        
        <div class="modal-actions">
          <button class="btn-outline-small" @click="closeCoachmark">닫기</button>
          <button class="btn-primary-small" @click="confirmAndGoCalendar">확인하고 이동</button>
        </div>
      </div>
    </div>

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
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showCoachmark = ref(false)
const isAnimating = ref(false)

const scheduleItems = [
  { date: "3월 11일 (수)", title: "React 기초 및 렌더링 최적화", duration: "2시간 예상" },
  { date: "3월 12일 (목)", title: "상태 관리 심화 (Zustand/Pinia)", duration: "1시간 30분 예상" },
  { date: "3월 13일 (금)", title: "TypeScript 고급 타입 활용", duration: "2시간 예상" },
  { date: "3월 14일 (토)", title: "번들 최적화 및 Lighthouse 분석", duration: "3시간 예상" },
  { date: "3월 16일 (월)", title: "Next.js App Router 심화", duration: "2시간 예상" },
  { date: "3월 17일 (화)", title: "API 설계 패턴 및 데이터 패칭 전략", duration: "1시간 30분 예상" },
]

const reasonSections = [
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
]

const openCoachmark = () => { showCoachmark.value = true }
const closeCoachmark = () => { showCoachmark.value = false }

// 🚀 라우팅 및 상태 전달 로직 🚀
const confirmAndGoCalendar = () => {
  showCoachmark.value = false
  isAnimating.value = true
  
  // 파트 1 애니메이션(압축)이 끝나는 0.6초 시점에 캘린더 페이지로 라우팅
  setTimeout(() => {
    // 캘린더 페이지에서만 2단계 애니메이션을 띄우도록 플래그 설정
    sessionStorage.setItem('playCalendarEntryAnim', 'true')
    router.push('/calendar')
  }, 600) 
}
</script>

<style scoped>
.page-root { height: 100vh; overflow-y: auto; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', sans-serif; display: block; padding-bottom: 60px; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid var(--border); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-left { display: flex; align-items: center; }
.btn-icon { background: transparent; border: none; padding: 8px 12px; font-weight: 700; font-size: 13px; color: var(--text-muted); cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: flex; align-items: center; gap: 8px; letter-spacing: 0.1em; }
.btn-icon:hover { color: var(--text-primary); transform: translateX(-4px); }
.step-indicator { font-size: 11px; font-weight: 800; background: var(--text-primary); color: var(--bg-base); padding: 6px 12px; letter-spacing: 0.1em; border-radius: 40px;}

.dashboard-content { max-width: 800px; margin: 0 auto; padding: 48px 24px; }
.title-section { text-align: center; margin-bottom: 40px; }
.icon-wrap { width: 48px; height: 48px; border: 1px solid var(--border); border-radius: 0; display: flex; align-items: center; justify-content: center; font-size: 20px; background: transparent; color: var(--text-primary); margin: 0 auto 16px; }
.title-section h2 { font-size: 28px; font-weight: 900; color: var(--text-primary); margin-bottom: 12px; }
.title-section p { font-size: 14px; font-weight: 600; color: var(--text-muted); line-height: 1.6; }

.mt-10 { margin-top: 40px; }

.brutal-panel { background: transparent; border: 1px solid var(--border); padding: 32px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
.brutal-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.panel-top { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
.panel-icon { font-size: 20px; color: var(--text-primary); }
.panel-top h3 { font-size: 18px; font-weight: 800; letter-spacing: 0.05em; color: var(--text-primary); margin: 0; }

.timeline-container { position: relative; padding-left: 20px; }
.timeline-line { position: absolute; left: 23px; top: 10px; bottom: 10px; width: 4px; background: var(--border); }
.timeline-item { position: relative; padding-bottom: 24px; padding-left: 24px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot { position: absolute; left: -4.5px; top: 4px; width: 12px; height: 12px; border: 2px solid var(--text-primary); background: var(--bg-base); z-index: 2; border-radius: 50%; }
.timeline-date { font-size: 13px; font-weight: 800; color: var(--text-muted); margin-bottom: 8px; font-family: inherit; }
.timeline-card { border: 1px solid var(--border); background: transparent; padding: 16px; display: flex; justify-content: space-between; align-items: center; gap: 16px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
.timeline-card:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateX(4px); }
.tc-title { font-size: 14px; font-weight: 800; color: var(--text-primary); line-height: 1.4; }
.tc-duration { font-size: 12px; font-weight: 700; color: var(--text-faint); display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.summary-badge { margin-top: 36px; display: inline-flex; align-items: center; gap: 8px; padding: 10px 16px; border: 1px solid var(--text-primary); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 800; border-radius: 40px; }

.reason-list { display: flex; flex-direction: column; gap: 16px; }
.reason-box { border: 1px solid var(--border); background: transparent; padding: 20px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
.reason-box:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.reason-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.reason-title-wrap { display: flex; align-items: center; gap: 8px; }
.reason-title { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.reason-meta { font-size: 11px; font-weight: 700; color: var(--text-muted); }
.reason-body { padding-left: 24px; display: flex; flex-direction: column; gap: 12px; }
.bullet-item { display: flex; align-items: flex-start; gap: 12px; }
.bullet-dot { margin-top: 6px; width: 4px; height: 4px; border-radius: 50%; background: var(--text-primary); flex-shrink: 0; }
.bullet-text { font-size: 13px; font-weight: 600; color: var(--text-primary); line-height: 1.6; }
.bullet-hint { color: var(--text-muted); font-weight: 800; margin-left: 4px; display: inline-block; }

.final-summary-box { display: flex; gap: 12px; align-items: flex-start; padding: 20px; background: transparent; border: 1px dashed var(--border); margin-top: 16px; }
.final-summary-icon { color: var(--text-primary); margin-top: 4px; }
.final-summary-text { font-size: 13px; font-weight: 600; color: var(--text-primary); line-height: 1.6; margin: 0; }

.action-panel { border: 1px solid var(--border); background: transparent; padding: 40px; display: flex; justify-content: space-between; align-items: center; gap: 24px; flex-wrap: wrap; margin-top: 48px; border-radius: 0; }
.action-text h3 { font-size: 18px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; letter-spacing: 0.05em; }
.action-text p { font-size: 13px; font-weight: 600; color: var(--text-muted); margin: 0; }

.btn-group { display: flex; gap: 16px; flex-wrap: wrap; }
.btn-outline, .btn-primary { padding: 14px 24px; font-size: 13px; font-weight: 800; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: flex; align-items: center; justify-content: center; gap: 8px; border: 1px solid var(--border); font-family: inherit; border-radius: 40px; letter-spacing: 0.05em; }
.btn-outline { background: transparent; color: var(--text-primary); }
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-primary { background: transparent; color: var(--text-primary); border-color: var(--text-primary); }
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 24px; }
.brutal-modal { width: 100%; max-width: 480px; background: var(--bg-surface); border: 1px solid var(--border); padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-icon { width: 48px; height: 48px; background: transparent; display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid var(--border); margin-bottom: 24px; border-radius: 0; }
.brutal-modal h3 { font-size: 18px; font-weight: 900; margin-bottom: 12px; line-height: 1.5; color: var(--text-primary); letter-spacing: 0.05em; }
.brutal-modal p { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; margin-bottom: 32px; }

.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-outline-small, .btn-primary-small { padding: 12px 24px; font-size: 13px; font-weight: 800; cursor: pointer; border: 1px solid var(--border); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; display: flex; align-items: center; justify-content: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em; }
.btn-outline-small { background: transparent; color: var(--text-primary); }
.btn-outline-small:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-primary-small { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.btn-primary-small:hover { background: transparent; color: var(--text-primary); }

/* 🚀 플라잉 애니메이션 Part 1 (중앙에서 알약 형태로 압축됨) */
.fly-overlay { position: fixed; inset: 0; z-index: 9999; pointer-events: none; display: flex; align-items: center; justify-content: center; }
.flying-curriculum-part1 { 
  display: flex; align-items: center; gap: 12px; padding: 24px 36px; 
  background: var(--text-primary); color: var(--bg-base); font-size: 20px; font-weight: 800; 
  border: 1px solid var(--text-primary); white-space: nowrap; font-family: inherit; 
  animation: packToCenter 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards; 
}
@keyframes packToCenter {
  0% { transform: scale(1); opacity: 0; border-radius: 0; }
  30% { transform: scale(1.1); opacity: 1; border-radius: 0; }
  100% { transform: scale(0.45); opacity: 1; border-radius: 100px; box-shadow: 0 0 40px rgba(0,0,0,0.3); }
}

@media (max-width: 640px) {
  .dashboard-content { padding: 24px 16px; }
  .title-section h2 { font-size: 22px; }
  .timeline-card { flex-direction: column; align-items: flex-start; gap: 8px; }
  .reason-header { flex-direction: column; align-items: flex-start; gap: 4px; }
  .btn-group { flex-direction: column; width: 100%; }
  .btn-outline, .btn-primary { width: 100%; justify-content: center; }
  .action-panel { flex-direction: column; align-items: flex-start; padding: 24px 16px; }
  .modal-actions { flex-direction: column; width: 100%; }
  .btn-outline-small, .btn-primary-small { width: 100%; justify-content: center; }
}
</style>