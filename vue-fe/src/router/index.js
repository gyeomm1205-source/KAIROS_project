import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ── 온보딩 ──────────────────────────────
  { path: '/',        name: 'landing',        component: () => import('@/views/LandingPage.vue')  },
  { path: '/login',   name: 'login',          component: () => import('@/views/LoginPage.vue')    },
  { path: '/signup',  name: 'signup',         component: () => import('@/views/SignUpPage.vue')   },
  { path: '/setup',   name: 'setup',          component: () => import('@/views/SetupPage.vue')    },
  { path: '/loading', name: 'loading',        component: () => import('@/views/LoadingPage.vue')  },

  // ── 분석 플로우 ──────────────────────────
  { path: '/analysis',           name: 'analysis',           component: () => import('@/views/DashboardPage.vue')        },
  { path: '/feedback',           name: 'feedback',           component: () => import('@/views/FeedbackPage.vue')         },
  { path: '/curriculum-suggest', name: 'curriculum-suggest', component: () => import('@/views/CurriculumSuggestPage.vue') },
  { path: '/activity-select',    name: 'activity-select',    component: () => import('@/views/ActivitySelectPage.vue')   },
  { path: '/curriculum-upload',  name: 'curriculum-upload',  component: () => import('@/views/CurriculumUploadPage.vue') },
  { path: '/quiz-activity',      name: 'quiz-activity',      component: () => import('@/views/QuizActivityPage.vue')     },

  // ── 앱 내부 ─────────────────────────────
  { path: '/calendar',       name: 'calendar',       component: () => import('@/views/MainPage.vue')          },
  { path: '/study-calendar', name: 'study-calendar', component: () => import('@/views/StudyCalendarPage.vue') },
  { path: '/recommend',      name: 'recommend',      component: () => import('@/views/RecommendPage.vue')     },
  { path: '/history',        name: 'history',        component: () => import('@/views/HistoryPage.vue')     },
  { path: '/quiz',           name: 'quiz',           component: () => import('@/views/QuizPage.vue')          },
  { path: '/mypage',         name: 'mypage',         component: () => import('@/views/MyPage.vue')            },
  { path: '/blog-assistant', name: 'blog-assistant', component: () => import('@/views/BlogAssistantPage.vue') },

  // fallback
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
