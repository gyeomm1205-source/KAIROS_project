package com.ssafy.springbootbe.persistence.analysis.entity;

import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import com.ssafy.springbootbe.persistence.analysis.type.FitLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analysis_recommended_position")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisRecommendedPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_recommended_position_id")
    private Long analysisRecommendedPositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id", nullable = false)
    private AnalysisReport analysisReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_position_id", nullable = false)
    private DevPosition devPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "fit_level", nullable = false)
    private FitLevel fitLevel;
}
