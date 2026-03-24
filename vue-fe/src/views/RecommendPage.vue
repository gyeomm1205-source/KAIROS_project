<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">
      <div class="page-header">
        <span class="page-title">레퍼런스 추천</span>
        <div class="header-right">
          <button class="icon-btn"><i class="fas fa-bell" /></button>
          <RouterLink to="/mypage" class="avatar-btn">K</RouterLink>
        </div>
      </div>

      <div class="rec-body fade-in">

        <div class="section-card">
          <div class="ai-header">
            <div class="ai-icon"><i class="fas fa-robot" /></div>
            <span>AI 학습 어시스턴트</span>
          </div>
          <div class="chat-bubbles">
            <div class="bubble bubble--ai">
              현재 학습 진행 상황을 분석하고 있습니다...
              <div class="typing-dots"><span /><span /><span /></div>
            </div>
            <div class="bubble bubble--msg">수학 기초 개념에서 약간의 부족함을 발견했습니다.</div>
            <div class="bubble bubble--msg">개인 맞춤형 학습 경로를 생성하고 있습니다...</div>
          </div>
        </div>

        <div class="rec-grid">
          <div class="section-card">
            <div class="rec-section-title"><i class="fas fa-graduation-cap" /> 학습 추천</div>
            <div v-for="c in courses" :key="c.title" class="rec-item">
              <h4>{{ c.title }}</h4>
              <p>{{ c.desc }}</p>
              <div class="rec-item-footer">
                <span class="rec-time"><i class="fas fa-clock" /> {{ c.time }}</span>
                <span class="rec-score">추천도 {{ c.score }}%</span>
              </div>
            </div>
          </div>
          <div class="section-card">
            <div class="rec-section-title"><i class="fas fa-book-open" /> 레퍼런스 추천</div>
            <div v-for="r in references" :key="r.title" class="rec-item">
              <h4>{{ r.title }}</h4>
              <p>{{ r.desc }}</p>
              <div class="rec-item-footer">
                <span class="ref-type"><i :class="r.icon" /> {{ r.type }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="action-row">
          <button class="btn-action btn-action--primary">
            <i class="fas fa-graduation-cap" /> 학습 추천
          </button>
          <button class="btn-action btn-action--secondary">
            <i class="fas fa-book-open" /> 레퍼런스 추천
          </button>
          <button class="btn-action btn-action--secondary">
            <i class="fas fa-plus" /> 새로운 공부 시작
          </button>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import AppSidebar from '@/components/AppSidebar.vue'

// ── 추천 데이터 ──
const courses = [
  { title: '기초 대수학',    desc: '현재 진도에 맞는 기초 개념 강화', time: '2시간',   score: 95 },
  { title: '문제 해결 전략', desc: '체계적인 문제 접근법 학습',        time: '1.5시간', score: 88 },
]
const references = [
  { title: 'Khan Academy - 대수학', desc: '기초부터 차근차근 설명하는 영상 강의', type: '영상 자료', icon: 'fas fa-play-circle' },
  { title: '수학의 정석 기초편',   desc: '체계적인 문제 풀이와 개념 정리',       type: '교재',    icon: 'fas fa-book' },
]
</script>

<style scoped>
.app-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.page-header {
  height: 64px; background: var(--bg-surface); border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px; flex-shrink: 0;
}
.page-title { font-weight: 700; font-size: 15px; color: var(--text-primary); }
.header-right { display: flex; align-items: center; gap: 14px; }
.icon-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); font-size: 16px; }
.avatar-btn {
  width: 32px; height: 32px; border-radius: 50%; text-decoration: none;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: #fff; font-weight: 700;
}

.rec-body {
  flex: 1; overflow-y: auto; padding: 28px 32px;
  display: flex; flex-direction: column; gap: 20px;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}
@keyframes fadeIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.fade-in { animation: fadeIn 0.35s ease both; }

.section-card {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 16px; padding: 22px 24px;
}

/* AI 헤더 */
.ai-header { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; font-weight: 700; font-size: 14px; color: var(--text-primary); }
.ai-icon { width: 32px; height: 32px; border-radius: 9px; background: rgba(129,140,248,0.15); color: #818cf8; display: flex; align-items: center; justify-content: center; font-size: 14px; }

/* 버블 */
.chat-bubbles { display: flex; flex-direction: column; gap: 10px; }
.bubble { display: inline-block; padding: 12px 16px; border-radius: 12px; font-size: 13px; color: var(--text-secondary); max-width: 80%; line-height: 1.55; }
.bubble--ai { background: var(--bg-elevated); border: 1px solid var(--border); border-bottom-left-radius: 4px; }
.bubble--msg { background: var(--bg-surface); border: 1px solid var(--border); box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.typing-dots { display: flex; gap: 4px; margin-top: 8px; }
.typing-dots span { width: 6px; height: 6px; border-radius: 50%; background: var(--text-faint); }
.typing-dots span:nth-child(1) { animation: bounce 1.2s infinite; }
.typing-dots span:nth-child(2) { animation: bounce 1.2s infinite 0.15s; }
.typing-dots span:nth-child(3) { animation: bounce 1.2s infinite 0.3s; }
@keyframes bounce { 0%,80%,100%{transform:translateY(0)} 40%{transform:translateY(-5px)} }

/* 추천 그리드 */
.rec-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }
.rec-section-title { display: flex; align-items: center; gap: 9px; font-weight: 700; font-size: 14px; color: var(--text-primary); margin-bottom: 16px; }
.rec-section-title i { color: #818cf8; }
.rec-item { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; margin-bottom: 12px; cursor: pointer; transition: all 0.15s; }
.rec-item:last-child { margin-bottom: 0; }
.rec-item:hover { background: var(--bg-hover); border-color: rgba(129,140,248,0.3); }
.rec-item h4 { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.rec-item p  { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; line-height: 1.5; }
.rec-item-footer { display: flex; justify-content: space-between; align-items: center; }
.rec-time { font-size: 11px; color: var(--text-faint); display: flex; align-items: center; gap: 5px; }
.rec-score { font-size: 11px; font-weight: 700; background: rgba(129,140,248,0.15); color: #818cf8; padding: 3px 9px; border-radius: 6px; }
.ref-type { font-size: 11px; color: var(--text-faint); display: flex; align-items: center; gap: 5px; }

/* 액션 버튼 */
.action-row { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-action { flex: 1; min-width: 140px; padding: 14px; border-radius: 12px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-action--primary { background: linear-gradient(135deg, #818cf8, #38bdf8); color: #fff; border: none; }
.btn-action--primary:hover { opacity: 0.87; }
.btn-action--secondary { background: var(--bg-surface); border: 1px solid var(--border); color: var(--text-muted); }
.btn-action--secondary:hover { background: var(--bg-hover); color: var(--text-primary); }
</style>