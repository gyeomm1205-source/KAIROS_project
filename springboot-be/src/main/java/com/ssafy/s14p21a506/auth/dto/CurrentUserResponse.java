package com.ssafy.s14p21a506.auth.dto;

import com.ssafy.s14p21a506.auth.AuthenticatedUser;
import java.util.List;

public record CurrentUserResponse(
        String source,
        String subject,
        String username,
        String email,
        List<String> groups,
        String tokenUse,
        String clientId
) {

    public static CurrentUserResponse from(String source, AuthenticatedUser user) {
        return new CurrentUserResponse(
                source,
                user.subject(),
                user.username(),
                user.email(),
                user.groups(),
                user.tokenUse(),
                user.clientId()
        );
    }
}
