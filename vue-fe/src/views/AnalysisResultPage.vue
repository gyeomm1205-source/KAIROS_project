<template>
  <div class="min-h-screen bg-gray-50 flex flex-col font-sans">
    <div class="h-14 bg-white border-b border-gray-200 flex items-center px-6">
      <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-gray-700 text-sm">
        <i class="fas fa-arrow-left"></i> 뒤로
      </button>
    </div>

    <div v-if="isLoadingAI" class="flex-1 flex flex-col items-center justify-center text-gray-500">
      <i class="fas fa-circle-notch fa-spin text-3xl mb-4 text-blue-500"></i>
      <p>AI가 사용자의 학습 활동을 분석하고 있습니다...</p>
    </div>

    <div v-else-if="analysisResult" class="flex-1 flex items-start justify-center pt-10 px-6 pb-12">
      <div class="w-full max-w-2xl">
        <h2 class="text-gray-900 mb-2 text-center text-2xl font-bold">
          현재 학습 상태를 이렇게 이해했어요
        </h2>
        <p class="text-gray-500 text-center mb-8 text-sm">
          최근 GitHub, Velog, Google Calendar 활동을 종합 분석한 결과입니다
        </p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="bg-white border border-gray-200 rounded-lg p-5">
            <div class="flex items-center gap-2 mb-3 text-gray-600 font-semibold text-sm">
              <i class="fas fa-code"></i>
              <h3>최근 활동 기술</h3>
            </div>
            <div class="flex flex-wrap gap-1.5">
              <span v-for="t in analysisResult.recentTechs" :key="t" class="px-2.5 py-1 bg-gray-100 text-gray-700 rounded-full text-xs">
                {{ t }}
              </span>
            </div>
          </div>

          <div class="bg-white border border-gray-200 rounded-lg p-5">
            <div class="flex items-center gap-2 mb-3 text-gray-600 font-semibold text-sm">
              <i class="fas fa-chart-bar"></i>
              <h3>전반적인 기술 숙련도</h3>
            </div>
            <div class="space-y-2">
              <div v-for="s in analysisResult.skillLevels" :key="s.name" class="flex items-center gap-2">
                <span class="w-20 text-gray-600 shrink-0 text-xs">{{ s.name }}</span>
                <div class="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full bg-gray-600 rounded-full" :style="{ width: s.level + '%' }"></div>
                </div>
                <span class="text-gray-400 w-8 text-right text-xs">{{ s.level }}%</span>
              </div>
            </div>
          </div>

          <div class="bg-white border border-gray-200 rounded-lg p-5">
            <div class="flex items-center gap-2 mb-3 text-gray-600 font-semibold text-sm">
              <i class="fas fa-redo"></i>
              <h3>반복적으로 다룬 기술</h3>
            </div>
            <div class="space-y-2">
              <div v-for="t in analysisResult.repeatedTechs" :key="t.name" class="flex items-center justify-between text-sm">
                <span class="text-gray-700">{{ t.name }}</span>
                <span class="text-gray-400 text-xs">{{ t.count }}회 등장</span>
              </div>
            </div>
          </div>

          <div class="bg-white border border-gray-200 rounded-lg p-5">
            <div class="flex items-center gap-2 mb-3 text-gray-600 font-semibold text-sm">
              <i class="fas fa-compass"></i>
              <h3>추천 포지션</h3>
            </div>
            <div class="space-y-2 text-sm">
              <div v-for="pos in analysisResult.recommendedPositions" :key="pos.title" class="flex items-center justify-between">
                <span class="text-gray-700">{{ pos.title }}</span>
                <span class="px-2 py-0.5 rounded-full text-xs" 
                      :class="pos.isHighMatch ? 'bg-gray-800 text-white' : 'bg-gray-200 text-gray-600'">
                  {{ pos.isHighMatch ? '적합도 높음' : '가능성 있음' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white border border-gray-200 rounded-lg p-5 mb-8">
          <h3 class="text-gray-800 mb-2 font-semibold text-sm">요약</h3>
          <p class="text-gray-600 text-sm leading-relaxed">
            {{ analysisResult.summary }}
          </p>
        </div>

        <div class="bg-white border border-gray-200 rounded-lg p-5 text-center">
          <p class="text-gray-700 mb-4 font-semibold">분석 결과가 실제와 잘 맞나요?</p>
          <div class="flex gap-3 justify-center">
            <button @click="router.push('/curriculum/suggest')" class="px-6 py-2.5 bg-gray-800 text-white rounded hover:bg-gray-700 text-sm">
              결과가 맞아요
            </button>
            <button @click="showFeedback = true" class="px-6 py-2.5 border border-gray-300 text-gray-600 rounded hover:bg-gray-50 text-sm">
              수정할게요
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="flex-1 flex flex-col items-center justify-center text-red-500">
      <i class="fas fa-exclamation-triangle text-3xl mb-4"></i>
      <p>분석 데이터를 불러오지 못했습니다.</p>
    </div>

    <div v-if="showFeedback" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl w-full max-w-md mx-4 p-6 shadow-lg">
        <div class="flex items-center justify-between mb-1">
          <h3 class="text-gray-800 font-bold text-lg">어떤 부분을 수정하면 좋을까요?</h3>
          <button @click="closeFeedback" class="text-gray-400 hover:text-gray-600">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <p class="text-gray-400 mb-4 text-xs">
          기술 숙련도, 관심 분야, 추천 포지션 등 수정이 필요한 내용을 자유롭게 적어주세요
        </p>
        <textarea
          v-model="feedbackText"
          placeholder="예: TypeScript 숙련도가 실제보다 낮게 나온 것 같아요."
          class="w-full border border-gray-300 rounded-lg p-3 text-gray-800 text-sm focus:outline-none focus:border-gray-500 resize-none"
          rows="4"
        ></textarea>
        <div class="flex gap-3 mt-4 justify-end">
          <button @click="closeFeedback" class="px-4 py-2 border border-gray-300 text-gray-500 rounded hover:bg-gray-50 text-sm">
            취소
          </button>
          <button
            @click="submitFeedback"
            :disabled="!feedbackText.trim() || isSubmitting"
            :class="feedbackText.trim() ? 'bg-gray-800 text-white hover:bg-gray-700' : 'bg-gray-200 text-gray-400 cursor-not-allowed'"
            class="px-4 py-2 rounded flex items-center gap-1.5 text-sm transition"
          >
            <i class="fas fa-paper-plane" v-if="!isSubmitting"></i>
            <i class="fas fa-spinner fa-spin" v-else></i>
            피드백 제출
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useCalendarStore } from '@/stores/useCalendarStore'

const router = useRouter()
const store = useCalendarStore()

// Store 데이터 연동
const { analysisResult, isLoadingAI } = storeToRefs(store)

const showFeedback = ref(false)
const feedbackText = ref('')
const isSubmitting = ref(false)

onMounted(async () => {
  // 실제 백엔드 연동: 데이터가 없으면 API 호출
  if (!analysisResult.value) {
    await store.loadAnalysisResult('user-123') // 실제 유저 ID 매핑 필요
  }
})

const closeFeedback = () => {
  showFeedback.value = false
  feedbackText.value = ''
}

const submitFeedback = async () => {
  if (feedbackText.value.trim()) {
    isSubmitting.value = true
    try {
      // 피드백 전송 API 호출 (캘린더 API에 구현했다고 가정)
      // await store.submitAnalysisFeedback({ text: feedbackText.value })
      
      // 완료 후 이동
      router.push('/calendar')
    } catch (e) {
      console.error(e)
    } finally {
      isSubmitting.value = false
      closeFeedback()
    }
  }
}
</script>