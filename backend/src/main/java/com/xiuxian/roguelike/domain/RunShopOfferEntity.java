package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_shop_offer", indexes = {
        @Index(name = "idx_shop_offer_shop_status", columnList = "shop_id,status,slot_number")
})
public class RunShopOfferEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "shop_id", nullable = false, length = 36)
    private String shopId;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "card_id", nullable = false, length = 60)
    private String cardId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 20)
    private String archetype;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(name = "effect_text", nullable = false, length = 240)
    private String effectText;

    @Column(nullable = false)
    private int price;

    @Column(name = "slot_number", nullable = false)
    private int slot;

    @Column(nullable = false)
    private int generation;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RunShopOfferEntity() {
    }

    public RunShopOfferEntity(String shopId, String runId, String cardId, String category,
                              String archetype, String name, String rarity, String description,
                              String effectText, int price, int slot, int generation) {
        this.id = UUID.randomUUID().toString();
        this.shopId = shopId;
        this.runId = runId;
        this.cardId = cardId;
        this.category = category;
        this.archetype = archetype;
        this.name = name;
        this.rarity = rarity;
        this.description = description;
        this.effectText = effectText;
        this.price = price;
        this.slot = slot;
        this.generation = generation;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getShopId() { return shopId; }
    public String getRunId() { return runId; }
    public String getCardId() { return cardId; }
    public String getCategory() { return category; }
    public String getArchetype() { return archetype; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public String getEffectText() { return effectText; }
    public int getPrice() { return price; }
    public int getSlot() { return slot; }
    public int getGeneration() { return generation; }
    public String getStatus() { return status; }

    public void markSold() { this.status = "SOLD"; }
    public void expire() { this.status = "EXPIRED"; }
}
