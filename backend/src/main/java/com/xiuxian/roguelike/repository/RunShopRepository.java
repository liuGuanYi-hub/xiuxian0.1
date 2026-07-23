package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RunShopRepository extends JpaRepository<RunShopEntity, String> {
    Optional<RunShopEntity> findByIdAndRunId(String id, String runId);
    Optional<RunShopEntity> findByRunIdAndNodeId(String runId, String nodeId);
}
