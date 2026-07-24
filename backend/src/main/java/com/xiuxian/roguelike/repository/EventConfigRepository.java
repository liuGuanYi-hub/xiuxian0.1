package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.EventConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventConfigRepository extends JpaRepository<EventConfigEntity, String> {

    List<EventConfigEntity> findByEnabledTrueOrderByEventIdAsc();

    List<EventConfigEntity> findAllByOrderByEventIdAsc();
}
