package com.ssafy.s14p21a506.mockpost.entity;

import com.ssafy.s14p21a506.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mock_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MockPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 10_000)
    private String content;

    private MockPost(Long authorId, String title, String content) {
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }

    @Builder(builderMethodName = "createBuilder")
    public static MockPost create(Long authorId, String title, String content) {
        return new MockPost(authorId, title, content);
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
