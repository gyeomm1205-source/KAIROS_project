package com.ssafy.springbootbe.persistence.reference.entity;

import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reference_tech_stack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReferenceTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reference_tech_stack_id")
    private Long referenceTechStackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id", nullable = false)
    private Reference reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id", nullable = false)
    private TechStack techStack;
}
