package com.ssafy.springbootbe.persistence.oauth.repository;

import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    // OAuth 로그인 시 provider + providerAccountId로 계정 조회 (idx_oauth_provider_account)
    Optional<OAuthAccount> findByProviderAndProviderAccountId(OAuthProvider provider, String providerAccountId);
    // 유저의 전체 OAuth 계정 조회 (idx_oauth_user)
    List<OAuthAccount> findByUserUserId(Long userId);
    // 유저의 특정 provider OAuth 계정 조회
    Optional<OAuthAccount> findByUserUserIdAndProvider(Long userId, OAuthProvider provider);
}
