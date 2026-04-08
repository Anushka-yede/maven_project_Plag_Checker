package com.pc.api.repository;

import com.pc.api.entity.AnnotationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnotationRepository extends JpaRepository<AnnotationEntity, UUID> {
    List<AnnotationEntity> findByResultId(UUID resultId);
    List<AnnotationEntity> findByReviewerId(UUID reviewerId);
}
