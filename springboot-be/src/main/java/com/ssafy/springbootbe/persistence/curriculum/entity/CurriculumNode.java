package com.ssafy.springbootbe.persistence.curriculum.entity;

import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "curriculum_node")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurriculumNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curriculum_node_id")
    private Long curriculumNodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "expected_minutes", nullable = false)
    @Builder.Default
    private Integer expectedMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false)
    @Builder.Default
    private ProgressStatus progressStatus = ProgressStatus.NOT_STARTED;

    public void update(String title, String description, LocalDate scheduledDate,
                       ProgressStatus progressStatus) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (scheduledDate != null) this.scheduledDate = scheduledDate;
        if (progressStatus != null) this.progressStatus = progressStatus;
    }
}
