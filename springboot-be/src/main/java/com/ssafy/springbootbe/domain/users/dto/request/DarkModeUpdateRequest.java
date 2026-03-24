package com.ssafy.springbootbe.domain.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DarkModeUpdateRequest {

    private Boolean darkModeEnabled;
}
