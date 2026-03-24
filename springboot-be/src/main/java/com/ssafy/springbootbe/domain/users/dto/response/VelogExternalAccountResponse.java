package com.ssafy.springbootbe.domain.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelogExternalAccountResponse {

    private String username;
    private LocalDateTime lastSyncedAt;
    private int totalPosts;
    private LocalDate latestPostDate;
    private long totalViews;
}
