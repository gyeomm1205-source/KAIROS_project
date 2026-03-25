package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.auth.dto.request.GithubCollectAsyncRequest;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubCollectAsyncResponse;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;
    private final JWTUtils jwtUtils;
    private final AIRestClient aiRestClient;
    private final RedisService redisService;
    
    @Value("${ai.server-url}")
    private String aiServerUrl;
    @Value("${ai.collect-async-path}")
    private String aiCollectAsyncPath;

    // 🚨 외부에서 절대 유추할 수 없도록 복잡한 경로로 설정 (개발 서버 1회용)
    @GetMapping("/secret-inject-github-token-ssafy-test-77-xyz")
    @Transactional
    public String createTestUserAndInject(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "a506.test19@gmail.com") String email,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "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") String githubToken,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "zhy2on") String githubUsername) {
            
        // 1. 유저 계정 강제로 찾거나(없으면) 만들기 (구글 비밀번호 몰라도 됨!)
        User testUser = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .nickname(githubUsername) // 닉네임을 깃허브 유저네임으로 설정
                            .profileImageUrl("")
                            .status(UserStatus.GUEST)
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Velog 아이디 강제로 주입
        testUser.updateVelogUsername("zhy2on");
        userRepository.save(testUser);

        // 3. GitHub 토큰 강제로 주입
        oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, String.valueOf(testUser.getUserId()) + "_GH")
                .ifPresent(oAuthAccountRepository::delete);
                
        OAuthAccount fakeGithubAccount = OAuthAccount.builder()
                .user(testUser)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId(String.valueOf(testUser.getUserId()) + "_GH") // 충돌 방지용 고유 아이디
                .refreshToken(oAuthTokenCryptoService.encrypt(githubToken))
                .build();
        oAuthAccountRepository.save(fakeGithubAccount);

        // 4. FastAPI AI 서버에 깃허브 데이터 수집 강제 강제 요청(트리거) 쏘기!!
        try {
            GithubCollectAsyncRequest request = GithubCollectAsyncRequest.builder()
                    .userId(testUser.getUserId())
                    .githubToken(githubToken)
                    .githubUsername(githubUsername)
                    .build();

            GithubCollectAsyncResponse aiResponse = aiRestClient.buildAiRestClient()
                    .post()
                    .uri(aiServerUrl + aiCollectAsyncPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GithubCollectAsyncResponse.class);

            if (aiResponse != null && aiResponse.getTaskId() != null) {
                // 수집 태스크 ID를 Redis에 저장해둬야 벨로그 분석 트리거 시에 깃허브 수집 완료 여부를 알 수 있음
                redisService.save("githubTaskId:" + testUser.getUserId(), aiResponse.getTaskId(), 30L, java.util.concurrent.TimeUnit.MINUTES);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return "<h1>❌ 테스트 세팅 중 FastAPI GitHub 수집 트리거 전송 실패! AI 서버 상태를 확인하세요.</h1><p>" + e.getMessage() + "</p>";
        }

        return "<h1>✅ " + email + " 계정 DB 깃허브 세팅 및 AI 로직 트리거 대성공!!!</h1>"
             + "<p>이제 화면 구글 로그인에서 해당 이메일을 선택하는 순간 '이미 깃허브/벨로그 연동이 끝난 기존 회원'으로 취급되어 메인 화면으로 직행합니다!<br/>"
             + "(FastAPI 쪽 수집 작업 태스크도 정상적으로 요청이 들어갔습니다!)</p>";
    }
}
