package com.pc.api.repository;

import com.pc.api.entity.MatchedSpanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchedSpanRepository extends JpaRepository<MatchedSpanEntity, UUID> {
    List<MatchedSpanEntity> findByResultId(UUID resultId);
    List<MatchedSpanEntity> findByDocId(UUID docId);
}
