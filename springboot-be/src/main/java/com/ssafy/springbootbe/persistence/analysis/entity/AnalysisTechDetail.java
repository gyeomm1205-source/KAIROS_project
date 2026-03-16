package com.ssafy.springbootbe.persistence.analysis.entity;

import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analysis_tech_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisTechDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_tech_detail_id")
    private Long analysisTechDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_report_id", nullable = false)
    private AnalysisReport analysisReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id", nullable = false)
    private TechStack techStack;

    @Column(name = "proficiency_percentage", nullable = false)
    @Builder.Default
    private Integer proficiencyPercentage = 0;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;
}
