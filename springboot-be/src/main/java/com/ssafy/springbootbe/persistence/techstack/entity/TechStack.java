package com.ssafy.springbootbe.persistence.techstack.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tech_stack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Long techStackId;

    @Column(name = "tech_name", nullable = false, unique = true, length = 100)
    private String techName;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "color", length = 20)
    private String color;
}
