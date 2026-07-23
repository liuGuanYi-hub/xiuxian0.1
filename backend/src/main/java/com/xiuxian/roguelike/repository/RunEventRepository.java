package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunEventRepository extends JpaRepository<RunEventEntity, String> {

    List<RunEventEntity> findByRunIdOrderByTurnAsc(String runId);

    Optional<RunEventEntity> findByRequestId(String requestId);
}

