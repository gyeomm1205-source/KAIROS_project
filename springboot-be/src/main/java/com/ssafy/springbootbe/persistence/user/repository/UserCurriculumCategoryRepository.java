package com.ssafy.springbootbe.persistence.user.repository;

import com.ssafy.springbootbe.persistence.user.entity.UserCurriculumCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCurriculumCategoryRepository extends JpaRepository<UserCurriculumCategory, Long> {

    List<UserCurriculumCategory> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);
}
