<template>
  <div class="page-root custom-scroll">
    <header class="page-header">
      <div class="header-left">
        <button class="btn-icon" @click="$router.push('/curriculum/suggest')">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>
      <div class="step-indicator">ALTERNATIVE CURRICULUM</div>
    </header>

    <main class="dashboard-content">
      <div class="title-section">
        <div class="icon-wrap"><i class="fas fa-lightbulb" /></div>
        <h2>같은 데이터를 바탕으로 다른 커리큘럼도 준비했어요</h2>
        <p>이번에는 프로젝트 적용성과 협업 효율을 높이는 방향으로 다시 구성했습니다.<br>기존 추천안과 비교해 더 맞는 흐름을 선택해보세요.</p>
      </div>

      <section class="brutal-panel mb-10">
        <div class="panel-top">
          <i class="fas fa-calendar-alt panel-icon" />
          <h3>생성될 커리큘럼 일정 (대체안)</h3>
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

        <div class="summary-badge mt-6">
          <i class="fas fa-flag-checkered" /> 총 6개 학습 · 약 12시간 소요 예상
        </div>
      </section>

      <section class="brutal-panel mb-10">
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
              첫 추천안이 약점 보강형 학습 경로였다면, 이번 안은 현재 진행 중인 프론트엔드 작업을 더 빠르게 개선할 수 있도록 서버 상태 관리, 공통 컴포넌트 설계, 테스트 도입 순으로 재배치했습니다.
            </div>
          </div>
        </div>
      </section>

      <section class="action-panel">
        <div class="action-text">
          <h3>이 커리큘럼을 캘린더에 반영할까요?</h3>
          <p>일정이 캘린더에 자동으로 추가됩니다.</p>
        </div>
        <div class="btn-group">
          <button class="btn-outline" @click="$router.push('/curriculum/suggest')">
            <i class="fas fa-undo" /> 처음 추천 보기
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
  { date: "3월 11일 (수)", title: "TanStack Query로 서버 상태 설계하기", duration: "1시간 30분 예상" },
  { date: "3월 12일 (목)", title: "폼 검증 흐름 개선과 에러 UX 정리", duration: "2시간 예상" },
  { date: "3월 13일 (금)", title: "공통 컴포넌트 API와 접근성 기준 정리", duration: "2시간 예상" },
  { date: "3월 14일 (토)", title: "TypeScript로 props 타입과 상태 모델링 고도화", duration: "2시간 예상" },
  { date: "3월 16일 (월)", title: "React Testing Library로 핵심 화면 테스트 작성", duration: "2시간 30분 예상" },
  { date: "3월 17일 (화)", title: "기존 프로젝트 리팩터링 스프린트", duration: "2시간 예상" },
]

const reasonSections = [
  {
    icon: "fab fa-github",
    title: "GitHub",
    meta: "· 최근 90일 · UI/상태관리 커밋 비중 높음",
    bullets: [
      { text: "API 연결 이후 로딩·에러 처리 분기 커밋이 반복되고, fetch 로직이 페이지마다 분산", hint: "→ 1일차 서버 상태 설계" },
      { text: "공통 UI 수정이 잦지만 props 구조와 타입 선언 패턴이 제각각", hint: "→ 3~4일차 컴포넌트 API·타입 모델링" },
    ],
  },
  {
    icon: "fas fa-file-alt",
    title: "Velog",
    meta: "· 최근 6개월 · 실전 적용형 글 위주",
    bullets: [
      { text: "상태관리·성능 관련 글은 꾸준하지만 테스트와 접근성 회고는 거의 없음", hint: "→ 3·5일차 접근성·테스트 보강" },
      { text: "학습 내용을 실제 서비스 개선 사례로 연결하려는 시도가 많음", hint: "→ 마지막 날 리팩터링 스프린트" },
    ],
  },
  {
    icon: "fas fa-calendar",
    title: "Google Calendar + 사전설문",
    meta: "· 최근 30일",
    bullets: [
      { text: "평일 19~22시 집중 시간이 확보되고, 하루 2시간 안팎 학습 선호", hint: "→ 1.5~2.5시간 단위 실습형 일정 배치" },
      { text: "설문에서 즉시 프로젝트에 적용 가능한 학습을 더 선호한다고 응답", hint: "→ 개념보다 유지보수·협업 역량 중심 재구성" },
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
    sessionStorage.setItem('playCalendarEntryAnim', 'true')
    router.push('/calendar')
  }, 600)
}
</script>

<style scoped>
.page-root { height: 100vh; overflow-y: auto; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', sans-serif; display: block; padding-bottom: 60px; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-left { display: flex; align-items: center; }
.btn-icon { background: transparent; border: 2px solid var(--text-primary); padding: 8px 12px; font-weight: 900; font-size: 12px; color: var(--text-primary); cursor: pointer; transition: all 0.1s; }
.btn-icon:hover { background: var(--text-primary); color: var(--bg-base); transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #6b7280; }
.step-indicator { font-size: 11px; font-weight: 900; background: var(--text-primary); color: var(--bg-base); padding: 6px 10px; letter-spacing: 0.1em; }

.dashboard-content { max-width: 800px; margin: 0 auto; padding: 48px 24px; }
.title-section { text-align: center; margin-bottom: 40px; }
.icon-wrap { width: 48px; height: 48px; border: 2px solid var(--text-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; background: var(--bg-base); color: var(--text-primary); margin: 0 auto 16px; box-shadow: 4px 4px 0 #6b7280; }
.title-section h2 { font-size: 26px; font-weight: 900; color: var(--text-primary); margin-bottom: 12px; line-height: 1.4; }
.title-section p { font-size: 14px; font-weight: 700; color: var(--text-muted); line-height: 1.6; }

.brutal-panel { background: var(--bg-surface); border: 2px solid var(--text-primary); padding: 32px; box-shadow: 8px 8px 0 #6b7280; transition: all 0.2s; }
.brutal-panel:hover { transform: translate(-2px, -2px); box-shadow: 10px 10px 0 #6b7280; }

.panel-top { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; border-bottom: 2px solid var(--border); padding-bottom: 16px; }
.panel-icon { font-size: 20px; color: var(--text-primary); }
.panel-top h3 { font-size: 18px; font-weight: 900; letter-spacing: 0.05em; color: var(--text-primary); }

.timeline-container { position: relative; padding-left: 20px; }
.timeline-line { position: absolute; left: 23px; top: 10px; bottom: 10px; width: 4px; background: var(--border); }
.timeline-item { position: relative; padding-bottom: 24px; padding-left: 24px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot { position: absolute; left: -3px; top: 4px; width: 14px; height: 14px; border: 3px solid var(--text-primary); background: var(--bg-base); z-index: 2; }
.timeline-date { font-size: 13px; font-weight: 900; color: var(--text-muted); margin-bottom: 8px; font-family: 'Escoredream', sans-serif; }
.timeline-card { border: 2px solid var(--border); background: var(--bg-base); padding: 16px; display: flex; justify-content: space-between; align-items: center; gap: 16px; transition: all 0.1s; }
.timeline-card:hover { border-color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #6b7280; }
.tc-title { font-size: 15px; font-weight: 800; color: var(--text-primary); line-height: 1.4; }
.tc-duration { font-size: 12px; font-weight: 800; color: var(--text-faint); display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.summary-badge { margin-top: 36px; display: inline-flex; align-items: center; gap: 8px; padding: 10px 16px; border: 2px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-size: 13px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; }

.reason-list { display: flex; flex-direction: column; gap: 16px; }
.reason-box { border: 2px solid var(--border); background: var(--bg-base); padding: 16px; transition: all 0.1s; }
.reason-box:hover { border-color: var(--text-primary); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.reason-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.reason-title-wrap { display: flex; align-items: center; gap: 8px; }
.reason-title { font-size: 14px; font-weight: 900; color: var(--text-primary); }
.reason-meta { font-size: 11px; font-weight: 800; color: var(--text-muted); }
.reason-body { padding-left: 24px; display: flex; flex-direction: column; gap: 8px; }
.bullet-item { display: flex; align-items: flex-start; gap: 8px; }
.bullet-dot { margin-top: 6px; width: 6px; height: 6px; background: var(--text-primary); flex-shrink: 0; }
.bullet-text { font-size: 13px; font-weight: 700; color: var(--text-primary); line-height: 1.5; }
.bullet-hint { color: #2563eb; font-weight: 900; margin-left: 4px; display: inline-block; }

.final-summary-box { display: flex; gap: 12px; align-items: flex-start; padding: 16px; background: var(--bg-surface); border: 2px dashed var(--text-primary); margin-top: 12px; }
.final-summary-icon { color: var(--text-primary); margin-top: 4px; }
.final-summary-text { font-size: 13px; font-weight: 700; color: var(--text-primary); line-height: 1.6; margin: 0; }

.action-panel { border: 2px solid var(--text-primary); background: var(--bg-surface); padding: 32px; display: flex; justify-content: space-between; align-items: center; gap: 24px; box-shadow: 8px 8px 0 #6b7280; flex-wrap: wrap; }
.action-text h3 { font-size: 18px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; }
.action-text p { font-size: 13px; font-weight: 700; color: var(--text-muted); }

.btn-group { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-outline, .btn-primary { padding: 14px 20px; font-size: 14px; font-weight: 900; cursor: pointer; transition: all 0.1s; display: flex; align-items: center; justify-content: center; gap: 8px; border: 2px solid var(--text-primary); font-family: 'Space Grotesk', 'Escoredream', sans-serif; }
.btn-outline { background: var(--bg-base); color: var(--text-primary); }
.btn-outline:hover { background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.btn-primary { background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; }
.btn-primary:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(4px); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 24px; }
.brutal-modal { width: 100%; max-width: 480px; background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 12px 12px 0 #6b7280; padding: 32px; animation: popUp 0.2s ease-out; }
@keyframes popUp { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

.modal-icon { width: 48px; height: 48px; background: var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 20px; border: 2px solid var(--text-primary); margin-bottom: 20px; box-shadow: 4px 4px 0 #6b7280; }
.brutal-modal h3 { font-size: 20px; font-weight: 900; margin-bottom: 12px; line-height: 1.4; color: var(--text-primary); }
.brutal-modal p { font-size: 13px; font-weight: 700; color: var(--text-muted); line-height: 1.6; margin-bottom: 24px; }

.modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-outline-small, .btn-primary-small { padding: 12px 20px; font-size: 13px; font-weight: 900; cursor: pointer; border: 2px solid var(--text-primary); transition: all 0.1s; font-family: 'Space Grotesk', 'Escoredream', sans-serif; }
.btn-outline-small { background: var(--bg-base); color: var(--text-primary); }
.btn-outline-small:hover { background: var(--border); }
.btn-primary-small { background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; }
.btn-primary-small:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }

/* 🚀 플라잉 애니메이션 Part 1 (중앙에서 알약 형태로 압축됨) */
.fly-overlay { position: fixed; inset: 0; z-index: 9999; pointer-events: none; display: flex; align-items: center; justify-content: center; }
.flying-curriculum-part1 { 
  display: flex; align-items: center; gap: 12px; padding: 24px 36px; 
  background: var(--text-primary); color: var(--bg-base); font-size: 20px; font-weight: 900; 
  border: 4px solid var(--text-primary); white-space: nowrap; font-family: 'Space Grotesk', 'Escoredream', sans-serif; 
  animation: packToCenter 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards; 
}
@keyframes packToCenter {
  0% { transform: scale(1); opacity: 0; border-radius: 0; }
  30% { transform: scale(1.1); opacity: 1; border-radius: 0; box-shadow: 16px 16px 0 #6b7280; }
  100% { transform: scale(0.45); opacity: 1; border-radius: 100px; box-shadow: 0 0 40px rgba(0,0,0,0.3); }
}

@media (max-width: 640px) {
  .btn-group { flex-direction: column; width: 100%; }
  .btn-outline, .btn-primary { width: 100%; justify-content: center; }
  .action-panel { flex-direction: column; align-items: flex-start; }
}
</style>