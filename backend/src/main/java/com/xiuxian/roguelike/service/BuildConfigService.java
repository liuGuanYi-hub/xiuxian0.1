package com.xiuxian.roguelike.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.roguelike.domain.BuildConfigEntity;
import com.xiuxian.roguelike.domain.ItemConfigEntity;
import com.xiuxian.roguelike.domain.SkillConfigEntity;
import com.xiuxian.roguelike.domain.TalismanConfigEntity;
import com.xiuxian.roguelike.repository.ItemConfigRepository;
import com.xiuxian.roguelike.repository.SkillConfigRepository;
import com.xiuxian.roguelike.repository.TalismanConfigRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BuildConfigService {

    private final SkillConfigRepository skillRepository;
    private final ItemConfigRepository itemRepository;
    private final TalismanConfigRepository talismanRepository;
    private final ObjectMapper objectMapper;
    private volatile Map<String, CardDefinition> cache = Map.of();

    public BuildConfigService(SkillConfigRepository skillRepository, ItemConfigRepository itemRepository,
                              TalismanConfigRepository talismanRepository, ObjectMapper objectMapper) {
        this.skillRepository = skillRepository;
        this.itemRepository = itemRepository;
        this.talismanRepository = talismanRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void initialize() {
        try {
            List<CardSeed> seeds = objectMapper.readValue(
                    new ClassPathResource("card-config.json").getInputStream(),
                    new TypeReference<>() { }
            );
            for (CardSeed seed : seeds) {
                saveIfMissing(seed);
            }
            reload();
        } catch (IOException exception) {
            throw new IllegalStateException("卡牌配置初始化失败。", exception);
        }
    }

    public CardDefinition get(String cardId) {
        CardDefinition card = cache.get(cardId);
        if (card == null) {
            reload();
            card = cache.get(cardId);
        }
        if (card == null) {
            throw new IllegalArgumentException("构筑卡牌不存在：" + cardId);
        }
        return card;
    }

    public List<CardDefinition> starterCards() {
        return List.of(get("qi_guiding"), get("healing_talisman"));
    }

    public List<WeightedCard> poolFor(String nodeType) {
        int nodeWeight = switch (nodeType) {
            case "ELITE" -> 1;
            case "TREASURE" -> 2;
            default -> 0;
        };
        return cache.values().stream()
                .filter(CardDefinition::enabled)
                .map(card -> new WeightedCard(card.cardId(), switch (nodeWeight) {
                    case 1 -> card.eliteWeight();
                    case 2 -> card.treasureWeight();
                    default -> card.battleWeight();
                }))
                .filter(card -> card.weight() > 0)
                .toList();
    }

    public List<CardDefinition> all() {
        return new ArrayList<>(cache.values());
    }

    private void reload() {
        Map<String, CardDefinition> next = new HashMap<>();
        skillRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "功法")));
        itemRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "法宝")));
        talismanRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "符箓")));
        cache = Map.copyOf(next);
    }

    private void saveIfMissing(CardSeed seed) {
        if ("功法".equals(seed.category()) && !skillRepository.existsById(seed.cardId())) {
            skillRepository.save(new SkillConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.battleWeight(), seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        } else if ("法宝".equals(seed.category()) && !itemRepository.existsById(seed.cardId())) {
            itemRepository.save(new ItemConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.battleWeight(), seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        } else if ("符箓".equals(seed.category()) && !talismanRepository.existsById(seed.cardId())) {
            talismanRepository.save(new TalismanConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.battleWeight(), seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        }
    }

    private CardDefinition toDefinition(BuildConfigEntity row, String category) {
        return new CardDefinition(row.getCardId(), category, row.getName(), row.getRarity(), row.getDescription(),
                row.getEffectText(), row.getArchetype(), row.getHealthOnClaim(), row.getSpiritOnClaim(),
                row.getLifespanOnClaim(), row.getKarmaOnClaim(), row.getBattleHealthBonus(),
                row.getBattleSpiritBonus(), row.getBattleWeight(), row.getEliteWeight(),
                row.getTreasureWeight(), row.isEnabled());
    }

    public record WeightedCard(String cardId, int weight) { }

    public record CardDefinition(String cardId, String category, String name, String rarity,
                                 String description, String effectText, String archetype,
                                 int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                                 int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                                 int battleWeight, int eliteWeight, int treasureWeight, boolean enabled) { }

    private record CardSeed(String cardId, String category, String name, String rarity,
                            String description, String effectText, String archetype,
                            int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                            int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                            int battleWeight, int eliteWeight, int treasureWeight, boolean enabled) { }
}
