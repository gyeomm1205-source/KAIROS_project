package com.ssafy.s14p21a506.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.cognito")
public class CognitoProperties {

    private boolean enabled;
    private String region;
    private String userPoolId;
    private String clientId;
    private String issuerUri;
    private String tokenUse = "access";
    private String usernameClaim = "cognito:username";

    public String resolveIssuerUri() {
        if (StringUtils.hasText(issuerUri)) {
            return issuerUri;
        }
        if (!StringUtils.hasText(region) || !StringUtils.hasText(userPoolId)) {
            return null;
        }
        return "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
    }

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(resolveIssuerUri())
                && StringUtils.hasText(clientId)
                && StringUtils.hasText(tokenUse);
    }
}
