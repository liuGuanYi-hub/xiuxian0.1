package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.ConfigOperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigOperationLogRepository extends JpaRepository<ConfigOperationLogEntity, String> {

    List<ConfigOperationLogEntity> findTop50ByOrderByCreatedAtDesc();
}
