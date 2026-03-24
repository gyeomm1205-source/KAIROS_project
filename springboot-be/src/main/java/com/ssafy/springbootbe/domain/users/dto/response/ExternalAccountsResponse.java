package com.ssafy.springbootbe.domain.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAccountsResponse {

    private GithubExternalAccountResponse github;
    private VelogExternalAccountResponse velog;
}
