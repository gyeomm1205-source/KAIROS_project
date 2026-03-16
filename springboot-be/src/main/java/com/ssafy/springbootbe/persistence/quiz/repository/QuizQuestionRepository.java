package com.ssafy.springbootbe.persistence.quiz.repository;

import com.ssafy.springbootbe.persistence.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    // 세션 내 전체 문제 조회
    List<QuizQuestion> findByQuizSessionQuizSessionId(Long quizSessionId);

    // 세션 내 미제출 문제 존재 여부 확인 (complete 전 검증)
    boolean existsByQuizSessionQuizSessionIdAndSelectedAnswerIsNull(Long quizSessionId);
}
