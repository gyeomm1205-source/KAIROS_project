package com.ssafy.springbootbe.persistence.analysis.entity;

import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.analysis.type.AnalysisStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_report_id")
    private Long analysisReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void complete(String summary) {
        this.status = AnalysisStatus.COMPLETED;
        this.summary = summary;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = AnalysisStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void updateStatus(AnalysisStatus status) {
        this.status = status;
    }
}
