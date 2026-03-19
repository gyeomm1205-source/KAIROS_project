package com.ssafy.springbootbe.persistence.curriculum.entity;

import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "curriculum")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Curriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curriculum_id")
    private Long curriculumId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CurriculumStatus status = CurriculumStatus.ACTIVE;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateStatus(CurriculumStatus status) {
        this.status = status;
    }
}
