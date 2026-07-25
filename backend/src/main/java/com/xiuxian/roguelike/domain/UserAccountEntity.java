package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_account", indexes = {
        @Index(name = "uk_user_account_username", columnList = "username", unique = true)
})
public class UserAccountEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 40, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 300)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "causality_points", nullable = false)
    private int causalityPoints;

    @Column(name = "total_causality_earned", nullable = false)
    private int totalCausalityEarned;

    @Column(name = "total_causality_spent", nullable = false)
    private int totalCausalitySpent;

    protected UserAccountEntity() {
    }

    public UserAccountEntity(String id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.causalityPoints = 0;
        this.totalCausalityEarned = 0;
        this.totalCausalitySpent = 0;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getCausalityPoints() { return causalityPoints; }
    public int getTotalCausalityEarned() { return totalCausalityEarned; }
    public int getTotalCausalitySpent() { return totalCausalitySpent; }

    public void addCausality(int amount) {
        if (amount < 0) throw new IllegalArgumentException("因果点增加值不能为负数。");
        this.causalityPoints += amount;
        this.totalCausalityEarned += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void spendCausality(int amount) {
        if (amount < 0 || this.causalityPoints < amount) {
            throw new IllegalArgumentException("因果点不足。");
        }
        this.causalityPoints -= amount;
        this.totalCausalitySpent += amount;
        this.updatedAt = LocalDateTime.now();
    }
}
