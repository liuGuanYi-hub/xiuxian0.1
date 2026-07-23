package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_run")
public class GameRunEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 32)
    private String playerName;

    @Column(nullable = false, length = 32)
    private String origin;

    @Column(nullable = false, length = 32)
    private String realm;

    @Column(nullable = false)
    private int health;

    @Column(nullable = false)
    private int spirit;

    @Column(nullable = false)
    private int lifespan;

    @Column(nullable = false)
    private int karma;

    @Column(nullable = false)
    private long seed;

    @Column(nullable = false)
    private int turn;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 80)
    private String currentEventId;

    @Column(nullable = false, length = 36)
    private String currentNodeId;

    @Column(nullable = false)
    private int currentFloor;

    @Column(length = 40)
    private String endingId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    protected GameRunEntity() {
    }

    public GameRunEntity(String id, String playerName, String origin, long seed, String currentEventId) {
        this.id = id;
        this.playerName = playerName;
        this.origin = origin;
        this.realm = "炼气一层";
        this.health = 100;
        this.spirit = 30;
        this.lifespan = 80;
        this.karma = 0;
        this.seed = seed;
        this.turn = 0;
        this.status = "RUNNING";
        this.currentEventId = currentEventId;
        this.currentNodeId = "";
        this.currentFloor = 0;
        this.endingId = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getPlayerName() { return playerName; }
    public String getOrigin() { return origin; }
    public String getRealm() { return realm; }
    public int getHealth() { return health; }
    public int getSpirit() { return spirit; }
    public int getLifespan() { return lifespan; }
    public int getKarma() { return karma; }
    public long getSeed() { return seed; }
    public int getTurn() { return turn; }
    public String getStatus() { return status; }
    public String getCurrentEventId() { return currentEventId; }
    public String getCurrentNodeId() { return currentNodeId; }
    public int getCurrentFloor() { return currentFloor; }
    public String getEndingId() { return endingId; }

    public void applyChoice(int healthDelta, int spiritDelta, int lifespanDelta, int karmaDelta,
                            String nextEventId, String nextRealm, String nextStatus, String nextEndingId) {
        this.health = Math.max(0, this.health + healthDelta);
        this.spirit = Math.max(0, this.spirit + spiritDelta);
        this.lifespan = Math.max(0, this.lifespan + lifespanDelta);
        this.karma += karmaDelta;
        this.turn += 1;
        this.currentEventId = nextEventId;
        this.realm = nextRealm;
        this.status = nextStatus;
        this.endingId = nextEndingId;
        this.updatedAt = LocalDateTime.now();
    }

    public void enterNode(String nodeId, int floor, String eventId) {
        this.currentNodeId = nodeId;
        this.currentFloor = floor;
        this.currentEventId = eventId;
        this.updatedAt = LocalDateTime.now();
    }

    public void clearNode() {
        this.currentNodeId = "";
        this.currentEventId = "awaiting_node";
        this.updatedAt = LocalDateTime.now();
    }
}
