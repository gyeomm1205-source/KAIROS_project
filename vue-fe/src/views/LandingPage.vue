<template>
  <div class="landing-root">

    <!-- ── 헤더 ── -->
    <header class="landing-header">
      <div class="landing-logo">
        <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="url(#kgL)" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="url(#kgL)" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="url(#kgL)"/>
          <path d="M8 5h12l-4 4H12L8 5z" fill="url(#kgL)" opacity="0.5"/>
          <defs>
            <linearGradient id="kgL" x1="6" y1="3" x2="22" y2="25" gradientUnits="userSpaceOnUse">
              <stop stop-color="#818cf8"/><stop offset="1" stop-color="#38bdf8"/>
            </linearGradient>
          </defs>
        </svg>
        <span class="landing-logo-text">KAIROS</span>
      </div>
      <div class="landing-header-actions">
        <button class="btn-theme" @click="themeStore.toggle()">
          <i :class="themeStore.isDark ? 'fas fa-sun' : 'fas fa-moon'" />
        </button>
        <button class="btn-login" @click="$router.push('/login')">로그인</button>
        <button class="btn-signup" @click="$router.push('/signup')">회원가입</button>
      </div>
    </header>

    <!-- ── 히어로 ── -->
    <main class="landing-main">
      <div class="hero-badge">
        <i class="fas fa-bolt" /> AI 개발자 성장 플랫폼
      </div>

      <h1 class="hero-title">
        카이로스와 함께<br>
        <span class="hero-title-gradient">성장하세요</span>
      </h1>
      <p class="hero-desc">
        AI 기반 맞춤형 학습 경로 · GitHub/Velog 연동 · 스마트 캘린더로<br>
        개발자 커리어를 한 단계 끌어올리세요.
      </p>

      <div class="hero-cta">
        <button class="btn-google" @click="$router.push('/signup')">
          <div class="google-icon">G</div>
          Google로 시작하기
        </button>
        <button class="btn-intro">서비스 소개 보기</button>
      </div>

      <!-- Feature strip -->
      <div class="feature-strip">
        <span><i class="fas fa-code-branch" /> Velog / GitHub 연동</span>
        <span><i class="fas fa-calendar-alt" /> 캘린더</span>
        <span><i class="fas fa-lightbulb" /> 학습 추천</span>
        <span><i class="fas fa-th-large" /> 커리큘럼 작성</span>
        <span><i class="fas fa-question-circle" /> FAQ</span>
      </div>

      <!-- Feature cards -->
      <div class="feature-cards">
        <div v-for="f in features" :key="f.title" class="feature-card">
          <div class="feature-icon"><i :class="f.icon" /></div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </main>

    <!-- ── 푸터 ── -->
    <footer class="landing-footer">
      <div class="footer-inner">
        <div>
          <div class="footer-logo">
            <svg width="18" height="18" viewBox="0 0 28 28" fill="none">
              <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="url(#kgF)" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
              <circle cx="14" cy="14" r="2" fill="url(#kgF)"/>
              <defs>
                <linearGradient id="kgF" x1="6" y1="3" x2="22" y2="25" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#818cf8"/><stop offset="1" stop-color="#38bdf8"/>
                </linearGradient>
              </defs>
            </svg>
            <span>KAIROS</span>
          </div>
          <p class="footer-tagline">개발자 성장을 위한 AI 멘토링 플랫폼</p>
        </div>
        <div v-for="col in footerCols" :key="col.title">
          <h4>{{ col.title }}</h4>
          <ul>
            <li v-for="it in col.items" :key="it">{{ it }}</li>
          </ul>
        </div>
      </div>
      <div class="footer-copy">© 2026 Kairos. 모든 권리 보유.</div>
    </footer>

  </div>
</template>

<script setup>
import { useThemeStore } from '@/stores/useThemeStore'
const themeStore = useThemeStore()

const features = [
  { icon: 'fas fa-code-branch', title: 'Velog / GitHub 연동', desc: '개발 블로그와 GitHub 활동을 연동하여 학습 진도를 자동 추적하세요.' },
  { icon: 'fas fa-calendar-alt', title: '스마트 캘린더', desc: 'AI가 제안하는 맞춤형 학습 일정으로 체계적으로 관리하세요.' },
  { icon: 'fas fa-robot', title: 'AI 학습 추천', desc: '현재 레벨과 목표에 맞는 콘텐츠를 실시간 큐레이션합니다.' },
]

const footerCols = [
  { title: '서비스', items: ['학습 추천', '커리큘럼 작성', '진도 관리'] },
  { title: '지원',   items: ['FAQ', '문의하기', '이용약관'] },
]
</script>

<style scoped>
.landing-root {
  min-height: 100vh; overflow-y: auto; height: 100vh;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
  display: flex; flex-direction: column;
}

/* ── 헤더 ── */
.landing-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 40px; height: 64px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
  position: sticky; top: 0; z-index: 50;
  flex-shrink: 0;
}
.landing-logo {
  display: flex; align-items: center; gap: 10px;
}
.landing-logo-text {
  font-weight: 800; font-size: 17px; letter-spacing: 0.16em;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.landing-header-actions { display: flex; gap: 10px; align-items: center; }

.btn-theme {
  width: 36px; height: 36px; border-radius: 9px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); cursor: pointer; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.btn-theme:hover { background: var(--bg-hover); color: var(--text-primary); }

.btn-login {
  padding: 8px 18px; font-size: 13px; border: 1px solid var(--border);
  border-radius: 9px; cursor: pointer; background: var(--bg-elevated);
  color: var(--text-muted); transition: all 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.btn-login:hover { background: var(--bg-hover); color: var(--text-primary); }

.btn-signup {
  padding: 8px 18px; font-size: 13px; border: none; border-radius: 9px;
  cursor: pointer; font-weight: 600;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  color: #fff; transition: opacity 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.btn-signup:hover { opacity: 0.85; }

/* ── 히어로 ── */
.landing-main {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; text-align: center;
  padding: 80px 24px 60px;
}

.hero-badge {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 6px 16px; border-radius: 999px;
  background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.3);
  color: #818cf8; font-size: 12px; font-weight: 600; margin-bottom: 28px;
}

.hero-title {
  font-size: clamp(34px, 6vw, 60px); font-weight: 800;
  line-height: 1.1; color: var(--text-primary); margin-bottom: 20px;
  letter-spacing: -0.02em;
}
.hero-title-gradient {
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  color: var(--text-muted); font-size: 16px; line-height: 1.75;
  max-width: 520px; margin-bottom: 40px;
}

.hero-cta { display: flex; gap: 12px; flex-wrap: wrap; justify-content: center; margin-bottom: 56px; }

.btn-google {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 28px; border-radius: 12px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  color: #fff; font-weight: 700; font-size: 15px;
  box-shadow: 0 8px 32px rgba(129,140,248,0.35);
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-google:hover { opacity: 0.88; }
.google-icon {
  width: 22px; height: 22px; border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800;
}
.btn-intro {
  padding: 14px 24px; border-radius: 12px; cursor: pointer;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); font-size: 14px;
  transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-intro:hover { background: var(--bg-hover); color: var(--text-primary); }

/* Feature strip */
.feature-strip {
  display: flex; flex-wrap: wrap; justify-content: center; gap: 24px;
  font-size: 13px; color: var(--text-muted);
  border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
  padding: 16px 0; width: 100%; max-width: 800px; margin-bottom: 56px;
}
.feature-strip span { display: flex; align-items: center; gap: 7px; }
.feature-strip i { color: #818cf8; }

/* Feature cards */
.feature-cards {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px; max-width: 880px; width: 100%;
}
.feature-card {
  padding: 28px 24px; border-radius: 16px; text-align: left;
  background: var(--bg-surface); border: 1px solid var(--border);
  transition: border-color 0.2s, transform 0.2s;
}
.feature-card:hover { border-color: rgba(129,140,248,0.5); transform: translateY(-2px); }
.feature-icon {
  width: 44px; height: 44px; border-radius: 12px; margin-bottom: 18px;
  background: rgba(129,140,248,0.12);
  display: flex; align-items: center; justify-content: center;
  color: #818cf8; font-size: 18px;
}
.feature-card h3 { font-weight: 700; font-size: 15px; color: var(--text-primary); margin-bottom: 8px; }
.feature-card p  { color: var(--text-muted); font-size: 13px; line-height: 1.65; }

/* ── 푸터 ── */
.landing-footer {
  background: #0f0f14; color: #6b7280;
  padding: 48px 40px 28px; flex-shrink: 0;
}
.footer-inner {
  max-width: 880px; margin: 0 auto;
  display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 40px;
  margin-bottom: 32px;
}
.footer-logo {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
  font-weight: 800; letter-spacing: 0.12em;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; font-size: 14px;
}
.footer-tagline { font-size: 12px; line-height: 1.7; }
.footer-inner h4 { color: #e5e7eb; font-weight: 700; margin-bottom: 14px; font-size: 13px; }
.footer-inner ul { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.footer-inner li { font-size: 12px; cursor: pointer; transition: color 0.15s; }
.footer-inner li:hover { color: #9ca3af; }
.footer-copy {
  max-width: 880px; margin: 0 auto;
  padding-top: 24px; border-top: 1px solid #1f2937;
  text-align: center; font-size: 11px;
}
</style>
