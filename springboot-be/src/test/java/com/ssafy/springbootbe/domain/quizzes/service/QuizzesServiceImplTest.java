package com.ssafy.springbootbe.domain.quizzes.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizGenerateAsyncRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizGenerateAsyncResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionCachePayload;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAccessDeniedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionConflictException;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizQuestionRepository;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizSessionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
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
        stubCurriculumOwnedByUser();
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
    void startSession_진행중_세션이_있으면_CONFLICT를_던진다() {
        // given
        stubCurriculumOwnedByUser();
        given(quizQuestionRepository.existsByQuizSessionUserUserIdAndSelectedAnswerIsNull(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> quizzesService.startSession(1L, new QuizSessionStartRequest(10L)))
                .isInstanceOf(QuizSessionConflictException.class)
                .hasMessageContaining("userId=1");
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
        stubCurriculumOwnedByUser();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        quizzesService.startSession(1L, new QuizSessionStartRequest(10L));

        // then
        verify(quizzesService, never()).requestQuizGeneration(any());
    }

    @Test
    void startSession_Redis_miss면_FastAPI_호출후_원본을_캐시한다() throws Exception {
        // given
        stubCurriculumOwnedByUser();
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
        stubCurriculumOwnedByUser();
        given(redisService.get("recommendation:quiz:1:10")).willReturn(quizPayloadJson());

        // when
        QuizSessionStartResponse response = quizzesService.startSession(1L, new QuizSessionStartRequest(10L));
        String serialized = objectMapper.writeValueAsString(response);

        // then
        assertThat(serialized).contains("questionNumber");
        assertThat(serialized).doesNotContain("correctAnswer");
    }

    private void stubCurriculumOwnedByUser() {
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(redisService.hasKey("quiz_session:1")).willReturn(false);
        given(quizQuestionRepository.existsByQuizSessionUserUserIdAndSelectedAnswerIsNull(1L)).willReturn(false);
    }

    @Test
    void startSession_세션시작시_풀이상태를_Redis에_초기화한다() throws Exception {
        // given
        stubCurriculumOwnedByUser();
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
}
