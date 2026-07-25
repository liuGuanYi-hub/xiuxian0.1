package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunSettlementEntity;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunSettlementRepository extends JpaRepository<RunSettlementEntity, String> {

    Optional<RunSettlementEntity> findByRunId(String runId);

    List<RunSettlementEntity> findAllByOrderByScoreDescSettledAtAsc(Pageable pageable);

    List<RunSettlementEntity> findByUserIdOrderBySettledAtDesc(String userId, Pageable pageable);

    long countByUserId(String userId);
}
