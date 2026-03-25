<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-link" /> ACCOUNT MANAGEMENT</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <button class="btn-back" @click="$router.push('/mypage')">
          <i class="fas fa-arrow-left" /> 마이페이지로 돌아가기
        </button>

        <div class="flex-col gap-lg pb-xl">
          <!-- Linked Accounts -->
          <div v-for="acc in accounts" :key="acc.name" class="base-panel p-0 shadow-normal overflow-hidden">
            <!-- Header -->
            <div class="panel-header flex-between p-md pb-sm border-b">
              <div class="flex-align gap-md">
                <div class="icon-box">
                  <i :class="acc.icon" />
                </div>
                <div>
                  <div class="flex-align gap-sm mb-xs">
                    <span class="acc-name">{{ acc.name }}</span>
                    <span class="status-badge" :class="acc.status">
                      <i class="fas fa-check-circle" /> 연동 완료
                    </span>
                  </div>
                  <div class="acc-id">{{ acc.account }}</div>
                  <div class="acc-desc">{{ acc.desc }}</div>
                </div>
              </div>
            </div>

            <!-- Sync Info -->
            <div class="px-md pb-sm pt-sm">
              <div class="sync-info-box">
                <i class="fas fa-sync-alt spin-hover" />
                <span>
                  마지막 동기화: <strong>{{ acc.lastSync }}</strong>
                </span>
                <span class="dot">·</span>
                <span>자동 동기화 활성화됨</span>
              </div>
            </div>

            <!-- Stats -->
            <div class="px-md pb-md">
              <div class="grid-3 gap-sm">
                <div v-for="s in acc.stats" :key="s.label" class="stat-card">
                  <div class="stat-label">{{ s.label }}</div>
                  <div class="stat-value">{{ s.value }}</div>
                  <div class="stat-sub">{{ s.sub }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Google Calendar notice -->
          <div class="base-panel p-md shadow-normal account-notice">
            <div class="flex-start gap-md">
              <div class="icon-box warning shrink-0">
                <i class="fas fa-exclamation-triangle" />
              </div>
              <div class="flex-1">
                <div class="flex-align gap-sm mb-sm">
                  <span class="acc-name">Google Calendar</span>
                  <span class="status-badge readonly">읽기 전용</span>
                </div>
                <p class="acc-desc lh-lg mb-sm">
                  Google Calendar는 일정 동기화 전용으로 연동되어 있습니다. 캘린더 화면의 "외부 일정 가져오기"를 통해 일정을 불러올 수 있습니다.
                </p>
                <div class="text-xs text-muted font-bold flex-align gap-sm">
                  <i class="fas fa-check-circle" /> 연동 완료 <span class="dot">·</span> jung@email.com <span class="dot">·</span> 마지막 동기화 2026.03.09 14:23
                </div>
              </div>
            </div>
          </div>

          <!-- Info note -->
          <div class="info-note base-panel shadow-none">
            <div class="flex-start gap-md">
              <i class="fas fa-info-circle text-muted mt-xs text-sm shrink-0" />
              <p class="text-sm text-muted font-bold lh-lg m-0">
                연동 상태는 추천과 학습 기록 분석에 반영됩니다. 계정별 마지막 동기화 시각과 수집 범위는 이 화면에서 계속 확인할 수 있습니다.
              </p>
            </div>
          </div>

        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { getExternalAccounts } from '@/api/usersApi'

const router = useRouter()

function formatDateTime(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${m}.${day} ${h}:${min}`
}

const mockAccounts = [
  {
    icon: "fab fa-github",
    name: "GitHub",
    account: "junghyun-dev",
    status: "connected",
    lastSync: "2026.03.09 14:23",
    desc: "커밋, PR, 이슈 활동을 분석합니다",
    stats: [
      { label: "최근 커밋", value: "24개", sub: "이번 달" },
      { label: "PR", value: "6개", sub: "최근 30일" },
      { label: "리포지토리", value: "3개", sub: "활성 기준" },
    ],
  },
  {
    icon: "fas fa-file-alt",
    name: "Velog",
    account: "@junghyun",
    status: "connected",
    lastSync: "2026.03.09 12:10",
    desc: "블로그 발행 활동과 주제를 분석합니다",
    stats: [
      { label: "발행 글", value: "8편", sub: "최근 3개월" },
      { label: "최근 글", value: "3월 7일", sub: "" },
      { label: "총 조회수", value: "1,247", sub: "누적" },
    ],
  },
]

const accounts = ref([...mockAccounts])

onMounted(async () => {
  try {
    const { data } = await getExternalAccounts()
    const mapped = []
    if (data.github) {
      mapped.push({
        icon: "fab fa-github",
        name: "GitHub",
        account: data.github.username,
        status: "connected",
        lastSync: formatDateTime(data.github.lastSyncedAt),
        desc: "커밋, PR, 이슈 활동을 분석합니다",
        stats: [
          { label: "최근 커밋", value: `${data.github.recentCommits}개`, sub: "이번 달" },
          { label: "PR", value: `${data.github.recentPrs}개`, sub: "최근 30일" },
          { label: "리포지토리", value: `${data.github.totalRepos}개`, sub: "활성 기준" },
        ],
      })
    }
    if (data.velog) {
      mapped.push({
        icon: "fas fa-file-alt",
        name: "Velog",
        account: `@${data.velog.username}`,
        status: "connected",
        lastSync: formatDateTime(data.velog.lastSyncedAt),
        desc: "블로그 발행 활동과 주제를 분석합니다",
        stats: [
          { label: "발행 글", value: `${data.velog.totalPosts}편`, sub: "전체" },
          { label: "최근 글", value: data.velog.latestPostDate || '-', sub: "" },
          { label: "총 조회수", value: data.velog.totalViews?.toLocaleString() || '0', sub: "누적" },
        ],
      })
    }
    if (mapped.length > 0) accounts.value = mapped
  } catch (e) {
    console.error('external-accounts 조회 실패 (Mock 유지):', e)
  }
})
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { 
  display: flex !important; align-items: center; 
  height: 64px !important; min-height: 64px; max-height: 64px; 
  flex-shrink: 0;
  padding: 0 24px; border-bottom: 1px solid var(--border); 
  background: var(--bg-surface); position: sticky; top: 0; z-index: 10; 
  box-sizing: border-box; 
}
.header-title { 
  font-size: 15px; font-weight: 900; letter-spacing: 0.15em; 
  color: var(--text-primary); display: flex; align-items: center; gap: 12px; 
  text-transform: uppercase;
}
.header-title i { font-size: 18px; width: 24px; text-align: center; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 680px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-start { display: flex; align-items: flex-start; }
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); }
@media (max-width: 600px) { .grid-3 { grid-template-columns: 1fr; } }
.shrink-0 { flex-shrink: 0; }

.gap-sm { gap: 12px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 24px; }

.mb-xs { margin-bottom: 4px; }
.mb-sm { margin-bottom: 8px; }
.mb-md { margin-bottom: 16px; }
.mt-xs { margin-top: 4px; }
.p-0 { padding: 0 !important; }
.p-md { padding: 24px; }
.px-md { padding-left: 24px; padding-right: 24px; }
.pb-sm { padding-bottom: 12px; }
.pt-sm { padding-top: 12px; }
.pb-md { padding-bottom: 24px; }
.pb-xl { padding-bottom: 64px; }
.m-0 { margin: 0; }
.lh-lg { line-height: 1.5; }

.border-b { border-bottom: 1px solid var(--border); }
.overflow-hidden { overflow: hidden; }
.dot { margin: 0 4px; color: var(--text-muted); }

.text-muted { color: var(--text-muted); }
.text-xs { font-size: 11px; }
.text-sm { font-size: 13px; }
.font-bold { font-weight: 800; }

/* Panels & Shadows */
.base-panel { background: transparent; border: 1px solid var(--border); border-radius: 8px; transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s; }
.shadow-normal { box-shadow: none; }
.shadow-none { box-shadow: none; }

.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; margin-bottom: 24px; transition: color 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }

/* Icons */
.icon-box { width: 48px; height: 48px; background: transparent; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 24px; color: var(--text-primary); border-radius: 8px; }
.icon-box.warning { border-color: var(--text-muted); color: var(--text-muted); }
.spin-hover { transition: transform 0.3s; cursor: pointer; }
.spin-hover:hover { transform: rotate(180deg); color: var(--text-primary); }

/* Accounts styling */
.acc-name { font-size: 16px; font-weight: 900; color: var(--text-primary); }
.acc-id { font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 4px; }
.acc-desc { font-size: 12px; font-weight: 600; color: var(--text-muted); }

.status-badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 8px; font-size: 10px; font-weight: 900; border-radius: 40px; }
.status-badge.connected { background: transparent; border: 1px solid var(--text-primary); color: var(--text-primary); }
.status-badge.readonly { background: transparent; border: 1px solid var(--text-muted); color: var(--text-muted); }

.sync-info-box { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: transparent; border: 1px solid var(--border); border-radius: 8px; font-size: 12px; font-weight: 700; color: var(--text-muted); }
.sync-info-box strong { color: var(--text-primary); font-weight: 900; }

.stat-card { background: transparent; border: 1px solid var(--border); padding: 12px; border-radius: 8px; }
.stat-label { font-size: 11px; font-weight: 800; color: var(--text-muted); }
.stat-value { font-size: 16px; font-weight: 900; color: var(--text-primary); margin: 4px 0; }
.stat-sub { font-size: 10px; font-weight: 700; color: var(--text-muted); }

/* Additional Info */
.info-note { background: transparent; padding: 20px; border: 1px dashed var(--border); border-radius: 8px; }
</style>
