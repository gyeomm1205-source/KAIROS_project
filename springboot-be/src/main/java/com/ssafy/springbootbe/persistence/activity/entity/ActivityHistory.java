package com.ssafy.springbootbe.persistence.activity.entity;

import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;
@Entity
@Table(name = "activity_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActivityHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_history_id")
    private Long activityHistoryId;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private CurriculumCategory category;
    @Column(name = "curriculum_id")
    private Long curriculumId;
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "is_included", nullable = false)
    @Builder.Default
    private Boolean isIncluded = true;
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    public void toggleInclusion(boolean isIncluded) {
        this.isIncluded = isIncluded;
    }
}
