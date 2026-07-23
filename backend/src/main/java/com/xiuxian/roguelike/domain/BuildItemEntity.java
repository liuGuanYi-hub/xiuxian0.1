package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_build_item", indexes = {
        @Index(name = "idx_build_item_run", columnList = "run_id,created_at")
})
public class BuildItemEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(nullable = false, length = 60)
    private String cardId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(name = "effect_text", nullable = false, length = 240)
    private String effectText;

    @Column(name = "source_node_id", length = 36)
    private String sourceNodeId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected BuildItemEntity() {
    }

    public BuildItemEntity(String runId, String cardId, String category, String name, String rarity,
                           String description, String effectText, String sourceNodeId) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.cardId = cardId;
        this.category = category;
        this.name = name;
        this.rarity = rarity;
        this.description = description;
        this.effectText = effectText;
        this.sourceNodeId = sourceNodeId;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getCardId() { return cardId; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public String getEffectText() { return effectText; }
    public String getSourceNodeId() { return sourceNodeId; }
}
