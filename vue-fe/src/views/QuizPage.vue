<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">
      <!-- 헤더 -->
      <div class="page-header">
        <span class="page-title">퀴즈</span>
        <div class="header-right">
          <button class="icon-btn"><i class="fas fa-bell" /></button>
          <RouterLink to="/mypage" class="avatar-btn">K</RouterLink>
        </div>
      </div>

      <!-- 결과 화면 -->
      <div v-if="phase === 'result'" class="result-screen fade-in">
        <div class="result-card">
          <div class="result-emoji">{{ resultEmoji }}</div>
          <h2 class="result-title">{{ resultTitle }}</h2>
          <p class="result-sub">{{ correct }}개 정답 / {{ quizSet.length }}문항</p>

          <div class="result-score-ring">
            <svg viewBox="0 0 120 120" class="score-svg">
              <circle cx="60" cy="60" r="50" fill="none" stroke="var(--bg-elevated)" stroke-width="10"/>
              <circle cx="60" cy="60" r="50" fill="none"
                :stroke="scoreColor" stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="`${scoreDash} 314`"
                stroke-dashoffset="78.5"
                class="score-circle"/>
              <text x="60" y="56" text-anchor="middle" class="score-big">{{ scorePercent }}%</text>
              <text x="60" y="72" text-anchor="middle" class="score-small">점수</text>
            </svg>
          </div>

          <div class="result-breakdown">
            <div class="breakdown-item breakdown-correct">
              <i class="fas fa-check-circle" /> 정답 {{ correct }}개
            </div>
            <div class="breakdown-item breakdown-wrong">
              <i class="fas fa-times-circle" /> 오답 {{ quizSet.length - correct }}개
            </div>
          </div>

          <!-- 오답 노트 -->
          <div v-if="wrongAnswers.length" class="wrong-note">
            <div class="wrong-note-title"><i class="fas fa-book-open" /> 오답 노트</div>
            <div v-for="w in wrongAnswers" :key="w.q" class="wrong-item">
              <div class="wrong-q">{{ w.q }}</div>
              <div class="wrong-answer">
                <span class="wrong-my"><i class="fas fa-times" /> 내 답: {{ w.my }}</span>
                <span class="wrong-correct"><i class="fas fa-check" /> 정답: {{ w.answer }}</span>
              </div>
              <div class="wrong-explain">{{ w.explain }}</div>
            </div>
          </div>

          <!-- 캘린더 등록 버튼 -->
          <button class="btn-register-cal" @click="showCalModal = true">
            <i class="fas fa-calendar-plus" /> 학습 활동을 캘린더에 등록
          </button>

          <div class="result-actions">
            <button class="btn-retry" @click="restart">
              <i class="fas fa-redo" /> 다시 풀기
            </button>
            <button class="btn-category" @click="phase = 'select'">
              <i class="fas fa-list" /> 카테고리 선택
            </button>
          </div>
        </div>
      </div>

      <!-- 카테고리 선택 -->
      <div v-else-if="phase === 'select'" class="select-screen fade-in">
        <div class="select-inner">
          <h2 class="select-title">어떤 분야를 공부할까요?</h2>
          <p class="select-sub">카테고리를 선택하면 AI가 맞춤 퀴즈를 출제합니다</p>

          <div class="category-grid">
            <button
              v-for="cat in categories" :key="cat.id"
              class="cat-card"
              :class="{ 'cat-card--selected': selectedCat === cat.id }"
              :style="selectedCat === cat.id ? { borderColor: cat.color, background: cat.color + '12' } : {}"
              @click="selectedCat = cat.id"
            >
              <div class="cat-icon" :style="{ background: cat.color + '20', color: cat.color }">
                <i :class="cat.icon" />
              </div>
              <div class="cat-label">{{ cat.label }}</div>
              <div class="cat-count">{{ cat.count }}문항</div>
              <div v-if="selectedCat === cat.id" class="cat-check" :style="{ background: cat.color }">
                <i class="fas fa-check" />
              </div>
            </button>
          </div>

          <div class="select-options">
            <label class="opt-label">문항 수</label>
            <div class="count-tabs">
              <button v-for="n in [5, 10, 15]" :key="n"
                class="count-tab" :class="{ active: quizCount === n }"
                @click="quizCount = n">{{ n }}문항</button>
            </div>
            <label class="opt-label" style="margin-top:12px">난이도</label>
            <div class="count-tabs">
              <button v-for="lv in levels" :key="lv.value"
                class="count-tab" :class="{ active: difficulty === lv.value }"
                @click="difficulty = lv.value">{{ lv.label }}</button>
            </div>
          </div>

          <button class="btn-start" :disabled="!selectedCat" @click="startQuiz">
            <i class="fas fa-play" /> 퀴즈 시작
          </button>
        </div>
      </div>

      <!-- 퀴즈 진행 -->
      <div v-else class="quiz-screen fade-in">
        <div class="quiz-inner">
          <!-- 진행 표시 -->
          <div class="quiz-progress-bar">
            <div class="progress-fill" :style="{ width: ((current + 1) / quizSet.length * 100) + '%' }" />
          </div>
          <div class="quiz-meta">
            <span class="quiz-num">{{ current + 1 }} / {{ quizSet.length }}</span>
            <span class="quiz-cat-badge" :style="{ color: currentCatColor, background: currentCatColor + '18' }">
              {{ currentCatLabel }}
            </span>
            <span class="quiz-timer" :class="{ 'timer-warn': timeLeft <= 10 }">
              <i class="fas fa-stopwatch" /> {{ timeLeft }}s
            </span>
          </div>

          <!-- 문제 -->
          <div class="quiz-card">
            <div class="quiz-q-num">Q{{ current + 1 }}</div>
            <div class="quiz-question">{{ quizSet[current].q }}</div>

            <!-- 코드 블록 (있으면) -->
            <div v-if="quizSet[current].code" class="quiz-code">
              <pre>{{ quizSet[current].code }}</pre>
            </div>

            <!-- 선택지 -->
            <div class="quiz-options">
              <button
                v-for="(opt, i) in quizSet[current].options" :key="i"
                class="quiz-opt"
                :class="optClass(i)"
                :disabled="answered"
                @click="answer(i)"
              >
                <span class="opt-idx">{{ ['A','B','C','D'][i] }}</span>
                <span class="opt-text">{{ opt }}</span>
                <i v-if="answered && i === quizSet[current].answer" class="fas fa-check opt-icon opt-icon--ok" />
                <i v-else-if="answered && i === selectedOption && i !== quizSet[current].answer" class="fas fa-times opt-icon opt-icon--no" />
              </button>
            </div>

            <!-- 해설 -->
            <Transition name="explain-slide">
              <div v-if="answered" class="quiz-explain" :class="isCorrect ? 'explain--correct' : 'explain--wrong'">
                <div class="explain-header">
                  <i :class="isCorrect ? 'fas fa-check-circle' : 'fas fa-times-circle'" />
                  {{ isCorrect ? '정답입니다!' : '오답입니다' }}
                </div>
                <p>{{ quizSet[current].explain }}</p>
              </div>
            </Transition>
          </div>

          <!-- 다음 버튼 -->
          <div class="quiz-nav">
            <button v-if="!answered" class="btn-skip" @click="skip">건너뛰기</button>
            <button v-else class="btn-next" @click="next">
              {{ current < quizSet.length - 1 ? '다음 문제' : '결과 보기' }}
              <i class="fas fa-arrow-right" />
            </button>
          </div>
        </div>
      </div>

      <!-- ── 캘린더 등록 확인 모달 ── -->
      <Teleport to="body">
        <Transition name="modal-fade">
          <div v-if="showCalModal" class="cal-modal-overlay" @click.self="showCalModal = false">
            <div class="cal-modal-box">
              <template v-if="!calRegistered">
                <button class="cal-modal-close" @click="showCalModal = false">
                  <i class="fas fa-times" />
                </button>
                <div class="cal-modal-icon-pre">
                  <i class="fas fa-calendar-plus" />
                </div>
                <h3 class="cal-modal-title">캘린더에 등록</h3>
                <p class="cal-modal-sub">퀴즈 학습 활동을 학습 캘린더에 기록합니다</p>
                <div class="cal-form">
                  <div class="cal-form-row">
                    <label>날짜</label>
                    <input type="date" v-model="calDate" class="cal-input" />
                  </div>
                  <div class="cal-form-row">
                    <label>시간</label>
                    <input type="time" v-model="calTime" class="cal-input" />
                  </div>
                  <div class="cal-form-row">
                    <label>트랙</label>
                    <select v-model="calTrack" class="cal-input">
                      <option value="main">CS 기초</option>
                      <option value="algo">알고리즘</option>
                      <option value="react">React</option>
                      <option value="portfolio">포트폴리오</option>
                    </select>
                  </div>
                  <div class="cal-form-row">
                    <label>메모</label>
                    <input type="text" v-model="calMemo" class="cal-input" :placeholder="`퀴즈 ${scorePercent}점`" />
                  </div>
                </div>
                <button class="cal-modal-btn" @click="registerToCalendar">
                  <i class="fas fa-calendar-check" /> 등록하기
                </button>
              </template>

              <template v-else>
                <!-- 이미지 1 스타일: 큰 검정 원 체크 + 굵은 텍스트 + 검정 버튼 -->
                <div class="modal-check-circle">
                  <i class="fas fa-check" />
                </div>
                <h3 class="modal-done-title">첫 학습 활동이 등록되었어요!</h3>
                <p class="modal-done-sub">캘린더에서 학습 노드를 확인하세요</p>
                <RouterLink to="/calendar" class="modal-done-btn" @click="showCalModal = false">
                  확인
                </RouterLink>
              </template>
            </div>
          </div>
        </Transition>
      </Teleport>

    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
const calendarStore = useCalendarStore()

// ── 카테고리 ──
const categories = [
  { id: 'cs',   label: 'CS 기초',    icon: 'fas fa-microchip',     color: '#0ea5e9', count: 30 },
  { id: 'algo', label: '알고리즘',   icon: 'fas fa-code-branch',   color: '#ec4899', count: 25 },
  { id: 'web',  label: '웹 개발',    icon: 'fas fa-globe',         color: '#22c55e', count: 28 },
  { id: 'db',   label: '데이터베이스', icon: 'fas fa-database',    color: '#f97316', count: 20 },
  { id: 'net',  label: '네트워크',   icon: 'fas fa-network-wired', color: '#a855f7', count: 22 },
  { id: 'os',   label: '운영체제',   icon: 'fas fa-server',        color: '#14b8a6', count: 18 },
]
const levels = [
  { label: '쉬움',   value: 'easy'   },
  { label: '보통',   value: 'normal' },
  { label: '어려움', value: 'hard'   },
]

// ── 퀴즈 DB (카테고리별) ──
const QUIZ_DB = {
  cs: [
    { q: 'CPU의 구성 요소가 아닌 것은?', options: ['ALU', '레지스터', '캐시 메모리', '하드디스크'], answer: 3, explain: '하드디스크는 보조기억장치로, CPU 내부 구성 요소가 아닙니다.' },
    { q: '스택(Stack)의 특징은?', options: ['FIFO', 'LIFO', '랜덤 접근', '양방향 삽입'], answer: 1, explain: '스택은 Last In First Out(LIFO) 구조입니다. 가장 나중에 넣은 데이터가 가장 먼저 나옵니다.' },
    { q: '다음 중 선형 자료구조가 아닌 것은?', options: ['배열', '스택', '트리', '큐'], answer: 2, explain: '트리는 계층적 비선형 자료구조입니다. 배열, 스택, 큐는 선형 자료구조입니다.' },
    { q: '시간복잡도 O(n log n)에 해당하는 정렬 알고리즘은?', options: ['버블 정렬', '선택 정렬', '합병 정렬', '삽입 정렬'], answer: 2, explain: '합병 정렬(Merge Sort)의 평균 시간복잡도는 O(n log n)입니다.' },
    { q: 'Big-O 표기법에서 가장 빠른 것은?', options: ['O(n)', 'O(log n)', 'O(n²)', 'O(1)'], answer: 3, explain: 'O(1)은 상수 시간으로 입력 크기에 관계없이 항상 일정한 시간이 걸립니다.' },
    { q: '연결 리스트(Linked List)의 단점은?', options: ['삽입/삭제가 느리다', '메모리 낭비가 크다', '인덱스 접근이 O(n)이다', '크기 변경이 불가능하다'], answer: 2, explain: '연결 리스트는 포인터로 연결되므로 특정 인덱스 접근 시 O(n)의 시간이 필요합니다.' },
  ],
  algo: [
    { q: '이진 탐색(Binary Search)의 시간복잡도는?', options: ['O(n)', 'O(log n)', 'O(n log n)', 'O(1)'], answer: 1, explain: '이진 탐색은 매 단계마다 탐색 범위를 절반으로 줄이므로 O(log n)입니다.' },
    { q: 'BFS에서 사용하는 자료구조는?', options: ['스택', '큐', '힙', '트리'], answer: 1, explain: 'BFS(너비 우선 탐색)는 큐(Queue)를 사용하여 같은 레벨의 노드를 먼저 탐색합니다.' },
    { q: '다이나믹 프로그래밍의 핵심 조건이 아닌 것은?', options: ['최적 부분 구조', '중복 부분 문제', '그리디 선택 속성', '메모이제이션'], answer: 2, explain: '그리디 선택 속성은 그리디 알고리즘의 특성입니다. DP의 핵심은 최적 부분 구조와 중복 부분 문제입니다.' },
    { q: '퀵 정렬의 최악 시간복잡도는?', options: ['O(n log n)', 'O(n)', 'O(n²)', 'O(log n)'], answer: 2, explain: '퀵 정렬은 피벗 선택이 불균형할 경우 최악 O(n²)이 됩니다. 이미 정렬된 배열이 대표적입니다.' },
    { q: '다음 중 안정 정렬(Stable Sort)이 아닌 것은?', options: ['합병 정렬', '삽입 정렬', '퀵 정렬', '버블 정렬'], answer: 2, explain: '퀵 정렬은 동일한 값의 원소 순서를 보장하지 않는 불안정 정렬입니다.' },
  ],
  web: [
    { q: 'HTTP와 HTTPS의 차이점은?', options: ['속도', '포트 번호만 다름', 'SSL/TLS 암호화 여부', '프로토콜 버전'], answer: 2, explain: 'HTTPS는 SSL/TLS 암호화를 사용하여 데이터를 안전하게 전송합니다.' },
    { q: 'REST API에서 리소스 생성에 사용되는 HTTP 메서드는?', options: ['GET', 'PUT', 'POST', 'DELETE'], answer: 2, explain: 'POST는 서버에 새로운 리소스를 생성하는 HTTP 메서드입니다.' },
    { q: 'Vue 3에서 반응형 데이터를 선언하는 방법은?', options: ['data()', 'ref() / reactive()', 'useState()', 'observable()'], answer: 1, explain: 'Vue 3 Composition API에서는 ref()와 reactive()를 사용하여 반응형 데이터를 선언합니다.' },
    {
      q: '다음 코드의 출력 결과는?',
      code: `console.log(typeof null);\nconsole.log(null instanceof Object);`,
      options: ['object, true', 'null, false', 'object, false', 'undefined, false'],
      answer: 2,
      explain: 'typeof null은 역사적 버그로 "object"를 반환하지만, null instanceof Object는 false입니다.'
    },
    { q: 'CSS Flexbox에서 주축 방향을 설정하는 속성은?', options: ['align-items', 'justify-content', 'flex-direction', 'flex-wrap'], answer: 2, explain: 'flex-direction은 flex 컨테이너의 주축 방향을 row 또는 column으로 설정합니다.' },
  ],
  db: [
    { q: 'SQL에서 중복 제거에 사용하는 키워드는?', options: ['UNIQUE', 'DISTINCT', 'ONLY', 'FILTER'], answer: 1, explain: 'SELECT DISTINCT를 사용하면 결과에서 중복된 행을 제거할 수 있습니다.' },
    { q: '트랜잭션의 ACID 속성 중 "A"가 의미하는 것은?', options: ['Availability', 'Atomicity', 'Accuracy', 'Asynchrony'], answer: 1, explain: 'Atomicity(원자성)는 트랜잭션의 모든 연산이 완전히 수행되거나 전혀 수행되지 않아야 함을 의미합니다.' },
    { q: 'NoSQL 데이터베이스의 특징이 아닌 것은?', options: ['수평적 확장', '스키마 유연성', 'ACID 보장', '다양한 데이터 모델'], answer: 2, explain: 'NoSQL은 일반적으로 완전한 ACID를 보장하지 않고 BASE(Basically Available, Soft state, Eventually consistent)를 따릅니다.' },
    { q: '인덱스(Index)에 대한 설명으로 옳은 것은?', options: ['항상 성능을 향상시킨다', '쓰기 성능을 향상시킨다', '읽기 성능을 향상시키지만 쓰기 비용이 증가한다', '저장 공간을 줄인다'], answer: 2, explain: '인덱스는 검색 속도를 높이지만, 인덱스 자체의 저장 공간과 삽입/수정/삭제 시 인덱스 업데이트 비용이 추가됩니다.' },
  ],
  net: [
    { q: 'TCP와 UDP의 차이점으로 옳은 것은?', options: ['UDP가 더 빠르지만 신뢰성이 낮다', 'TCP가 더 빠르다', '둘 다 연결 지향이다', 'UDP가 더 신뢰성이 높다'], answer: 0, explain: 'UDP는 연결을 맺지 않아 빠르지만 데이터 전달을 보장하지 않습니다. TCP는 신뢰성 있는 연결 지향 프로토콜입니다.' },
    { q: 'HTTP 상태 코드 404의 의미는?', options: ['서버 오류', '리다이렉션', '요청 성공', '리소스를 찾을 수 없음'], answer: 3, explain: '404 Not Found는 요청한 리소스가 서버에 존재하지 않을 때 반환됩니다.' },
    { q: 'OSI 7계층 중 라우팅을 담당하는 계층은?', options: ['데이터 링크 계층', '네트워크 계층', '전송 계층', '세션 계층'], answer: 1, explain: '네트워크 계층(3계층)에서 IP를 이용한 라우팅이 수행됩니다.' },
  ],
  os: [
    { q: '프로세스와 스레드의 차이점으로 옳은 것은?', options: ['스레드는 독립적인 메모리를 가진다', '프로세스는 메모리를 공유한다', '스레드는 프로세스 내 실행 단위다', '프로세스 생성이 더 빠르다'], answer: 2, explain: '스레드는 프로세스 내에서 실행되는 경량 실행 단위로, 같은 프로세스의 스레드들은 메모리를 공유합니다.' },
    { q: '교착상태(Deadlock)의 필요 조건이 아닌 것은?', options: ['상호 배제', '점유와 대기', '선점', '순환 대기'], answer: 2, explain: '교착상태의 필요 조건은 상호 배제, 점유와 대기, 비선점, 순환 대기입니다. "선점"이 허용되면 교착상태가 발생하지 않습니다.' },
    { q: '페이지 교체 알고리즘 중 최적 알고리즘은?', options: ['FIFO', 'LRU', 'OPT', 'LFU'], answer: 2, explain: 'OPT(Optimal)는 앞으로 가장 오랫동안 사용되지 않을 페이지를 교체하는 이론적으로 최적인 알고리즘입니다.' },
  ],
}

// ── 상태 ──
const phase        = ref('select')
const selectedCat  = ref('cs')
const quizCount    = ref(10)
const difficulty   = ref('normal')
const quizSet      = ref([])
const current      = ref(0)
const selectedOption = ref(null)
const answered     = ref(false)
const isCorrect    = ref(false)
const correct      = ref(0)
const wrongAnswers = ref([])
const timeLeft     = ref(30)
let timerInterval  = null

const currentCatColor = computed(() => categories.find(c => c.id === selectedCat.value)?.color || '#818cf8')
const currentCatLabel = computed(() => categories.find(c => c.id === selectedCat.value)?.label || '')

function startQuiz() {
  const pool = QUIZ_DB[selectedCat.value] || []
  const shuffled = [...pool].sort(() => Math.random() - 0.5)
  quizSet.value = shuffled.slice(0, Math.min(quizCount.value, shuffled.length))
  current.value = 0
  correct.value = 0
  wrongAnswers.value = []
  answered.value = false
  selectedOption.value = null
  phase.value = 'quiz'
  startTimer()
}

function startTimer() {
  clearInterval(timerInterval)
  timeLeft.value = 30
  timerInterval = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) { clearInterval(timerInterval); skip() }
  }, 1000)
}

function answer(i) {
  if (answered.value) return
  clearInterval(timerInterval)
  selectedOption.value = i
  answered.value = true
  const q = quizSet.value[current.value]
  isCorrect.value = i === q.answer
  if (isCorrect.value) {
    correct.value++
  } else {
    wrongAnswers.value.push({ q: q.q, my: q.options[i], answer: q.options[q.answer], explain: q.explain })
  }
}

function skip() {
  if (answered.value) return
  clearInterval(timerInterval)
  const q = quizSet.value[current.value]
  wrongAnswers.value.push({ q: q.q, my: '(시간 초과)', answer: q.options[q.answer], explain: q.explain })
  answered.value = true
  isCorrect.value = false
  selectedOption.value = -1
}

function next() {
  if (current.value < quizSet.value.length - 1) {
    current.value++
    answered.value = false
    selectedOption.value = null
    startTimer()
  } else {
    clearInterval(timerInterval)
    phase.value = 'result'
  }
}

function restart() {
  startQuiz()
}

function optClass(i) {
  if (!answered.value) return ''
  if (i === quizSet.value[current.value].answer) return 'opt--correct'
  if (i === selectedOption.value) return 'opt--wrong'
  return 'opt--dimmed'
}

const scorePercent = computed(() => Math.round(correct.value / (quizSet.value.length || 1) * 100))
const scoreDash    = computed(() => scorePercent.value / 100 * 314)
const scoreColor   = computed(() => scorePercent.value >= 80 ? '#10b981' : scorePercent.value >= 50 ? '#f59e0b' : '#ef4444')
const resultEmoji  = computed(() => scorePercent.value >= 80 ? '🎉' : scorePercent.value >= 50 ? '💪' : '📚')
const resultTitle  = computed(() => scorePercent.value >= 80 ? '훌륭해요!' : scorePercent.value >= 50 ? '잘 하고 있어요!' : '더 공부가 필요해요')

// ── 캘린더 등록 ──
const showCalModal  = ref(false)
const calRegistered = ref(false)
const calDate = ref(new Date().toISOString().slice(0, 10))
const calTime = ref('09:00')
const calTrack = ref('main')
const calMemo  = ref('')

function registerToCalendar() {
  const title = calMemo.value.trim() || `퀴즈 ${scorePercent.value}점 · ${correct.value}/${quizSet.value.length} 정답`
  const catLabel = categories.find(c => c.id === selectedCat.value)?.label || '퀴즈'
  calendarStore.createSchedule({
    day:   calDate.value,
    track: calTrack.value,
    text:  title,
    tooltip: {
      time:  calTime.value,
      title: `[퀴즈] ${catLabel} ${scorePercent.value}점`,
      tags:  ['#퀴즈', `#${catLabel}`],
    },
  })
  calRegistered.value = true
}

watch(showCalModal, (v) => { if (!v) setTimeout(() => { calRegistered.value = false }, 300) })

onUnmounted(() => clearInterval(timerInterval))
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); color: var(--text-primary); font-family: 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.page-header { height: 64px; background: var(--bg-surface); border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; padding: 0 28px; flex-shrink: 0; }
.page-title { font-weight: 700; font-size: 15px; color: var(--text-primary); }
.header-right { display: flex; align-items: center; gap: 12px; }
.icon-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); font-size: 16px; }
.avatar-btn { width: 32px; height: 32px; border-radius: 50%; text-decoration: none; background: linear-gradient(135deg, #818cf8, #38bdf8); display: flex; align-items: center; justify-content: center; font-size: 13px; color: #fff; font-weight: 700; }

@keyframes fadeIn { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:translateY(0)} }
.fade-in { animation: fadeIn 0.3s ease both; }

/* ── 카테고리 선택 ── */
.select-screen { flex: 1; overflow-y: auto; display: flex; justify-content: center; padding: 32px 24px; scrollbar-width: thin; }
.select-inner { width: 100%; max-width: 700px; }
.select-title { font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; }
.select-sub   { font-size: 13px; color: var(--text-muted); margin-bottom: 28px; }

.category-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; margin-bottom: 28px; }
.cat-card {
  background: var(--bg-surface); border: 2px solid var(--border); border-radius: 14px;
  padding: 20px 18px; cursor: pointer; transition: all 0.2s; text-align: left;
  position: relative; display: flex; flex-direction: column; gap: 10px;
  font-family: 'Escoredream', sans-serif;
}
.cat-card:hover { border-color: var(--border-mid); transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
.cat-card--selected { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.cat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.cat-label { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.cat-count { font-size: 11px; color: var(--text-faint); }
.cat-check { position: absolute; top: 12px; right: 12px; width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; color: #fff; }

.select-options { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 14px; padding: 20px 22px; margin-bottom: 24px; }
.opt-label { font-size: 12px; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 10px; }
.count-tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.count-tab { padding: 7px 18px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg-elevated); color: var(--text-muted); font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif; }
.count-tab.active { background: rgba(129,140,248,0.15); border-color: #818cf8; color: #818cf8; }

.btn-start { width: 100%; padding: 15px; border-radius: 12px; border: none; cursor: pointer; background: linear-gradient(135deg, #818cf8, #38bdf8); color: #fff; font-weight: 700; font-size: 15px; display: flex; align-items: center; justify-content: center; gap: 10px; transition: opacity 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-start:hover:not(:disabled) { opacity: 0.87; }
.btn-start:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 퀴즈 진행 ── */
.quiz-screen { flex: 1; overflow-y: auto; display: flex; justify-content: center; padding: 24px 24px 40px; scrollbar-width: thin; }
.quiz-inner { width: 100%; max-width: 640px; display: flex; flex-direction: column; gap: 16px; }

.quiz-progress-bar { height: 4px; background: var(--bg-elevated); border-radius: 2px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #818cf8, #38bdf8); border-radius: 2px; transition: width 0.4s ease; }

.quiz-meta { display: flex; align-items: center; gap: 10px; }
.quiz-num { font-size: 13px; font-weight: 700; color: var(--text-muted); }
.quiz-cat-badge { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px; }
.quiz-timer { margin-left: auto; font-size: 13px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 5px; transition: color 0.3s; }
.timer-warn { color: #ef4444 !important; animation: timerPulse 0.5s infinite alternate; }
@keyframes timerPulse { to { opacity: 0.5; } }

.quiz-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 18px; padding: 28px 28px 24px; }
.quiz-q-num { font-size: 11px; font-weight: 800; color: #818cf8; margin-bottom: 10px; letter-spacing: 0.08em; }
.quiz-question { font-size: 17px; font-weight: 700; color: var(--text-primary); line-height: 1.55; margin-bottom: 20px; }

.quiz-code { background: var(--bg-base); border: 1px solid var(--border); border-radius: 10px; padding: 14px 16px; margin-bottom: 20px; overflow-x: auto; }
.quiz-code pre { font-size: 13px; color: var(--text-secondary); font-family: 'Consolas', monospace; line-height: 1.6; margin: 0; }

.quiz-options { display: flex; flex-direction: column; gap: 10px; }
.quiz-opt {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-radius: 12px;
  border: 1.5px solid var(--border); background: var(--bg-elevated);
  color: var(--text-secondary); cursor: pointer; text-align: left;
  transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.quiz-opt:hover:not(:disabled) { border-color: #818cf8; background: rgba(129,140,248,0.08); color: var(--text-primary); }
.quiz-opt:disabled { cursor: default; }
.opt--correct { border-color: #10b981 !important; background: rgba(16,185,129,0.1) !important; color: #10b981 !important; }
.opt--wrong   { border-color: #ef4444 !important; background: rgba(239,68,68,0.08) !important; color: #ef4444 !important; }
.opt--dimmed  { opacity: 0.45; }
.opt-idx { width: 28px; height: 28px; border-radius: 8px; background: var(--bg-base); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; flex-shrink: 0; border: 1px solid var(--border); }
.opt-text { flex: 1; font-size: 14px; font-weight: 500; }
.opt-icon { margin-left: auto; font-size: 16px; }
.opt-icon--ok { color: #10b981; } .opt-icon--no { color: #ef4444; }

.quiz-explain { margin-top: 18px; border-radius: 12px; padding: 16px 18px; }
.quiz-explain--correct { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.3); }
.explain--correct { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.3); }
.explain--wrong   { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.25); }
.explain-header { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 14px; margin-bottom: 8px; }
.explain--correct .explain-header { color: #10b981; }
.explain--wrong   .explain-header { color: #ef4444; }
.quiz-explain p { font-size: 13px; color: var(--text-secondary); line-height: 1.65; }

.explain-slide-enter-active { transition: all 0.25s ease; }
.explain-slide-enter-from { opacity: 0; transform: translateY(-6px); }

.quiz-nav { display: flex; justify-content: flex-end; gap: 10px; }
.btn-skip { padding: 11px 20px; border-radius: 10px; border: 1px solid var(--border); background: transparent; color: var(--text-faint); font-size: 13px; cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-skip:hover { background: var(--bg-hover); color: var(--text-muted); }
.btn-next { padding: 11px 22px; border-radius: 10px; border: none; background: linear-gradient(135deg, #818cf8, #38bdf8); color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: opacity 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-next:hover { opacity: 0.87; }

/* ── 결과 화면 ── */
.result-screen { flex: 1; overflow-y: auto; display: flex; justify-content: center; padding: 32px 24px 48px; scrollbar-width: thin; }
.result-card { width: 100%; max-width: 540px; background: var(--bg-surface); border: 1px solid var(--border); border-radius: 20px; padding: 40px 36px; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.result-emoji { font-size: 52px; }
.result-title { font-size: 26px; font-weight: 800; color: var(--text-primary); }
.result-sub   { font-size: 14px; color: var(--text-muted); }

.result-score-ring { width: 160px; }
.score-svg { width: 100%; }
.score-circle { transition: stroke-dasharray 1s ease; }
.score-big { font-size: 24px; font-weight: 800; fill: var(--text-primary); }
.score-small { font-size: 10px; fill: var(--text-faint); }

.result-breakdown { display: flex; gap: 16px; }
.breakdown-item { display: flex; align-items: center; gap: 7px; font-size: 14px; font-weight: 700; padding: 8px 18px; border-radius: 10px; }
.breakdown-correct { background: rgba(16,185,129,0.12); color: #10b981; }
.breakdown-wrong   { background: rgba(239,68,68,0.1);  color: #ef4444; }

.wrong-note { width: 100%; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 14px; padding: 18px 20px; }
.wrong-note-title { font-weight: 700; font-size: 13px; color: var(--text-primary); margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.wrong-note-title i { color: #818cf8; }
.wrong-item { padding: 12px 0; border-bottom: 1px solid var(--border); }
.wrong-item:last-child { border-bottom: none; padding-bottom: 0; }
.wrong-q { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.wrong-answer { display: flex; gap: 16px; margin-bottom: 6px; flex-wrap: wrap; }
.wrong-my      { font-size: 12px; color: #ef4444; display: flex; align-items: center; gap: 5px; }
.wrong-correct { font-size: 12px; color: #10b981; display: flex; align-items: center; gap: 5px; }
.wrong-explain { font-size: 12px; color: var(--text-faint); line-height: 1.6; background: var(--bg-surface); border-radius: 8px; padding: 8px 12px; }

.result-actions { display: flex; gap: 12px; width: 100%; }
.btn-retry { flex: 1; padding: 13px; border-radius: 12px; border: none; cursor: pointer; background: linear-gradient(135deg, #818cf8, #38bdf8); color: #fff; font-weight: 700; font-size: 14px; display: flex; align-items: center; justify-content: center; gap: 8px; transition: opacity 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-retry:hover { opacity: 0.87; }
.btn-category { flex: 1; padding: 13px; border-radius: 12px; border: 1px solid var(--border); cursor: pointer; background: var(--bg-elevated); color: var(--text-muted); font-weight: 600; font-size: 14px; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-category:hover { background: var(--bg-hover); color: var(--text-primary); }

/* ── 캘린더 등록 버튼 ── */
.btn-register-cal {
  width: 100%; padding: 13px; border-radius: 12px; border: 1.5px dashed rgba(129,140,248,0.5);
  background: rgba(129,140,248,0.07); color: #818cf8;
  font-size: 14px; font-weight: 700; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 9px;
  transition: all 0.18s; font-family: 'Escoredream', sans-serif;
}
.btn-register-cal:hover { background: rgba(129,140,248,0.14); border-style: solid; }

/* ── 캘린더 모달 ── */
.cal-modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
  backdrop-filter: blur(6px);
}
@keyframes boxSlideUp { from{opacity:0;transform:translateY(14px) scale(0.96)} to{opacity:1;transform:translateY(0) scale(1)} }
.cal-modal-box {
  width: 100%; max-width: 420px;
  background: var(--modal-bg, var(--bg-surface));
  border: 1px solid var(--modal-border, var(--border));
  border-radius: 22px;
  padding: 36px 32px;
  box-shadow: 0 24px 60px rgba(0,0,0,0.2);
  animation: boxSlideUp 0.28s cubic-bezier(0.34,1.56,0.64,1) both;
  position: relative;
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  font-family: 'Escoredream', sans-serif;
}
.cal-modal-close {
  position: absolute; top: 16px; right: 16px;
  width: 30px; height: 30px; border-radius: 8px;
  background: var(--bg-hover); border: 1px solid var(--border);
  color: var(--text-faint); cursor: pointer; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.cal-modal-close:hover { color: var(--text-primary); background: var(--bg-elevated); }

.cal-modal-icon-pre {
  width: 52px; height: 52px; border-radius: 14px; margin-bottom: 4px;
  background: rgba(129,140,248,0.15);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #818cf8;
}
.cal-modal-title { font-size: 18px; font-weight: 800; color: var(--text-primary); }
.cal-modal-sub   { font-size: 12px; color: var(--text-muted); margin-bottom: 8px; }

.cal-form { width: 100%; display: flex; flex-direction: column; gap: 10px; margin-bottom: 4px; }
.cal-form-row { display: flex; flex-direction: column; gap: 5px; }
.cal-form-row label { font-size: 11px; font-weight: 700; color: var(--text-muted); }
.cal-input {
  padding: 10px 13px; border-radius: 9px;
  background: var(--modal-input-bg); border: 1px solid var(--modal-border);
  color: var(--text-primary); font-size: 13px; outline: none;
  font-family: 'Escoredream', sans-serif; transition: border-color 0.15s;
  width: 100%;
}
.cal-input:focus { border-color: #818cf8; }

.cal-modal-btn {
  width: 100%; padding: 13px; border-radius: 12px; border: none; cursor: pointer;
  background: #1a1a1a; color: #fff;
  font-weight: 700; font-size: 15px; margin-top: 4px;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.cal-modal-btn:hover { opacity: 0.85; }

/* 등록 완료 (이미지 1 스타일) */
.cal-done-icon {
  width: 64px; height: 64px; border-radius: 50%; margin-bottom: 8px;
  background: #1a1a1a;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #fff;
}
.cal-done-title { font-size: 20px; font-weight: 800; color: var(--text-primary); text-align: center; }
.cal-done-sub   { font-size: 13px; color: var(--text-muted); text-align: center; margin-bottom: 8px; }
.cal-done-btn {
  display: block; width: 100%; padding: 14px; border-radius: 12px;
  background: #1a1a1a; color: #fff; text-align: center;
  font-weight: 700; font-size: 15px; text-decoration: none;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.cal-done-btn:hover { opacity: 0.85; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

/* 완료 화면 - 이미지 1 스타일 */
.modal-check-circle {
  width: 72px; height: 72px; border-radius: 50%;
  background: #1a1a1a;
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; color: #fff;
  margin-bottom: 10px;
  box-shadow: 0 8px 28px rgba(0,0,0,0.3);
  animation: popIn 0.3s cubic-bezier(0.34,1.56,0.64,1) both;
}
@keyframes popIn { from{opacity:0;transform:scale(0.7)} to{opacity:1;transform:scale(1)} }

.modal-done-title {
  font-size: 20px; font-weight: 800;
  color: var(--modal-text, #111);
  text-align: center; line-height: 1.3;
}
.modal-done-sub {
  font-size: 13px; color: var(--modal-sub, #6b7280);
  text-align: center; margin-bottom: 8px;
}
.modal-done-btn {
  display: block; width: 100%; padding: 15px;
  border-radius: 12px; background: #1a1a1a; color: #fff;
  text-align: center; font-size: 16px; font-weight: 700;
  text-decoration: none; transition: opacity 0.15s;
  font-family: 'Escoredream', sans-serif; margin-top: 4px;
}
.modal-done-btn:hover { opacity: 0.85; }

</style>
