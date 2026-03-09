package com.ssafy.s14p21a506.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CognitoProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/api/test/ping/**"
    };

    private static final String[] PROTECTED_ENDPOINTS = {
            "/api/test/me",
            "/api/test/mock-posts/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CognitoProperties cognitoProperties,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
            if (cognitoProperties.isConfigured()) {
                authorize.requestMatchers(PROTECTED_ENDPOINTS).authenticated();
            } else {
                authorize.requestMatchers(PROTECTED_ENDPOINTS).denyAll();
            }
            authorize.anyRequest().permitAll();
        });

        if (cognitoProperties.isConfigured()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder(cognitoProperties))));
        }

        return http.build();
    }

    private JwtDecoder jwtDecoder(CognitoProperties cognitoProperties) {
        String issuerUri = cognitoProperties.resolveIssuerUri();
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                tokenUseValidator(cognitoProperties),
                clientIdValidator(cognitoProperties)
        ));
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> tokenUseValidator(CognitoProperties cognitoProperties) {
        return jwt -> {
            String actualTokenUse = jwt.getClaimAsString("token_use");
            String expectedTokenUse = cognitoProperties.getTokenUse();

            if (expectedTokenUse.equals(actualTokenUse)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Invalid Cognito token_use. Expected " + expectedTokenUse + ".",
                    null
            ));
        };
    }

    private OAuth2TokenValidator<Jwt> clientIdValidator(CognitoProperties cognitoProperties) {
        return jwt -> {
            String expectedClientId = cognitoProperties.getClientId();
            String tokenUse = jwt.getClaimAsString("token_use");

            boolean valid = switch (tokenUse) {
                case "id" -> jwt.getAudience().contains(expectedClientId);
                case "access" -> expectedClientId.equals(jwt.getClaimAsString("client_id"));
                default -> false;
            };

            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Invalid Cognito client identifier.",
                    null
            ));
        };
    }
}
