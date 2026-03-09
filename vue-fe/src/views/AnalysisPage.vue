<template>
  <div class="page-root">
    <header class="page-header">
      <div class="header-left"><i class="fas fa-chart-line" /> 분석 대시보드</div>
      <div class="header-right"><i class="fas fa-bell" /> <div class="avatar" /></div>
    </header>

    <main class="dashboard-content">
      <section class="profile-section">
        <div class="profile-avatar"><i class="fas fa-user-astronaut" /></div>
        <div class="profile-info">
          <h2>김철수님</h2>
          <p>분석 완료일: 2026년 3월 15일</p>
        </div>
      </section>

      <section class="summary-cards">
        <div class="card">
          <div class="card-top">
            <div class="card-icon"><i class="fas fa-code" /></div>
            <span class="card-label">총 기술</span>
          </div>
          <h3>12</h3>
          <p>확인된 기술 스택</p>
        </div>
        <div class="card">
          <div class="card-top">
            <div class="card-icon"><i class="fas fa-star" /></div>
            <span class="card-label">숙련도</span>
          </div>
          <h3>중급</h3>
          <p>전반적 수준</p>
        </div>
        <div class="card">
          <div class="card-top">
            <div class="card-icon"><i class="fas fa-trophy" /></div>
            <span class="card-label">강점</span>
          </div>
          <h3>프론트엔드</h3>
          <p>주요 전문 분야</p>
        </div>
      </section>

      <section class="chart-section panel">
        <div class="panel-header">
          <h3>기술 스택 분포</h3>
          <button class="btn-outline"><i class="fas fa-download" /> 내보내기</button>
        </div>
        <div class="chart-placeholder">
          <i class="fas fa-chart-pie" />
          <p>기술 스택 차트<br><span>카테고리별 기술 분포 그래프</span></p>
        </div>
        <div class="chart-legend">
          <span><div class="dot" style="background:#111" /> 프론트엔드</span>
          <span><div class="dot" style="background:#666" /> 백엔드</span>
          <span><div class="dot" style="background:#aaa" /> 데이터베이스</span>
          <span><div class="dot" style="background:#ddd" /> 도구</span>
        </div>
      </section>

      <section class="skills-section panel">
        <h3>상세 기술 목록</h3>
        <div class="skill-list">
          <div v-for="skill in skills" :key="skill.name" class="skill-item">
            <div class="skill-info">
              <div class="skill-icon"><i :class="skill.icon" /></div>
              <div>
                <h4>{{ skill.name }}</h4>
                <p>{{ skill.desc }}</p>
              </div>
            </div>
            <div class="skill-bar-wrap">
              <div class="skill-bar"><div class="fill" :style="{ width: skill.score + '%' }" /></div>
              <span class="skill-score">{{ skill.score }}%</span>
            </div>
          </div>
        </div>
      </section>

      <section class="confirm-section">
        <div class="confirm-icon"><i class="fas fa-question" /></div>
        <h3>이 결과가 맞나요?</h3>
        <p>분석된 기술 스택과 숙련도가 정확한지 확인해 주세요.</p>
        <div class="confirm-actions">
          <button class="btn-yes" @click="$router.push('/curriculum-suggest')"><i class="fas fa-check" /> 네</button>
          <button class="btn-no" @click="$router.push('/feedback')"><i class="fas fa-times" /> 아니오</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
const skills = [
  { name: 'React', desc: '프론트엔드 프레임워크', score: 85, icon: 'fab fa-react' },
  { name: 'Node.js', desc: '백엔드 런타임', score: 70, icon: 'fab fa-node-js' },
  { name: 'Python', desc: '프로그래밍 언어', score: 65, icon: 'fab fa-python' },
  { name: 'Docker', desc: '컨테이너 플랫폼', score: 60, icon: 'fab fa-docker' },
]
</script>

<style scoped>
.page-root { height: 100vh; overflow-y: auto; background: #fafafa; font-family: 'Escoredream', sans-serif; color: #111; padding-bottom: 60px; }
.page-header { display: flex; justify-content: space-between; padding: 16px 24px; background: #fff; border-bottom: 1px solid #eaeaea; }
.header-left { font-weight: 700; font-size: 15px; display: flex; align-items: center; gap: 8px; }
.header-right { display: flex; align-items: center; gap: 16px; color: #666; }
.avatar { width: 28px; height: 28px; border-radius: 50%; background: #ddd; }

.dashboard-content { max-width: 860px; margin: 0 auto; padding: 40px 20px; }
.profile-section { display: flex; align-items: center; gap: 16px; margin-bottom: 32px; }
.profile-avatar { width: 56px; height: 56px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #555; }
.profile-info h2 { font-size: 22px; font-weight: 800; margin-bottom: 4px; }
.profile-info p { font-size: 13px; color: #666; }

.summary-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.card { background: #fff; border: 1px solid #eaeaea; border-radius: 12px; padding: 20px; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-icon { width: 32px; height: 32px; background: #111; color: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.card-label { font-size: 12px; color: #888; font-weight: 600; }
.card h3 { font-size: 24px; font-weight: 800; margin-bottom: 4px; }
.card p { font-size: 12px; color: #666; }

.panel { background: #fff; border: 1px solid #eaeaea; border-radius: 12px; padding: 24px; margin-bottom: 24px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.panel h3 { font-size: 16px; font-weight: 700; }
.btn-outline { padding: 8px 14px; border: 1px solid #ddd; border-radius: 6px; background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; display: flex; gap: 6px; align-items: center; }

.chart-placeholder { height: 240px; background: #f9f9f9; border: 1px dashed #ddd; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; text-align: center; margin-bottom: 16px; gap: 12px; }
.chart-placeholder i { font-size: 32px; color: #ccc; }
.chart-placeholder span { font-size: 11px; color: #aaa; }
.chart-legend { display: flex; justify-content: center; gap: 24px; font-size: 12px; color: #555; }
.chart-legend span { display: flex; align-items: center; gap: 6px; }
.dot { width: 10px; height: 10px; border-radius: 3px; }

.skill-list { display: flex; flex-direction: column; gap: 16px; margin-top: 20px; }
.skill-item { display: flex; align-items: center; justify-content: space-between; }
.skill-info { display: flex; align-items: center; gap: 12px; width: 200px; }
.skill-icon { width: 40px; height: 40px; background: #f5f5f5; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #333; }
.skill-info h4 { font-size: 14px; font-weight: 700; margin-bottom: 2px; }
.skill-info p { font-size: 11px; color: #888; }
.skill-bar-wrap { flex: 1; display: flex; align-items: center; gap: 16px; max-width: 300px; }
.skill-bar { flex: 1; height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
.fill { height: 100%; background: #111; border-radius: 3px; }
.skill-score { font-size: 13px; font-weight: 600; width: 36px; text-align: right; }

.confirm-section { background: #fafafa; border: 1px solid #eaeaea; border-radius: 12px; padding: 40px 20px; text-align: center; margin-top: 40px; }
.confirm-icon { width: 48px; height: 48px; background: #111; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; margin: 0 auto 16px; }
.confirm-section h3 { font-size: 18px; font-weight: 800; margin-bottom: 8px; }
.confirm-section p { font-size: 13px; color: #666; margin-bottom: 24px; }
.confirm-actions { display: flex; justify-content: center; gap: 12px; }
.confirm-actions button { padding: 12px 32px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: opacity 0.2s; font-family: 'Escoredream', sans-serif; }
.btn-yes { background: #111; color: #fff; border: none; }
.btn-no { background: #fff; color: #111; border: 1px solid #ddd; }
.confirm-actions button:hover { opacity: 0.8; }
</style>