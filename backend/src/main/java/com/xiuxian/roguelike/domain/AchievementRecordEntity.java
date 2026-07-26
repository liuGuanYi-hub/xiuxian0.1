package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "achievement_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_achievement_record_user_achievement", columnNames = {"user_id", "achievement_id"})
}, indexes = {
        @Index(name = "idx_achievement_record_user", columnList = "user_id,awarded_at")
})
public class AchievementRecordEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(name = "achievement_id", length = 60, nullable = false, updatable = false)
    private String achievementId;

    @Column(name = "trigger_run_id", length = 36, updatable = false)
    private String triggerRunId;

    @Column(name = "reward_causality", nullable = false, updatable = false)
    private int rewardCausality;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    protected AchievementRecordEntity() {
    }

    public AchievementRecordEntity(String userId, String achievementId, String triggerRunId, int rewardCausality) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.achievementId = achievementId;
        this.triggerRunId = triggerRunId;
        this.rewardCausality = rewardCausality;
        this.awardedAt = LocalDateTime.now();
    }

    public String getUserId() {
        return userId;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public String getTriggerRunId() {
        return triggerRunId;
    }

    public int getRewardCausality() {
        return rewardCausality;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }
}
