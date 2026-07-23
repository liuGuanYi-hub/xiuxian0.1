package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunCombatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RunCombatRepository extends JpaRepository<RunCombatEntity, String> {

    Optional<RunCombatEntity> findByRunIdAndStatus(String runId, String status);
}
