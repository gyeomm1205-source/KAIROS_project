package com.ssafy.springbootbe.domain.curricula.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.service.GoogleCalendarClientService;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumConfirmRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumNodeUpdateRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumPreviewRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.AnalysisDataRequest;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumConfirmResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumGenerateResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumPreviewResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewNodeDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewReasonDto;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNotFoundException;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNodeCalendarSync;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumRecommendationReason;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeCalendarSyncRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRecommendationReasonRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurriculaServiceImplTest {

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private CurriculumNodeRepository curriculumNodeRepository;
    @Mock private CurriculumNodeCalendarSyncRepository curriculumNodeCalendarSyncRepository;
    @Mock private CurriculumRecommendationReasonRepository curriculumRecommendationReasonRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    @Mock private OAuthAccountRepository oAuthAccountRepository;
    @Mock private GoogleCalendarClientService googleCalendarClientService;
    @Mock private OAuthTokenCryptoService oAuthTokenCryptoService;
    @Mock private AIRestClient aiRestClient;
    @Mock private RedisService redisService;
    @Mock private ObjectMapper objectMapper;

    private CurriculaServiceImpl curriculaService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private User user;
    private User otherUser;
    private Curriculum curriculum;
    private CurriculumNode node;

    @BeforeEach
    void setUp() {
        curriculaService = spy(new CurriculaServiceImpl(
                curriculumRepository,
                curriculumNodeRepository,
                curriculumNodeCalendarSyncRepository,
                curriculumRecommendationReasonRepository,
                userRepository,
                activityHistoryTechStackRepository,
                oAuthAccountRepository,
                googleCalendarClientService,
                oAuthTokenCryptoService,
                aiRestClient,
                redisService,
                objectMapper
        ));

        ReflectionTestUtils.setField(curriculaService, "aiServerUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(curriculaService, "curriculumGeneratePath", "/api/v1/ai/curriculum/generate");

        user = User.builder()
                .userId(USER_ID)
                .email("user@test.com")
                .nickname("tester")
                .considerPersonalSchedule(false)
                .build();

        otherUser = User.builder()
                .userId(OTHER_USER_ID)
                .email("other@test.com")
                .nickname("other")
                .build();

        curriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(user)
                .status(CurriculumStatus.ACTIVE)
                .duration(7)
                .build();

        node = CurriculumNode.builder()
                .curriculumNodeId(5L)
                .curriculum(curriculum)
                .title("Spring Boot 기초")
                .description("Spring Boot 학습")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .expectedMinutes(60)
                .build();
    }

    // ===== preview =====

    @Test
    void preview_성공_considerPersonalSchedule_false() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(activityHistoryTechStackRepository.findTechStackCountsByUserId(eq(USER_ID), any()))
                .willReturn(List.of());

        CurriculumGenerateResponse aiResponse = CurriculumGenerateResponse.builder()
                .recommendationReason(PreviewReasonDto.builder().summaryLine("백엔드 커리큘럼").build())
                .nodes(List.of(
                        PreviewNodeDto.builder()
                                .title("Spring 기초")
                                .scheduledDate(LocalDate.of(2025, 3, 1))
                                .expectedMinutes(60)
                                .build(),
                        PreviewNodeDto.builder()
                                .title("JPA 심화")
                                .scheduledDate(LocalDate.of(2025, 3, 7))
                                .expectedMinutes(90)
                                .build()
                ))
                .build();
        doReturn(aiResponse).when(curriculaService).callFastApi(any());

        CurriculumPreviewRequest request = CurriculumPreviewRequest.builder()
                .analysisData(AnalysisDataRequest.builder().summary("백엔드 활동 중심").build())
                .build();

        // when
        CurriculumPreviewResponse response = curriculaService.preview(USER_ID, request);

        // then
        assertThat(response.getDuration()).isEqualTo(7);
        assertThat(response.getNodes()).hasSize(2);
        assertThat(response.getRecommendationReason().getSummaryLine()).isEqualTo("백엔드 커리큘럼");
        assertThat(response.getCurriculumPreviewKey()).startsWith("curriculumPreview:1:");
        verify(oAuthAccountRepository, never()).findByUserUserIdAndProvider(any(), any());
    }

    @Test
    void preview_성공_considerPersonalSchedule_true_OAuth_없음() {
        // given
        User userWithSchedule = User.builder()
                .userId(USER_ID)
                .email("user@test.com")
                .nickname("tester")
                .considerPersonalSchedule(true)
                .build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(userWithSchedule));
        given(activityHistoryTechStackRepository.findTechStackCountsByUserId(eq(USER_ID), any()))
                .willReturn(List.of());
        given(oAuthAccountRepository.findByUserUserIdAndProvider(eq(USER_ID), any()))
                .willReturn(Optional.empty());

        CurriculumGenerateResponse aiResponse = CurriculumGenerateResponse.builder()
                .nodes(List.of(
                        PreviewNodeDto.builder()
                                .title("노드1")
                                .scheduledDate(LocalDate.of(2025, 3, 1))
                                .build()
                ))
                .build();
        doReturn(aiResponse).when(curriculaService).callFastApi(any());

        CurriculumPreviewRequest request = CurriculumPreviewRequest.builder()
                .analysisData(AnalysisDataRequest.builder().summary("분석 결과").build())
                .build();

        // when
        CurriculumPreviewResponse response = curriculaService.preview(USER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDuration()).isEqualTo(1);
    }

    @Test
    void preview_사용자_없으면_IllegalArgumentException() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        CurriculumPreviewRequest request = CurriculumPreviewRequest.builder()
                .analysisData(AnalysisDataRequest.builder().summary("분석 결과").build())
                .build();

        // when & then
        assertThatThrownBy(() -> curriculaService.preview(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId=" + USER_ID);
    }

    // ===== confirm =====

    @Test
    void confirm_previewKey_없으면_IllegalArgumentException() {
        // given
        given(redisService.get("invalid-key")).willReturn(null);

        CurriculumConfirmRequest request = new CurriculumConfirmRequest();
        ReflectionTestUtils.setField(request, "curriculumPreviewKey", "invalid-key");

        // when & then
        assertThatThrownBy(() -> curriculaService.confirm(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid-key");
    }

    @Test
    void confirm_성공_Google_Calendar_없음() throws JacksonException {
        // given
        String previewKey = "curriculumPreview:1:abc12345";
        String json = "{\"nodes\":[]}";

        CurriculumGenerateResponse aiResponse = CurriculumGenerateResponse.builder()
                .recommendationReason(PreviewReasonDto.builder()
                        .summaryLine("백엔드 커리큘럼")
                        .userContext("백엔드 학습 중심")
                        .build())
                .nodes(List.of(
                        PreviewNodeDto.builder()
                                .title("Spring 기초")
                                .scheduledDate(LocalDate.of(2025, 3, 10))
                                .expectedMinutes(60)
                                .build()
                ))
                .build();

        given(redisService.get(previewKey)).willReturn(json);
        given(objectMapper.readValue(json, CurriculumGenerateResponse.class)).willReturn(aiResponse);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(oAuthAccountRepository.findByUserUserIdAndProvider(eq(USER_ID), any()))
                .willReturn(Optional.empty());

        CurriculumConfirmRequest request = new CurriculumConfirmRequest();
        ReflectionTestUtils.setField(request, "curriculumPreviewKey", previewKey);

        // when
        CurriculumConfirmResponse response = curriculaService.confirm(USER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CurriculumStatus.ACTIVE);
        assertThat(response.getNodes()).hasSize(1);
        assertThat(response.getNodes().get(0).getTitle()).isEqualTo("Spring 기초");
        verify(curriculumRepository).save(any(Curriculum.class));
        verify(curriculumNodeRepository).save(any(CurriculumNode.class));
        verify(redisService).delete(previewKey);
    }

    @Test
    void confirm_사용자_없으면_IllegalArgumentException() throws JacksonException {
        // given
        String previewKey = "curriculumPreview:1:abc12345";
        CurriculumGenerateResponse aiResponse = CurriculumGenerateResponse.builder()
                .nodes(List.of())
                .build();

        given(redisService.get(previewKey)).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(CurriculumGenerateResponse.class))).willReturn(aiResponse);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        CurriculumConfirmRequest request = new CurriculumConfirmRequest();
        ReflectionTestUtils.setField(request, "curriculumPreviewKey", previewKey);

        // when & then
        assertThatThrownBy(() -> curriculaService.confirm(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId=" + USER_ID);
    }

    // ===== getReason =====

    @Test
    void getReason_성공() {
        // given
        CurriculumRecommendationReason reason = CurriculumRecommendationReason.builder()
                .curriculumRecommendationReasonId(1L)
                .curriculum(curriculum)
                .summaryLine("백엔드 커리큘럼")
                .userContext("백엔드 학습 중심")
                .aiInterpretation("Spring Boot 역량 강화 필요")
                .curriculumRationale("실습 위주 구성")
                .build();

        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(curriculumRecommendationReasonRepository.findByCurriculumCurriculumId(10L))
                .willReturn(Optional.of(reason));

        // when
        CurriculumReasonResponse response = curriculaService.getReason(USER_ID, 10L);

        // then
        assertThat(response.getCurriculumId()).isEqualTo(10L);
        assertThat(response.getSummaryLine()).isEqualTo("백엔드 커리큘럼");
        assertThat(response.getUserContext()).isEqualTo("백엔드 학습 중심");
    }

    @Test
    void getReason_curriculum_없으면_CurriculumNotFoundException() {
        // given
        given(curriculumRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> curriculaService.getReason(USER_ID, 99L))
                .isInstanceOf(CurriculumNotFoundException.class);
    }

    @Test
    void getReason_다른_유저_접근시_CurriculumAccessDeniedException() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));

        // when & then
        assertThatThrownBy(() -> curriculaService.getReason(OTHER_USER_ID, 10L))
                .isInstanceOf(CurriculumAccessDeniedException.class);
    }

    @Test
    void getReason_reason_없으면_CurriculumNotFoundException() {
        // given
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(curriculum));
        given(curriculumRecommendationReasonRepository.findByCurriculumCurriculumId(10L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> curriculaService.getReason(USER_ID, 10L))
                .isInstanceOf(CurriculumNotFoundException.class);
    }

    // ===== getNode =====

    @Test
    void getNode_성공() {
        // given
        given(curriculumNodeRepository.findById(5L)).willReturn(Optional.of(node));

        // when
        CurriculumNodeResponse response = curriculaService.getNode(USER_ID, 5L);

        // then
        assertThat(response.getCurriculumNodeId()).isEqualTo(5L);
        assertThat(response.getTitle()).isEqualTo("Spring Boot 기초");
        assertThat(response.getProgressStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void getNode_노드_없으면_CurriculumNodeNotFoundException() {
        // given
        given(curriculumNodeRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> curriculaService.getNode(USER_ID, 99L))
                .isInstanceOf(CurriculumNodeNotFoundException.class);
    }

    @Test
    void getNode_다른_유저_접근시_CurriculumNodeAccessDeniedException() {
        // given
        given(curriculumNodeRepository.findById(5L)).willReturn(Optional.of(node));

        // when & then
        assertThatThrownBy(() -> curriculaService.getNode(OTHER_USER_ID, 5L))
                .isInstanceOf(CurriculumNodeAccessDeniedException.class);
    }

    // ===== updateNode =====

    @Test
    void updateNode_성공() {
        // given
        given(curriculumNodeRepository.findById(5L)).willReturn(Optional.of(node));
        given(curriculumNodeCalendarSyncRepository.findByCurriculumNodeCurriculumNodeId(5L))
                .willReturn(Optional.empty());

        CurriculumNodeUpdateRequest request = new CurriculumNodeUpdateRequest();
        ReflectionTestUtils.setField(request, "progressStatus", ProgressStatus.IN_PROGRESS);

        // when
        CurriculumNodeResponse response = curriculaService.updateNode(USER_ID, 5L, request);

        // then
        assertThat(response.getCurriculumNodeId()).isEqualTo(5L);
        assertThat(response.getProgressStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void updateNode_수정할_필드_없으면_IllegalArgumentException() {
        // given
        CurriculumNodeUpdateRequest emptyRequest = new CurriculumNodeUpdateRequest();

        // when & then
        assertThatThrownBy(() -> curriculaService.updateNode(USER_ID, 5L, emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수정할 필드가 없습니다.");
    }

    @Test
    void updateNode_노드_없으면_CurriculumNodeNotFoundException() {
        // given
        given(curriculumNodeRepository.findById(99L)).willReturn(Optional.empty());

        CurriculumNodeUpdateRequest request = new CurriculumNodeUpdateRequest();
        ReflectionTestUtils.setField(request, "progressStatus", ProgressStatus.DONE);

        // when & then
        assertThatThrownBy(() -> curriculaService.updateNode(USER_ID, 99L, request))
                .isInstanceOf(CurriculumNodeNotFoundException.class);
    }

    @Test
    void updateNode_다른_유저_접근시_CurriculumNodeAccessDeniedException() {
        // given
        given(curriculumNodeRepository.findById(5L)).willReturn(Optional.of(node));

        CurriculumNodeUpdateRequest request = new CurriculumNodeUpdateRequest();
        ReflectionTestUtils.setField(request, "progressStatus", ProgressStatus.DONE);

        // when & then
        assertThatThrownBy(() -> curriculaService.updateNode(OTHER_USER_ID, 5L, request))
                .isInstanceOf(CurriculumNodeAccessDeniedException.class);
    }

    @Test
    void updateNode_날짜_변경시_캘린더_캐시_무효화() {
        // given
        CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(1L)
                .curriculumNode(node)
                .build();
        given(curriculumNodeRepository.findById(5L)).willReturn(Optional.of(node));
        given(curriculumNodeCalendarSyncRepository.findByCurriculumNodeCurriculumNodeId(5L))
                .willReturn(Optional.of(sync));

        CurriculumNodeUpdateRequest request = new CurriculumNodeUpdateRequest();
        ReflectionTestUtils.setField(request, "scheduledDate", LocalDate.of(2025, 4, 1));

        // when
        curriculaService.updateNode(USER_ID, 5L, request);

        // then — 날짜가 달라졌으므로 두 달 캐시 모두 삭제
        verify(redisService).delete("calendar:1:2025:3");
        verify(redisService).delete("calendar:1:2025:4");
    }
}
