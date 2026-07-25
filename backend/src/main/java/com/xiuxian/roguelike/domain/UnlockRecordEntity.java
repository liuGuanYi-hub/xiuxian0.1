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
@Table(name = "unlock_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_unlock_record_user_unlock", columnNames = {"user_id", "unlock_id"})
}, indexes = {
        @Index(name = "idx_unlock_record_user", columnList = "user_id,unlocked_at")
})
public class UnlockRecordEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(name = "unlock_id", length = 60, nullable = false, updatable = false)
    private String unlockId;

    @Column(nullable = false, updatable = false)
    private int cost;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private LocalDateTime unlockedAt;

    protected UnlockRecordEntity() {
    }

    public UnlockRecordEntity(String userId, String unlockId, int cost) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.unlockId = unlockId;
        this.cost = cost;
        this.unlockedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUnlockId() { return unlockId; }
    public int getCost() { return cost; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
}
