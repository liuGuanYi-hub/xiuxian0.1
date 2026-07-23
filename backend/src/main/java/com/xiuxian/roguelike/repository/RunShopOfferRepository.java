package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.RunShopOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunShopOfferRepository extends JpaRepository<RunShopOfferEntity, String> {
    List<RunShopOfferEntity> findByShopIdAndStatusOrderBySlotAsc(String shopId, String status);
    Optional<RunShopOfferEntity> findByIdAndShopId(String id, String shopId);
}
