package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunMapNodeRepository extends JpaRepository<RunMapNodeEntity, String> {

    List<RunMapNodeEntity> findByRunIdOrderByFloorAscPositionAsc(String runId);

    Optional<RunMapNodeEntity> findByIdAndRunId(String id, String runId);
}

