package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_combat", indexes = {
        @Index(name = "idx_run_combat_run_status", columnList = "run_id,status")
})
public class RunCombatEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "node_id", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "enemy_id", nullable = false, length = 40)
    private String enemyId;

    @Column(name = "enemy_name", nullable = false, length = 80)
    private String enemyName;

    @Column(name = "enemy_type", nullable = false, length = 20)
    private String enemyType;

    @Column(name = "enemy_description", nullable = false, length = 240)
    private String enemyDescription;

    @Column(name = "max_health", nullable = false)
    private int maxHealth;

    @Column(nullable = false)
    private int health;

    @Column(name = "enemy_block", nullable = false)
    private int enemyBlock;

    @Column(name = "enemy_power", nullable = false)
    private int enemyPower;

    @Column(name = "enemy_poison", nullable = false)
    private int enemyPoison;

    @Column(name = "player_block", nullable = false)
    private int playerBlock;

    @Column(name = "player_poison", nullable = false)
    private int playerPoison;

    @Column(nullable = false, length = 20)
    private String intent;

    @Column(name = "intent_value", nullable = false)
    private int intentValue;

    @Column(nullable = false)
    private int turn;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "combat_log", nullable = false, columnDefinition = "TEXT")
    private String combatLog;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected RunCombatEntity() {
    }

    public RunCombatEntity(String runId, String nodeId, String enemyId, String enemyName,
                           String enemyType, String enemyDescription, int maxHealth,
                           String intent, int intentValue) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.nodeId = nodeId;
        this.enemyId = enemyId;
        this.enemyName = enemyName;
        this.enemyType = enemyType;
        this.enemyDescription = enemyDescription;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.enemyBlock = 0;
        this.enemyPower = 0;
        this.enemyPoison = 0;
        this.playerBlock = 0;
        this.playerPoison = 0;
        this.intent = intent;
        this.intentValue = intentValue;
        this.turn = 1;
        this.status = "ACTIVE";
        this.combatLog = "战斗开始：" + enemyName + "看穿了你的来意。";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getNodeId() { return nodeId; }
    public String getEnemyId() { return enemyId; }
    public String getEnemyName() { return enemyName; }
    public String getEnemyType() { return enemyType; }
    public String getEnemyDescription() { return enemyDescription; }
    public int getMaxHealth() { return maxHealth; }
    public int getHealth() { return health; }
    public int getEnemyBlock() { return enemyBlock; }
    public int getEnemyPower() { return enemyPower; }
    public int getEnemyPoison() { return enemyPoison; }
    public int getPlayerBlock() { return playerBlock; }
    public int getPlayerPoison() { return playerPoison; }
    public String getIntent() { return intent; }
    public int getIntentValue() { return intentValue; }
    public int getTurn() { return turn; }
    public String getStatus() { return status; }
    public String getCombatLog() { return combatLog; }

    public void setHealth(int health) { this.health = Math.max(0, health); touch(); }
    public void setEnemyBlock(int enemyBlock) { this.enemyBlock = Math.max(0, enemyBlock); touch(); }
    public void setEnemyPower(int enemyPower) { this.enemyPower = Math.max(0, enemyPower); touch(); }
    public void setEnemyPoison(int enemyPoison) { this.enemyPoison = Math.max(0, enemyPoison); touch(); }
    public void setPlayerBlock(int playerBlock) { this.playerBlock = Math.max(0, playerBlock); touch(); }
    public void setPlayerPoison(int playerPoison) { this.playerPoison = Math.max(0, playerPoison); touch(); }
    public void setIntent(String intent, int intentValue) {
        this.intent = intent;
        this.intentValue = Math.max(0, intentValue);
        touch();
    }
    public void nextTurn() { this.turn += 1; touch(); }
    public void win() { this.status = "WON"; touch(); }
    public void lose() { this.status = "LOST"; touch(); }

    public void appendLog(String message) {
        if (message == null || message.isBlank()) return;
        this.combatLog = this.combatLog == null || this.combatLog.isBlank()
                ? message : this.combatLog + "\n" + message;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
