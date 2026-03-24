package com.ssafy.springbootbe.domain.quizzes.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizAnswerSubmitRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizGenerateAsyncRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizAnswerSubmitResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizGenerateAsyncResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionCachePayload;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionCompleteResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAlreadyCompletedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAccessDeniedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAnswerConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizIncompleteException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizQuestionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionConflictException;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.quiz.entity.QuizQuestion;
import com.ssafy.springbootbe.persistence.quiz.entity.QuizSession;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizQuestionRepository;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizSessionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizzesServiceImplTest {

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private QuizSessionRepository quizSessionRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private ActivityHistoryRepository activityHistoryRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    @Mock private TechStackRepository techStackRepository;
    @Mock private RedisService redisService;
    @Mock private AIRestClient aiRestClient;

    private QuizzesServiceImpl quizzesService;
    private ObjectMapper objectMapper;

    private User user;
    private Curriculum curriculum;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        quizzesService = spy(new QuizzesServiceImpl(
                curriculumRepository,
                quizSessionRepository,
                quizQuestionRepository,
                userTechStackRepository,
                activityHistoryRepository,
                activityHistoryTechStackRepository,
                techStackRepository,
                redisService,
                aiRestClient,
                objectMapper
        ));

        ReflectionTestUtils.setField(quizzesService, "aiServerUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(
                quizzesService,
                "aiQuizzesGenerateAsyncPath",
                "/api/v1/ai/quizzes/generate-async"
        );

        user = User.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("kairos")
                .position(UserPosition.JOB_SEEKER)
                .build();

        curriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(user)
                .status(CurriculumStatus.ACTIVE)
                .duration(30)
                .createdAt(LocalDateTime.of(2026, 3, 23, 12, 0))
                .build();
    }

    @Test
    void startSession_curriculumId만으로_정상_세션을_시작한다() throws Exception {
        // given
        stubOwnedCurriculum();
        stubNoInProgressStartSession();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        QuizSessionStartResponse response = quizzesService.startSession(1L, new QuizSessionStartRequest(10L));
        String serialized = objectMapper.writeValueAsString(response);

        // then
        assertThat(response.getCurriculumId()).isEqualTo(10L);
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getTitle()).isEqualTo("Spring 핵심 개념 점검 퀴즈");
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().getFirst().getQuestionNumber()).isEqualTo(1);
        assertThat(response.getQuestions().getFirst().getQuizType()).isEqualTo("MULTIPLE_CHOICE");
        assertThat(serialized).doesNotContain("correctAnswer");
        verify(redisService).save(
                eq("quiz_session:1"),
                any(String.class),
                eq(2L),
                eq(TimeUnit.HOURS)
        );
    }

    @Test
    void startSession_다른_유저의_커리큘럼이면_ACCESS_DENIED를_던진다() {
        // given
        Curriculum otherUsersCurriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(User.builder().userId(2L).email("other@gmail.com").nickname("other").build())
                .status(CurriculumStatus.ACTIVE)
                .duration(20)
                .build();
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(otherUsersCurriculum));

        // when & then
        assertThatThrownBy(() -> quizzesService.startSession(1L, new QuizSessionStartRequest(10L)))
                .isInstanceOf(QuizAccessDeniedException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void startSession_존재하지_않는_curriculumId면_NOT_FOUND를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizzesService.startSession(1L, new QuizSessionStartRequest(10L)))
                .isInstanceOf(QuizCurriculumNotFoundException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void startSession_Redis에_진행중_세션이_있으면_CONFLICT를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(redisService.hasKey("quiz_session:1")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> quizzesService.startSession(1L, new QuizSessionStartRequest(10L)))
                .isInstanceOf(QuizSessionConflictException.class)
                .hasMessageContaining("userId=1");
    }

    @Test
    void startSession_Redis_hit면_FastAPI_호출없이_응답한다() {
        // given
        stubOwnedCurriculum();
        stubNoInProgressStartSession();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        quizzesService.startSession(1L, new QuizSessionStartRequest(10L));

        // then
        verify(quizzesService, never()).requestQuizGeneration(any());
    }

    @Test
    void startSession_Redis_miss면_FastAPI_호출후_원본을_캐시한다() throws Exception {
        // given
        stubOwnedCurriculum();
        stubNoInProgressStartSession();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(null);
        given(redisService.get("recommendation:1:10")).willReturn(recommendationPayloadJson());

        QuizGenerateAsyncResponse generatedQuiz = objectMapper.readValue(
                quizPayloadJson(),
                QuizGenerateAsyncResponse.class
        );
        ArgumentCaptor<QuizGenerateAsyncRequest> requestCaptor = ArgumentCaptor.forClass(QuizGenerateAsyncRequest.class);
        doReturn(generatedQuiz).when(quizzesService).requestQuizGeneration(requestCaptor.capture());
        doReturn(12345L).when(quizzesService).calculateRecommendationQuizCacheTtlSeconds();

        // when
        quizzesService.startSession(1L, new QuizSessionStartRequest(10L));

        // then
        assertThat(requestCaptor.getValue().getCurriculumId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().getTargetTechStacks()).containsExactly("Spring", "Java");
        assertThat(requestCaptor.getValue().getUserLevel()).isEqualTo("MID");
        verify(redisService).save(
                eq("recommendation:quiz:1:10"),
                eq(quizPayloadJsonCompact()),
                eq(12345L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void startSession_응답에서_correctAnswer를_제거한다() throws Exception {
        // given
        stubOwnedCurriculum();
        stubNoInProgressStartSession();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        QuizSessionStartResponse response = quizzesService.startSession(1L, new QuizSessionStartRequest(10L));
        String serialized = objectMapper.writeValueAsString(response);

        // then
        assertThat(serialized).contains("questionNumber");
        assertThat(serialized).doesNotContain("correctAnswer");
    }

    private void stubOwnedCurriculum() {
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
    }

    private void stubNoInProgressStartSession() {
        given(redisService.hasKey("quiz_session:1")).willReturn(false);
    }

    @Test
    void startSession_세션시작시_풀이상태를_Redis에_초기화한다() throws Exception {
        // given
        stubOwnedCurriculum();
        stubNoInProgressStartSession();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        quizzesService.startSession(1L, new QuizSessionStartRequest(10L));

        // then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).save(
                eq("quiz_session:1"),
                payloadCaptor.capture(),
                eq(2L),
                eq(TimeUnit.HOURS)
        );

        QuizSessionCachePayload cachedPayload =
                objectMapper.readValue(payloadCaptor.getValue(), QuizSessionCachePayload.class);

        assertThat(cachedPayload.getCurriculumId()).isEqualTo(10L);
        assertThat(cachedPayload.getQuestions()).hasSize(2);
        assertThat(cachedPayload.getQuestions().getFirst().getCorrectAnswer()).isEqualTo("singleton");
        assertThat(cachedPayload.getQuestions().getFirst().getSelectedAnswer()).isNull();
        assertThat(cachedPayload.getQuestions().getFirst().getIsCorrect()).isNull();
    }

    @Test
    void submitAnswer_정상_답안_제출시_정답여부와_정답을_반환하고_Redis에_저장한다() throws Exception {
        // given
        stubOwnedCurriculum();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());
        given(redisService.get("quiz_session:1")).willReturn(quizSessionPayloadJson());

        // when
        QuizAnswerSubmitResponse response =
                quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(1, " singleton "));

        // then
        assertThat(response.getQuestionNumber()).isEqualTo(1);
        assertThat(response.getIsCorrect()).isTrue();
        assertThat(response.getCorrectAnswer()).isEqualTo("singleton");
        assertThat(response.getSelectedAnswer()).isEqualTo("singleton");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).save(
                eq("quiz_session:1"),
                payloadCaptor.capture(),
                eq(2L),
                eq(TimeUnit.HOURS)
        );

        QuizSessionCachePayload updatedPayload =
                objectMapper.readValue(payloadCaptor.getValue(), QuizSessionCachePayload.class);
        QuizSessionCachePayload.Question updatedQuestion = updatedPayload.getQuestions().getFirst();

        assertThat(updatedQuestion.getQuestionNumber()).isEqualTo(1);
        assertThat(updatedQuestion.getSelectedAnswer()).isEqualTo("singleton");
        assertThat(updatedQuestion.getCorrectAnswer()).isEqualTo("singleton");
        assertThat(updatedQuestion.getIsCorrect()).isTrue();
    }

    @Test
    void submitAnswer_다른_유저의_커리큘럼이면_ACCESS_DENIED를_던진다() {
        // given
        Curriculum otherUsersCurriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(User.builder().userId(2L).email("other@gmail.com").nickname("other").build())
                .status(CurriculumStatus.ACTIVE)
                .duration(20)
                .build();
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(otherUsersCurriculum));

        // when & then
        assertThatThrownBy(() -> quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(2, "singleton")))
                .isInstanceOf(QuizAccessDeniedException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void submitAnswer_존재하지_않는_curriculumId면_NOT_FOUND를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(2, "singleton")))
                .isInstanceOf(QuizCurriculumNotFoundException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void submitAnswer_존재하지_않는_questionNumber면_NOT_FOUND를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when & then
        assertThatThrownBy(() -> quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(99, "singleton")))
                .isInstanceOf(QuizQuestionNotFoundException.class)
                .hasMessageContaining("questionNumber=99");
    }

    @Test
    void submitAnswer_이미_제출한_문제면_CONFLICT를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());
        given(redisService.get("quiz_session:1")).willReturn(submittedQuizSessionPayloadJson());

        // when & then
        assertThatThrownBy(() -> quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(2, "singleton")))
                .isInstanceOf(QuizAnswerConflictException.class)
                .hasMessageContaining("questionNumber=2");
    }

    @Test
    void submitAnswer_진행중_세션이_없으면_NOT_FOUND를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());
        given(redisService.get("quiz_session:1")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> quizzesService.submitAnswer(1L, 10L, new QuizAnswerSubmitRequest(2, "singleton")))
                .isInstanceOf(QuizSessionNotFoundException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void completeSession_모든_문제_제출시_DB저장과_Redis삭제후_응답을_반환한다() throws Exception {
        // given
        stubOwnedCurriculum();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());
        given(redisService.get("quiz_session:1")).willReturn(completedQuizSessionPayloadJson());
        given(redisService.get("recommendation:1:10")).willReturn(recommendationPayloadJson());
        given(quizSessionRepository.findByUserUserIdAndCurriculumCurriculumId(1L, 10L)).willReturn(Optional.empty());
        given(quizSessionRepository.existsByUserUserIdAndCurriculumCurriculumId(1L, 10L)).willReturn(false);

        QuizSession savedQuizSession = QuizSession.builder()
                .quizSessionId(100L)
                .user(user)
                .curriculum(curriculum)
                .totalScore(0)
                .build();
        given(quizSessionRepository.save(any(QuizSession.class))).willReturn(savedQuizSession);
        given(activityHistoryRepository.save(any(ActivityHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(techStackRepository.findByTechName("Spring"))
                .willReturn(Optional.of(TechStack.builder().techStackId(1L).techName("Spring").build()));
        given(techStackRepository.findByTechName("Java"))
                .willReturn(Optional.of(TechStack.builder().techStackId(2L).techName("Java").build()));
        given(redisService.delete("quiz_session:1")).willReturn(true);
        given(redisService.delete("recommendation:quiz:1:10")).willReturn(true);

        // when
        QuizSessionCompleteResponse response = quizzesService.completeSession(1L, 10L);
        String serialized = objectMapper.writeValueAsString(response);

        // then
        assertThat(response.getCurriculumId()).isEqualTo(10L);
        assertThat(response.getTotalScore()).isEqualTo(100);
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().getFirst().getCorrectAnswer()).isEqualTo("singleton");
        assertThat(response.getResults().get(1).getSelectedAnswer()).isEqualTo("결합도를 낮춘다.");
        assertThat(serialized).contains("curriculumId", "totalScore", "correctAnswer", "selectedAnswer");

        ArgumentCaptor<QuizSession> quizSessionCaptor = ArgumentCaptor.forClass(QuizSession.class);
        verify(quizSessionRepository).save(quizSessionCaptor.capture());
        assertThat(quizSessionCaptor.getValue().getCurriculum().getCurriculumId()).isEqualTo(10L);
        assertThat(quizSessionCaptor.getValue().getTotalScore()).isEqualTo(100);

        ArgumentCaptor<List<QuizQuestion>> quizQuestionCaptor = ArgumentCaptor.forClass(List.class);
        verify(quizQuestionRepository).saveAll(quizQuestionCaptor.capture());
        assertThat(quizQuestionCaptor.getValue()).hasSize(2);
        assertThat(quizQuestionCaptor.getValue().getFirst().getQuestion()).isEqualTo("Spring Bean의 기본 스코프는?");
        assertThat(quizQuestionCaptor.getValue().getFirst().getSelectedAnswer()).isEqualTo("singleton");
        assertThat(quizQuestionCaptor.getValue().getFirst().getIsCorrect()).isTrue();

        ArgumentCaptor<ActivityHistory> activityCaptor = ArgumentCaptor.forClass(ActivityHistory.class);
        verify(activityHistoryRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getTitle()).isEqualTo("퀴즈 완료 — Spring, Java");

        ArgumentCaptor<List<ActivityHistoryTechStack>> activityTechCaptor = ArgumentCaptor.forClass(List.class);
        verify(activityHistoryTechStackRepository).saveAll(activityTechCaptor.capture());
        assertThat(activityTechCaptor.getValue()).hasSize(2);

        verify(redisService).delete("quiz_session:1");
        verify(redisService).delete("recommendation:quiz:1:10");
    }

    @Test
    void completeSession_다른_유저의_커리큘럼이면_ACCESS_DENIED를_던진다() {
        // given
        Curriculum otherUsersCurriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(User.builder().userId(2L).email("other@gmail.com").nickname("other").build())
                .status(CurriculumStatus.ACTIVE)
                .duration(20)
                .build();
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(otherUsersCurriculum));

        // when & then
        assertThatThrownBy(() -> quizzesService.completeSession(1L, 10L))
                .isInstanceOf(QuizAccessDeniedException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void completeSession_존재하지_않는_curriculumId면_NOT_FOUND를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizzesService.completeSession(1L, 10L))
                .isInstanceOf(QuizCurriculumNotFoundException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void completeSession_미제출_문제가_남아있으면_INVALID_INPUT을_던진다() {
        // given
        stubOwnedCurriculum();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());
        given(redisService.get("quiz_session:1")).willReturn(quizSessionPayloadJson());
        given(quizSessionRepository.existsByUserUserIdAndCurriculumCurriculumId(1L, 10L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> quizzesService.completeSession(1L, 10L))
                .isInstanceOf(QuizIncompleteException.class)
                .hasMessageContaining("curriculumId=10");
    }

    @Test
    void completeSession_이미_완료된_퀴즈이면_CONFLICT를_던진다() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(quizSessionRepository.existsByUserUserIdAndCurriculumCurriculumId(1L, 10L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> quizzesService.completeSession(1L, 10L))
                .isInstanceOf(QuizAlreadyCompletedException.class)
                .hasMessageContaining("curriculumId=10");
    }

    private String recommendationPayloadJson() {
        return """
                {
                  "userId": 1,
                  "curriculumId": 10,
                  "currentStatus": {
                    "summary": "학습 흐름이 좋습니다.",
                    "detail": "Spring과 Java 중심으로 이어지고 있습니다.",
                    "topSkills": ["Spring", "Java"]
                  }
                }
                """;
    }

    private String quizPayloadJson() {
        return """
                {
                  "curriculumId": 10,
                  "totalQuestions": 2,
                  "title": "Spring 핵심 개념 점검 퀴즈",
                  "description": "추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.",
                  "expectedMinutes": 15,
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "Spring Bean의 기본 스코프는?",
                      "quizType": "MULTIPLE_CHOICE",
                      "options": ["singleton", "prototype", "request", "session"],
                      "correctAnswer": "singleton"
                    },
                    {
                      "questionNumber": 2,
                      "question": "DI의 장점을 한 문장으로 설명하세요.",
                      "quizType": "SHORT_ANSWER",
                      "options": null,
                      "correctAnswer": "결합도를 낮춘다."
                    }
                  ]
                }
                """;
    }

    private String quizPayloadJsonCompact() throws Exception {
        return objectMapper.writeValueAsString(objectMapper.readValue(quizPayloadJson(), QuizGenerateAsyncResponse.class));
    }

    private String quizSessionPayloadJson() {
        return """
                {
                  "curriculumId": 10,
                  "totalQuestions": 2,
                  "title": "Spring 핵심 개념 점검 퀴즈",
                  "description": "추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.",
                  "expectedMinutes": 15,
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "Spring Bean의 기본 스코프는?",
                      "quizType": "MULTIPLE_CHOICE",
                      "options": ["singleton", "prototype", "request", "session"],
                      "correctAnswer": "singleton",
                      "selectedAnswer": null,
                      "isCorrect": null
                    },
                    {
                      "questionNumber": 2,
                      "question": "DI의 장점을 한 문장으로 설명하세요.",
                      "quizType": "SHORT_ANSWER",
                      "options": null,
                      "correctAnswer": "결합도를 낮춘다.",
                      "selectedAnswer": null,
                      "isCorrect": null
                    }
                  ]
                }
                """;
    }

    private String submittedQuizSessionPayloadJson() {
        return """
                {
                  "curriculumId": 10,
                  "totalQuestions": 2,
                  "title": "Spring 핵심 개념 점검 퀴즈",
                  "description": "추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.",
                  "expectedMinutes": 15,
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "Spring Bean의 기본 스코프는?",
                      "quizType": "MULTIPLE_CHOICE",
                      "options": ["singleton", "prototype", "request", "session"],
                      "correctAnswer": "singleton",
                      "selectedAnswer": null,
                      "isCorrect": null
                    },
                    {
                      "questionNumber": 2,
                      "question": "DI의 장점을 한 문장으로 설명하세요.",
                      "quizType": "SHORT_ANSWER",
                      "options": null,
                      "correctAnswer": "결합도를 낮춘다.",
                      "selectedAnswer": "결합도를 낮춘다.",
                      "isCorrect": true
                    }
                  ]
                }
                """;
    }

    private String completedQuizSessionPayloadJson() {
        return """
                {
                  "curriculumId": 10,
                  "totalQuestions": 2,
                  "title": "Spring 핵심 개념 점검 퀴즈",
                  "description": "추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.",
                  "expectedMinutes": 15,
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "Spring Bean의 기본 스코프는?",
                      "quizType": "MULTIPLE_CHOICE",
                      "options": ["singleton", "prototype", "request", "session"],
                      "correctAnswer": "singleton",
                      "selectedAnswer": "singleton",
                      "isCorrect": true
                    },
                    {
                      "questionNumber": 2,
                      "question": "DI의 장점을 한 문장으로 설명하세요.",
                      "quizType": "SHORT_ANSWER",
                      "options": null,
                      "correctAnswer": "결합도를 낮춘다.",
                      "selectedAnswer": "결합도를 낮춘다.",
                      "isCorrect": true
                    }
                  ]
                }
                """;
    }
}
