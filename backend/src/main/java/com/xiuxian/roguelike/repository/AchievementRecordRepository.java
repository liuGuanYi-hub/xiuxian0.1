package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.AchievementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRecordRepository extends JpaRepository<AchievementRecordEntity, String> {

    Optional<AchievementRecordEntity> findByUserIdAndAchievementId(String userId, String achievementId);

    List<AchievementRecordEntity> findByUserIdOrderByAwardedAtAsc(String userId);
}
