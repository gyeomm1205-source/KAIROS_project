package com.ssafy.springbootbe.persistence.reference.repository;

import com.ssafy.springbootbe.persistence.reference.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    // URL 중복 확인 (idx_reference_url)
    boolean existsByUrl(String url);

    Optional<Reference> findByUrl(String url);
}
