<template>
  <div class="app-layout">
    <AppSidebar />
    <main class="main-content custom-scroll">
      
      <header class="page-header">
        <div>
          <h1 class="page-title">대시보드</h1>
          <p class="page-desc">현재 학습 진행 상황과 AI 분석 결과를 확인하세요.</p>
        </div>
      </header>

      <div class="page-body">
        <section class="ai-summary-card">
          <div class="ai-header">
            <i class="fas fa-sparkles ai-icon"></i>
            <h3>KAIROS AI 분석 요약</h3>
          </div>
          <p class="ai-text">
            최근 <strong>'Spring Boot'</strong> 트랙의 진척도가 매우 좋습니다! 반면 <strong>'CS 전공지식'</strong> 트랙은 지난주 대비 학습량이 30% 감소했습니다. 이번 주말에는 운영체제 복습에 시간을 투자하는 것을 추천합니다.
          </p>
        </section>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-title">진행 중인 트랙</div>
            <div class="stat-value">4<span class="stat-unit">개</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-title">이번 주 완료 일정</div>
            <div class="stat-value">12<span class="stat-unit">건</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-title">연속 학습 일수</div>
            <div class="stat-value">5<span class="stat-unit">일</span></div>
          </div>
        </div>

        <div class="grid-2col">
          <section class="dash-section">
            <h2 class="section-title">트랙별 진행 상황</h2>
            <div class="progress-list">
              <div v-for="track in store.activeTracks" :key="track.id" class="progress-item">
                <div class="prog-header">
                  <div class="prog-info">
                    <span class="prog-dot" :style="{ background: track.color }"></span>
                    <span class="prog-name">{{ track.name }}</span>
                  </div>
                  <span class="prog-pct">{{ Math.floor(Math.random() * 40) + 30 }}%</span>
                </div>
                <div class="prog-bar-bg">
                  <div class="prog-bar-fill" :style="{ width: Math.floor(Math.random() * 40) + 30 + '%', background: track.color }"></div>
                </div>
              </div>
            </div>
          </section>

          <section class="dash-section">
            <h2 class="section-title">최근 완료한 활동</h2>
            <div class="recent-list">
              <div class="recent-item">
                <div class="recent-date">어제</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: #607D8B">졸업작품</span>
                  <span class="recent-title">DB ERD 설계 완료</span>
                </div>
              </div>
              <div class="recent-item">
                <div class="recent-date">2일 전</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: #8B9A73">알고리즘</span>
                  <span class="recent-title">다익스트라 알고리즘 문제 풀이</span>
                </div>
              </div>
              <div class="recent-item">
                <div class="recent-date">3일 전</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: #B97A7E">정처기</span>
                  <span class="recent-title">필기 1과목 기출 오답노트</span>
                </div>
              </div>
            </div>
          </section>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const store = useCalendarStore()
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; background: var(--bg-base); }

.page-header { padding: 32px 40px; border-bottom: 1px solid var(--border); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.page-title { font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 6px; }
.page-desc { font-size: 13px; color: var(--text-faint); }

.page-body { padding: 32px 40px; display: flex; flex-direction: column; gap: 24px; max-width: 1100px; width: 100%; margin: 0 auto; }

/* AI 카드 */
.ai-summary-card { background: var(--bg-elevated); border: 1px solid var(--border); border-left: 4px solid var(--accent); border-radius: 12px; padding: 20px 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
.ai-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.ai-icon { color: var(--accent); font-size: 16px; }
.ai-header h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.ai-text { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
.ai-text strong { color: var(--text-primary); font-weight: 700; }

/* 통계 그리드 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.stat-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 8px; }
.stat-title { font-size: 12px; font-weight: 600; color: var(--text-muted); }
.stat-value { font-size: 28px; font-weight: 800; color: var(--text-primary); font-family: 'Inter', sans-serif; }
.stat-unit { font-size: 14px; font-weight: 600; color: var(--text-faint); margin-left: 4px; }

/* 2단 그리드 */
.grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
@media (max-width: 900px) { .grid-2col { grid-template-columns: 1fr; } }

.dash-section { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 12px; padding: 24px; }
.section-title { font-size: 15px; font-weight: 700; margin-bottom: 20px; color: var(--text-primary); }

.progress-list { display: flex; flex-direction: column; gap: 16px; }
.progress-item { display: flex; flex-direction: column; gap: 8px; }
.prog-header { display: flex; justify-content: space-between; align-items: center; }
.prog-info { display: flex; align-items: center; gap: 8px; }
.prog-dot { width: 8px; height: 8px; border-radius: 50%; }
.prog-name { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.prog-pct { font-size: 12px; font-weight: 700; color: var(--text-primary); font-family: 'Inter', sans-serif; }
.prog-bar-bg { width: 100%; height: 6px; background: var(--bg-base); border-radius: 999px; overflow: hidden; }
.prog-bar-fill { height: 100%; border-radius: 999px; transition: width 1s ease-out; }

.recent-list { display: flex; flex-direction: column; gap: 12px; }
.recent-item { display: flex; align-items: center; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--bg-base); }
.recent-item:last-child { border-bottom: none; padding-bottom: 0; }
.recent-date { font-size: 11px; font-weight: 600; color: var(--text-faint); width: 45px; flex-shrink: 0; }
.recent-content { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.recent-badge { font-size: 10px; font-weight: 700; color: #fff; padding: 2px 6px; border-radius: 4px; white-space: nowrap; }
.recent-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>