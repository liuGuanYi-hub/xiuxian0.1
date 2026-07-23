package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.BuildItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildItemRepository extends JpaRepository<BuildItemEntity, String> {

    List<BuildItemEntity> findByRunIdOrderByCreatedAtAsc(String runId);
}
