package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.ItemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemConfigRepository extends JpaRepository<ItemConfigEntity, String> {
    List<ItemConfigEntity> findByEnabledTrueOrderByCardIdAsc();
}
