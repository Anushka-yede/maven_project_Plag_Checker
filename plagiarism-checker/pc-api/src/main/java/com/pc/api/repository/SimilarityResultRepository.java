package com.pc.api.repository;

import com.pc.api.entity.SimilarityResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimilarityResultRepository extends JpaRepository<SimilarityResultEntity, UUID> {

    List<SimilarityResultEntity> findByDocAIdOrDocBId(UUID docAId, UUID docBId);

    @Query("""
        SELECT r FROM SimilarityResultEntity r
        WHERE r.docAId IN :submissionIds OR r.docBId IN :submissionIds
        ORDER BY r.finalScore DESC
    """)
    List<SimilarityResultEntity> findByCohort(List<UUID> submissionIds);
}
