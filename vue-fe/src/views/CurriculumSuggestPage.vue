<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 일자별 커리큘럼 예시 데이터
const curriculumSchedule = [
  { date: '3월 8일 (수)', title: 'React 기초 및 렌더링 최적화', time: '2시간' },
  { date: '3월 9일 (목)', title: '상태 관리 심화 (Zustand/Pinia)', time: '1시간 30분' },
  { date: '3월 10일 (금)', title: 'TypeScript 고급 타입 활용', time: '2시간' },
  { date: '3월 11일 (토)', title: '번들 최적화 및 Lighthouse 분석', time: '3시간' },
]
</script>

<template>
  <div class="modal-root">
    <div class="modal-card">
      <div class="modal-header">
        <h2>다음 주 커리큘럼을 제안드려요</h2>
        <button class="btn-close" @click="router.push('/analysis')"><i class="fas fa-times"></i></button>
      </div>
      
      <div class="modal-body">
        <p class="desc">
          최근 블로그와 GitHub 활동을 바탕으로 맞춤형 학습 커리큘럼을 작성했어요.<br>
          이대로 캘린더에 업로드해볼까요?
        </p>

        <h3 class="section-title"><i class="fas fa-book-open"></i> 제안된 학습 주제</h3>
        <div class="topic-list">
          <div class="topic-item">
            <div class="dot"></div>
            <div>
              <h4>React 고급 패턴 학습</h4>
              <p>컴포넌트 최적화 및 상태 관리 심화</p>
            </div>
          </div>
          <div class="topic-item">
            <div class="dot"></div>
            <div>
              <h4>TypeScript 실전 활용</h4>
              <p>타입 안정성 강화 및 고급 타입 시스템</p>
            </div>
          </div>
          <div class="topic-item">
            <div class="dot"></div>
            <div>
              <h4>성능 최적화 기법</h4>
              <p>번들 최적화 및 렌더링 성능 개선</p>
            </div>
          </div>
        </div>

        <!-- 새로 추가된 실제 커리큘럼(일정표) UI -->
        <h3 class="section-title"><i class="fas fa-calendar-check"></i> 생성될 커리큘럼 일정</h3>
        <div class="schedule-timeline">
          <div v-for="(day, index) in curriculumSchedule" :key="index" class="schedule-item">
            <div class="schedule-marker">
              <div class="marker-dot"></div>
              <div class="marker-line" v-if="index !== curriculumSchedule.length - 1"></div>
            </div>
            <div class="schedule-content">
              <span class="schedule-date">{{ day.date }}</span>
              <div class="schedule-card">
                <h5>{{ day.title }}</h5>
                <span class="schedule-time"><i class="far fa-clock"></i> {{ day.time }} 예상</span>
              </div>
            </div>
          </div>
        </div>

        <h3 class="section-title"><i class="fas fa-lightbulb"></i> 추천 이유 요약</h3>
        <div class="reason-box">
          <div class="reason-item">
            <i class="fab fa-github"></i>
            <div>
              <h5>GitHub 활동 분석</h5>
              <p>최근 React 프로젝트 커밋 패턴과 코드 리뷰 내용을 기반으로 학습 방향을 설정했어요.</p>
            </div>
          </div>
          <div class="reason-item">
            <i class="fab fa-blogger-b"></i>
            <div>
              <h5>블로그 포스팅 트렌드</h5>
              <p>작성하신 기술 블로그 주제들과 관심 분야를 분석하여 연관성 높은 커리큘럼을 구성했어요.</p>
            </div>
          </div>
          <div class="reason-item">
            <i class="fas fa-chart-line"></i>
            <div>
              <h5>학습 진도 예측</h5>
              <p>현재 실력 수준을 고려하여 적절한 난이도와 학습량으로 계획을 세웠어요.</p>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-text" @click="router.push('/activity-select')">다른 활동도 알아볼래요</button>
        <button class="btn-primary" @click="router.push('/main')">네, 캘린더에 업로드할게요</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-root { height: 100%; min-height: 100vh; overflow-y: auto; display: flex; align-items: flex-start; justify-content: center; background: rgba(0,0,0,0.4); font-family: 'Escoredream', sans-serif; padding: 40px 20px; }
.modal-card { width: 100%; max-width: 540px; background: #fff; border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); overflow: hidden; margin-top: 20px; margin-bottom: 20px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 24px 32px; border-bottom: 1px solid #eee; }
.modal-header h2 { font-size: 18px; font-weight: 800; color: #111; }
.btn-close { background: none; border: none; font-size: 18px; color: #999; cursor: pointer; }

.modal-body { padding: 32px; }
.desc { font-size: 13px; color: #555; line-height: 1.6; margin-bottom: 32px; }
.section-title { font-size: 14px; font-weight: 700; color: #333; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.section-title i { color: #818cf8; }

.topic-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 36px; }
.topic-item { display: flex; gap: 14px; padding: 16px; border: 1px solid #eaeaea; border-radius: 10px; background: #fafafa; }
.dot { width: 8px; height: 8px; background: #818cf8; border-radius: 50%; margin-top: 5px; flex-shrink: 0; }
.topic-item h4 { font-size: 14px; font-weight: 600; color: #222; margin-bottom: 4px; }
.topic-item p { font-size: 12px; color: #777; }

/* 새로 추가된 스케줄 타임라인 스타일 */
.schedule-timeline { margin-bottom: 36px; padding-left: 8px; }
.schedule-item { display: flex; gap: 16px; min-height: 70px; }
.schedule-marker { display: flex; flex-direction: column; align-items: center; width: 12px; }
.marker-dot { width: 12px; height: 12px; border-radius: 50%; background: #fff; border: 3px solid #818cf8; z-index: 2; margin-top: 2px; }
.marker-line { width: 2px; height: 100%; background: #e0e7ff; margin-top: -2px; flex-grow: 1; }
.schedule-content { flex: 1; padding-bottom: 20px; }
.schedule-date { font-size: 12px; font-weight: 700; color: #818cf8; margin-bottom: 6px; display: block; }
.schedule-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); display: flex; justify-content: space-between; align-items: center; }
.schedule-card h5 { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0; }
.schedule-time { font-size: 12px; color: #64748b; display: flex; align-items: center; gap: 4px; }

.reason-box { background: #fafafa; border: 1px solid #eaeaea; border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 20px; }
.reason-item { display: flex; gap: 14px; }
.reason-item i { font-size: 18px; color: #555; margin-top: 2px; width: 20px; text-align: center; }
.reason-item h5 { font-size: 13px; font-weight: 700; color: #333; margin-bottom: 4px; }
.reason-item p { font-size: 12px; color: #666; line-height: 1.5; }

.modal-footer { display: flex; justify-content: flex-end; align-items: center; gap: 16px; padding: 24px 32px; border-top: 1px solid #eee; background: #fafafa; }
.btn-text { background: #fff; border: 1px solid #ddd; padding: 12px 20px; border-radius: 8px; font-size: 13px; font-weight: 600; color: #555; cursor: pointer; font-family: inherit; }
.btn-primary { background: #111; border: none; padding: 12px 28px; border-radius: 8px; font-size: 13px; font-weight: 600; color: #fff; cursor: pointer; font-family: inherit; transition: background 0.2s; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.btn-primary:hover { background: #333; }
</style>