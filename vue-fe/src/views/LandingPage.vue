<template>
  <div class="landing-root custom-scroll" ref="scrollContainer" @scroll.passive="handleScroll">

    <header class="landing-header">
      <div class="landing-logo">
        <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
          <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
        </svg>
        <span class="landing-logo-text">KAIROS</span>
      </div>
      <div class="landing-header-actions">
        <button class="btn-theme" @click="themeStore.toggle()">
          <i :class="themeStore.isDark ? 'fas fa-sun' : 'fas fa-moon'" />
        </button>
        <button class="btn-login" @click="$router.push('/login')">LOGIN</button>
        <button class="btn-signup" @click="$router.push('/signup')">START NOW</button>
      </div>
    </header>

    <main class="landing-main">
      <section class="hero-section">
        <div class="anim-target reveal-up">
          <div class="hero-badge">
            <i class="fas fa-bolt" /> THE ULTIMATE AI DEV PLATFORM
          </div>
        </div>

        <h1 class="hero-title anim-target reveal-up delay-1">
          SEIZE YOUR<br>
          <span class="hero-title-solid">DEVELOPMENT TIME.</span>
        </h1>
        <p class="hero-desc anim-target reveal-up delay-2">
          AI 기반 맞춤형 학습 경로, GitHub 활동 연동, 그리고 강력한 일정 관리.<br>
          성장에만 집중할 수 있도록 설계된 개발자를 위한 통합 작업 공간입니다.
        </p>

        <div class="hero-cta anim-target reveal-up delay-3">
          <button class="btn-google" @click="$router.push('/signup')">
            <div class="google-icon">G</div>
            CONTINUE WITH GOOGLE
          </button>
          <a href="#" class="btn-intro" @click.prevent="scrollToSection('features')">EXPLORE PLATFORM <i class="fas fa-arrow-down" /></a>
        </div>
      </section>

      <div class="marquee-container border-y">
        <div class="marquee-content">
          <span>AI CURATION • GITHUB SYNC • NODE CALENDAR • CAREER TRACKING •</span>
          <span>AI CURATION • GITHUB SYNC • NODE CALENDAR • CAREER TRACKING •</span>
          <span>AI CURATION • GITHUB SYNC • NODE CALENDAR • CAREER TRACKING •</span>
        </div>
      </div>

      <nav class="feature-strip-nav" ref="navBar">
        <a href="#" class="nav-anchor" :class="{ active: activeSection === 'features' }" @click.prevent="scrollToSection('features')"><i class="fas fa-star" /> FEATURES</a>
        <a href="#" class="nav-anchor" :class="{ active: activeSection === 'workflow' }" @click.prevent="scrollToSection('workflow')"><i class="fas fa-project-diagram" /> WORKFLOW</a>
        <a href="#" class="nav-anchor" :class="{ active: activeSection === 'integration' }" @click.prevent="scrollToSection('integration')"><i class="fas fa-code-branch" /> INTEGRATION</a>
        <a href="#" class="nav-anchor" :class="{ active: activeSection === 'cta' }" @click.prevent="scrollToSection('cta')"><i class="fas fa-rocket" /> JOIN NOW</a>
      </nav>

      <section id="features" class="content-section pt-gap">
        <div class="section-header anim-target reveal-up">
          <h2 class="section-title">CORE CAPABILITIES</h2>
          <p class="section-sub">카이로스만이 제공하는 압도적인 개발자 성장 도구</p>
        </div>

        <div class="bento-grid">
          <div class="bento-card bento-large anim-target reveal-up">
            <div class="bento-icon"><i class="fas fa-code-branch" /></div>
            <div class="bento-content">
              <h3>GITHUB & VELOG SYNC</h3>
              <p>커밋 기록과 블로그 포스팅이 자동으로 학습 캘린더에 연동됩니다. 분산된 개발 기록을 하나의 타임라인으로 통합하여 관리하세요.</p>
            </div>
            <div class="bento-deco-box"></div>
          </div>

          <div class="bento-card anim-target reveal-up delay-1">
            <div class="bento-icon"><i class="fas fa-robot" /></div>
            <div class="bento-content">
              <h3>AI CURATION</h3>
              <p>당신의 현재 레벨과 목표 트랙을 분석하여 최적의 학습 자료와 다음 단계를 AI가 직접 추천합니다.</p>
            </div>
          </div>

          <div class="bento-card anim-target reveal-up delay-2">
            <div class="bento-icon"><i class="fas fa-project-diagram" /></div>
            <div class="bento-content">
              <h3>NODE GRAPH CALENDAR</h3>
              <p>단순한 달력이 아닙니다. 학습 간의 인과관계를 그래프 형태로 연결하여 시각적인 커리큘럼 맵을 생성합니다.</p>
            </div>
          </div>
        </div>
      </section>

      <section id="workflow" class="content-section bg-inverted pt-gap">
        <div class="section-header anim-target reveal-up">
          <h2 class="section-title">HOW IT WORKS</h2>
          <p class="section-sub">단 3단계로 끝나는 완벽한 성장 루프</p>
        </div>

        <div class="workflow-steps">
          <div class="step-box anim-target reveal-up">
            <div class="step-num">01</div>
            <h3>SET YOUR TRACK</h3>
            <p>목표하는 기술 스택(예: React, Spring)을 설정하고 커리큘럼을 등록합니다.</p>
          </div>
          <div class="step-box anim-target reveal-up delay-1">
            <div class="step-num">02</div>
            <h3>AUTO TRACKING</h3>
            <p>GitHub에 푸시하거나 블로그에 글을 쓰면 AI가 내용을 분석해 자동으로 일정을 완료 처리합니다.</p>
          </div>
          <div class="step-box anim-target reveal-up delay-2">
            <div class="step-num">03</div>
            <h3>GET INSIGHTS</h3>
            <p>매주 제공되는 분석 리포트와 AI 어시스턴트의 조언을 통해 학습의 빈틈을 메웁니다.</p>
          </div>
        </div>
      </section>

      <section id="integration" class="content-section pt-gap">
        <div class="anim-target draw-line"></div>
        <div class="stat-grid">
          <div class="stat-item anim-target reveal-up">
            <div class="stat-value">100%</div>
            <div class="stat-label">데이터 동기화율</div>
          </div>
          <div class="stat-item anim-target reveal-up delay-1">
            <div class="stat-value">ZERO</div>
            <div class="stat-label">수동 입력 시간</div>
          </div>
          <div class="stat-item anim-target reveal-up delay-2">
            <div class="stat-value">24/7</div>
            <div class="stat-label">AI 멘토링 대기</div>
          </div>
        </div>
        <div class="anim-target draw-line"></div>
      </section>

      <section id="cta" class="bottom-cta pt-gap">
        <h2 class="cta-title anim-target reveal-up">READY TO COMMIT?</h2>
        <p class="cta-desc anim-target reveal-up delay-1">더 이상 도구를 관리하는 데 시간을 낭비하지 마세요.<br>본질인 '개발'과 '학습'에 집중할 시간입니다.</p>
        <div class="anim-target reveal-up delay-2">
          <button class="btn-giant" @click="$router.push('/signup')">
            START YOUR JOURNEY <i class="fas fa-arrow-right" />
          </button>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <div class="footer-inner">
        <div>
          <div class="footer-logo">
            <svg width="20" height="20" viewBox="0 0 28 28" fill="none">
              <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
              <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
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
      <div class="footer-copy">© 2026 KAIROS. ALL RIGHTS RESERVED.</div>
    </footer>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore'
const themeStore = useThemeStore()

const footerCols = [
  { title: 'PRODUCT', items: ['Features', 'Integrations', 'Pricing', 'Changelog'] },
  { title: 'SUPPORT', items: ['Documentation', 'API Reference', 'Contact Us'] },
  { title: 'LEGAL',   items: ['Terms of Service', 'Privacy Policy'] }
]

const scrollContainer = ref(null)
const activeSection = ref('')
let observer = null

// 스크롤 시 섹션 감지 (Scrollspy 로직 개선)
const handleScroll = () => {
  if (!scrollContainer.value) return
  const sections = ['features', 'workflow', 'integration', 'cta']
  const containerTop = scrollContainer.value.getBoundingClientRect().top
  
  // 헤더(64) + 네비바(56) + 오차 허용치(30) = 150px
  // 이 기준선을 통과한 가장 "마지막" 섹션이 active 되도록 덮어씌움
  const triggerPoint = 150 
  let current = ''

  for (const id of sections) {
    const el = document.getElementById(id)
    if (el) {
      const rect = el.getBoundingClientRect()
      // 섹션의 머리(top)가 네비바 바로 아래(triggerPoint)로 올라오면 선택
      if (rect.top - containerTop <= triggerPoint) {
        current = id
      }
    }
  }
  activeSection.value = current
}

// 클릭 시 스크롤 이동
const scrollToSection = (id) => {
  const el = document.getElementById(id)
  if (el && scrollContainer.value) {
    // 헤더(64) + 네비바(56) 높이만큼 정확하게 빼서 스크롤 보정
    const offset = 120 
    scrollContainer.value.scrollTo({
      top: el.offsetTop - offset,
      behavior: 'smooth'
    })
  }
}

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
      }
    })
  }, { 
    threshold: 0.1, 
    rootMargin: "0px 0px -50px 0px"
  })

  document.querySelectorAll('.anim-target').forEach((el) => {
    observer.observe(el)
  })
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
/* 기본 레이아웃 & 스크롤 */
.landing-root {
  position: relative; /* 중요: el.offsetTop 계산을 위한 기준점 설정 (오차 방지) */
  height: 100vh;
  overflow-y: auto; 
  overflow-x: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
  display: flex; flex-direction: column;
}

.custom-scroll::-webkit-scrollbar { display: none; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }

/* ── 스크롤 애니메이션 클래스 ── */
.anim-target { will-change: transform, opacity; }
.reveal-up {
  opacity: 0; transform: translateY(40px);
  transition: transform 0.8s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal-up.is-visible { opacity: 1; transform: translateY(0); }
.draw-line { width: 0%; height: 2px; background: var(--text-primary); transition: width 1.2s cubic-bezier(0.16, 1, 0.3, 1); }
.draw-line.is-visible { width: 100%; }
.delay-1 { transition-delay: 0.15s; }
.delay-2 { transition-delay: 0.3s; }
.delay-3 { transition-delay: 0.45s; }

/* ── 마키(Marquee) 효과 ── */
.marquee-container { overflow: hidden; white-space: nowrap; width: 100%; background: var(--bg-surface); padding: 16px 0; }
.border-y { border-top: 2px solid var(--text-primary); border-bottom: 2px solid var(--text-primary); }
.marquee-content {
  display: inline-block; font-size: 14px; font-weight: 900; letter-spacing: 0.2em;
  color: var(--text-primary); animation: marquee 20s linear infinite;
}
.marquee-content span { padding-right: 40px; }
@keyframes marquee { 0% { transform: translateX(0); } 100% { transform: translateX(-33.3333%); } }

/* ── 헤더 ── */
.landing-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 40px; height: 64px;
  border-bottom: 2px solid var(--text-primary); background: var(--bg-base);
  position: sticky; top: 0; z-index: 100; flex-shrink: 0;
}
.landing-logo { display: flex; align-items: center; gap: 10px; color: var(--text-primary); }
.landing-logo-text { font-weight: 900; font-size: 20px; letter-spacing: 0.15em; }
.landing-header-actions { display: flex; gap: 12px; align-items: center; }

/* 회색 그림자 적용 (#6b7280) */
.btn-theme {
  width: 36px; height: 36px; border-radius: 0; background: transparent; border: 2px solid var(--text-primary);
  color: var(--text-primary); cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; transition: 0.1s;
}
.btn-theme:hover { background: var(--text-primary); color: var(--bg-base); transform: translate(-2px, -2px); box-shadow: 2px 2px 0 #6b7280; }

.btn-login {
  padding: 8px 20px; font-size: 13px; border: 2px solid var(--text-primary); border-radius: 0; cursor: pointer; background: transparent;
  color: var(--text-primary); font-weight: 800; letter-spacing: 0.05em; transition: 0.1s;
}
.btn-login:hover { background: var(--bg-hover); transform: translate(-2px, -2px); box-shadow: 2px 2px 0 #6b7280; }

.btn-signup {
  padding: 8px 20px; font-size: 13px; border: 2px solid var(--text-primary); border-radius: 0; cursor: pointer; font-weight: 900; letter-spacing: 0.05em;
  background: var(--text-primary); color: var(--bg-base); transition: 0.1s;
}
.btn-signup:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 2px 2px 0 #6b7280; }

/* ── 히어로 ── */
.hero-section {
  display: flex; flex-direction: column; align-items: center; text-align: center; padding: 140px 24px 120px;
}
.hero-badge {
  display: inline-flex; align-items: center; gap: 8px; padding: 6px 16px; border: 2px solid var(--text-primary);
  color: var(--text-primary); font-size: 12px; font-weight: 900; letter-spacing: 0.1em; margin-bottom: 40px; box-shadow: 4px 4px 0 #6b7280;
}
.hero-title { font-size: clamp(52px, 8vw, 96px); font-weight: 900; line-height: 1.05; color: var(--text-primary); margin-bottom: 32px; letter-spacing: -0.02em; text-transform: uppercase; }
.hero-desc { color: var(--text-muted); font-size: 18px; line-height: 1.6; font-weight: 600; max-width: 600px; margin-bottom: 56px; word-break: keep-all; }

.hero-cta { display: flex; gap: 20px; flex-wrap: wrap; justify-content: center; }
.btn-google { display: flex; align-items: center; gap: 12px; padding: 18px 32px; border: 2px solid var(--text-primary); cursor: pointer; background: var(--text-primary); color: var(--bg-base); font-weight: 900; font-size: 15px; letter-spacing: 0.05em; transition: 0.1s; box-shadow: 6px 6px 0 #6b7280; }
.btn-google:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 8px 8px 0 #6b7280; }
.google-icon { width: 24px; height: 24px; border: 2px solid var(--bg-base); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 900; transition: border-color 0.1s; }
.btn-google:hover .google-icon { border-color: var(--text-primary); }

.btn-intro { display: flex; align-items: center; gap: 10px; text-decoration: none; padding: 18px 32px; border: 2px solid var(--text-primary); cursor: pointer; background: transparent; color: var(--text-primary); font-size: 15px; font-weight: 900; transition: 0.1s; letter-spacing: 0.05em; box-shadow: 4px 4px 0 #6b7280; }
.btn-intro:hover { background: var(--bg-hover); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }

/* ── STICKY 네비게이션 ── */
.feature-strip-nav {
  position: sticky; top: 64px; z-index: 90; 
  display: flex; flex-wrap: wrap; justify-content: center; gap: 16px;
  background: var(--bg-surface);
  border-bottom: 2px solid var(--text-primary);
  padding: 12px 24px; width: 100%; height: 56px;
}
.nav-anchor {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 900; letter-spacing: 0.1em;
  color: var(--text-primary); text-decoration: none;
  padding: 6px 16px; border: 2px solid transparent; transition: all 0.2s;
}
.nav-anchor:hover { border-color: var(--border); }
.nav-anchor.active {
  background: var(--text-primary);
  color: var(--bg-base);
  border-color: var(--text-primary);
  transform: translateY(-2px);
  box-shadow: 4px 4px 0 #6b7280;
}

/* ── 공통 섹션 스타일 ── */
.pt-gap { padding-top: 100px; padding-bottom: 120px; }
.content-section { display: flex; flex-direction: column; align-items: center; max-width: 1200px; margin: 0 auto; width: 100%; padding-left: 40px; padding-right: 40px; }
.bottom-cta { text-align: center; background: var(--bg-base); border-top: 2px solid var(--text-primary); display: flex; flex-direction: column; align-items: center; }

.bg-inverted { max-width: 100%; background: var(--text-primary); color: var(--bg-base); }
.bg-inverted .section-title, .bg-inverted .section-sub { color: var(--bg-base); }

.section-header { text-align: center; margin-bottom: 64px; }
.section-title { font-size: 40px; font-weight: 900; letter-spacing: 0.05em; margin-bottom: 16px; }
.section-sub { font-size: 16px; font-weight: 700; color: var(--text-muted); }

/* ── 4. FEATURES ── */
.bento-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; width: 100%; }
.bento-card { border: 2px solid var(--text-primary); background: var(--bg-surface); padding: 40px; display: flex; flex-direction: column; gap: 24px; box-shadow: 6px 6px 0 #6b7280; transition: 0.15s; }
.bento-card:hover { transform: translate(-4px, -4px); box-shadow: 10px 10px 0 #6b7280; background: var(--bg-base); }
.bento-large { grid-column: span 2; flex-direction: row; align-items: center; gap: 48px; position: relative; overflow: hidden; }

.bento-icon { font-size: 40px; color: var(--text-primary); }
.bento-content h3 { font-size: 24px; font-weight: 900; margin-bottom: 16px; letter-spacing: 0.05em; }
.bento-content p { font-size: 15px; line-height: 1.6; color: var(--text-secondary); font-weight: 600; }
.bento-deco-box { position: absolute; right: -40px; bottom: -40px; width: 160px; height: 160px; border: 4px solid var(--text-primary); opacity: 0.1; transform: rotate(15deg); }

/* ── 5. WORKFLOW ── */
.workflow-steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 32px; width: 100%; max-width: 1200px; padding: 0 40px; }
.step-box { border: 2px solid var(--bg-base); padding: 40px; background: transparent; display: flex; flex-direction: column; gap: 20px; }
.step-num { font-size: 48px; font-weight: 900; font-family: monospace; color: var(--bg-base); border-bottom: 4px solid var(--bg-base); padding-bottom: 12px; width: max-content; }
.step-box h3 { font-size: 22px; font-weight: 900; letter-spacing: 0.05em; color: var(--bg-base); }
.step-box p { font-size: 15px; line-height: 1.6; color: var(--bg-base); opacity: 0.8; font-weight: 600; }

/* ── 6. INTEGRATION ── */
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); width: 100%; text-align: center; }
.stat-item { border-right: 2px solid var(--text-primary); padding: 60px 40px; }
.stat-item:last-child { border-right: none; }
.stat-value { font-size: 72px; font-weight: 900; font-family: monospace; letter-spacing: -0.05em; margin-bottom: 12px; color: var(--text-primary); }
.stat-label { font-size: 16px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.1em; text-transform: uppercase; }

/* ── 7. CTA ── */
.cta-title { font-size: 56px; font-weight: 900; letter-spacing: 0.05em; margin-bottom: 24px; color: var(--text-primary); }
.cta-desc { font-size: 18px; font-weight: 600; color: var(--text-muted); margin-bottom: 48px; line-height: 1.6; }
.btn-giant { display: flex; align-items: center; gap: 16px; padding: 24px 48px; background: var(--text-primary); color: var(--bg-base); border: 4px solid var(--text-primary); font-size: 20px; font-weight: 900; letter-spacing: 0.1em; cursor: pointer; box-shadow: 12px 12px 0 #6b7280; transition: 0.1s; }
.btn-giant:hover { background: transparent; color: var(--text-primary); transform: translate(-4px, -4px); box-shadow: 16px 16px 0 #6b7280; }

/* ── 8. 푸터 ── */
.landing-footer { background: var(--bg-surface); color: var(--text-primary); border-top: 2px solid var(--text-primary); padding: 64px 40px 32px; flex-shrink: 0; }
.footer-inner { max-width: 1200px; margin: 0 auto 64px; display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; }
.footer-logo { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; font-weight: 900; letter-spacing: 0.15em; font-size: 18px; }
.footer-tagline { font-size: 13px; font-weight: 600; color: var(--text-muted); }
.footer-inner h4 { font-weight: 900; margin-bottom: 20px; font-size: 14px; letter-spacing: 0.1em; }
.footer-inner ul { list-style: none; display: flex; flex-direction: column; gap: 12px; }
.footer-inner li { font-size: 13px; font-weight: 600; color: var(--text-muted); cursor: pointer; transition: 0.1s; }
.footer-inner li:hover { color: var(--text-primary); text-decoration: underline; }
.footer-copy { max-width: 1200px; margin: 0 auto; padding-top: 24px; border-top: 2px solid var(--border); text-align: center; font-size: 12px; font-weight: 700; letter-spacing: 0.05em; color: var(--text-muted); }

/* 반응형 */
@media (max-width: 900px) {
  .bento-grid { grid-template-columns: 1fr; }
  .bento-large { grid-column: span 1; flex-direction: column; align-items: flex-start; }
  .bento-deco-box { display: none; }
  .stat-grid { grid-template-columns: 1fr; border-top: 2px solid var(--text-primary); border-bottom: 2px solid var(--text-primary); }
  .stat-item { border-right: none; border-bottom: 2px solid var(--text-primary); }
  .stat-item:last-child { border-bottom: none; }
  .footer-inner { grid-template-columns: 1fr; }
}
</style>