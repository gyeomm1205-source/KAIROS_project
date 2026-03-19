<template>
  <div class="landing-root" ref="scrollContainer" @mousemove="updateCursor">
    <!-- Custom Cursor (Desktop Only) -->
    <div class="custom-cursor" :class="{ 'is-hovering': isHovering }" :style="{ transform: `translate3d(${cursor.x}px, ${cursor.y}px, 0)` }"></div>

    <!-- HEADER -->
    <header class="landing-header" :class="{'is-sticky': isStickyMode}">
      <div class="landing-logo" @click="scrollToTop">
        <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
          <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
        </svg>
        <span>KAIROS</span>
      </div>

      <nav class="header-nav" :class="{'nav-visible': isStickyMode}">
        <a class="nav-anchor" :class="{ active: activeSection === 'features' }" @click.prevent="scrollToSection('features')">FEATURES</a>
        <a class="nav-anchor" :class="{ active: activeSection === 'workflow' }" @click.prevent="scrollToSection('workflow')">WORKFLOW</a>
        <a class="nav-anchor" :class="{ active: activeSection === 'integration' }" @click.prevent="scrollToSection('integration')">INTEGRATION</a>
        <a class="nav-anchor" :class="{ active: activeSection === 'cta' }" @click.prevent="scrollToSection('cta')">JOIN NOW</a>
      </nav>

      <div class="landing-header-actions">
        <button class="btn-outline" @mouseenter="isHovering=true" @mouseleave="isHovering=false" @click="$router.push('/login')">
          LOGIN
        </button>
        <button class="btn-solid" @mouseenter="isHovering=true" @mouseleave="isHovering=false" @click="$router.push('/signup')">
          GET STARTED <span class="btn-arrow">→</span>
        </button>
      </div>
    </header>

    <!-- MAIN -->
    <main class="landing-main">

      <!-- HERO SECTION -->
      <section class="hero-section">
        <div class="hero-video-wrapper">
          <video
            ref="heroVideo"
            class="hero-video"
            src="/grok-video-3c4d3961-6f79-41fb-90f1-890309aacb86.mp4"
            muted loop playsinline autoplay
            @timeupdate="checkVideoTime"
          ></video>
          <div class="hero-video-overlay"></div>
        </div>

        <div class="hero-content">
          <div class="hero-badge reveal-elem">
            <span class="badge-dot"></span>
            AI-POWERED DEVELOPER PLATFORM
          </div>
          <div class="hero-typography">
            <div class="reveal-wrap"><h1 class="reveal-elem">SEIZE YOUR</h1></div>
            <div class="reveal-wrap"><h1 class="reveal-elem delay-1 hero-accent">DEVELOPMENT</h1></div>
            <div class="reveal-wrap"><h1 class="reveal-elem delay-2">TIME</h1></div>
          </div>
          <p class="hero-desc reveal-elem delay-3">
            GitHub 활동 자동 연동, AI 맞춤 커리큘럼, 노드 기반 학습 캘린더.<br>
            개발자 성장을 위한 가장 스마트한 방법.
          </p>
          <div class="hero-cta reveal-elem delay-4">
            <button class="btn-cta-primary" @mouseenter="isHovering=true" @mouseleave="isHovering=false" @click="$router.push('/signup')">
              <span>무료로 시작하기</span>
              <span class="btn-cta-arrow">→</span>
            </button>
            <button class="btn-cta-ghost" @mouseenter="isHovering=true" @mouseleave="isHovering=false" @click.prevent="scrollToSection('features')">
              더 알아보기
            </button>
          </div>
        </div>

        <div class="scroll-indicator" :class="{ 'is-visible': isScrollIndicatorVisible }">
          <span class="scroll-text">SCROLL</span>
          <div class="scroll-line"></div>
        </div>
      </section>

      <!-- MARQUEE -->
      <div class="marquee-wrapper">
        <div class="marquee-track">
          <div class="marquee-content">
            <span>AI CURATION</span>
            <span class="marquee-dot">✦</span>
            <span>GITHUB SYNC</span>
            <span class="marquee-dot">✦</span>
            <span>NODE CALENDAR</span>
            <span class="marquee-dot">✦</span>
            <span>CAREER TRACKING</span>
            <span class="marquee-dot">✦</span>
            <span>VELOG SYNC</span>
            <span class="marquee-dot">✦</span>
            <span>AI CURATION</span>
            <span class="marquee-dot">✦</span>
            <span>GITHUB SYNC</span>
            <span class="marquee-dot">✦</span>
            <span>NODE CALENDAR</span>
            <span class="marquee-dot">✦</span>
            <span>CAREER TRACKING</span>
            <span class="marquee-dot">✦</span>
            <span>VELOG SYNC</span>
            <span class="marquee-dot">✦</span>
          </div>
        </div>
      </div>

      <!-- SECTION NAV (original position) -->
      <div class="nav-placeholder" ref="navPlaceholder">
        <nav class="section-nav" :class="{'is-hidden': isStickyMode}">
          <a class="nav-anchor" :class="{ active: activeSection === 'features' }" @click.prevent="scrollToSection('features')">FEATURES</a>
          <a class="nav-anchor" :class="{ active: activeSection === 'workflow' }" @click.prevent="scrollToSection('workflow')">WORKFLOW</a>
          <a class="nav-anchor" :class="{ active: activeSection === 'integration' }" @click.prevent="scrollToSection('integration')">INTEGRATION</a>
          <a class="nav-anchor" :class="{ active: activeSection === 'cta' }" @click.prevent="scrollToSection('cta')">JOIN NOW</a>
        </nav>
      </div>

      <!-- FEATURES SECTION -->
      <section class="content-section" id="features">
        <div class="section-label reveal-elem">CORE CAPABILITIES</div>
        <div class="reveal-wrap"><h2 class="section-title reveal-elem">카이로스만의<br><em>기하학적 성장 도구</em></h2></div>

        <div class="features-grid">
          <div class="feature-card reveal-elem" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="fc-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/></svg>
            </div>
            <div class="fc-number">01</div>
            <h3>GITHUB & VELOG SYNC</h3>
            <p>커밋 기록과 블로그 포스팅이 자동으로 학습 캘린더에 연동됩니다. 분산된 개발 기록을 하나의 흐름으로 통합하세요.</p>
            <div class="fc-arrow">→</div>
          </div>
          <div class="feature-card reveal-elem parallax-delay-1" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="fc-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z"/><path d="M12 16v-4M12 8h.01"/></svg>
            </div>
            <div class="fc-number">02</div>
            <h3>AI CURATION</h3>
            <p>현재 레벨과 목표 트랙을 분석하여 최적의 학습 자료와 다음 단계를 AI가 미니멀하고 직관적으로 추천합니다.</p>
            <div class="fc-arrow">→</div>
          </div>
          <div class="feature-card reveal-elem parallax-delay-2" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="fc-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <div class="fc-number">03</div>
            <h3>NODE CALENDAR</h3>
            <p>단순한 달력을 넘어섰습니다. 학습 간의 인과관계를 그래프 형태로 연결하여 완벽한 커리큘럼 맵을 구축합니다.</p>
            <div class="fc-arrow">→</div>
          </div>
        </div>
      </section>

      <!-- WORKFLOW SECTION -->
      <section class="content-section workflow-section" id="workflow">
        <div class="section-label reveal-elem">HOW IT WORKS</div>
        <div class="reveal-wrap"><h2 class="section-title reveal-elem">단 3단계로 끝나는<br><em>완벽한 성장 루프</em></h2></div>

        <div class="workflow-list">
          <div class="workflow-item reveal-elem" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="wf-left">
              <div class="wf-index">01</div>
            </div>
            <div class="wf-content">
              <h3>SET YOUR TRACK</h3>
              <p>목표하는 기술 스택을 선언하고 커리큘럼을 등록합니다.</p>
            </div>
            <div class="wf-arrow">→</div>
          </div>
          <div class="workflow-item reveal-elem delay-1" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="wf-left">
              <div class="wf-index">02</div>
            </div>
            <div class="wf-content">
              <h3>AUTO TRACKING</h3>
              <p>GitHub 푸시와 기술 블로그 글을 AI가 스캔하여 일정을 자동 완료 처리합니다.</p>
            </div>
            <div class="wf-arrow">→</div>
          </div>
          <div class="workflow-item reveal-elem delay-2" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
            <div class="wf-left">
              <div class="wf-index">03</div>
            </div>
            <div class="wf-content">
              <h3>GET INSIGHTS</h3>
              <p>깊이 있는 분석 리포트와 AI 멘토링을 통해 성장의 빈틈을 완전히 메웁니다.</p>
            </div>
            <div class="wf-arrow">→</div>
          </div>
        </div>
      </section>

      <!-- INTEGRATION STATS -->
      <section class="section-stats" id="integration">
        <div class="stat-bl reveal-elem" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
          <div class="stat-val">100<span class="stat-unit">%</span></div>
          <div class="stat-lbl">DATA SYNC</div>
          <div class="stat-desc">GitHub, Velog 자동 연동</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-bl reveal-elem delay-1" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
          <div class="stat-val">ZERO</div>
          <div class="stat-lbl">MANUAL INPUT</div>
          <div class="stat-desc">손으로 입력할 필요 없음</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-bl reveal-elem delay-2" @mouseenter="isHovering=true" @mouseleave="isHovering=false">
          <div class="stat-val">24<span class="stat-unit">/7</span></div>
          <div class="stat-lbl">AI MENTORING</div>
          <div class="stat-desc">언제나 곁에 있는 AI 코치</div>
        </div>
      </section>

      <!-- CTA SECTION -->
      <section class="section-cta" id="cta">
        <div class="cta-inner">
          <div class="reveal-wrap"><h2 class="cta-title reveal-elem">READY TO<br>COMMIT?</h2></div>
          <p class="cta-desc reveal-elem delay-1">본질인 '개발'과 '학습'에 집중할 시간입니다.</p>
          <div class="cta-actions reveal-elem delay-2">
            <button class="btn-cta-giant" @mouseenter="isHovering=true" @mouseleave="isHovering=false" @click="$router.push('/signup')">
              <span class="btn-giant-text">START YOUR JOURNEY</span>
              <span class="btn-giant-icon">→</span>
            </button>
          </div>
        </div>
      </section>
    </main>

    <!-- FOOTER -->
    <footer class="landing-footer">
      <div class="footer-top">
        <div class="footer-brand" @click="scrollToTop">
          <svg width="18" height="18" viewBox="0 0 28 28" fill="none">
            <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
            <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
          </svg>
          KAIROS
        </div>
        <div class="footer-links">
          <span class="footer-link" @click="scrollToSection('features')">FEATURES</span>
          <span class="footer-link" @click="scrollToSection('workflow')">WORKFLOW</span>
          <span class="footer-link" @click="scrollToSection('integration')">INTEGRATION</span>
          <span class="footer-link" @click="scrollToSection('cta')">JOIN NOW</span>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© 2026 KAIROS. ALL RIGHTS RESERVED.</span>
        <span>DESIGNED FOR DEVELOPERS</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'

/* ============================
   1. CUSTOM CURSOR & MAGNETIC
============================ */
const cursor = reactive({ x: -100, y: -100 })
const isHovering = ref(false)

const updateCursor = (e) => {
  cursor.x = e.clientX
  cursor.y = e.clientY
}

const handleMagneticMove = (e) => {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  const x = (e.clientX - rect.left - rect.width / 2) * 0.3
  const y = (e.clientY - rect.top - rect.height / 2) * 0.3
  el.style.transform = `translate(${x}px, ${y}px)`
}

const handleMagneticLeave = (e) => {
  const el = e.currentTarget
  el.style.transform = `translate(0px, 0px)`
  isHovering.value = false
}

/* ============================
   2. SCROLL REVEAL & SPY
============================ */
let observer = null
const scrollContainer = ref(null)
const activeSection = ref('')
const isStickyMode = ref(false)
const navPlaceholder = ref(null)

const handleScroll = () => {
  if (!scrollContainer.value) return
  const containerTop = scrollContainer.value.getBoundingClientRect().top

  if (navPlaceholder.value) {
    const rect = navPlaceholder.value.getBoundingClientRect()
    isStickyMode.value = rect.top - containerTop <= 80
  }

  const sections = ['features', 'workflow', 'integration', 'cta']
  const triggerPoint = 200
  let current = ''

  for (const id of sections) {
    const el = document.getElementById(id)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.top - containerTop <= triggerPoint) {
        current = id
      }
    }
  }
  activeSection.value = current
}

const scrollToSection = (id) => {
  const el = document.getElementById(id)
  if (el && scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: el.offsetTop,
      behavior: 'smooth'
    })
  }
}

const scrollToTop = () => {
  if (scrollContainer.value) scrollContainer.value.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ============================
   3. VIDEO TIME UPDATE
============================ */
const heroVideo = ref(null)
const isScrollIndicatorVisible = ref(false)

const checkVideoTime = () => {
  if (heroVideo.value && heroVideo.value.currentTime >= 4.5 && !isScrollIndicatorVisible.value) {
    isScrollIndicatorVisible.value = true
  }
}

/* ============================
   4. MOUNTED / UNMOUNTED
============================ */
onMounted(() => {
  // Reveal observer
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed')
      }
    })
  }, { threshold: 0.08, rootMargin: '0px 0px -60px 0px' })

  document.querySelectorAll('.reveal-elem').forEach(el => observer.observe(el))

  if (scrollContainer.value) {
    scrollContainer.value.addEventListener('scroll', handleScroll, { passive: true })
  }

  // show scroll indicator on fallback
  setTimeout(() => { isScrollIndicatorVisible.value = true }, 5000)
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  if (scrollContainer.value) {
    scrollContainer.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style scoped>
/* ──────────────────────────────
  BASE & ROOT
────────────────────────────── */
.landing-root {
  background: #000;
  color: #fff;
  height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
  font-family: 'Escoredream', 'Helvetica Neue', Arial, sans-serif;
  position: relative;
  -webkit-font-smoothing: antialiased;
  scroll-behavior: auto;
}
.landing-root::-webkit-scrollbar { display: none; }
.landing-root { -ms-overflow-style: none; scrollbar-width: none; }

/* ──────────────────────────────
  CUSTOM CURSOR
────────────────────────────── */
.custom-cursor { display: none; }
@media (hover: hover) and (pointer: fine) {
  .landing-root * { cursor: none !important; }
  .custom-cursor {
    display: block; position: fixed; top: 0; left: 0;
    width: 14px; height: 14px; border-radius: 50%; background: #fff;
    mix-blend-mode: difference; z-index: 99999; pointer-events: none;
    transform: translate3d(0,0,0);
    transition: width 0.35s cubic-bezier(0.16,1,0.3,1), height 0.35s cubic-bezier(0.16,1,0.3,1);
    margin-top: -7px; margin-left: -7px;
    will-change: transform;
  }
  .custom-cursor.is-hovering {
    width: 56px; height: 56px; margin-top: -28px; margin-left: -28px;
  }
}

/* ──────────────────────────────
  HEADER
────────────────────────────── */
.landing-header {
  position: fixed; top: 0; left: 0; width: 100%; height: 72px;
  padding: 0 40px; display: flex; justify-content: space-between; align-items: center;
  z-index: 500;
  background: transparent;
  border-bottom: 1px solid transparent;
  transition: background 0.5s ease, border-color 0.5s ease, backdrop-filter 0.5s;
}
.landing-header.is-sticky {
  background: rgba(0,0,0,0.88);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.landing-logo {
  display: flex; align-items: center; gap: 10px;
  font-size: 18px; font-weight: 900; letter-spacing: 0.16em; color: #fff;
  cursor: pointer;
  transition: opacity 0.2s;
}
.landing-logo:hover { opacity: 0.7; }

/* Header Nav (sticky) */
.header-nav {
  display: flex; align-items: center; gap: 28px;
  opacity: 0; pointer-events: none; transform: translateY(8px);
  transition: all 0.4s cubic-bezier(0.16,1,0.3,1);
}
.header-nav.nav-visible { opacity: 1; pointer-events: auto; transform: translateY(0); }
@media (max-width: 900px) { .header-nav { display: none; } }

.nav-anchor {
  font-size: 11px; font-weight: 800; letter-spacing: 0.18em;
  color: rgba(255,255,255,0.45); text-decoration: none;
  padding: 6px 0; position: relative; cursor: pointer;
  transition: color 0.2s;
}
.nav-anchor::after {
  content: ''; position: absolute; bottom: -2px; left: 0; width: 0; height: 1px;
  background: #fff; transition: width 0.3s cubic-bezier(0.16,1,0.3,1);
}
.nav-anchor:hover { color: #fff; }
.nav-anchor:hover::after { width: 100%; }
.nav-anchor.active { color: #fff; }
.nav-anchor.active::after { width: 100%; }

/* Header Actions */
.landing-header-actions { display: flex; gap: 12px; align-items: center; }

.btn-outline {
  padding: 10px 22px; background: transparent; color: rgba(255,255,255,0.8);
  border: 1px solid rgba(255,255,255,0.25); border-radius: 40px;
  font-size: 12px; font-weight: 700; letter-spacing: 0.08em;
  font-family: inherit; transition: all 0.3s cubic-bezier(0.16,1,0.3,1); cursor: pointer;
}
.btn-outline:hover {
  color: #fff; border-color: rgba(255,255,255,0.7);
  background: rgba(255,255,255,0.06);
  transform: translateY(-1px);
}

.btn-solid {
  padding: 10px 22px; background: #fff; color: #000;
  border: 1px solid #fff; border-radius: 40px;
  font-size: 12px; font-weight: 800; letter-spacing: 0.08em;
  font-family: inherit; display: flex; align-items: center; gap: 6px;
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1); cursor: pointer;
}
.btn-solid:hover {
  background: transparent; color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(255,255,255,0.15);
}
.btn-arrow {
  display: inline-block;
  transition: transform 0.3s cubic-bezier(0.16,1,0.3,1);
}
.btn-solid:hover .btn-arrow { transform: translateX(4px); }

/* ──────────────────────────────
  HERO SECTION
────────────────────────────── */
.hero-section {
  position: relative; height: 100vh; display: flex; align-items: center; justify-content: center;
  overflow: hidden; background: #000;
}
.hero-video-wrapper {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 380px; height: 680px; overflow: hidden; z-index: 1;
  border-radius: 2px; border: 1px solid rgba(255,255,255,0.08);
}
.hero-video { width: 100%; height: 100%; object-fit: cover; opacity: 0.85; }
.hero-video-overlay {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse at center, transparent 30%, rgba(0,0,0,0.6) 100%);
}
@media (max-width: 768px) {
  .hero-video-wrapper { width: 100vw; height: 100vh; border-radius: 0; border: none; }
}

.hero-content {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 100%; text-align: center; z-index: 2; pointer-events: none;
  padding: 0 24px;
}

.hero-badge {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 10px; font-weight: 700; letter-spacing: 0.2em;
  color: rgba(255,255,255,0.55); margin-bottom: 32px;
  border: 1px solid rgba(255,255,255,0.15); border-radius: 40px;
  padding: 8px 16px;
}
.badge-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #fff;
  animation: pulse-dot 2s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

.hero-typography {
  display: flex; flex-direction: column; gap: 4px; margin-bottom: 28px;
  mix-blend-mode: difference;
}
.hero-typography h1 {
  font-size: clamp(48px, 7vw, 96px); line-height: 0.95; margin: 0;
  font-weight: 900; color: #fff; letter-spacing: -0.03em; text-transform: uppercase;
}
.hero-accent {
  -webkit-text-stroke: 1px rgba(255,255,255,0.4);
  color: transparent !important;
}
@media (max-width: 768px) {
  .hero-typography h1 { font-size: 13vw; }
}

.hero-desc {
  font-size: 16px; color: rgba(255,255,255,0.6); line-height: 1.75;
  font-weight: 400; max-width: 540px; margin: 0 auto 36px;
  mix-blend-mode: normal; pointer-events: auto;
}

.hero-cta {
  display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;
  pointer-events: auto;
}

.btn-cta-primary {
  display: inline-flex; align-items: center; gap: 10px;
  padding: 16px 32px; background: #fff; color: #000;
  border: none; border-radius: 50px; font-size: 14px; font-weight: 800;
  letter-spacing: 0.04em; font-family: inherit; cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16,1,0.3,1);
  box-shadow: 0 0 0 0 rgba(255,255,255,0);
}
.btn-cta-primary:hover {
  background: transparent; color: #fff;
  box-shadow: 0 0 0 1px #fff, 0 16px 40px rgba(255,255,255,0.15);
  transform: translateY(-2px);
}
.btn-cta-arrow {
  display: inline-block;
  transition: transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.btn-cta-primary:hover .btn-cta-arrow { transform: translateX(6px); }

.btn-cta-ghost {
  display: inline-flex; align-items: center; gap: 10px;
  padding: 16px 32px; background: transparent; color: rgba(255,255,255,0.7);
  border: 1px solid rgba(255,255,255,0.2); border-radius: 50px; font-size: 14px;
  font-weight: 600; letter-spacing: 0.04em; font-family: inherit; cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16,1,0.3,1);
}
.btn-cta-ghost:hover {
  color: #fff; border-color: rgba(255,255,255,0.6);
  background: rgba(255,255,255,0.06); transform: translateY(-2px);
}

/* Scroll Indicator */
.scroll-indicator {
  position: absolute; bottom: 40px; left: 50%; transform: translateX(-50%);
  z-index: 10; display: flex; flex-direction: column; align-items: center; gap: 12px;
  opacity: 0; transition: opacity 1.5s cubic-bezier(0.16,1,0.3,1);
}
.scroll-indicator.is-visible { opacity: 1; }
.scroll-text { font-size: 9px; font-weight: 800; letter-spacing: 0.25em; color: rgba(255,255,255,0.4); }
.scroll-line { width: 1px; height: 48px; background: rgba(255,255,255,0.15); position: relative; overflow: hidden; }
.scroll-line::after {
  content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: #fff;
  animation: scrollDown 2s infinite cubic-bezier(0.16,1,0.3,1);
}
@keyframes scrollDown {
  0% { transform: scaleY(0) translateY(0); transform-origin: top; }
  50% { transform: scaleY(1) translateY(0); transform-origin: top; }
  51% { transform-origin: bottom; }
  100% { transform: scaleY(0); transform-origin: bottom; }
}

/* ──────────────────────────────
  MARQUEE
────────────────────────────── */
.marquee-wrapper {
  overflow: hidden; white-space: nowrap;
  border-top: 1px solid rgba(255,255,255,0.1);
  border-bottom: 1px solid rgba(255,255,255,0.1);
  padding: 18px 0; background: rgba(255,255,255,0.02);
}
.marquee-track { display: flex; width: max-content; animation: marquee-scroll 30s linear infinite; }
.marquee-content {
  display: inline-flex; align-items: center; gap: 32px;
  font-size: 12px; font-weight: 700; letter-spacing: 0.18em; color: rgba(255,255,255,0.4);
  padding-right: 32px;
}
.marquee-content span { white-space: nowrap; }
.marquee-dot { color: rgba(255,255,255,0.2); font-size: 10px; }
@keyframes marquee-scroll { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }

/* ──────────────────────────────
  SECTION NAV (original position)
────────────────────────────── */
.nav-placeholder {
  width: 100%; display: flex; align-items: center; justify-content: center;
  padding: 28px 0; border-bottom: 1px solid rgba(255,255,255,0.08);
}
.section-nav {
  display: flex; align-items: center; gap: 36px;
  transition: opacity 0.3s ease;
}
.section-nav.is-hidden { opacity: 0; pointer-events: none; }
@media (max-width: 768px) { .nav-placeholder { padding: 20px 0; } }

/* ──────────────────────────────
  REVEAL ANIMATIONS
────────────────────────────── */
.reveal-wrap { overflow: hidden; display: block; }
.reveal-elem {
  opacity: 0; transform: translateY(60px);
  transition: transform 1s cubic-bezier(0.16,1,0.3,1), opacity 0.9s ease;
  will-change: transform, opacity;
}
.reveal-elem.is-revealed { opacity: 1; transform: translateY(0); }
.delay-1 { transition-delay: 0.12s; }
.delay-2 { transition-delay: 0.24s; }
.delay-3 { transition-delay: 0.36s; }
.delay-4 { transition-delay: 0.48s; }
.parallax-delay-1 { transition-delay: 0.1s; }
.parallax-delay-2 { transition-delay: 0.2s; }

/* ──────────────────────────────
  SECTION LAYOUT BASICS
────────────────────────────── */
.content-section { padding: 90px 40px; max-width: 1360px; margin: 0 auto; min-height: 100vh; display: flex; flex-direction: column; justify-content: center; box-sizing: border-box; }
@media (max-width: 768px) { .content-section { padding: 80px 24px; min-height: 100vh; } }

.section-label {
  font-size: 11px; font-weight: 800; letter-spacing: 0.22em;
  color: rgba(255,255,255,0.35); margin-bottom: 24px; text-transform: uppercase;
}
.section-title {
  font-size: clamp(36px, 4.5vw, 60px); font-weight: 900;
  letter-spacing: -0.025em; line-height: 1.1; margin: 0 0 72px;
}
.section-title em { font-style: normal; -webkit-text-stroke: 1px rgba(255,255,255,0.5); color: transparent; }

/* ──────────────────────────────
  FEATURES GRID
────────────────────────────── */
.features-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  border: 1px solid rgba(255,255,255,0.12); gap: 0;
}
.feature-card {
  background: #000; padding: 56px 40px;
  display: flex; flex-direction: column; gap: 20px;
  border-right: 1px solid rgba(255,255,255,0.12);
  position: relative; overflow: hidden;
  transition: background 0.4s cubic-bezier(0.16,1,0.3,1);
  cursor: default;
}
.feature-card:last-child { border-right: none; }

/* Hover top-line sweep effect */
.feature-card::before {
  content: ''; position: absolute; top: 0; left: 0;
  width: 100%; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent);
  transform: translateX(-100%);
  transition: transform 0.6s cubic-bezier(0.16,1,0.3,1);
}
.feature-card:hover::before { transform: translateX(100%); }
.feature-card:hover { background: rgba(255,255,255,0.03); }

.fc-icon {
  width: 40px; height: 40px; color: rgba(255,255,255,0.4);
  transition: color 0.3s, transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.feature-card:hover .fc-icon { color: rgba(255,255,255,0.85); transform: translateY(-2px); }

.fc-number {
  font-size: 12px; font-family: monospace; color: rgba(255,255,255,0.2);
  letter-spacing: 0.1em;
}
.feature-card h3 {
  font-size: 20px; font-weight: 800; letter-spacing: 0.03em; line-height: 1.3; margin: 0;
}
.feature-card p { font-size: 14px; color: rgba(255,255,255,0.5); line-height: 1.7; margin: 0; }

.fc-arrow {
  font-size: 18px; color: rgba(255,255,255,0.2); margin-top: auto;
  transition: color 0.3s, transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.feature-card:hover .fc-arrow { color: rgba(255,255,255,0.7); transform: translateX(6px); }

@media (max-width: 900px) {
  .features-grid { grid-template-columns: 1fr; }
  .feature-card { border-right: none; border-bottom: 1px solid rgba(255,255,255,0.12); }
  .feature-card:last-child { border-bottom: none; }
}

/* ──────────────────────────────
  WORKFLOW SECTION
────────────────────────────── */
.workflow-section { background: transparent; }
.workflow-list {
  display: flex; flex-direction: column;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.workflow-item {
  display: flex; gap: 48px; padding: 56px 0;
  border-bottom: 1px solid rgba(255,255,255,0.1); align-items: center;
  cursor: default;
  transition: padding-left 0.5s cubic-bezier(0.16,1,0.3,1);
  position: relative; overflow: hidden;
}
/* Subtle left-fill on hover */
.workflow-item::before {
  content: ''; position: absolute; left: 0; top: 0;
  width: 3px; height: 0; background: #fff;
  transition: height 0.5s cubic-bezier(0.16,1,0.3,1);
}
.workflow-item:hover::before { height: 100%; }
.workflow-item:hover { padding-left: 20px; }

.wf-left { flex-shrink: 0; }
.wf-index {
  font-size: 56px; font-weight: 900; letter-spacing: -0.04em;
  color: rgba(255,255,255,0.08); line-height: 1;
  transition: color 0.4s;
}
.workflow-item:hover .wf-index { color: rgba(255,255,255,0.18); }

.wf-content { flex: 1; }
.wf-content h3 {
  font-size: 28px; font-weight: 900; margin: 0 0 12px; letter-spacing: -0.01em;
  transition: letter-spacing 0.4s cubic-bezier(0.16,1,0.3,1);
}
.workflow-item:hover .wf-content h3 { letter-spacing: 0.02em; }
.wf-content p { font-size: 16px; color: rgba(255,255,255,0.5); line-height: 1.65; margin: 0; max-width: 560px; }

.wf-arrow {
  font-size: 24px; color: rgba(255,255,255,0.15); flex-shrink: 0;
  transition: color 0.4s, transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.workflow-item:hover .wf-arrow { color: rgba(255,255,255,0.6); transform: translateX(8px); }

@media (max-width: 768px) {
  .workflow-item { flex-direction: column; gap: 16px; padding: 40px 0; align-items: flex-start; }
  .wf-index { font-size: 36px; }
  .wf-content h3 { font-size: 22px; }
  .wf-arrow { display: none; }
}

/* ──────────────────────────────
  STATS SECTION
────────────────────────────── */
.section-stats {
  display: grid; grid-template-columns: 1fr auto 1fr auto 1fr;
  padding: 80px 80px; align-items: center;
  min-height: 100vh;
  border-top: 1px solid rgba(255,255,255,0.1);
  border-bottom: 1px solid rgba(255,255,255,0.1);
  background: #000;
}
.stat-divider { width: 1px; height: 80px; background: rgba(255,255,255,0.1); }

.stat-bl {
  display: flex; flex-direction: column; align-items: center; text-align: center;
  padding: 20px 40px; cursor: default;
  transition: transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.stat-bl:hover { transform: translateY(-8px); }

.stat-val {
  font-size: clamp(40px, 6vw, 72px); line-height: 1; font-weight: 900; letter-spacing: -0.04em;
  transition: opacity 0.3s;
}
.stat-bl:hover .stat-val { opacity: 0.75; }
.stat-unit { font-size: 0.5em; font-weight: 400; opacity: 0.5; vertical-align: super; }

.stat-lbl {
  font-size: 11px; font-weight: 800; letter-spacing: 0.2em;
  color: rgba(255,255,255,0.35); margin-top: 16px; text-transform: uppercase;
}
.stat-desc {
  font-size: 13px; color: rgba(255,255,255,0.25); margin-top: 8px;
  transition: color 0.3s;
}
.stat-bl:hover .stat-desc { color: rgba(255,255,255,0.45); }

@media (max-width: 768px) {
  .section-stats {
    grid-template-columns: 1fr; gap: 48px; padding: 80px 24px;
  }
  .stat-divider { width: 80px; height: 1px; }
}

/* ──────────────────────────────
  CTA SECTION
────────────────────────────── */
.section-cta {
  padding: 90px 40px; text-align: center;
  min-height: 100vh;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
/* animated radial background */
.section-cta::before {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(ellipse 60% 60% at 50% 100%, rgba(255,255,255,0.04) 0%, transparent 70%);
  pointer-events: none;
}

.cta-inner { position: relative; z-index: 1; }
.cta-title {
  font-size: clamp(48px, 8vw, 96px); font-weight: 900; letter-spacing: -0.03em;
  margin: 0 0 28px; line-height: 1;
}
.cta-desc {
  font-size: 18px; color: rgba(255,255,255,0.5); margin-bottom: 56px; line-height: 1.6;
}
.cta-actions { display: flex; justify-content: center; }

.btn-cta-giant {
  display: inline-flex; align-items: center; gap: 16px;
  padding: 22px 52px; background: #fff; color: #000;
  border: none; border-radius: 80px; font-size: 15px; font-weight: 800;
  letter-spacing: 0.06em; font-family: inherit; cursor: pointer;
  transition: all 0.5s cubic-bezier(0.16,1,0.3,1);
  box-shadow: 0 0 0 0 rgba(255,255,255,0.3);
}
.btn-cta-giant:hover {
  background: transparent; color: #fff;
  box-shadow: 0 0 0 1px #fff, 0 24px 60px rgba(255,255,255,0.1);
  transform: translateY(-4px) scale(1.02);
}
.btn-giant-text { transition: letter-spacing 0.4s cubic-bezier(0.16,1,0.3,1); }
.btn-cta-giant:hover .btn-giant-text { letter-spacing: 0.1em; }
.btn-giant-icon {
  display: inline-block; font-size: 20px;
  transition: transform 0.4s cubic-bezier(0.16,1,0.3,1);
}
.btn-cta-giant:hover .btn-giant-icon { transform: translateX(10px); }

/* ──────────────────────────────
  FOOTER
────────────────────────────── */
.landing-footer {
  border-top: 1px solid rgba(255,255,255,0.1);
  padding: 56px 40px 36px; background: #000;
}
.footer-top {
  display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 80px;
}
.footer-brand {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 900; letter-spacing: 0.18em;
  cursor: pointer; opacity: 0.8; transition: opacity 0.3s;
}
.footer-brand:hover { opacity: 1; }

.footer-links { display: flex; gap: 36px; align-items: center; }
.footer-link {
  font-size: 11px; font-weight: 700; letter-spacing: 0.15em;
  color: rgba(255,255,255,0.35); cursor: pointer;
  transition: color 0.3s; position: relative;
}
.footer-link::after {
  content: ''; position: absolute; bottom: -3px; left: 0;
  width: 0; height: 1px; background: rgba(255,255,255,0.6);
  transition: width 0.3s cubic-bezier(0.16,1,0.3,1);
}
.footer-link:hover { color: rgba(255,255,255,0.8); }
.footer-link:hover::after { width: 100%; }

.footer-bottom {
  display: flex; justify-content: space-between;
  border-top: 1px solid rgba(255,255,255,0.08); padding-top: 28px;
  font-size: 11px; color: rgba(255,255,255,0.25); font-weight: 600; letter-spacing: 0.06em;
}

@media (max-width: 768px) {
  .footer-top { flex-direction: column; gap: 32px; margin-bottom: 48px; }
  .footer-links { flex-wrap: wrap; gap: 20px; }
  .footer-bottom { flex-direction: column; gap: 12px; }
}
</style>