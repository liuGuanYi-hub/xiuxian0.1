package com.xiuxian.roguelike.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_config")
public class ItemConfigEntity extends BuildConfigEntity {

    protected ItemConfigEntity() {
    }

    public ItemConfigEntity(String cardId, String name, String rarity, String description,
                            String effectText, String archetype, int healthOnClaim,
                            int spiritOnClaim, int lifespanOnClaim, int karmaOnClaim,
                            int battleHealthBonus, int battleSpiritBonus, int battleWeight,
                            int eliteWeight, int treasureWeight, boolean enabled) {
        super(cardId, name, rarity, description, effectText, archetype, healthOnClaim,
                spiritOnClaim, lifespanOnClaim, karmaOnClaim, battleHealthBonus,
                battleSpiritBonus, battleWeight, eliteWeight, treasureWeight, enabled);
    }
}
