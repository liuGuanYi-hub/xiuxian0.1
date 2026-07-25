package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.PlayerCharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacterEntity, String> {

    List<PlayerCharacterEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<PlayerCharacterEntity> findByIdAndUserId(String id, String userId);
}
