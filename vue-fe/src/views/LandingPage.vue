<template>
  <div class="land" ref="rootEl">
    <canvas ref="cvs" class="gl-canvas" />


    <!-- ══════════ FIXED TOP BAR (Logo & Buttons) ══════════ -->
    <div class="hd-fixed-top">
      <div class="hd-logo" @click="goTop">
        <svg width="18" height="18" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z"
            stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
          <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
        </svg>
        KAIROS
      </div>
      <div class="hd-right">
        <button class="btn-outline" @click="$router.push('/login')">로그인</button>
        <button class="btn-solid" @click="$router.push('/onboarding/connect')">시작하기 ↗</button>
      </div>
    </div>


    <!-- ══════════ HERO ══════════ -->
    <section class="s-hero">
      <!-- liquid glass orbs -->
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />

      <div class="hero-inner" :class="{in: heroIn}">
        <div class="hero-tag slide-up" style="--sd:0.05s">
          <span class="tag-dot" />
          AI-POWERED DEVELOPER GROWTH
        </div>
        <h1 class="hero-title">
          <span class="line slide-up" style="--sd:0.12s">Seize Your</span>
          <span class="line slide-up outline-text" style="--sd:0.22s">Development</span>
          <span class="line slide-up" style="--sd:0.32s">Time.</span>
        </h1>
        <p class="hero-sub slide-up" style="--sd:0.42s">
          GitHub 자동 연동 · AI 맞춤 커리큘럼 · 노드 기반 학습 캘린더<br>
          개발자 성장의 가장 스마트한 방법
        </p>
        <div class="hero-cta slide-up" style="--sd:0.52s">
          <button class="btn-solid lg" @click="$router.push('/onboarding/connect')">
            지금 바로 성장 시작하기 <span>→</span>
          </button>
          <button class="btn-ghost-text" @click="goto('features')">
            어떻게 작동하나요? ↓
          </button>
        </div>
        <!-- 손실 회피: 불안 해소 + 사회적 증거 -->
        <div class="hero-trust slide-up" style="--sd:0.62s">
          <span class="trust-item"><span class="trust-check">✓</span> 무료 · 카드 불필요</span>
          <span class="trust-sep">·</span>
          <span class="trust-item"><span class="trust-check">✓</span> 연동 데이터는 추천에만 사용</span>
          <span class="trust-sep">·</span>
          <span class="trust-item"><span class="trust-check">✓</span> 언제든 삭제 가능</span>
        </div>
      </div>

      <!-- glass stat cards -->
      <div class="glass-card gc-1 slide-up" :class="{in: heroIn}" style="--sd:0.65s">
        <div class="gc-val">100%</div>
        <div class="gc-label">Auto Sync</div>
      </div>
      <div class="glass-card gc-2 slide-up" :class="{in: heroIn}" style="--sd:0.8s">
        <div class="gc-val">AI</div>
        <div class="gc-label">Curated</div>
      </div>
      <div class="glass-card gc-3 slide-up" :class="{in: heroIn}" style="--sd:0.95s">
        <div class="gc-val">24/7</div>
        <div class="gc-label">Mentoring</div>
      </div>

      <div class="scroll-hint" :class="{in: heroIn}">
        <span>SCROLL</span>
        <div class="sh-line"><div class="sh-runner" /></div>
      </div>
    </section>

    <!-- ══════════ STICKY NAV BAR ══════════ -->
    <header class="hd-sticky" :class="{solid: scrolled}">
      <nav class="hd-nav">
        <span v-for="s in nav" :key="s.id"
          :class="{act: activeId===s.id}"
          @click="goto(s.id)">
          {{ s.label }}
        </span>
      </nav>
    </header>

    <!-- ══════════ TICKER ══════════ -->
    <div class="ticker-wrap">
      <div class="ticker-track">
        <div class="ticker-row" v-for="_ in 2" :key="_">
          <span v-for="t in tickers" :key="t+_">{{ t }}</span>
        </div>
      </div>
    </div>

    <!-- ══════════ FEATURES ══════════ -->
    <section class="sec" id="features">
      <div class="sec-label rv-fade">CORE CAPABILITIES</div>
      <h2 class="sec-title rv-slide">카이로스만의<br><em>성장 엔진</em></h2>

      <div class="feat-grid">
        <div class="feat-card rv-card"
          v-for="(f,i) in feats" :key="i"
          :style="{'--cd': i*0.08+'s'}">
          <div class="fc-top">
            <span class="fc-num">{{ String(i+1).padStart(2,'0') }}</span>
            <div class="fc-icon" v-html="f.ico" />
          </div>
          <h3>{{ f.t }}</h3>
          <p>{{ f.d }}</p>
          <div class="fc-footer">→</div>
          <!-- liquid glass shimmer -->
          <div class="card-shimmer" />
        </div>
      </div>
    </section>

    <!-- ══════════ MARQUEE 2 ══════════ -->
    <div class="ticker-wrap ticker-rev">
      <div class="ticker-track">
        <div class="ticker-row ticker-row-rev" v-for="_ in 2" :key="_">
          <span v-for="t in tickers2" :key="t+_">{{ t }}</span>
        </div>
      </div>
    </div>

    <!-- ══════════ WORKFLOW ══════════ -->
    <section class="sec sec-alt" id="workflow">
      <div class="sec-label rv-fade">HOW IT WORKS</div>
      <h2 class="sec-title rv-slide">5단계로 완성되는<br><em>성장 루프</em></h2>

      <div class="wf-list">
        <div class="wf-item rv-wf"
          v-for="(s,i) in steps" :key="i"
          :style="{'--cd': i*0.07+'s'}">
          <div class="wf-bar" />
          <span class="wf-num">{{ String(i+1).padStart(2,'0') }}</span>
          <div class="wf-body">
            <h3>{{ s.t }}</h3>
            <p>{{ s.d }}</p>
          </div>
          <span class="wf-arrow">→</span>
        </div>
      </div>
    </section>

    <!-- ══════════ LIQUID GLASS STATS ══════════ -->
    <!-- ② 자이가르닉 효과: 온보딩 4단계 진행 미리보기 -->
    <section class="sec sec-steps" id="integration">
      <div class="sec-label rv-fade">ONBOARDING FLOW</div>
      <h2 class="sec-title rv-slide">4단계로<br><em>시작합니다</em></h2>
      <div class="steps-preview rv-card" style="--cd:0.05s">
        <div class="sp-item" v-for="(st, i) in onboardSteps" :key="i">
          <div class="sp-num">{{ String(i+1).padStart(2,'0') }}</div>
          <div class="sp-body">
            <div class="sp-title">{{ st.t }}</div>
            <div class="sp-desc">{{ st.d }}</div>
          </div>
          <div class="sp-arrow" v-if="i < 3">→</div>
        </div>
      </div>
      <div class="stats-wrap" style="margin-top: 80px;">
        <div class="stat-glass rv-scale"
          v-for="(s,i) in stats" :key="i"
          :style="{'--cd': i*0.12+'s'}">
          <div class="sg-inner">
            <div class="sg-val">{{ s.v }}</div>
            <div class="sg-label">{{ s.l }}</div>
            <div class="sg-desc">{{ s.d }}</div>
          </div>
          <div class="sg-glow" />
        </div>
      </div>
    </section>

    <!-- ③ 현재 편향: 즉각 가치 미리보기 — "연동하면 바로 이게 보입니다" -->
    <section class="sec sec-preview">
      <div class="sec-label rv-fade">INSTANT VALUE</div>
      <h2 class="sec-title rv-slide">연동하면<br><em>바로 이게 보입니다</em></h2>
      <div class="preview-grid rv-card" style="--cd:0.05s">
        <div class="pv-card">
          <div class="pv-label">GitHub 분석</div>
          <div class="pv-mock">
            <div class="pv-bar" style="--w:78%"><span>React</span><span>78%</span></div>
            <div class="pv-bar" style="--w:55%"><span>TypeScript</span><span>55%</span></div>
            <div class="pv-bar" style="--w:42%"><span>Node.js</span><span>42%</span></div>
            <div class="pv-bar" style="--w:28%"><span>Docker</span><span>28%</span></div>
          </div>
          <div class="pv-caption">내 커밋 패턴을 AI가 자동 분석</div>
        </div>
        <div class="pv-card">
          <div class="pv-label">AI 추천 커리큘럼</div>
          <div class="pv-timeline">
            <div class="pt-item" v-for="(item, i) in previewTimeline" :key="i">
              <div class="pt-dot" :class="{'pt-done': i < 2}"></div>
              <div class="pt-text">{{ item }}</div>
            </div>
          </div>
          <div class="pv-caption">지금 내 수준에 맞는 다음 학습 자동 제안</div>
        </div>
        <div class="pv-card">
          <div class="pv-label">오늘의 추천</div>
          <div class="pv-recommend">
            <div class="pr-badge">AI 추천</div>
            <div class="pr-title">React Server Components 심화</div>
            <div class="pr-meta">최근 Next.js 커밋 흐름 기반 · 예상 4시간</div>
            <div class="pr-tags">
              <span>React</span><span>SSR</span><span>Next.js</span>
            </div>
          </div>
          <div class="pv-caption">활동 데이터 기반 오늘의 맞춤 학습</div>
        </div>
      </div>
    </section>

    <!-- ══════════ BENTO ══════════ -->
    <section class="sec sec-alt" id="bento">
      <div class="sec-label rv-fade">PLATFORM</div>
      <h2 class="sec-title rv-slide">하나의 플랫폼,<br><em>모든 성장 도구</em></h2>

      <div class="bento-grid">
        <!-- GitHub card -->
        <div class="bento-card bc-wide rv-card" style="--cd:0s">
          <div class="bc-label">GitHub Sync</div>
          <h3>커밋 하나도<br>놓치지 않습니다</h3>
          <p>모든 GitHub 활동이 자동 수집되어 AI 분석에 사용됩니다.</p>
          <div class="commit-bars">
            <div class="cb" v-for="(h,i) in commitH" :key="i"
              :style="{height: h+'%', '--cd': i*0.03+'s'}" />
          </div>
          <div class="card-shimmer" />
        </div>

        <!-- AI card -->
        <div class="bento-card rv-card" style="--cd:0.08s">
          <div class="bc-label">AI Curriculum</div>
          <h3>나만을 위한<br>학습 경로</h3>
          <p>분석 기반 맞춤 커리큘럼</p>
          <div class="node-viz">
            <svg width="100%" height="90" viewBox="0 0 180 90" fill="none">
              <line x1="40" y1="45" x2="90" y2="25" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <line x1="40" y1="45" x2="90" y2="65" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <line x1="90" y1="25" x2="140" y2="15" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <line x1="90" y1="25" x2="140" y2="45" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <line x1="90" y1="65" x2="140" y2="45" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <line x1="90" y1="65" x2="140" y2="75" stroke="currentColor" stroke-width="0.5" opacity="0.3"/>
              <circle cx="40" cy="45" r="6" fill="currentColor" opacity="0.8"/>
              <circle cx="90" cy="25" r="5" fill="currentColor" opacity="0.5"/>
              <circle cx="90" cy="65" r="5" fill="currentColor" opacity="0.5"/>
              <circle cx="140" cy="15" r="4" fill="currentColor" opacity="0.3"/>
              <circle cx="140" cy="45" r="4" fill="currentColor" opacity="0.3"/>
              <circle cx="140" cy="75" r="4" fill="currentColor" opacity="0.3"/>
            </svg>
          </div>
          <div class="card-shimmer" />
        </div>

        <!-- Growth tall card -->
        <div class="bento-card bc-tall rv-card" style="--cd:0.16s">
          <div class="bc-label">Growth Track</div>
          <h3>성장을<br>눈으로<br>확인하세요</h3>
          <p>히스토리와 성장 일지</p>
          <div class="growth-chart">
            <div class="gc-bar" v-for="(h,i) in growthH" :key="i"
              :style="{height: h+'%', '--cd': i*0.04+'s'}" />
          </div>
          <div class="card-shimmer" />
        </div>

        <!-- Calendar card -->
        <div class="bento-card rv-card" style="--cd:0.1s">
          <div class="bc-label">Calendar</div>
          <h3>학습 일정<br>자동 등록</h3>
          <p>Google Calendar 연동</p>
          <div class="mini-cal">
            <div class="mc-row" v-for="r in 4" :key="r">
              <div class="mc-cell" v-for="(c,ci) in 7" :key="ci"
                :class="{on: [1,3,5].includes(ci)&&r<3 || [0,2,4,6].includes(ci)&&r===3}" />
            </div>
          </div>
          <div class="card-shimmer" />
        </div>

        <!-- Velog wide card -->
        <div class="bento-card bc-wide rv-card" style="--cd:0.18s">
          <div class="bc-label">Velog Sync</div>
          <h3>블로그 활동도<br>자동으로 분석</h3>
          <p>Velog 포스팅을 분석해 기술 역량 지도를 업데이트합니다.</p>
          <div class="tag-cloud">
            <span v-for="t in velogTags" :key="t">{{ t }}</span>
          </div>
          <div class="card-shimmer" />
        </div>
      </div>
    </section>

    <!-- ══════════ CTA ══════════ -->
    <section class="s-cta" id="cta">
      <div class="cta-orb cta-orb-1" />
      <div class="cta-orb cta-orb-2" />
      <div class="cta-inner rv-cta">
        <!-- 피크 엔드 룰: 마지막 인상이 전체 경험을 결정 -->
        <p class="cta-tag">지금 시작하지 않으면?</p>
        <h2 class="cta-title">오늘도 방향 없이<br><span class="cta-em">시간을 쓰고 있습니다.</span></h2>
        <p class="cta-sub">KAIROS는 당신의 GitHub, Velog, 캘린더를 연결해<br>지금 당장 해야 할 학습을 알려드립니다.</p>
        <!-- 사회적 증거 -->
        <div class="cta-social rv-fade">
          <div class="cs-item"><span class="cs-val">6+</span><span class="cs-lbl">연동된 개발자</span></div>
          <div class="cs-div"></div>
          <div class="cs-item"><span class="cs-val">100+</span><span class="cs-lbl">분석된 커밋</span></div>
          <div class="cs-div"></div>
          <div class="cs-item"><span class="cs-val">4.8 / 5</span><span class="cs-lbl">사용자 만족도</span></div>
        </div>
        <button class="btn-cta-lg"
          @click="$router.push('/onboarding/connect')">
          <span class="bcl-text">지금 성장 시작하기</span>
          <span class="bcl-arr">↗</span>
          <div class="bcl-fill" />
        </button>
        <!-- 손실 회피: 최종 불안 해소 -->
        <p class="cta-reassure">5분이면 연동 완료 · 언제든 탈퇴 가능 · 완전 무료</p>
      </div>
    </section>

    <!-- ══════════ FOOTER ══════════ -->
    <footer class="ft">
      <div class="ft-row">
        <div class="ft-logo" @click="goTop">
          <svg width="14" height="14" viewBox="0 0 28 28" fill="none">
            <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z"
              stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
            <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
          </svg>
          KAIROS
        </div>
        <div class="ft-links">
          <span v-for="s in nav" :key="s.id" @click="goto(s.id)">{{ s.label }}</span>
        </div>
      </div>
      <div class="ft-bottom">
        <span>© 2026 KAIROS · SSAFY 14기 A506</span>
        <span>All rights reserved.</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

/* ── refs ── */
const rootEl = ref(null)
const cvs    = ref(null)
const scrolled = ref(false)
const heroIn   = ref(false)
const activeId = ref('')

/* ── data ── */
const nav = [
  { id: 'features',    label: 'Features'    },
  { id: 'workflow',    label: 'Workflow'     },
  { id: 'integration', label: 'Integration' },
  { id: 'bento',       label: 'Platform'    },
  { id: 'cta',         label: 'Join'        },
]
const tickers  = ['AI CURATION ✦', 'GITHUB SYNC ✦', 'NODE CALENDAR ✦', 'CAREER TRACKING ✦', 'VELOG SYNC ✦', 'AI MENTORING ✦', 'QUIZ GENERATION ✦', 'GROWTH ANALYTICS ✦']
const tickers2 = ['COMMIT HISTORY ✦', 'TECH STACK ANALYSIS ✦', 'LEARNING ROADMAP ✦', 'CALENDAR SYNC ✦', 'PERSONALIZED AI ✦', 'DAILY RECOMMEND ✦']

const feats = [
  { ico:`<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`, t:'캘린더 기반 학습 관리', d:'학습 일정을 노드 캘린더에서 관리하고 흐름을 한눈에 파악합니다.' },
  { ico:`<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>`, t:'AI 맞춤 추천', d:'최근 활동을 분석해 지금 이어서 하기 좋은 학습을 실시간 제안합니다.' },
  { ico:`<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/></svg>`, t:'외부 서비스 완전 연동', d:'GitHub, Velog, Google Calendar를 자동 수집합니다.' },
  { ico:`<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>`, t:'성장 추적 & 분석', d:'학습 히스토리와 기술 역량 지도로 변화를 기록하고 확인합니다.' },
]
const steps = [
  { t:'연동',        d:'GitHub, Velog, Google Calendar를 연결합니다.' },
  { t:'수집 & 분석', d:'AI가 활동 데이터와 기술 맥락을 자동 분석합니다.' },
  { t:'커리큘럼 추천', d:'맞춤형 학습 경로와 다음 액션을 제안합니다.' },
  { t:'캘린더 반영', d:'추천 학습이 노드 캘린더에 자동 등록됩니다.' },
  { t:'성장 추적',   d:'학습 기록, 퀴즈, 성장 일지로 변화를 확인합니다.' },
]
const stats = [
  { v:'100%', l:'DATA SYNC',    d:'GitHub / Velog 자동 연동' },
  { v:'ZERO', l:'MANUAL INPUT', d:'손으로 입력할 필요 없음' },
  { v:'24/7', l:'AI COACH',     d:'언제나 곁에 있는 AI 코치' },
]
const commitH = [30,55,40,70,50,85,60,75,45,90,65,80]
const onboardSteps = [
  { t:'계정 연동', d:'GitHub + Velog 연결. 5분이면 완료.' },
  { t:'AI 분석', d:'활동 데이터를 자동으로 분석합니다.' },
  { t:'커리큘럼 생성', d:'나만을 위한 학습 경로가 만들어집니다.' },
  { t:'성장 추적', d:'캘린더에 반영하고 기록을 쌓아갑니다.' },
]
const previewTimeline = [
  'React 상태관리 복습 ✓',
  'TypeScript 제네릭 심화 ✓',
  'React Server Components →',
  'Next.js App Router',
  'TanStack Query 실전',
]
const growthH = [20,35,28,55,42,70,60,85,72,90,78,95]
const velogTags = ['React','TypeScript','Spring Boot','Docker','AWS','Redis','Next.js','Kubernetes']

/* 커서: App.vue 전역 처리 */

/* ── scroll ── */
function onScroll(){
  if(!rootEl.value) return
  scrolled.value = rootEl.value.scrollTop > 60
  const ids=['features','workflow','integration','bento','cta']
  let cur=''
  for(const id of ids){
    const el=document.getElementById(id)
    if(el && el.getBoundingClientRect().top < 200) cur=id
  }
  activeId.value=cur
}
function goto(id){
  const el=document.getElementById(id)
  if(el&&rootEl.value) rootEl.value.scrollTo({top:el.offsetTop-68,behavior:'smooth'})
}
function goTop(){ if(rootEl.value) rootEl.value.scrollTo({top:0,behavior:'smooth'}) }

/* ── scroll reveal ── */
let rvObs=null
function setupReveal(){
  const opts={ threshold:0.1, rootMargin:'0px 0px -40px 0px' }
  rvObs=new IntersectionObserver(entries=>{
    entries.forEach(e=>{ if(e.isIntersecting) e.target.classList.add('in') })
  }, opts)
  document.querySelectorAll('.rv-fade,.rv-slide,.rv-card,.rv-wf,.rv-scale,.rv-cta').forEach(el=>rvObs.observe(el))
}

/* ── Three.js particle mesh ── */
let renderer, scene, camera, pts, lns, threeRaf=null
const m3=new THREE.Vector2()
window.addEventListener('mousemove',e=>{
  m3.x=(e.clientX/window.innerWidth)*2-1
  m3.y=-(e.clientY/window.innerHeight)*2+1
})

function initThree(){
  const canvas=cvs.value; if(!canvas) return
  renderer=new THREE.WebGLRenderer({canvas,antialias:true,alpha:true})
  renderer.setPixelRatio(Math.min(window.devicePixelRatio,2))
  renderer.setSize(window.innerWidth,window.innerHeight)
  renderer.setClearColor(0x000000,0)
  scene=new THREE.Scene()
  camera=new THREE.PerspectiveCamera(55,window.innerWidth/window.innerHeight,0.1,1000)
  camera.position.z=6

  const N=260, pd=[], pos=new Float32Array(N*3)
  for(let i=0;i<N;i++){
    const x=(Math.random()-.5)*20, y=(Math.random()-.5)*12, z=(Math.random()-.5)*8
    pos[i*3]=x; pos[i*3+1]=y; pos[i*3+2]=z
    pd.push({x,y,z,vx:(Math.random()-.5)*.003,vy:(Math.random()-.5)*.003})
  }
  const pGeo=new THREE.BufferGeometry()
  pGeo.setAttribute('position',new THREE.BufferAttribute(pos,3))

  // 파티클 색 — CSS 변수에서 읽어서 동기화
  const isDark=document.documentElement.classList.contains('theme-dark')||
    document.body.classList.contains('is-dark')
  const ptColor = isDark ? 0xffffff : 0x0a0a0a
  const lineColor = isDark ? 0xffffff : 0x0a0a0a

  pts=new THREE.Points(pGeo,new THREE.PointsMaterial({color:ptColor,size:0.02,transparent:true,opacity:0.45}))
  scene.add(pts)

  const lp=[]
  for(let i=0;i<N;i++) for(let j=i+1;j<N;j++){
    const d=Math.hypot(pd[i].x-pd[j].x,pd[i].y-pd[j].y,pd[i].z-pd[j].z)
    if(d<2.8){lp.push(pd[i].x,pd[i].y,pd[i].z,pd[j].x,pd[j].y,pd[j].z)}
  }
  const lGeo=new THREE.BufferGeometry()
  lGeo.setAttribute('position',new THREE.BufferAttribute(new Float32Array(lp),3))
  lns=new THREE.LineSegments(lGeo,new THREE.LineBasicMaterial({color:lineColor,transparent:true,opacity:0.06}))
  scene.add(lns)

  let t=0
  function tick(){
    threeRaf=requestAnimationFrame(tick); t+=0.004
    const p=pts.geometry.attributes.position
    for(let i=0;i<N;i++){
      pd[i].x+=pd[i].vx+Math.sin(t+i*.5)*.0008
      pd[i].y+=pd[i].vy+Math.cos(t+i*.4)*.0008
      if(Math.abs(pd[i].x)>10) pd[i].vx*=-1
      if(Math.abs(pd[i].y)>6)  pd[i].vy*=-1
      p.setXYZ(i,pd[i].x,pd[i].y,pd[i].z)
    }
    p.needsUpdate=true
    pts.rotation.x+=(m3.y*.1-pts.rotation.x)*.04
    pts.rotation.y+=(m3.x*.1-pts.rotation.y)*.04
    lns.rotation.copy(pts.rotation)
    renderer.render(scene,camera)
  }
  tick()
  window.addEventListener('resize',onResize)
}
function onResize(){
  if(!camera||!renderer) return
  camera.aspect=window.innerWidth/window.innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(window.innerWidth,window.innerHeight)
}

onMounted(()=>{
  initThree(); setupReveal()
  if(rootEl.value) rootEl.value.addEventListener('scroll',onScroll,{passive:true})
  setTimeout(()=>{ heroIn.value=true }, 80)
})
onUnmounted(()=>{
  cancelAnimationFrame(threeRaf)
  if(rvObs) rvObs.disconnect()
  window.removeEventListener('resize',onResize)
  if(rootEl.value) rootEl.value.removeEventListener('scroll',onScroll)
  if(renderer) renderer.dispose()
})
</script>

<style scoped>
/* ════════════════════════════════
   BASE — 모노그램 CSS 변수 연동
════════════════════════════════ */
.land {
  background: var(--bg-base);
  color: var(--text-primary);
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  font-family: 'Escoredream', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  position: relative;
  user-select: none;
  -webkit-user-select: none;
}
.land::-webkit-scrollbar { display: none; }
.land { scrollbar-width: none; }

.gl-canvas {
  position: fixed; inset: 0;
  width: 100%; height: 100%;
  pointer-events: none; z-index: 0;
}

/* 커서: App.vue 전역 처리 */

/* ════════════════════════════════
   HEADER
════════════════════════════════ */
.hd-fixed-top {
  position: fixed; top: 0; left: 0;
  width: 100%; height: 68px;
  padding: 0 40px;
  display: flex; align-items: center; justify-content: space-between;
  z-index: 700;
  pointer-events: none;
}
.hd-fixed-top .hd-logo,
.hd-fixed-top .hd-right {
  pointer-events: auto;
}

.hd-sticky {
  position: sticky; top: 0; left: 0;
  width: 100%; height: 68px;
  display: flex; align-items: center; justify-content: center;
  z-index: 600;
  border-bottom: 1px solid transparent;
  margin-bottom: 60px; /* Space above ticker-wrap */
  transition: background .4s, border-color .4s, backdrop-filter .4s;
}
.hd-sticky.solid {
  background: color-mix(in srgb, var(--bg-base) 85%, transparent);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-bottom-color: var(--border);
}
.hd-logo {
  display: flex; align-items: center; gap: 9px;
  font-size: 14px; font-weight: 900; letter-spacing: .18em;
  cursor: pointer; color: var(--text-primary);
  transition: opacity .25s;
}
.hd-logo:hover { opacity: .6; }
.hd-nav {
  display: flex; gap: 36px;
}
.hd-nav span {
  font-size: 11px; font-weight: 700; letter-spacing: .14em;
  color: var(--text-muted); cursor: pointer;
  text-transform: uppercase; position: relative;
  transition: color .25s;
}
.hd-nav span::after {
  content: ''; position: absolute; bottom: -3px; left: 0;
  width: 0; height: 1px; background: var(--text-primary);
  transition: width .3s cubic-bezier(.16,1,.3,1);
}
.hd-nav span:hover, .hd-nav span.act { color: var(--text-primary); }
.hd-nav span:hover::after, .hd-nav span.act::after { width: 100%; }
.hd-right { display: flex; gap: 10px; }

/* 버튼 공통 */
.btn-outline {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 12px; font-weight: 700;
  padding: 8px 18px; letter-spacing: .06em;
  font-family: inherit; cursor: pointer;
  transition: all .25s;
}
.btn-outline:hover { border-color: var(--border-mid); color: var(--text-primary); }

.btn-solid {
  background: var(--text-primary); color: var(--bg-base);
  border: none; font-size: 12px; font-weight: 800;
  padding: 9px 20px; letter-spacing: .06em;
  font-family: inherit; cursor: pointer;
  transition: opacity .25s, transform .25s;
}
.btn-solid:hover { opacity: .85; transform: translateY(-1px); }
.btn-solid.lg { font-size: 13px; padding: 14px 32px; letter-spacing: .08em; }

.btn-ghost-text {
  background: transparent; border: none;
  color: var(--text-muted); font-size: 13px; font-weight: 700;
  letter-spacing: .06em; font-family: inherit; cursor: pointer;
  transition: color .25s;
}
.btn-ghost-text:hover { color: var(--text-primary); }

@media (max-width: 768px) { .hd-nav { display: none; } .hd { padding: 0 20px; } }

/* ════════════════════════════════
   LIQUID GLASS HELPERS
════════════════════════════════ */
/* orb — 큰 블러 원형 빛 */
.orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(80px);
  opacity: .12;
}
/* 라이트 모드에서는 어두운 색, 다크 모드에서는 흰색 */
:root .orb { background: var(--text-primary); }

/* card shimmer (liquid glass 광택) */
.card-shimmer {
  position: absolute; inset: 0;
  background: linear-gradient(
    135deg,
    transparent 0%,
    color-mix(in srgb, var(--text-primary) 3%, transparent) 50%,
    transparent 100%
  );
  opacity: 0;
  transition: opacity .5s, transform .5s;
  transform: translateX(-100%) rotate(10deg);
  pointer-events: none;
}
.feat-card:hover .card-shimmer,
.bento-card:hover .card-shimmer { opacity: 1; transform: translateX(100%) rotate(10deg); }

/* glass card (hero float) */
.glass-card {
  position: absolute;
  background: color-mix(in srgb, var(--bg-surface) 60%, transparent);
  border: 1px solid var(--border);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 14px 20px;
  display: flex; flex-direction: column; gap: 4px;
}
.gc-val { font-size: 24px; font-weight: 900; letter-spacing: -.03em; line-height: 1; }
.gc-label { font-size: 9px; font-weight: 700; letter-spacing: .15em; color: var(--text-muted); }

/* ════════════════════════════════
   HERO
════════════════════════════════ */
.s-hero {
  position: relative; z-index: 2;
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  padding: 60px 40px 100px; overflow: hidden;
}
.orb-1 { width: 600px; height: 600px; top: -100px; left: -100px; animation: orbFloat 8s ease-in-out infinite; }
.orb-2 { width: 500px; height: 500px; bottom: -80px; right: -80px; animation: orbFloat 10s ease-in-out infinite reverse; }
.orb-3 { width: 300px; height: 300px; top: 50%; left: 50%; transform: translate(-50%,-50%); animation: orbPulse 6s ease-in-out infinite; }
@keyframes orbFloat { 0%,100% { transform: translate(0,0) } 50% { transform: translate(30px,-20px) } }
@keyframes orbPulse { 0%,100% { opacity:.08 } 50% { opacity:.16 } }

.hero-inner { text-align: center; max-width: 880px; position: relative; z-index: 1; }

/* slide-up 기본 */
.slide-up {
  opacity: 0; transform: translateY(32px);
  transition: opacity .8s cubic-bezier(.16,1,.3,1), transform .8s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--sd, 0s);
}
.hero-inner.in .slide-up,
.glass-card.in { opacity: 1; transform: none; }
.glass-card { opacity: 0; transform: translateY(20px); transition: opacity .7s, transform .7s; transition-delay: var(--sd,0s); }

.hero-tag {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 10px; font-weight: 900; letter-spacing: .24em;
  color: var(--text-muted); margin-bottom: 28px;
  border: 1px solid var(--border);
  padding: 6px 14px;
  background: color-mix(in srgb, var(--bg-surface) 50%, transparent);
  backdrop-filter: blur(10px);
}
.tag-dot {
  width: 5px; height: 5px; border-radius: 50%;
  background: var(--text-primary);
  animation: blink 2s ease-in-out infinite;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:.3} }

.hero-title {
  display: flex; flex-direction: column; align-items: center;
  font-size: clamp(52px, 9vw, 118px);
  font-weight: 900; letter-spacing: -.04em; line-height: .92;
  margin: 0 0 16px;
}
.hero-title .line { display: block; }
.outline-text {
  color: transparent;
  -webkit-text-stroke: 1.5px var(--border-mid);
}

.hero-sub {
  font-size: 16px; line-height: 1.85;
  color: var(--text-muted); margin-bottom: 32px;
}
.hero-cta { display: flex; gap: 14px; justify-content: center; }

/* glass stat positions */
.gc-1 { bottom: 26%; left: 5%; }
.gc-2 { top: 30%; right: 5%; }
.gc-3 { bottom: 28%; right: 8%; }
@media (max-width: 768px) { .glass-card { display: none; } }

.scroll-hint {
  position: absolute; bottom: 20px; left: 50%;
  transform: translateX(-50%);
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  color: var(--text-faint); font-size: 9px; font-weight: 900; letter-spacing: .2em;
  opacity: 0; transition: opacity .7s 1.2s;
}
.scroll-hint.in { opacity: 1; }
.sh-line { width: 1px; height: 40px; background: var(--border); overflow: hidden; position: relative; }
.sh-runner {
  width: 100%; height: 50%; background: var(--text-primary);
  animation: shrun 1.6s ease-in-out infinite;
}
@keyframes shrun { 0%{transform:translateY(-100%)} 100%{transform:translateY(200%)} }

/* ════════════════════════════════
   TICKER
════════════════════════════════ */
.ticker-wrap {
  position: relative; z-index: 2;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  overflow: hidden; padding: 16px 0;
  background: color-mix(in srgb, var(--bg-surface) 40%, transparent);
}
.ticker-track { display: flex; }
.ticker-row {
  display: flex; flex-shrink: 0;
  animation: tickL 26s linear infinite;
}
.ticker-row-rev { animation: tickR 26s linear infinite; }
.ticker-row span {
  font-size: 10px; font-weight: 900; letter-spacing: .22em;
  color: var(--text-faint); padding: 0 32px; white-space: nowrap;
}
@keyframes tickL { from{transform:translateX(0)} to{transform:translateX(-100%)} }
@keyframes tickR { from{transform:translateX(-100%)} to{transform:translateX(0)} }

/* ════════════════════════════════
   SECTION COMMON
════════════════════════════════ */
.sec {
  position: relative; z-index: 2;
  padding: 120px 80px;
}
.sec-alt {
  background: color-mix(in srgb, var(--bg-elevated) 50%, transparent);
}
.sec-label {
  font-size: 10px; font-weight: 900; letter-spacing: .22em;
  color: var(--text-faint); display: block; margin-bottom: 18px;
}
.sec-title {
  font-size: clamp(34px, 5vw, 64px);
  font-weight: 900; letter-spacing: -.03em; line-height: 1.08;
  margin: 0 0 72px;
}
.sec-title em { font-style: italic; color: var(--text-muted); }

@media (max-width: 768px) { .sec { padding: 80px 24px; } }

/* ── SCROLL REVEAL STATES ── */
.rv-fade { opacity: 0; transition: opacity .8s cubic-bezier(.16,1,.3,1) var(--cd,0s); }
.rv-fade.in { opacity: 1; }

.rv-slide {
  opacity: 0; transform: translateY(32px);
  transition: opacity .85s cubic-bezier(.16,1,.3,1),
              transform .85s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--cd, 0.05s);
}
.rv-slide.in { opacity: 1; transform: none; }

.rv-card {
  opacity: 0; transform: translateY(28px) scale(.98);
  transition: opacity .8s cubic-bezier(.16,1,.3,1),
              transform .8s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--cd, 0s);
}
.rv-card.in { opacity: 1; transform: none; }

.rv-wf {
  opacity: 0; transform: translateX(-24px);
  transition: opacity .75s cubic-bezier(.16,1,.3,1),
              transform .75s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--cd, 0s);
}
.rv-wf.in { opacity: 1; transform: none; }

.rv-scale {
  opacity: 0; transform: scale(.9);
  transition: opacity .8s cubic-bezier(.16,1,.3,1),
              transform .8s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--cd, 0s);
}
.rv-scale.in { opacity: 1; transform: none; }

.rv-cta {
  opacity: 0; transform: translateY(40px);
  transition: opacity 1s cubic-bezier(.16,1,.3,1),
              transform 1s cubic-bezier(.16,1,.3,1);
}
.rv-cta.in { opacity: 1; transform: none; }

/* ════════════════════════════════
   FEATURES
════════════════════════════════ */
.feat-grid {
  display: grid; grid-template-columns: repeat(2, 1fr);
  gap: 1px; background: var(--border);
}
.feat-card {
  background: var(--bg-surface);
  padding: 48px 40px; position: relative; overflow: hidden;
  cursor: default;
  transition: background .4s;
}
.feat-card:hover { background: var(--bg-elevated); }
.fc-top { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; }
.fc-num { font-size: 11px; font-weight: 900; letter-spacing: .15em; color: var(--text-faint); }
.fc-icon { color: var(--text-muted); transition: color .3s; }
.feat-card:hover .fc-icon { color: var(--text-primary); }
.feat-card h3 {
  font-size: 20px; font-weight: 800; margin: 0 0 12px;
  letter-spacing: -.01em;
  transition: letter-spacing .4s;
}
.feat-card:hover h3 { letter-spacing: .01em; }
.feat-card p { font-size: 14px; color: var(--text-muted); line-height: 1.75; margin: 0; }
.fc-footer {
  position: absolute; bottom: 32px; right: 36px;
  font-size: 18px; color: var(--border-mid);
  transition: color .3s, transform .35s cubic-bezier(.16,1,.3,1);
}
.feat-card:hover .fc-footer { color: var(--text-primary); transform: translate(4px,-4px); }
@media (max-width: 768px) { .feat-grid { grid-template-columns: 1fr; } .feat-card { padding: 36px 24px; } }

/* ════════════════════════════════
   WORKFLOW
════════════════════════════════ */
.wf-list { border-top: 1px solid var(--border); }
.wf-item {
  display: flex; align-items: center; gap: 44px;
  padding: 44px 0; border-bottom: 1px solid var(--border);
  cursor: default; position: relative; overflow: hidden;
  transition: padding-left .4s cubic-bezier(.16,1,.3,1);
}
.wf-bar {
  position: absolute; left: 0; top: 0;
  width: 2px; height: 0; background: var(--text-primary);
  transition: height .4s cubic-bezier(.16,1,.3,1);
}
.wf-item:hover .wf-bar { height: 100%; }
.wf-item:hover { padding-left: 14px; }
.wf-num {
  font-size: 48px; font-weight: 900; letter-spacing: -.04em;
  color: var(--border); flex-shrink: 0; line-height: 1;
  transition: color .4s;
}
.wf-item:hover .wf-num { color: var(--border-mid); }
.wf-body { flex: 1; }
.wf-body h3 { font-size: 23px; font-weight: 800; margin: 0 0 8px; transition: letter-spacing .4s; }
.wf-item:hover .wf-body h3 { letter-spacing: .015em; }
.wf-body p { font-size: 14px; color: var(--text-muted); margin: 0; line-height: 1.65; }
.wf-arrow {
  font-size: 18px; color: var(--border); flex-shrink: 0;
  transition: color .3s, transform .4s cubic-bezier(.16,1,.3,1);
}
.wf-item:hover .wf-arrow { color: var(--text-primary); transform: translateX(8px); }
@media (max-width: 768px) { .wf-item { gap: 20px; padding: 32px 0; } .wf-num { font-size: 32px; } .wf-arrow { display: none; } }

/* ════════════════════════════════
   LIQUID GLASS STATS
════════════════════════════════ */
.stats-wrap {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.stat-glass {
  position: relative; overflow: hidden;
  background: color-mix(in srgb, var(--bg-surface) 55%, transparent);
  border: 1px solid var(--border);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  padding: 64px 44px; text-align: center;
  cursor: default;
  transition: transform .4s cubic-bezier(.16,1,.3,1),
              border-color .3s,
              background .3s;
}
.stat-glass:hover {
  transform: translateY(-6px);
  border-color: var(--border-mid);
  background: color-mix(in srgb, var(--bg-elevated) 60%, transparent);
}
.sg-glow {
  position: absolute; width: 150px; height: 150px;
  border-radius: 50%; filter: blur(40px);
  background: var(--text-primary); opacity: 0;
  bottom: -40px; right: -40px;
  transition: opacity .4s;
}
.stat-glass:hover .sg-glow { opacity: .06; }
.sg-val {
  font-size: clamp(40px, 6vw, 72px); font-weight: 900;
  letter-spacing: -.04em; line-height: 1; margin-bottom: 14px;
}
.sg-label {
  font-size: 10px; font-weight: 900; letter-spacing: .2em;
  color: var(--text-faint); margin-bottom: 10px;
}
.sg-desc { font-size: 13px; color: var(--text-muted); line-height: 1.5; }
@media (max-width: 768px) { .stats-wrap { grid-template-columns: 1fr; gap: 12px; } .stat-glass { padding: 48px 24px; } }

/* ════════════════════════════════
   BENTO GRID
════════════════════════════════ */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: auto auto;
  gap: 1px; background: var(--border);
}
.bento-card {
  background: var(--bg-surface);
  padding: 40px; position: relative; overflow: hidden;
  cursor: default; transition: background .4s;
}
.bento-card:hover { background: var(--bg-elevated); }
.bc-wide { grid-column: span 2; }
.bc-tall { grid-row: span 2; }
.bc-label {
  font-size: 10px; font-weight: 900; letter-spacing: .18em;
  color: var(--text-faint); margin-bottom: 16px;
}
.bento-card h3 {
  font-size: 22px; font-weight: 800; line-height: 1.25;
  margin: 0 0 10px; letter-spacing: -.01em;
}
.bento-card p {
  font-size: 13px; color: var(--text-muted);
  line-height: 1.65; margin: 0 0 24px;
}

/* commit bars */
.commit-bars {
  display: flex; align-items: flex-end; gap: 4px;
  height: 60px; margin-top: auto;
}
.cb {
  flex: 1; background: var(--border-mid);
  border-radius: 2px;
  animation: cbup .7s var(--cd) both cubic-bezier(.16,1,.3,1);
  transition: background .3s;
}
.bento-card:hover .cb { background: var(--text-primary); }
@keyframes cbup { from{transform:scaleY(0);transform-origin:bottom} to{transform:scaleY(1);transform-origin:bottom} }

/* node viz */
.node-viz { margin-top: auto; color: var(--text-primary); }

/* growth chart */
.growth-chart {
  display: flex; align-items: flex-end; gap: 5px;
  height: 90px; margin-top: auto; padding-top: 16px;
}
.gc-bar {
  flex: 1; background: var(--border-mid);
  border-radius: 2px;
  animation: cbup .8s var(--cd) both cubic-bezier(.16,1,.3,1);
  transition: background .3s;
}
.bento-card:hover .gc-bar { background: var(--text-primary); }

/* mini calendar */
.mini-cal { display: flex; flex-direction: column; gap: 5px; margin-top: auto; }
.mc-row { display: flex; gap: 5px; }
.mc-cell {
  width: 13px; height: 13px; border-radius: 2px;
  background: var(--border); transition: background .3s;
}
.mc-cell.on { background: var(--text-primary); opacity: .6; }
.bento-card:hover .mc-cell.on { opacity: 1; }

/* tag cloud */
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; margin-top: auto; }
.tag-cloud span {
  font-size: 11px; font-weight: 700; letter-spacing: .06em;
  padding: 5px 12px;
  border: 1px solid var(--border);
  color: var(--text-muted);
  transition: all .25s;
  background: color-mix(in srgb, var(--bg-surface) 60%, transparent);
  backdrop-filter: blur(8px);
}
.bento-card:hover .tag-cloud span {
  border-color: var(--border-mid);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .bento-grid { grid-template-columns: 1fr; }
  .bc-wide, .bc-tall { grid-column: span 1; grid-row: span 1; }
  .bento-card { padding: 28px; }
}

/* ════════════════════════════════
   CTA
════════════════════════════════ */
.s-cta {
  position: relative; z-index: 2;
  padding: 160px 40px; text-align: center; overflow: hidden;
}
.cta-orb {
  position: absolute; border-radius: 50%;
  filter: blur(80px); opacity: .1;
  background: var(--text-primary);
  pointer-events: none;
}
.cta-orb-1 { width: 500px; height: 500px; bottom: -200px; left: 50%; transform: translateX(-50%); }
.cta-orb-2 { width: 300px; height: 300px; top: 0; right: 10%; animation: orbFloat 9s ease-in-out infinite; }

.cta-inner { position: relative; z-index: 1; }
.cta-tag {
  font-size: 10px; font-weight: 900; letter-spacing: .3em;
  color: var(--text-faint); margin-bottom: 20px;
}
.cta-title {
  font-size: clamp(48px, 9vw, 110px); font-weight: 900;
  letter-spacing: -.04em; line-height: .95; margin: 0 0 24px;
}
.cta-sub {
  font-size: 17px; color: var(--text-muted);
  margin-bottom: 52px; line-height: 1.7;
}

/* liquid glass CTA button */
.btn-cta-lg {
  display: inline-flex; padding: 0;
  background: color-mix(in srgb, var(--bg-elevated) 50%, transparent);
  border: 1px solid var(--border-mid);
  color: var(--text-primary);
  font-family: inherit; cursor: pointer; overflow: hidden;
  position: relative;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: border-color .4s, box-shadow .4s;
}
.btn-cta-lg:hover {
  border-color: var(--text-primary);
  box-shadow: 0 0 40px color-mix(in srgb, var(--text-primary) 10%, transparent);
}
.bcl-text, .bcl-arr {
  position: relative; z-index: 1;
  transition: color .4s;
}
.bcl-text {
  display: block; padding: 20px 40px;
  font-size: 13px; font-weight: 800; letter-spacing: .1em;
}
.bcl-arr {
  display: flex; align-items: center;
  padding-right: 28px; font-size: 18px;
  transition: transform .4s;
}
.btn-cta-lg:hover .bcl-arr { transform: translate(4px,-4px); }
.bcl-fill {
  position: absolute; inset: 0;
  background: var(--text-primary);
  transform: translateY(102%);
  transition: transform .45s cubic-bezier(.16,1,.3,1);
}
.btn-cta-lg:hover .bcl-fill { transform: translateY(0); }
.btn-cta-lg:hover .bcl-text,
.btn-cta-lg:hover .bcl-arr { color: var(--bg-base); }

/* ════════════════════════════════
   FOOTER
════════════════════════════════ */
.ft {
  position: relative; z-index: 2;
  border-top: 1px solid var(--border);
  padding: 48px 80px 32px;
  background: color-mix(in srgb, var(--bg-surface) 60%, transparent);
  backdrop-filter: blur(12px);
}
.ft-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 48px;
}
.ft-logo {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 900; letter-spacing: .18em;
  cursor: pointer; color: var(--text-primary); opacity: .55;
  transition: opacity .3s;
}
.ft-logo:hover { opacity: 1; }
.ft-links { display: flex; gap: 28px; }
.ft-links span {
  font-size: 11px; font-weight: 700; letter-spacing: .12em;
  color: var(--text-faint); cursor: pointer;
  transition: color .25s;
}
.ft-links span:hover { color: var(--text-primary); }
.ft-bottom {
  display: flex; justify-content: space-between;
  font-size: 10px; font-weight: 600; letter-spacing: .06em;
  color: var(--text-faint);
  border-top: 1px solid var(--border); padding-top: 24px;
}
@media (max-width: 768px) {
  .ft { padding: 36px 24px 28px; }
  .ft-row { flex-direction: column; gap: 20px; align-items: flex-start; }
  .ft-bottom { flex-direction: column; gap: 6px; }
}

/* ════════════════════════════════
   UX 심리학 원칙 추가 요소
════════════════════════════════ */

/* ① 손실 회피 — Hero 신뢰 문구 */
.hero-trust {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; flex-wrap: wrap;
  margin-top: 14px;
  opacity: 0; transform: translateY(12px);
  transition: opacity .7s cubic-bezier(.16,1,.3,1),
              transform .7s cubic-bezier(.16,1,.3,1);
  transition-delay: var(--sd, 0s);
}
.hero-inner.in .hero-trust { opacity: 1; transform: none; }
.trust-item {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 700; letter-spacing: .04em;
  color: var(--text-muted);
}
.trust-check {
  color: var(--text-primary); font-weight: 900; font-size: 12px;
}
.trust-sep { color: var(--border-mid); font-size: 11px; }

/* ② 자이가르닉 효과 — 온보딩 4단계 미리보기 */
.sec-steps {}
.steps-preview {
  display: flex; align-items: stretch;
  border: 1px solid var(--border);
  overflow: hidden;
}
.sp-item {
  flex: 1; padding: 36px 28px;
  display: flex; flex-direction: column; gap: 12px;
  border-right: 1px solid var(--border);
  position: relative; cursor: default;
  transition: background .35s;
}
.sp-item:last-child { border-right: none; }
.sp-item:hover { background: var(--bg-elevated); }
.sp-arrow {
  position: absolute; right: -12px; top: 50%;
  transform: translateY(-50%);
  font-size: 14px; color: var(--border-mid);
  z-index: 2; background: var(--bg-base);
  padding: 2px 0;
}
.sp-num {
  font-size: 28px; font-weight: 900; letter-spacing: -.03em;
  color: var(--border); line-height: 1;
  transition: color .3s;
}
.sp-item:hover .sp-num { color: var(--border-mid); }
.sp-title { font-size: 15px; font-weight: 800; letter-spacing: -.01em; }
.sp-desc { font-size: 12px; color: var(--text-muted); line-height: 1.6; font-weight: 600; }
@media (max-width: 768px) {
  .steps-preview { flex-direction: column; }
  .sp-item { border-right: none; border-bottom: 1px solid var(--border); }
  .sp-item:last-child { border-bottom: none; }
  .sp-arrow { display: none; }
}

/* ③ 현재 편향 — 즉각 가치 프리뷰 */
.sec-preview {}
.preview-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 1px; background: var(--border);
}
.pv-card {
  background: var(--bg-surface);
  padding: 36px 32px; display: flex; flex-direction: column; gap: 20px;
  cursor: default; transition: background .4s;
  position: relative; overflow: hidden;
}
.pv-card:hover { background: var(--bg-elevated); }
.pv-label {
  font-size: 10px; font-weight: 900; letter-spacing: .18em;
  color: var(--text-faint);
}
/* GitHub bar mock */
.pv-mock { display: flex; flex-direction: column; gap: 10px; margin-top: auto; }
.pv-bar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 10px; font-size: 12px; font-weight: 700; color: var(--text-secondary);
  position: relative;
}
.pv-bar::after {
  content: '';
  position: absolute; bottom: -4px; left: 0;
  width: var(--w); height: 2px;
  background: var(--text-primary); opacity: .3;
  border-radius: 1px;
}
/* Timeline mock */
.pv-timeline { display: flex; flex-direction: column; gap: 10px; margin-top: auto; }
.pt-item { display: flex; align-items: center; gap: 10px; }
.pt-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 1px solid var(--border-mid); flex-shrink: 0;
  background: transparent;
}
.pt-dot.pt-done { background: var(--text-primary); border-color: var(--text-primary); }
.pt-text { font-size: 12px; font-weight: 700; color: var(--text-muted); }
.pt-item:nth-child(3) .pt-text { color: var(--text-primary); font-weight: 800; }
/* Recommend mock */
.pv-recommend {
  display: flex; flex-direction: column; gap: 8px; margin-top: auto;
  padding: 16px; border: 1px solid var(--border);
}
.pr-badge {
  display: inline-block; font-size: 9px; font-weight: 900; letter-spacing: .1em;
  padding: 3px 8px; background: var(--text-primary); color: var(--bg-base);
  width: fit-content;
}
.pr-title { font-size: 14px; font-weight: 800; line-height: 1.3; }
.pr-meta { font-size: 11px; color: var(--text-muted); font-weight: 600; }
.pr-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.pr-tags span {
  font-size: 10px; font-weight: 700; padding: 3px 8px;
  border: 1px solid var(--border); color: var(--text-muted);
}
.pv-caption {
  font-size: 11px; color: var(--text-faint); font-weight: 700;
  letter-spacing: .03em;
}
@media (max-width: 768px) {
  .preview-grid { grid-template-columns: 1fr; }
}

/* ④ 피크 엔드 룰 + 사회적 증거 — CTA 강화 */
.cta-em {
  color: transparent;
  -webkit-text-stroke: 1.5px var(--border-mid);
}
.cta-social {
  display: flex; align-items: center; justify-content: center;
  gap: 24px; margin: 0 auto 40px;
  padding: 20px 40px;
  border: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-surface) 50%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  width: fit-content;
  opacity: 0; transform: translateY(20px);
  transition: opacity .8s cubic-bezier(.16,1,.3,1), transform .8s cubic-bezier(.16,1,.3,1);
  transition-delay: .1s;
}
.rv-cta.in .cta-social { opacity: 1; transform: none; }
.cs-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.cs-val { font-size: 22px; font-weight: 900; letter-spacing: -.02em; }
.cs-lbl { font-size: 10px; font-weight: 700; letter-spacing: .12em; color: var(--text-faint); }
.cs-div { width: 1px; height: 32px; background: var(--border); }
.cta-reassure {
  margin-top: 20px;
  font-size: 11px; font-weight: 700; letter-spacing: .05em;
  color: var(--text-faint);
}
@media (max-width: 600px) {
  .cta-social { flex-direction: column; gap: 16px; padding: 20px; }
  .cs-div { width: 40px; height: 1px; }
  .trust-sep { display: none; }
  .hero-trust { flex-direction: column; gap: 6px; }
}

</style>
