import { createRouter, createWebHistory } from 'vue-router'
import LandingPage from '../views/LandingPage.vue'
import LoginPage from '../views/LoginPage.vue'
import OnboardingConnect from '../views/OnboardingConnect.vue'
import OnboardingSurvey from '../views/OnboardingSurvey.vue'
import LoadingPage from '../views/LoadingPage.vue'
import AnalysisResultPage from '../views/AnalysisResultPage.vue'
import CalendarPage from '../views/CalendarPage.vue'
import RecommendPage from '../views/RecommendPage.vue'
import AlternativeActivities from '../views/AlternativeActivities.vue'
import AlternativeCurriculumPage from '../views/AlternativeCurriculumPage.vue'
import NewLearning from '../views/NewLearning.vue'
import NewCurriculumResult from '../views/NewCurriculumResult.vue'
import CurriculumSuggestPage from '../views/CurriculumSuggestPage.vue'
import ActivityProgress from '../views/ActivityProgress.vue'
import HistoryPage from '../views/HistoryPage.vue'
import GrowthJournal from '../views/GrowthJournal.vue'
import MyPage from '../views/MyPage.vue'
import ProfileSettings from '../views/ProfileSettings.vue'
import AccountManagement from '../views/AccountManagement.vue'

const routes = [
  { path: '/', name: 'Landing', component: LandingPage },
  { path: '/login', name: 'Login', component: LoginPage },

  // 온보딩
  { path: '/onboarding/connect', name: 'OnboardingConnect', component: OnboardingConnect },
  { path: '/onboarding/survey', name: 'OnboardingSurvey', component: OnboardingSurvey },
  { path: '/onboarding/loading', name: 'OnboardingLoading', component: LoadingPage },
  { path: '/onboarding/result', name: 'AnalysisResult', component: AnalysisResultPage },

  // 메인 서비스
  { path: '/calendar', name: 'Calendar', component: CalendarPage },

  // 추천 및 커리큘럼
  { path: '/recommend', name: 'Recommend', component: RecommendPage },
  { path: '/recommend/alternatives', name: 'AlternativeActivities', component: AlternativeActivities },
  { path: '/recommend/new-learning', name: 'NewLearning', component: NewLearning },
  { path: '/recommend/new-curriculum', name: 'NewCurriculumResult', component: NewCurriculumResult },
  { path: '/curriculum/suggest', name: 'CurriculumSuggest', component: CurriculumSuggestPage },
  { path: '/curriculum/alternative', name: 'AlternativeCurriculum', component: AlternativeCurriculumPage },

  // 학습 활동
  { path: '/activity', name: 'ActivityProgress', component: ActivityProgress },

  // 기록 및 마이페이지
  { path: '/history', name: 'History', component: HistoryPage },
  { path: '/history/growth', name: 'GrowthJournal', component: GrowthJournal },
  { path: '/mypage', name: 'MyPage', component: MyPage },
  { path: '/mypage/profile', name: 'ProfileSettings', component: ProfileSettings },
  { path: '/mypage/accounts', name: 'AccountManagement', component: AccountManagement },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
