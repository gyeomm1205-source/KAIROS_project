package com.ssafy.s14p21a506.mockpost.repository;

import com.ssafy.s14p21a506.mockpost.entity.MockPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockPostRepository extends JpaRepository<MockPost, Long> {
}

