<template>
  <aside class="sidebar">
    <RouterLink to="/" class="sidebar-brand">
      <div class="brand-logo">
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="url(#kg)" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="url(#kg)" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="url(#kg)"/>
          <path d="M8 5h12l-4 4H12L8 5z" fill="url(#kg)" opacity="0.5"/>
          <defs>
            <linearGradient id="kg" x1="6" y1="3" x2="22" y2="25" gradientUnits="userSpaceOnUse">
              <stop stop-color="#818cf8"/><stop offset="1" stop-color="#38bdf8"/>
            </linearGradient>
          </defs>
        </svg>
      </div>
      <div class="brand-text">
        <span class="brand-name">KAIROS</span>
        <span class="brand-sub">시간의 기회를 잡다</span>
      </div>
    </RouterLink>

    <nav class="sidebar-nav">
      <div class="nav-section-label">메인</div>

      <template v-for="item in NAV_ITEMS" :key="item.to ?? item.id">

        <div v-if="item.sub" ref="calWrapRef" class="nav-item-wrap">
          <button
            class="nav-item nav-item--has-sub"
            :class="{ 'nav-item--active': isCalActive, 'nav-item--open': calOpen }"
            @click.stop="toggleCal"
          >
            <i :class="item.icon" />
            <span>{{ item.label }}</span>
            <i class="fas fa-chevron-right nav-sub-arrow" :class="{ 'nav-sub-arrow--open': calOpen }" />
          </button>

          <Transition name="sub-expand">
            <div v-if="calOpen" class="sub-menu">
              <RouterLink
                v-for="s in item.sub" :key="s.to"
                :to="s.to"
                class="sub-item"
                :class="{ 'sub-item--active': route.path === s.to }"
                @click="calOpen = false"
              >
                <div class="sub-item-left">
                  <div class="sub-dot" :class="{ 'sub-dot--active': route.path === s.to }" />
                  <div class="sub-info">
                    <span class="sub-label">{{ s.label }}</span>
                    <span class="sub-desc">{{ s.desc }}</span>
                  </div>
                </div>
                <i v-if="route.path === s.to" class="fas fa-check sub-check" />
              </RouterLink>
            </div>
          </Transition>
        </div>

        <RouterLink
          v-else
          :to="item.to"
          class="nav-item"
          active-class="nav-item--active"
        >
          <i :class="item.icon" />
          <span>{{ item.label }}</span>
        </RouterLink>

      </template>
    </nav>

    <div class="sidebar-footer">
      <div class="theme-toggle" @click="themeStore.toggle()">
        <div class="theme-track" :class="{ 'theme-track--light': !themeStore.isDark }">
          <div class="theme-thumb">
            <i :class="themeStore.isDark ? 'fas fa-moon' : 'fas fa-sun'" />
          </div>
        </div>
        <span class="theme-label">{{ themeStore.isDark ? '다크' : '라이트' }} 모드</span>
      </div>

      <RouterLink to="/mypage" class="user-info user-info--link">
        <div class="user-avatar"><i class="fab fa-github" /></div>
        <div class="user-detail">
          <div class="user-name">김싸피</div>
          <div class="user-sub">GitHub 연동됨</div>
        </div>
        <i class="fas fa-chevron-right user-arrow" />
      </RouterLink>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/useThemeStore'

const themeStore = useThemeStore()
const route      = useRoute()
const calOpen    = ref(false)
const calWrapRef = ref(null)

function toggleCal() { calOpen.value = !calOpen.value }

function onDocClick(e) {
  const el = Array.isArray(calWrapRef.value) ? calWrapRef.value[0] : calWrapRef.value
  if (el && !el.contains(e.target)) calOpen.value = false
}

onMounted(()  => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))

const isCalActive = computed(() =>
  route.path === '/calendar' || route.path === '/study-calendar'
)

// ★ 요쳥하신 4개 항목을 모두 제거했습니다.
const NAV_ITEMS = [
  {
    id: 'cal',
    to: '/calendar',
    icon: 'fas fa-calendar-alt',
    label: '캘린더',
    sub: [
      { to: '/calendar',       label: '캘린더 플로우',  desc: '브랜치 기반 학습 캘린더'     },
      { to: '/study-calendar', label: '학습 캘린더',    desc: '일별 학습 관리 & 구글 연동'  },
      { to: '/prompt',         label: 'AI 어시스턴트',  desc: '프롬프트 및 블로그 내역'     }, // ★ 추가됨
    ],
  },
  { to: '/recommend', icon: 'fas fa-lightbulb',       label: '추천'         },
  { to: '/history',   icon: 'fas fa-history',         label: '히스토리'     },
]
</script>

<style scoped>
.sidebar {
  width: 210px; background: var(--bg-surface); border-right: 1px solid var(--border);
  display: flex; flex-direction: column; flex-shrink: 0; z-index: 40;
  transition: background 0.3s, border-color 0.3s;
}

.sidebar-brand {
  height: 64px; display: flex; align-items: center; justify-content: center;
  padding: 0 16px; gap: 8px; border-bottom: 1px solid var(--border);
  text-decoration: none; cursor: pointer; transition: background 0.15s; flex-shrink: 0;
}
.sidebar-brand:hover { background: var(--bg-hover); }
.brand-logo { flex-shrink: 0; filter: drop-shadow(0 0 10px rgba(129,140,248,0.3)); }
.brand-text { display: flex; flex-direction: column; gap: 2px; }
.brand-name {
  font-family: 'Escoredream', serif; font-weight: 800; font-size: 16px; letter-spacing: 0.18em;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; line-height: 1;
}
.brand-sub { font-size: 8px; color: var(--text-faint); letter-spacing: 0.08em; }

.sidebar-nav {
  padding: 20px 10px; display: flex; flex-direction: column; gap: 2px;
  flex: 1; overflow-y: auto; overflow-x: hidden;
}
.nav-section-label {
  font-size: 10px; font-weight: 700; color: var(--text-faint);
  letter-spacing: 0.1em; text-transform: uppercase; padding: 0 10px; margin-bottom: 6px;
}

.nav-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 10px;
  font-size: 13px; font-weight: 600; text-decoration: none; color: var(--text-muted);
  transition: all 0.15s; width: 100%; background: none; border: none; cursor: pointer;
  font-family: 'Escoredream', system-ui, sans-serif; text-align: left;
}
.nav-item i { width: 16px; text-align: center; font-size: 14px; flex-shrink: 0; }
.nav-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.nav-item--active { background: rgba(59,130,246,0.12); color: #60a5fa; font-weight: 700; }
.theme-dark  .nav-item--active { color: #60a5fa; }
.theme-light .nav-item--active { color: #2563eb; }
.nav-item--open { background: var(--bg-hover); color: var(--text-primary); }

.nav-item-wrap { display: flex; flex-direction: column; }
.nav-item--has-sub { width: 100%; }
.nav-sub-arrow {
  margin-left: auto; font-size: 10px; color: var(--text-faint);
  transition: transform 0.22s cubic-bezier(0.34,1.3,0.64,1);
}
.nav-sub-arrow--open { transform: rotate(90deg); color: #818cf8; }

.sub-menu {
  margin: 4px 0 4px 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 6px;
  display: flex; flex-direction: column; gap: 2px;
  overflow: hidden;
}
.sub-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 12px; border-radius: 8px;
  text-decoration: none; transition: background 0.13s;
}
.sub-item:hover { background: var(--bg-hover); }
.sub-item--active { background: rgba(129,140,248,0.12); }

.sub-item-left { display: flex; align-items: center; gap: 10px; }
.sub-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--border-mid); flex-shrink: 0; transition: background 0.15s;
}
.sub-dot--active { background: #818cf8; box-shadow: 0 0 6px rgba(129,140,248,0.5); }
.sub-item:hover .sub-dot:not(.sub-dot--active) { background: var(--text-muted); }

.sub-info { display: flex; flex-direction: column; gap: 2px; }
.sub-label { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.sub-item--active .sub-label { color: #818cf8; }
.sub-desc  { font-size: 10px; color: var(--text-faint); }
.sub-check { font-size: 11px; color: #818cf8; }

.sub-expand-enter-active { transition: all 0.22s cubic-bezier(0.34,1.2,0.64,1); }
.sub-expand-leave-active { transition: all 0.15s ease; }
.sub-expand-enter-from, .sub-expand-leave-to { opacity: 0; transform: translateY(-4px); max-height: 0; }

.sidebar-footer { border-top: 1px solid var(--border); padding: 14px 14px 16px; display: flex; flex-direction: column; gap: 12px; }
.theme-toggle { display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 6px 4px; border-radius: 8px; transition: background 0.15s; }
.theme-toggle:hover { background: var(--bg-hover); }
.theme-track { width: 40px; height: 22px; background: var(--border-mid); border-radius: 11px; position: relative; transition: background 0.3s; flex-shrink: 0; }
.theme-track--light { background: #bfdbfe; }
.theme-thumb { position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; border-radius: 50%; background: var(--bg-surface); display: flex; align-items: center; justify-content: center; font-size: 9px; color: #facc15; transition: transform 0.3s; }
.theme-track--light .theme-thumb { transform: translateX(18px); color: #f59e0b; }
.theme-label { font-size: 12px; font-weight: 500; color: var(--text-muted); }

.user-info--link { display: flex; align-items: center; gap: 10px; text-decoration: none; padding: 6px 4px; border-radius: 8px; cursor: pointer; transition: background 0.15s; }
.user-info--link:hover { background: var(--bg-hover); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; background: var(--bg-elevated); border: 1px solid var(--border-mid); display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 14px; flex-shrink: 0; }
.user-detail { flex: 1; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.user-sub  { font-size: 10px; color: var(--text-faint); margin-top: 1px; }
.user-arrow { font-size: 10px; color: var(--text-faint); transition: transform 0.15s; }
.user-info--link:hover .user-arrow { transform: translateX(2px); }
</style>