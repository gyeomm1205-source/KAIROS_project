package com.ssafy.springbootbe.domain.quizzes.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizAnswerSubmitRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizGenerateAsyncRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizAnswerSubmitResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizGenerateAsyncResponse;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionCachePayload;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAccessDeniedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAnswerConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizGenerationException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizPayloadParsingException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizQuestionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizRedisException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionPersistenceException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSourceNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationCachePayload;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizQuestionRepository;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizSessionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizzesServiceImpl implements QuizzesService {

    private static final String RECOMMENDATION_KEY_PREFIX = "recommendation:";
    private static final String RECOMMENDATION_QUIZ_KEY_PREFIX = "recommendation:quiz:";
    private static final String QUIZ_SESSION_KEY_PREFIX = "quiz_session:";
    private static final long QUIZ_SESSION_TTL_HOURS = 2L;
    private static final String FAST_API_LEVEL_JUNIOR = "JUNIOR";
    private static final String FAST_API_LEVEL_MID = "MID";
    private static final String FAST_API_LEVEL_SENIOR = "SENIOR";

    private final CurriculumRepository curriculumRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final RedisService redisService;
    private final AIRestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.server-url}")
    private String aiServerUrl;

    @Value("${ai.quizzes-generate-async-path:/api/v1/ai/quizzes/generate-async}")
    private String aiQuizzesGenerateAsyncPath;

    @Override
    @Transactional
    public QuizSessionStartResponse startSession(Long userId, QuizSessionStartRequest request) {
        Curriculum curriculum = findCurriculumOrThrow(request.getCurriculumId());
        validateCurriculumOwnership(userId, curriculum);
        validateNoInProgressSession(userId);

        QuizGenerateAsyncResponse quizPayload = findOrGenerateQuizPayload(userId, curriculum);
        cacheQuizSession(userId, quizPayload);

        return mapStartResponse(quizPayload);
    }

    @Override
    public QuizAnswerSubmitResponse submitAnswer(Long userId, Long curriculumId, QuizAnswerSubmitRequest request) {
        Curriculum curriculum = findCurriculumOrThrow(curriculumId);
        validateCurriculumOwnership(userId, curriculum);

        String normalizedSelectedAnswer = normalizeAnswer(request.getSelectedAnswer());
        QuizGenerateAsyncResponse quizSource = findQuizSourceOrThrow(userId, curriculumId);
        QuizGenerateAsyncResponse.Question sourceQuestion =
                findSourceQuestionOrThrow(quizSource, curriculumId, request.getQuestionNumber());
        QuizSessionCachePayload sessionPayload = findQuizSessionOrThrow(userId, curriculumId);
        QuizSessionCachePayload.Question sessionQuestion =
                findSessionQuestionOrThrow(sessionPayload, curriculumId, request.getQuestionNumber());

        validateAnswerNotSubmitted(curriculumId, sessionQuestion);

        String normalizedCorrectAnswer = normalizeAnswer(sourceQuestion.getCorrectAnswer());
        boolean isCorrect = normalizedCorrectAnswer.equals(normalizedSelectedAnswer);

        QuizSessionCachePayload updatedPayload = updateSessionPayload(
                sessionPayload,
                request.getQuestionNumber(),
                sourceQuestion.getCorrectAnswer(),
                normalizedSelectedAnswer,
                isCorrect
        );
        saveQuizSession(userId, updatedPayload);

        return QuizAnswerSubmitResponse.builder()
                .questionNumber(request.getQuestionNumber())
                .isCorrect(isCorrect)
                .correctAnswer(sourceQuestion.getCorrectAnswer())
                .selectedAnswer(normalizedSelectedAnswer)
                .build();
    }

    QuizGenerateAsyncResponse requestQuizGeneration(QuizGenerateAsyncRequest request) {
        try {
            QuizGenerateAsyncResponse response = aiRestClient.buildAiRestClient()
                    .post()
                    .uri(buildQuizGenerateUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(QuizGenerateAsyncResponse.class);

            validateQuizPayload(response);
            return response;
        } catch (RestClientException e) {
            throw new QuizGenerationException("FastAPI 퀴즈 생성 호출에 실패했습니다.", e);
        }
    }

    String buildQuizGenerateUri() {
        return aiServerUrl + aiQuizzesGenerateAsyncPath;
    }

    long calculateRecommendationQuizCacheTtlSeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1L, Duration.between(now, nextMidnight).getSeconds());
    }

    private Curriculum findCurriculumOrThrow(Long curriculumId) {
        return curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new QuizCurriculumNotFoundException(curriculumId));
    }

    private void validateCurriculumOwnership(Long userId, Curriculum curriculum) {
        if (!Objects.equals(curriculum.getUser().getUserId(), userId)) {
            throw new QuizAccessDeniedException(curriculum.getCurriculumId());
        }
    }

    private void validateNoInProgressSession(Long userId) {
        try {
            if (redisService.hasKey(buildQuizSessionKey(userId))) {
                throw new QuizSessionConflictException(userId);
            }

            if (quizQuestionRepository.existsByQuizSessionUserUserIdAndSelectedAnswerIsNull(userId)) {
                throw new QuizSessionConflictException(userId);
            }
        } catch (QuizSessionConflictException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new QuizSessionPersistenceException("진행 중 퀴즈 세션 조회에 실패했습니다. userId=" + userId, e);
        }
    }

    private QuizGenerateAsyncResponse findOrGenerateQuizPayload(Long userId, Curriculum curriculum) {
        Long curriculumId = curriculum.getCurriculumId();

        try {
            String cachedQuizPayload = redisService.get(buildRecommendationQuizKey(userId, curriculumId));
            if (cachedQuizPayload != null && !cachedQuizPayload.isBlank()) {
                return objectMapper.readValue(cachedQuizPayload, QuizGenerateAsyncResponse.class);
            }
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 원본 Redis payload 파싱에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException("퀴즈 원본 Redis 조회에 실패했습니다. curriculumId=" + curriculumId, e);
        }

        QuizGenerateAsyncRequest generateRequest = QuizGenerateAsyncRequest.builder()
                .curriculumId(curriculumId)
                .targetTechStacks(buildTargetTechStacks(userId, curriculumId))
                .userLevel(mapUserLevel(curriculum.getUser().getPosition()))
                .build();

        QuizGenerateAsyncResponse generatedQuiz = requestQuizGeneration(generateRequest);
        cacheRecommendationQuiz(userId, curriculumId, generatedQuiz);
        return generatedQuiz;
    }

    private List<String> buildTargetTechStacks(Long userId, Long curriculumId) {
        LinkedHashSet<String> techStacks = new LinkedHashSet<>();
        addRecommendationTopSkills(userId, curriculumId, techStacks);

        if (techStacks.isEmpty()) {
            userTechStackRepository.findTop6ByUserUserIdOrderByScoreDesc(userId).stream()
                    .map(UserTechStack::getTechStack)
                    .map(TechStack::getTechName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        }

        if (techStacks.isEmpty()) {
            userTechStackRepository.findByUserUserId(userId).stream()
                    .map(UserTechStack::getTechStack)
                    .map(TechStack::getTechName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        }

        if (techStacks.isEmpty()) {
            throw new QuizGenerationException("퀴즈 생성용 targetTechStacks를 구성할 수 없습니다. userId=" + userId);
        }

        return techStacks.stream().limit(5).toList();
    }

    private void addRecommendationTopSkills(Long userId, Long curriculumId, LinkedHashSet<String> techStacks) {
        try {
            String recommendationPayload = redisService.get(buildRecommendationKey(userId, curriculumId));
            if (recommendationPayload == null || recommendationPayload.isBlank()) {
                return;
            }

            RecommendationCachePayload payload =
                    objectMapper.readValue(recommendationPayload, RecommendationCachePayload.class);

            if (payload.getCurrentStatus() == null || payload.getCurrentStatus().getTopSkills() == null) {
                return;
            }

            payload.getCurrentStatus().getTopSkills().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        } catch (JacksonException e) {
            log.warn("추천 payload 파싱에 실패해 userTechStack 기반으로 대체합니다. curriculumId={}", curriculumId, e);
        } catch (RuntimeException e) {
            throw new QuizRedisException("추천 Redis 조회에 실패했습니다. curriculumId=" + curriculumId, e);
        }
    }

    private String mapUserLevel(UserPosition position) {
        if (position == null) {
            return FAST_API_LEVEL_MID;
        }

        return switch (position) {
            case STUDENT, JUNIOR -> FAST_API_LEVEL_JUNIOR;
            case SENIOR -> FAST_API_LEVEL_SENIOR;
            case JOB_SEEKER, OTHER -> FAST_API_LEVEL_MID;
        };
    }

    private void cacheRecommendationQuiz(Long userId, Long curriculumId, QuizGenerateAsyncResponse quizPayload) {
        try {
            redisService.save(
                    buildRecommendationQuizKey(userId, curriculumId),
                    objectMapper.writeValueAsString(quizPayload),
                    calculateRecommendationQuizCacheTtlSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 원본 Redis 저장 직렬화에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException("퀴즈 원본 Redis 저장에 실패했습니다. curriculumId=" + curriculumId, e);
        }
    }

    private void cacheQuizSession(Long userId, QuizGenerateAsyncResponse quizPayload) {
        try {
            QuizSessionCachePayload payload = QuizSessionCachePayload.builder()
                    .curriculumId(quizPayload.getCurriculumId())
                    .totalQuestions(quizPayload.getTotalQuestions())
                    .title(quizPayload.getTitle())
                    .description(quizPayload.getDescription())
                    .expectedMinutes(quizPayload.getExpectedMinutes())
                    .questions(quizPayload.getQuestions().stream()
                            .map(question -> QuizSessionCachePayload.Question.builder()
                                    .questionNumber(question.getQuestionNumber())
                                    .question(question.getQuestion())
                                    .quizType(question.getQuizType())
                                    .options(question.getOptions())
                                    .correctAnswer(question.getCorrectAnswer())
                                    .selectedAnswer(null)
                                    .isCorrect(null)
                                    .build())
                            .toList())
                    .build();

            redisService.save(
                    buildQuizSessionKey(userId),
                    objectMapper.writeValueAsString(payload),
                    QUIZ_SESSION_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 세션 Redis 저장 직렬화에 실패했습니다. userId=" + userId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException(
                    "퀴즈 세션 Redis 저장에 실패했습니다. userId=" + userId,
                    e
            );
        }
    }

    private QuizGenerateAsyncResponse findQuizSourceOrThrow(Long userId, Long curriculumId) {
        try {
            String cachedQuizPayload = redisService.get(buildRecommendationQuizKey(userId, curriculumId));
            if (cachedQuizPayload == null || cachedQuizPayload.isBlank()) {
                throw new QuizSourceNotFoundException(curriculumId);
            }

            return objectMapper.readValue(cachedQuizPayload, QuizGenerateAsyncResponse.class);
        } catch (QuizSourceNotFoundException e) {
            throw e;
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 원본 Redis payload 파싱에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException("퀴즈 원본 Redis 조회에 실패했습니다. curriculumId=" + curriculumId, e);
        }
    }

    private QuizSessionCachePayload findQuizSessionOrThrow(Long userId, Long curriculumId) {
        try {
            String cachedSessionPayload = redisService.get(buildQuizSessionKey(userId));
            if (cachedSessionPayload == null || cachedSessionPayload.isBlank()) {
                throw new QuizSessionNotFoundException(curriculumId);
            }

            QuizSessionCachePayload payload =
                    objectMapper.readValue(cachedSessionPayload, QuizSessionCachePayload.class);

            if (!Objects.equals(payload.getCurriculumId(), curriculumId)) {
                throw new QuizSessionNotFoundException(curriculumId);
            }

            return payload;
        } catch (QuizSessionNotFoundException e) {
            throw e;
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 세션 Redis payload 파싱에 실패했습니다. userId=" + userId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException("퀴즈 세션 Redis 조회에 실패했습니다. userId=" + userId, e);
        }
    }

    private QuizGenerateAsyncResponse.Question findSourceQuestionOrThrow(
            QuizGenerateAsyncResponse quizSource,
            Long curriculumId,
            Integer questionNumber
    ) {
        return quizSource.getQuestions().stream()
                .filter(question -> Objects.equals(question.getQuestionNumber(), questionNumber))
                .findFirst()
                .orElseThrow(() -> new QuizQuestionNotFoundException(curriculumId, questionNumber));
    }

    private QuizSessionCachePayload.Question findSessionQuestionOrThrow(
            QuizSessionCachePayload sessionPayload,
            Long curriculumId,
            Integer questionNumber
    ) {
        return sessionPayload.getQuestions().stream()
                .filter(question -> Objects.equals(question.getQuestionNumber(), questionNumber))
                .findFirst()
                .orElseThrow(() -> new QuizQuestionNotFoundException(curriculumId, questionNumber));
    }

    private void validateAnswerNotSubmitted(Long curriculumId, QuizSessionCachePayload.Question sessionQuestion) {
        if (sessionQuestion.getSelectedAnswer() != null && !sessionQuestion.getSelectedAnswer().isBlank()) {
            throw new QuizAnswerConflictException(curriculumId, sessionQuestion.getQuestionNumber());
        }
    }

    private QuizSessionCachePayload updateSessionPayload(
            QuizSessionCachePayload sessionPayload,
            Integer questionNumber,
            String correctAnswer,
            String selectedAnswer,
            boolean isCorrect
    ) {
        return QuizSessionCachePayload.builder()
                .curriculumId(sessionPayload.getCurriculumId())
                .totalQuestions(sessionPayload.getTotalQuestions())
                .title(sessionPayload.getTitle())
                .description(sessionPayload.getDescription())
                .expectedMinutes(sessionPayload.getExpectedMinutes())
                .questions(sessionPayload.getQuestions().stream()
                        .map(question -> mapUpdatedQuestion(
                                question,
                                questionNumber,
                                correctAnswer,
                                selectedAnswer,
                                isCorrect
                        ))
                        .toList())
                .build();
    }

    private QuizSessionCachePayload.Question mapUpdatedQuestion(
            QuizSessionCachePayload.Question question,
            Integer questionNumber,
            String correctAnswer,
            String selectedAnswer,
            boolean isCorrect
    ) {
        if (!Objects.equals(question.getQuestionNumber(), questionNumber)) {
            return question;
        }

        return QuizSessionCachePayload.Question.builder()
                .questionNumber(question.getQuestionNumber())
                .question(question.getQuestion())
                .quizType(question.getQuizType())
                .options(question.getOptions())
                .correctAnswer(correctAnswer)
                .selectedAnswer(selectedAnswer)
                .isCorrect(isCorrect)
                .build();
    }

    private void saveQuizSession(Long userId, QuizSessionCachePayload payload) {
        try {
            redisService.save(
                    buildQuizSessionKey(userId),
                    objectMapper.writeValueAsString(payload),
                    QUIZ_SESSION_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JacksonException e) {
            throw new QuizPayloadParsingException(
                    "퀴즈 세션 Redis 저장 직렬화에 실패했습니다. userId=" + userId,
                    e
            );
        } catch (RuntimeException e) {
            throw new QuizRedisException("퀴즈 세션 Redis 저장에 실패했습니다. userId=" + userId, e);
        }
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? null : answer.trim();
    }

    private QuizSessionStartResponse mapStartResponse(QuizGenerateAsyncResponse quizPayload) {
        return QuizSessionStartResponse.builder()
                .curriculumId(quizPayload.getCurriculumId())
                .totalQuestions(quizPayload.getTotalQuestions())
                .title(quizPayload.getTitle())
                .description(quizPayload.getDescription())
                .expectedMinutes(quizPayload.getExpectedMinutes())
                .questions(quizPayload.getQuestions().stream()
                        .map(question -> QuizSessionStartResponse.Question.builder()
                                .questionNumber(question.getQuestionNumber())
                                .question(question.getQuestion())
                                .quizType(question.getQuizType())
                                .options(question.getOptions())
                                .build())
                        .toList())
                .build();
    }

    private void validateQuizPayload(QuizGenerateAsyncResponse response) {
        if (response == null) {
            throw new QuizGenerationException("FastAPI 퀴즈 생성 응답이 비어 있습니다.");
        }

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new QuizGenerationException("FastAPI 퀴즈 생성 응답에 questions가 없습니다.");
        }

        for (QuizGenerateAsyncResponse.Question question : response.getQuestions()) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                throw new QuizGenerationException("FastAPI 퀴즈 생성 응답에 correctAnswer가 없습니다.");
            }
        }
    }

    private String buildRecommendationKey(Long userId, Long curriculumId) {
        return RECOMMENDATION_KEY_PREFIX + userId + ":" + curriculumId;
    }

    private String buildRecommendationQuizKey(Long userId, Long curriculumId) {
        return RECOMMENDATION_QUIZ_KEY_PREFIX + userId + ":" + curriculumId;
    }

    private String buildQuizSessionKey(Long userId) {
        return QUIZ_SESSION_KEY_PREFIX + userId;
    }
}
