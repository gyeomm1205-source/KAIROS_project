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
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAccessDeniedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizActivityHistoryPersistenceException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAlreadyCompletedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAnswerConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizGenerationException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizIncompleteException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizPayloadParsingException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizQuestionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizRedisCleanupException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizRedisException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionPersistenceException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSourceNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationCachePayload;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumTechStack;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumTechStackRepository;
import com.ssafy.springbootbe.persistence.quiz.entity.QuizQuestion;
import com.ssafy.springbootbe.persistence.quiz.entity.QuizSession;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizQuestionRepository;
import com.ssafy.springbootbe.persistence.quiz.repository.QuizSessionRepository;
import com.ssafy.springbootbe.persistence.quiz.type.QuizType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
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
    private final CurriculumTechStackRepository curriculumTechStackRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final TechStackRepository techStackRepository;
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

    @Override
    @Transactional
    public QuizSessionCompleteResponse completeSession(Long userId, Long curriculumId) {
        // FastAPI에서 넘어오는 데이터가 객관식이 아닌 것이 있어서, 기능 테스트는 차후 예정
        // 요청 처리는 정상
        Curriculum curriculum = findCurriculumOrThrow(curriculumId);
        validateCurriculumOwnership(userId, curriculum);
        validateNotCompleted(userId, curriculumId);

        QuizGenerateAsyncResponse quizSource = findQuizSourceOrThrow(userId, curriculumId);
        QuizSessionCachePayload sessionPayload = findQuizSessionOrThrow(userId, curriculumId);
        validateAllQuestionsSubmitted(curriculumId, quizSource, sessionPayload);

        List<QuizResultRow> quizResults = buildQuizResults(curriculumId, quizSource, sessionPayload);
        int totalScore = calculateTotalScore(quizResults, quizSource.getTotalQuestions());

        QuizSession quizSession = saveQuizSessionResult(curriculum, totalScore);
        saveQuizQuestions(quizSession, quizResults);
        saveQuizActivityHistory(curriculum, quizResults);
        cleanupQuizRedisKeys(userId, curriculumId);

        return QuizSessionCompleteResponse.builder()
                .curriculumId(curriculumId)
                .totalScore(totalScore)
                .results(quizResults.stream()
                        .map(result -> QuizSessionCompleteResponse.Result.builder()
                                .questionNumber(result.questionNumber())
                                .question(result.question())
                                .options(result.options())
                                .correctAnswer(result.correctAnswer())
                                .selectedAnswer(result.selectedAnswer())
                                .isCorrect(result.isCorrect())
                                .build())
                        .toList())
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
        if (redisService.hasKey(buildQuizSessionKey(userId))) {
            throw new QuizSessionConflictException(userId);
        }
    }

    private void validateNotCompleted(Long userId, Long curriculumId) {
        if (quizSessionRepository.existsByUserUserIdAndCurriculumCurriculumId(userId, curriculumId)) {
            throw new QuizAlreadyCompletedException(curriculumId);
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
        addCurriculumTechStacks(curriculumId, techStacks);
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

    private void addCurriculumTechStacks(Long curriculumId, LinkedHashSet<String> techStacks) {
        curriculumTechStackRepository.findByCurriculumCurriculumId(curriculumId).stream()
                .map(CurriculumTechStack::getTechStack)
                .map(TechStack::getTechName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(techStacks::add);
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

    private void validateAllQuestionsSubmitted(
            Long curriculumId,
            QuizGenerateAsyncResponse quizSource,
            QuizSessionCachePayload sessionPayload
    ) {
        int totalQuestions = resolveTotalQuestions(quizSource, sessionPayload);
        long submittedCount = sessionPayload.getQuestions().stream()
                .filter(question -> question.getSelectedAnswer() != null && !question.getSelectedAnswer().isBlank())
                .count();

        if (submittedCount != totalQuestions) {
            throw new QuizIncompleteException(curriculumId);
        }
    }

    private int resolveTotalQuestions(QuizGenerateAsyncResponse quizSource, QuizSessionCachePayload sessionPayload) {
        if (quizSource.getTotalQuestions() != null) {
            return quizSource.getTotalQuestions();
        }
        if (sessionPayload.getTotalQuestions() != null) {
            return sessionPayload.getTotalQuestions();
        }
        return quizSource.getQuestions().size();
    }

    private List<QuizResultRow> buildQuizResults(
            Long curriculumId,
            QuizGenerateAsyncResponse quizSource,
            QuizSessionCachePayload sessionPayload
    ) {
        return quizSource.getQuestions().stream()
                .map(sourceQuestion -> {
                    QuizSessionCachePayload.Question sessionQuestion = findSessionQuestionOrThrow(
                            sessionPayload,
                            curriculumId,
                            sourceQuestion.getQuestionNumber()
                    );

                    return new QuizResultRow(
                            sourceQuestion.getQuestionNumber(),
                            sourceQuestion.getQuestion(),
                            sourceQuestion.getQuizType(),
                            sourceQuestion.getOptions(),
                            sourceQuestion.getCorrectAnswer(),
                            sessionQuestion.getSelectedAnswer(),
                            Boolean.TRUE.equals(sessionQuestion.getIsCorrect())
                    );
                })
                .sorted(Comparator.comparing(QuizResultRow::questionNumber))
                .toList();
    }

    private int calculateTotalScore(List<QuizResultRow> quizResults, Integer totalQuestions) {
        long correctCount = quizResults.stream()
                .filter(QuizResultRow::isCorrect)
                .count();

        int questionCount = totalQuestions == null || totalQuestions == 0 ? quizResults.size() : totalQuestions;
        if (questionCount == 0) {
            return 0;
        }
        return (int) ((correctCount * 100) / questionCount);
    }

    private QuizSession saveQuizSessionResult(Curriculum curriculum, int totalScore) {
        try {
            QuizSession quizSession = quizSessionRepository.findByUserUserIdAndCurriculumCurriculumId(
                            curriculum.getUser().getUserId(),
                            curriculum.getCurriculumId()
                    )
                    .orElseGet(() -> QuizSession.builder()
                            .user(curriculum.getUser())
                            .curriculum(curriculum)
                            .build());
            quizSession.complete(totalScore);
            return quizSessionRepository.save(quizSession);
        } catch (RuntimeException e) {
            throw new QuizSessionPersistenceException(
                    "퀴즈 세션 저장에 실패했습니다. curriculumId=" + curriculum.getCurriculumId(),
                    e
            );
        }
    }

    private void saveQuizQuestions(QuizSession quizSession, List<QuizResultRow> quizResults) {
        try {
            List<QuizQuestion> quizQuestions = quizResults.stream()
                    .map(result -> QuizQuestion.builder()
                            .quizSession(quizSession)
                            .question(result.question())
                            .quizType(parseQuizType(result.quizType()))
                            .options(result.options())
                            .correctAnswer(result.correctAnswer())
                            .selectedAnswer(result.selectedAnswer())
                            .isCorrect(result.isCorrect())
                            .build())
                    .toList();
            quizQuestionRepository.saveAll(quizQuestions);
        } catch (RuntimeException e) {
            throw new QuizSessionPersistenceException(
                    "퀴즈 문항 저장에 실패했습니다. quizSessionId=" + quizSession.getQuizSessionId(),
                    e
            );
        }
    }

    private QuizType parseQuizType(String quizType) {
        if (quizType == null || quizType.isBlank()) {
            return QuizType.MULTIPLE_CHOICE;
        }

        try {
            return QuizType.valueOf(quizType.trim());
        } catch (IllegalArgumentException e) {
            throw new QuizSessionPersistenceException("지원하지 않는 quizType입니다. quizType=" + quizType, e);
        }
    }

    private void saveQuizActivityHistory(Curriculum curriculum, List<QuizResultRow> quizResults) {
        List<String> techStackNames = buildQuizRelatedTechStacks(curriculum.getUser().getUserId(), curriculum.getCurriculumId());
        String techStackLabel = techStackNames.isEmpty() ? "기술 스택 미지정" : String.join(", ", techStackNames);
        long correctCount = quizResults.stream().filter(QuizResultRow::isCorrect).count();

        try {
            ActivityHistory activityHistory = activityHistoryRepository.save(ActivityHistory.builder()
                    .user(curriculum.getUser())
                    .activityType(ActivityType.QUIZ)
                    .title("퀴즈 완료 — " + techStackLabel)
                    .description("정답 " + correctCount + "/" + quizResults.size())
                    .activityDate(LocalDateTime.now())
                    .build());

            List<ActivityHistoryTechStack> links = new ArrayList<>();
            for (String techStackName : techStackNames) {
                techStackRepository.findByTechName(techStackName)
                        .ifPresent(techStack -> links.add(ActivityHistoryTechStack.builder()
                                .activityHistory(activityHistory)
                                .techStack(techStack)
                                .build()));
            }

            if (!links.isEmpty()) {
                activityHistoryTechStackRepository.saveAll(links);
            }
        } catch (RuntimeException e) {
            throw new QuizActivityHistoryPersistenceException(
                    "퀴즈 활동 이력 저장에 실패했습니다. curriculumId=" + curriculum.getCurriculumId(),
                    e
            );
        }
    }

    private List<String> buildQuizRelatedTechStacks(Long userId, Long curriculumId) {
        LinkedHashSet<String> techStacks = new LinkedHashSet<>();
        addCurriculumTechStacks(curriculumId, techStacks);
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

        return techStacks.stream().limit(3).toList();
    }

    private void cleanupQuizRedisKeys(Long userId, Long curriculumId) {
        deleteRedisKey(buildQuizSessionKey(userId), "퀴즈 세션 Redis 삭제에 실패했습니다. userId=" + userId);
        deleteRedisKey(
                buildRecommendationQuizKey(userId, curriculumId),
                "퀴즈 원본 Redis 삭제에 실패했습니다. curriculumId=" + curriculumId
        );
    }

    private void deleteRedisKey(String key, String message) {
        try {
            if (!redisService.delete(key)) {
                throw new QuizRedisCleanupException(message + ", key=" + key);
            }
        } catch (QuizRedisCleanupException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new QuizRedisCleanupException(message + ", key=" + key, e);
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

    private record QuizResultRow(
            Integer questionNumber,
            String question,
            String quizType,
            List<String> options,
            String correctAnswer,
            String selectedAnswer,
            boolean isCorrect
    ) {
    }
}
