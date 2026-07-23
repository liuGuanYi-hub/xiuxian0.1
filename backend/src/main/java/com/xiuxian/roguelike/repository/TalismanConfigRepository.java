package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.TalismanConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TalismanConfigRepository extends JpaRepository<TalismanConfigEntity, String> {
    List<TalismanConfigEntity> findByEnabledTrueOrderByCardIdAsc();
}
