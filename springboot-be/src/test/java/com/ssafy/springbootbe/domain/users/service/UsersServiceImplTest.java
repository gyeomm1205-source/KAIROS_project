package com.ssafy.springbootbe.domain.users.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import com.ssafy.springbootbe.persistence.position.repository.DevPositionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserCurriculumCategory;
import com.ssafy.springbootbe.persistence.user.entity.UserDesiredPosition;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserCurriculumCategoryRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserDesiredPositionRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsersServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDesiredPositionRepository userDesiredPositionRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private UserCurriculumCategoryRepository userCurriculumCategoryRepository;
    @Mock private DevPositionRepository devPositionRepository;
    @Mock private TechStackRepository techStackRepository;
    @Mock private RedisService redisService;
    @Mock private ObjectMapper objectMapper;
    @Mock private JWTUtils jwtUtils;

    @InjectMocks
    private UsersServiceImpl usersService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("teddynu")
                .darkModeEnabled(false)
                .calendarSyncEnabled(false)
                .build();
    }

    // ===== findProfile =====

    @Test
    void findProfile_캐시_히트() throws Exception {
        // given
        String cacheKey = "user:profile:1";
        String cachedJson = "{\"userId\":1}";
        UserProfileResponse cachedResponse = UserProfileResponse.builder()
                .userId(1L)
                .nickname("teddynu")
                .build();

        given(redisService.get(cacheKey)).willReturn(cachedJson);
        given(objectMapper.readValue(cachedJson, UserProfileResponse.class)).willReturn(cachedResponse);

        // when
        UserProfileResponse response = usersService.findProfile(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("teddynu");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void findProfile_캐시_미스_DB_조회() throws Exception {
        // given
        given(redisService.get(anyString())).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        // when
        UserProfileResponse response = usersService.findProfile(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@gmail.com");
        verify(redisService).save(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void findProfile_캐시_역직렬화_실패시_DB_조회() throws Exception {
        // given
        given(redisService.get(anyString())).willReturn("invalid-json");
        given(objectMapper.readValue(anyString(), eq(UserProfileResponse.class)))
                .willThrow(mock(JacksonException.class));
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        // when
        UserProfileResponse response = usersService.findProfile(1L);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    void findProfile_실패_유저_없음() {
        // given
        given(redisService.get(anyString())).willReturn(null);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> usersService.findProfile(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ===== updateProfile =====

    @Test
    void updateProfile_성공_포지션_수정() {
        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                UserPosition.JUNIOR, null, null, null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of());

        // when
        UserProfileUpdateResponse response = usersService.updateProfile(1L, request);

        // then
        assertThat(response.getPosition()).isEqualTo(UserPosition.JUNIOR);
        verify(redisService).delete("user:profile:1");
    }

    @Test
    void updateProfile_성공_기술스택_수정() {
        // given
        TechStack techStack = TechStack.builder().techStackId(1L).techName("Java").build();
        UserTechStack userTechStack = UserTechStack.builder().user(mockUser).techStack(techStack).build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, List.of(1L), null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(techStackRepository.findAllById(List.of(1L))).willReturn(List.of(techStack));
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of(userTechStack));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of());

        // when
        UserProfileUpdateResponse response = usersService.updateProfile(1L, request);

        // then
        assertThat(response.getTechStacks()).hasSize(1);
        assertThat(response.getTechStacks().get(0).getTechName()).isEqualTo("Java");
        verify(userTechStackRepository).deleteByUserUserId(1L);
        verify(userTechStackRepository).saveAll(any());
    }

    @Test
    void updateProfile_성공_희망포지션_수정() {
        // given
        DevPosition devPosition = DevPosition.builder().devPositionId(1L).positionName("Backend").build();
        UserDesiredPosition userDesiredPosition = UserDesiredPosition.builder()
                .user(mockUser).devPosition(devPosition).build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, List.of(1L), null, null, null);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(devPositionRepository.findAllById(List.of(1L))).willReturn(List.of(devPosition));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of(userDesiredPosition));
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of());

        // when
        UserProfileUpdateResponse response = usersService.updateProfile(1L, request);

        // then
        assertThat(response.getDesiredPositions()).hasSize(1);
        assertThat(response.getDesiredPositions().get(0).getPositionName()).isEqualTo("Backend");
        verify(userDesiredPositionRepository).deleteByUserUserId(1L);
    }

    @Test
    void updateProfile_성공_커리큘럼카테고리_수정() {
        // given
        UserCurriculumCategory category = UserCurriculumCategory.builder()
                .user(mockUser).category(CurriculumCategory.PRACTICE).build();

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, null, List.of(CurriculumCategory.PRACTICE), null);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userDesiredPositionRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(userCurriculumCategoryRepository.findByUserUserId(1L)).willReturn(List.of(category));

        // when
        UserProfileUpdateResponse response = usersService.updateProfile(1L, request);

        // then
        assertThat(response.getCurriculumCategories()).containsExactly(CurriculumCategory.PRACTICE);
        verify(userCurriculumCategoryRepository).deleteByUserUserId(1L);
    }

    @Test
    void updateProfile_실패_유저_없음() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> usersService.updateProfile(99L, new UserProfileUpdateRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ===== updateDarkMode =====

    @Test
    void updateDarkMode_성공_활성화() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        Map<String, Boolean> response = usersService.updateDarkMode(1L, new DarkModeUpdateRequest(true));

        // then
        assertThat(response.get("darkModeEnabled")).isTrue();
        verify(redisService).delete("user:profile:1");
    }

    @Test
    void updateDarkMode_성공_비활성화() {
        // given
        User darkModeUser = User.builder().userId(1L).darkModeEnabled(true).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(darkModeUser));

        // when
        Map<String, Boolean> response = usersService.updateDarkMode(1L, new DarkModeUpdateRequest(false));

        // then
        assertThat(response.get("darkModeEnabled")).isFalse();
    }

    @Test
    void updateDarkMode_실패_유저_없음() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> usersService.updateDarkMode(99L, new DarkModeUpdateRequest(true)))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ===== deleteUser =====

    @Test
    void deleteUser_성공_토큰_블랙리스트_등록() {
        // given
        String token = "valid-token";
        long futureMillis = System.currentTimeMillis() + 3600_000L;
        Claims claims = mock(Claims.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(jwtUtils.getClaims(token)).willReturn(claims);
        given(claims.getExpiration()).willReturn(new Date(futureMillis));

        // when
        usersService.deleteUser(1L, token);

        // then
        verify(userRepository).delete(mockUser);
        verify(redisService).delete("user:profile:1");
        verify(redisService).save(eq("blacklist:" + token), eq("deleted"), anyLong(), any());
    }

    @Test
    void deleteUser_성공_만료된_토큰은_블랙리스트_미등록() {
        // given
        String token = "expired-token";
        long pastMillis = System.currentTimeMillis() - 1000L;
        Claims claims = mock(Claims.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(jwtUtils.getClaims(token)).willReturn(claims);
        given(claims.getExpiration()).willReturn(new Date(pastMillis));

        // when
        usersService.deleteUser(1L, token);

        // then
        verify(userRepository).delete(mockUser);
        verify(redisService, never()).save(anyString(), eq("deleted"), anyLong(), any());
    }

    @Test
    void deleteUser_실패_유저_없음() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> usersService.deleteUser(99L, "some-token"))
                .isInstanceOf(UserNotFoundException.class);
    }
}