package com.ssafy.springbootbe.persistence.position.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dev_position")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DevPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_position_id")
    private Long devPositionId;

    @Column(name = "position_name", nullable = false, length = 100)
    private String positionName;
}
