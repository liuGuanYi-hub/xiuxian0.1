package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.GameRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRunRepository extends JpaRepository<GameRunEntity, String> {

    Optional<GameRunEntity> findByIdAndUserId(String id, String userId);

    List<GameRunEntity> findByUserIdOrderByUpdatedAtDesc(String userId);
}
