package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;
    private final JWTUtils jwtUtils;

    // 🚨 외부에서 절대 유추할 수 없도록 복잡한 경로로 설정 (개발 서버 1회용)
    @GetMapping("/secret-inject-github-token-ssafy-test-77-xyz")
    @Transactional
    public String createTestUserAndInject(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "a506.test19@gmail.com") String email,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "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") String githubToken) {
            
        // 1. 유저 계정 강제로 찾거나(없으면) 만들기 (구글 비밀번호 몰라도 됨!)
        User testUser = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .nickname(email.split("@")[0] + "_테스터")
                            .profileImageUrl("")
                            .status(UserStatus.ONBOARDING)
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Velog 아이디 강제로 주입
        testUser.updateVelogUsername("zhy2on");
        userRepository.save(testUser);

        // 3. GitHub 토큰 강제로 주입
        oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, testUser.getEmail() + "_GH")
                .ifPresent(oAuthAccountRepository::delete);
                
        OAuthAccount fakeGithubAccount = OAuthAccount.builder()
                .user(testUser)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId(testUser.getEmail() + "_GH") // 충돌 방지용 고유 아이디
                .refreshToken(oAuthTokenCryptoService.encrypt(githubToken))
                .build();
        oAuthAccountRepository.save(fakeGithubAccount);

        return "<h1>✅ " + email + " 계정 DB 프리패스 세팅 성공!</h1>"
             + "<p>이제 화면 구글 로그인에서 해당 이메일을 선택하는 순간 '이미 깃허브/벨로그 연동이 끝난 기존 회원'으로 취급되어 메인 화면으로 직행합니다!</p>";
    }
}
