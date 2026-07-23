package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BuildConfigEntity {

    @Id
    @Column(name = "card_id", length = 60, nullable = false, updatable = false)
    private String cardId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(name = "effect_text", nullable = false, length = 240)
    private String effectText;

    @Column(nullable = false, length = 20)
    private String archetype;

    @Column(name = "health_on_claim", nullable = false)
    private int healthOnClaim;

    @Column(name = "spirit_on_claim", nullable = false)
    private int spiritOnClaim;

    @Column(name = "lifespan_on_claim", nullable = false)
    private int lifespanOnClaim;

    @Column(name = "karma_on_claim", nullable = false)
    private int karmaOnClaim;

    @Column(name = "battle_health_bonus", nullable = false)
    private int battleHealthBonus;

    @Column(name = "battle_spirit_bonus", nullable = false)
    private int battleSpiritBonus;

    @Column(name = "battle_weight", nullable = false)
    private int battleWeight;

    @Column(name = "elite_weight", nullable = false)
    private int eliteWeight;

    @Column(name = "treasure_weight", nullable = false)
    private int treasureWeight;

    @Column(nullable = false)
    private boolean enabled;

    protected BuildConfigEntity() {
    }

    protected BuildConfigEntity(String cardId, String name, String rarity, String description,
                                String effectText, String archetype, int healthOnClaim,
                                int spiritOnClaim, int lifespanOnClaim, int karmaOnClaim,
                                int battleHealthBonus, int battleSpiritBonus, int battleWeight,
                                int eliteWeight, int treasureWeight, boolean enabled) {
        this.cardId = cardId;
        this.name = name;
        this.rarity = rarity;
        this.description = description;
        this.effectText = effectText;
        this.archetype = archetype;
        this.healthOnClaim = healthOnClaim;
        this.spiritOnClaim = spiritOnClaim;
        this.lifespanOnClaim = lifespanOnClaim;
        this.karmaOnClaim = karmaOnClaim;
        this.battleHealthBonus = battleHealthBonus;
        this.battleSpiritBonus = battleSpiritBonus;
        this.battleWeight = battleWeight;
        this.eliteWeight = eliteWeight;
        this.treasureWeight = treasureWeight;
        this.enabled = enabled;
    }

    public String getCardId() { return cardId; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public String getEffectText() { return effectText; }
    public String getArchetype() { return archetype; }
    public int getHealthOnClaim() { return healthOnClaim; }
    public int getSpiritOnClaim() { return spiritOnClaim; }
    public int getLifespanOnClaim() { return lifespanOnClaim; }
    public int getKarmaOnClaim() { return karmaOnClaim; }
    public int getBattleHealthBonus() { return battleHealthBonus; }
    public int getBattleSpiritBonus() { return battleSpiritBonus; }
    public int getBattleWeight() { return battleWeight; }
    public int getEliteWeight() { return eliteWeight; }
    public int getTreasureWeight() { return treasureWeight; }
    public boolean isEnabled() { return enabled; }
}
