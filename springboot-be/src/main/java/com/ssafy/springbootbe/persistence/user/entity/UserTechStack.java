package com.ssafy.springbootbe.persistence.user.entity;

import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_tech_stack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_tech_stack_id")
    private Long userTechStackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id", nullable = false)
    private TechStack techStack;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;

    public void updateScore(Integer score) {
        this.score = score;
    }
}
