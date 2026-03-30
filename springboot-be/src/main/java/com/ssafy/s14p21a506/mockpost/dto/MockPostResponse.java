package com.ssafy.s14p21a506.mockpost.dto;

import com.ssafy.s14p21a506.mockpost.entity.MockPost;
import java.time.Instant;

public record MockPostResponse(
        Long id,
        String title,
        String content,
        String authorSub,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
    public static MockPostResponse from(MockPost post) {
        return new MockPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorSub(),
                post.getAuthorName(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
