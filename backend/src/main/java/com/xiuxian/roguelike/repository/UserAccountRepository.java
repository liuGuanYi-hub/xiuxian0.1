package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    Optional<UserAccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
