package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.activities.exception.ActivitySyncFailedException;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivitySyncServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private OAuthAccountRepository oAuthAccountRepository;
    @Mock private ActivityHistoryRepository activityHistoryRepository;
    @Mock private OAuthTokenCryptoService oAuthTokenCryptoService;
    @Mock private RedisService redisService;
    @Mock private ObjectMapper objectMapper;
    @Mock private GithubApiService githubApiService;
    @Mock private VelogRssService velogRssService;

    @InjectMocks
    private ActivitySyncServiceImpl activitySyncService;

    @Test
    void syncGithub_외부계정_캐시에_비공개_레포까지_포함한다() throws Exception {
        OAuthAccount githubAccount = OAuthAccount.builder()
                .provider(OAuthProvider.GITHUB)
                .refreshToken("encrypted")
                .build();
        User user = User.builder().userId(1L).build();
        GithubApiService.GithubUserDetailResponse userDetail = mock(GithubApiService.GithubUserDetailResponse.class);

        given(oAuthAccountRepository.findByUserUserIdAndProvider(1L, OAuthProvider.GITHUB))
                .willReturn(Optional.of(githubAccount));
        given(oAuthTokenCryptoService.decrypt("encrypted")).willReturn("access-token");
        given(githubApiService.fetchUserDetail("access-token")).willReturn(userDetail);
        given(userDetail.getLogin()).willReturn("octocat");
        given(userDetail.getPublicRepos()).willReturn(3);
        given(userDetail.getTotalPrivateRepos()).willReturn(2);
        given(activityHistoryRepository.findLatestActivityDateByUserIdAndActivityType(1L, ActivityType.GITHUB_COMMIT))
                .willReturn(Optional.empty());
        given(githubApiService.fetchCommitsSince("access-token", "octocat", null))
                .willReturn(List.of(new GithubApiService.CommitInfo("feat: sync", LocalDateTime.of(2026, 3, 24, 10, 0))));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(activityHistoryRepository.countByUserIdAndActivityTypeSince(eq(1L), eq(ActivityType.GITHUB_COMMIT), any()))
                .willReturn(7L);
        given(githubApiService.countRecentPullRequests("access-token", "octocat")).willReturn(4);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"ok\":true}");

        activitySyncService.syncActivities(1L, "github");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("username")).isEqualTo("octocat");
        assertThat(payload.get("recentCommits")).isEqualTo(7L);
        assertThat(payload.get("recentPrs")).isEqualTo(4);
        assertThat(payload.get("totalRepos")).isEqualTo(5);
        verify(redisService).save("external-account:1:github", "{\"ok\":true}", 1L, TimeUnit.HOURS);
    }

    @Test
    void syncVelog_외부계정_캐시에_DB기반_메타데이터를_저장한다() throws Exception {
        User user = User.builder().userId(1L).velogUsername("junghyun").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(activityHistoryRepository.findLatestActivityDateByUserIdAndActivityType(1L, ActivityType.VELOG_POST))
                .willReturn(
                        Optional.empty(),
                        Optional.of(LocalDateTime.of(2026, 3, 20, 9, 0))
                );
        given(velogRssService.fetchPostsSince("junghyun", null))
                .willReturn(List.of(new VelogRssService.PostInfo("post", LocalDateTime.of(2026, 3, 24, 11, 0))));
        given(activityHistoryRepository.countByUserIdAndActivityType(1L, ActivityType.VELOG_POST))
                .willReturn(15L);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"ok\":true}");

        activitySyncService.syncActivities(1L, "velog");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload.get("username")).isEqualTo("junghyun");
        assertThat(payload.get("totalPosts")).isEqualTo(15L);
        assertThat(payload.get("latestPostDate")).isEqualTo("2026-03-20");
        assertThat(payload.get("totalViews")).isEqualTo(0);
        verify(redisService).save("external-account:1:velog", "{\"ok\":true}", 1L, TimeUnit.HOURS);
    }

    @Test
    void syncGithub_Redis_저장_실패시_예외를_던진다() throws Exception {
        OAuthAccount githubAccount = OAuthAccount.builder()
                .provider(OAuthProvider.GITHUB)
                .refreshToken("encrypted")
                .build();
        GithubApiService.GithubUserDetailResponse userDetail = mock(GithubApiService.GithubUserDetailResponse.class);

        given(oAuthAccountRepository.findByUserUserIdAndProvider(1L, OAuthProvider.GITHUB))
                .willReturn(Optional.of(githubAccount));
        given(oAuthTokenCryptoService.decrypt("encrypted")).willReturn("access-token");
        given(githubApiService.fetchUserDetail("access-token")).willReturn(userDetail);
        given(userDetail.getLogin()).willReturn("octocat");
        given(userDetail.getPublicRepos()).willReturn(1);
        given(userDetail.getTotalPrivateRepos()).willReturn(0);
        given(activityHistoryRepository.findLatestActivityDateByUserIdAndActivityType(1L, ActivityType.GITHUB_COMMIT))
                .willReturn(Optional.empty());
        given(githubApiService.fetchCommitsSince("access-token", "octocat", null)).willReturn(List.of());
        given(activityHistoryRepository.countByUserIdAndActivityTypeSince(eq(1L), eq(ActivityType.GITHUB_COMMIT), any()))
                .willReturn(0L);
        given(githubApiService.countRecentPullRequests("access-token", "octocat")).willReturn(0);
        given(objectMapper.writeValueAsString(any())).willReturn("{\"ok\":true}");
        doThrow(new RuntimeException("redis down"))
                .when(redisService)
                .save(eq("external-account:1:github"), eq("{\"ok\":true}"), eq(1L), eq(TimeUnit.HOURS));

        assertThatThrownBy(() -> activitySyncService.syncActivities(1L, "github"))
                .isInstanceOf(ActivitySyncFailedException.class)
                .hasMessageContaining("github 동기화 결과 Redis 저장에 실패했습니다.");
    }
}
