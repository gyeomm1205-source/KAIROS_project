package com.ssafy.springbootbe.domain.activities.service;

public interface LearningStateService {

    void recalculateForUser(Long userId);

    void recalculateForAllUsers();
}
