package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RewardOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardOfferRepository extends JpaRepository<RewardOfferEntity, String> {

    List<RewardOfferEntity> findByRunIdAndStatusOrderByCreatedAtAsc(String runId, String status);

    Optional<RewardOfferEntity> findByIdAndRunId(String id, String runId);
}
