package com.xiuxian.roguelike.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill_config")
public class SkillConfigEntity extends BuildConfigEntity {

    protected SkillConfigEntity() {
    }

    public SkillConfigEntity(String cardId, String name, String rarity, String description,
                             String effectText, String archetype, int healthOnClaim,
                             int spiritOnClaim, int lifespanOnClaim, int karmaOnClaim,
                             int battleHealthBonus, int battleSpiritBonus,
                             int combatDamageBonus, int combatBlockBonus, int combatSpiritGain,
                             int combatPoisonBonus, int battleWeight, int eliteWeight,
                             int treasureWeight, boolean enabled) {
        super(cardId, name, rarity, description, effectText, archetype, healthOnClaim,
                spiritOnClaim, lifespanOnClaim, karmaOnClaim, battleHealthBonus,
                battleSpiritBonus, combatDamageBonus, combatBlockBonus, combatSpiritGain,
                combatPoisonBonus, battleWeight, eliteWeight, treasureWeight, enabled);
    }
}
