package com.ssafy.springbootbe.domain.activities.service;

public interface LearningStateBaselineService {

    void captureInitialBaselineIfAbsent(Long userId);
}
