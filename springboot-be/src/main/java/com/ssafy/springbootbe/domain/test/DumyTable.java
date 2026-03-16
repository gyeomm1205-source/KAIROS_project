package com.ssafy.springbootbe.domain.test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DumyTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dumyId;

    private String dumyName;
}
