package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.GameRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRunRepository extends JpaRepository<GameRunEntity, String> {
}

