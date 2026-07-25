package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.UnlockRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnlockRecordRepository extends JpaRepository<UnlockRecordEntity, String> {

    List<UnlockRecordEntity> findByUserIdOrderByUnlockedAtAsc(String userId);

    Optional<UnlockRecordEntity> findByUserIdAndUnlockId(String userId, String unlockId);
}
