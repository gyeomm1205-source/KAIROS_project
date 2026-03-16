package com.ssafy.springbootbe.persistence.quiz.repository;

import com.ssafy.springbootbe.persistence.quiz.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    // 유저의 퀴즈 세션 목록 (idx_quiz_session_user)
    List<QuizSession> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
