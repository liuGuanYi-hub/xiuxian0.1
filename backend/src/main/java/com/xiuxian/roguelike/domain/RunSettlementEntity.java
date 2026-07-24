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
@Table(name = "run_settlement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_run_settlement_run_id", columnNames = "run_id")
}, indexes = {
        @Index(name = "idx_run_settlement_score", columnList = "score,settled_at")
})
public class RunSettlementEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", length = 36, nullable = false, updatable = false)
    private String runId;

    @Column(name = "player_name", length = 32, nullable = false)
    private String playerName;

    @Column(length = 32, nullable = false)
    private String origin;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "ending_id", length = 40)
    private String endingId;

    @Column(name = "ending_title", length = 120, nullable = false)
    private String endingTitle;

    @Column(name = "floor_reached", nullable = false)
    private int floorReached;

    @Column(nullable = false)
    private int turn;

    @Column(nullable = false)
    private int health;

    @Column(nullable = false)
    private int spirit;

    @Column(nullable = false)
    private int lifespan;

    @Column(nullable = false)
    private int karma;

    @Column(name = "spirit_stones", nullable = false)
    private int spiritStones;

    @Column(name = "active_cards", nullable = false)
    private int activeCards;

    @Column(name = "elite_count", nullable = false)
    private int eliteCount;

    @Column(nullable = false)
    private int score;

    @Column(name = "run_seed", nullable = false)
    private long runSeed;

    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;

    protected RunSettlementEntity() {
    }

    public RunSettlementEntity(String runId, String playerName, String origin, String status,
                               String endingId, String endingTitle, int floorReached, int turn,
                               int health, int spirit, int lifespan, int karma, int spiritStones,
                               int activeCards, int eliteCount, int score, long runSeed) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.playerName = playerName;
        this.origin = origin;
        this.status = status;
        this.endingId = endingId;
        this.endingTitle = endingTitle;
        this.floorReached = floorReached;
        this.turn = turn;
        this.health = health;
        this.spirit = spirit;
        this.lifespan = lifespan;
        this.karma = karma;
        this.spiritStones = spiritStones;
        this.activeCards = activeCards;
        this.eliteCount = eliteCount;
        this.score = score;
        this.runSeed = runSeed;
        this.settledAt = LocalDateTime.now();
    }

    public String getRunId() { return runId; }
    public String getPlayerName() { return playerName; }
    public String getOrigin() { return origin; }
    public String getStatus() { return status; }
    public String getEndingId() { return endingId; }
    public String getEndingTitle() { return endingTitle; }
    public int getFloorReached() { return floorReached; }
    public int getTurn() { return turn; }
    public int getHealth() { return health; }
    public int getSpirit() { return spirit; }
    public int getLifespan() { return lifespan; }
    public int getKarma() { return karma; }
    public int getSpiritStones() { return spiritStones; }
    public int getActiveCards() { return activeCards; }
    public int getEliteCount() { return eliteCount; }
    public int getScore() { return score; }
    public long getRunSeed() { return runSeed; }
    public LocalDateTime getSettledAt() { return settledAt; }
}
