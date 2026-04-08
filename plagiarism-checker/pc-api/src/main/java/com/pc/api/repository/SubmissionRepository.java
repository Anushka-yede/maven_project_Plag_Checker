package com.pc.api.repository;

import com.pc.api.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {
    List<SubmissionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SubmissionEntity> findByAssignmentIdOrderByCreatedAtDesc(String assignmentId);
    long countByUserId(UUID userId);
}
