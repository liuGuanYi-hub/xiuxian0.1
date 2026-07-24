package com.xiuxian.roguelike.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.roguelike.domain.BuildConfigEntity;
import com.xiuxian.roguelike.domain.ItemConfigEntity;
import com.xiuxian.roguelike.domain.SkillConfigEntity;
import com.xiuxian.roguelike.domain.TalismanConfigEntity;
import com.xiuxian.roguelike.domain.ConfigOperationLogEntity;
import com.xiuxian.roguelike.repository.ConfigOperationLogRepository;
import com.xiuxian.roguelike.repository.ItemConfigRepository;
import com.xiuxian.roguelike.repository.SkillConfigRepository;
import com.xiuxian.roguelike.repository.TalismanConfigRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BuildConfigService {

    private final SkillConfigRepository skillRepository;
    private final ItemConfigRepository itemRepository;
    private final TalismanConfigRepository talismanRepository;
    private final ConfigOperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;
    private volatile Map<String, CardDefinition> cache = Map.of();

    public BuildConfigService(SkillConfigRepository skillRepository, ItemConfigRepository itemRepository,
                              TalismanConfigRepository talismanRepository,
                              ConfigOperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.skillRepository = skillRepository;
        this.itemRepository = itemRepository;
        this.talismanRepository = talismanRepository;
        this.operationLogRepository = operationLogRepository;
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
            normalizeLegacyVersions();
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

    public synchronized void reload() {
        Map<String, CardDefinition> next = new HashMap<>();
        skillRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "功法")));
        itemRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "法宝")));
        talismanRepository.findByEnabledTrueOrderByCardIdAsc().forEach(row -> next.put(row.getCardId(), toDefinition(row, "符箓")));
        cache = Map.copyOf(next);
    }

    public List<CardConfigSnapshot> allConfigs(boolean includeDisabled) {
        List<CardConfigSnapshot> snapshots = new ArrayList<>();
        skillRepository.findAll().forEach(row -> snapshots.add(toSnapshot(row, "功法")));
        itemRepository.findAll().forEach(row -> snapshots.add(toSnapshot(row, "法宝")));
        talismanRepository.findAll().forEach(row -> snapshots.add(toSnapshot(row, "符箓")));
        return snapshots.stream()
                .filter(row -> includeDisabled || row.enabled())
                .sorted(Comparator.comparing(CardConfigSnapshot::cardId))
                .toList();
    }

    public CardValidationResult validateCurrent() {
        List<String> errors = new ArrayList<>();
        Map<String, String> categories = new HashMap<>();
        for (CardConfigSnapshot card : allConfigs(true)) {
            String previousCategory = categories.putIfAbsent(card.cardId(), card.category());
            if (previousCategory != null && !previousCategory.equals(card.category())) {
                errors.add(card.cardId() + " 同时存在于多个卡牌类别");
            }
            validateSnapshot(card, errors);
        }
        if (skillRepository.findById("qi_guiding").filter(BuildConfigEntity::isEnabled).isEmpty()) {
            errors.add("缺少启用的 starter 功法 qi_guiding");
        }
        if (talismanRepository.findById("healing_talisman").filter(BuildConfigEntity::isEnabled).isEmpty()) {
            errors.add("缺少启用的 starter 符箓 healing_talisman");
        }
        return new CardValidationResult(errors.isEmpty(), categories.size(),
                errors.isEmpty() ? "卡牌配置校验通过。" : String.join("；", errors));
    }

    @jakarta.transaction.Transactional
    public synchronized CardImportResult importConfigs(List<CardConfigPayload> payloads, String operatorName) {
        if (payloads == null || payloads.isEmpty()) {
            throw new IllegalArgumentException("至少需要导入一张卡牌配置。");
        }
        String operator = normalizeOperator(operatorName);
        Map<String, String> existingCategories = new HashMap<>();
        for (CardConfigSnapshot card : allConfigs(true)) {
            String previous = existingCategories.putIfAbsent(card.cardId(), card.category());
            if (previous != null && !previous.equals(card.category())) {
                throw new IllegalArgumentException("现有配置中存在跨类别重复卡牌：" + card.cardId());
            }
        }
        Set<String> importedIds = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (CardConfigPayload payload : payloads) {
            validatePayload(payload, errors);
            if (payload != null && !importedIds.add(payload.cardId())) {
                errors.add("导入内容中重复卡牌编号：" + payload.cardId());
            }
            if (payload != null && existingCategories.containsKey(payload.cardId())
                    && !existingCategories.get(payload.cardId()).equals(payload.category())) {
                errors.add(payload.cardId() + " 不能从 " + existingCategories.get(payload.cardId())
                        + " 改为 " + payload.category());
            }
        }
        if (!errors.isEmpty()) {
            operationLogRepository.save(new ConfigOperationLogEntity("CARD", null, "IMPORT", 0,
                    operator, "FAILED", String.join("；", errors)));
            throw new IllegalArgumentException("卡牌配置校验失败：" + String.join("；", errors));
        }

        for (CardConfigPayload payload : payloads) {
            upsert(payload, operator);
            operationLogRepository.save(new ConfigOperationLogEntity("CARD", payload.cardId(), "IMPORT",
                    payload.version(), operator, "SUCCESS", "卡牌配置导入成功。"));
        }
        reload();
        CardValidationResult validation = validateCurrent();
        if (!validation.valid()) {
            throw new IllegalStateException("卡牌配置导入后校验失败：" + validation.message());
        }
        int active = (int) allConfigs(false).size();
        int disabled = (int) allConfigs(true).stream().filter(card -> !card.enabled()).count();
        return new CardImportResult(payloads.size(), active, disabled, validation.message());
    }

    private void saveIfMissing(CardSeed seed) {
        if ("功法".equals(seed.category()) && !skillRepository.existsById(seed.cardId())) {
            skillRepository.save(new SkillConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.combatDamageBonus(), seed.combatBlockBonus(),
                    seed.combatSpiritGain(), seed.combatPoisonBonus(), seed.battleWeight(),
                    seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        } else if ("法宝".equals(seed.category()) && !itemRepository.existsById(seed.cardId())) {
            itemRepository.save(new ItemConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.combatDamageBonus(), seed.combatBlockBonus(),
                    seed.combatSpiritGain(), seed.combatPoisonBonus(), seed.battleWeight(),
                    seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        } else if ("符箓".equals(seed.category()) && !talismanRepository.existsById(seed.cardId())) {
            talismanRepository.save(new TalismanConfigEntity(seed.cardId(), seed.name(), seed.rarity(), seed.description(),
                    seed.effectText(), seed.archetype(), seed.healthOnClaim(), seed.spiritOnClaim(),
                    seed.lifespanOnClaim(), seed.karmaOnClaim(), seed.battleHealthBonus(),
                    seed.battleSpiritBonus(), seed.combatDamageBonus(), seed.combatBlockBonus(),
                    seed.combatSpiritGain(), seed.combatPoisonBonus(), seed.battleWeight(),
                    seed.eliteWeight(), seed.treasureWeight(), seed.enabled()));
        }
    }

    private void normalizeLegacyVersions() {
        skillRepository.findAll().stream()
                .filter(row -> row.getConfigVersion() <= 0)
                .forEach(row -> {
                    row.setConfigVersion(1);
                    skillRepository.save(row);
                });
        itemRepository.findAll().stream()
                .filter(row -> row.getConfigVersion() <= 0)
                .forEach(row -> {
                    row.setConfigVersion(1);
                    itemRepository.save(row);
                });
        talismanRepository.findAll().stream()
                .filter(row -> row.getConfigVersion() <= 0)
                .forEach(row -> {
                    row.setConfigVersion(1);
                    talismanRepository.save(row);
                });
    }

    private CardDefinition toDefinition(BuildConfigEntity row, String category) {
        return new CardDefinition(row.getCardId(), category, row.getName(), row.getRarity(), row.getDescription(),
                row.getEffectText(), row.getArchetype(), row.getHealthOnClaim(), row.getSpiritOnClaim(),
                row.getLifespanOnClaim(), row.getKarmaOnClaim(), row.getBattleHealthBonus(),
                row.getBattleSpiritBonus(), row.getCombatDamageBonus(), row.getCombatBlockBonus(),
                row.getCombatSpiritGain(), row.getCombatPoisonBonus(), row.getBattleWeight(), row.getEliteWeight(),
                row.getTreasureWeight(), row.isEnabled());
    }

    private CardConfigSnapshot toSnapshot(BuildConfigEntity row, String category) {
        return new CardConfigSnapshot(row.getCardId(), category, row.getName(), row.getRarity(),
                row.getDescription(), row.getEffectText(), row.getArchetype(), row.getHealthOnClaim(),
                row.getSpiritOnClaim(), row.getLifespanOnClaim(), row.getKarmaOnClaim(),
                row.getBattleHealthBonus(), row.getBattleSpiritBonus(), row.getCombatDamageBonus(),
                row.getCombatBlockBonus(), row.getCombatSpiritGain(), row.getCombatPoisonBonus(),
                row.getBattleWeight(), row.getEliteWeight(), row.getTreasureWeight(),
                row.getConfigVersion(), row.isEnabled());
    }

    private void validatePayload(CardConfigPayload payload, List<String> errors) {
        if (payload == null) {
            errors.add("存在空卡牌配置");
            return;
        }
        if (blank(payload.cardId()) || payload.cardId().length() > 60) errors.add("卡牌编号不能为空且不能超过 60 个字符");
        if (!Set.of("功法", "法宝", "符箓").contains(payload.category())) errors.add(payload.cardId() + " 的类别无效");
        if (blank(payload.name()) || payload.name().length() > 80) errors.add(payload.cardId() + " 缺少合法名称");
        if (blank(payload.rarity()) || blank(payload.description()) || blank(payload.effectText()) || blank(payload.archetype())) {
            errors.add(payload.cardId() + " 缺少稀有度、描述、效果或流派");
        }
        if (payload.version() <= 0) errors.add(payload.cardId() + " 版本号必须大于 0");
        if (payload.battleWeight() < 0 || payload.eliteWeight() < 0 || payload.treasureWeight() < 0) {
            errors.add(payload.cardId() + " 权重不能为负数");
        }
    }

    private void validateSnapshot(CardConfigSnapshot card, List<String> errors) {
        if (card.version() <= 0) errors.add(card.cardId() + " 版本号必须大于 0");
        if (card.battleWeight() < 0 || card.eliteWeight() < 0 || card.treasureWeight() < 0) {
            errors.add(card.cardId() + " 权重不能为负数");
        }
    }

    private void upsert(CardConfigPayload payload, String operator) {
        BuildConfigEntity existing = switch (payload.category()) {
            case "功法" -> skillRepository.findById(payload.cardId()).orElse(null);
            case "法宝" -> itemRepository.findById(payload.cardId()).orElse(null);
            case "符箓" -> talismanRepository.findById(payload.cardId()).orElse(null);
            default -> null;
        };
        if (existing != null) {
            existing.update(payload.name(), payload.rarity(), payload.description(), payload.effectText(),
                    payload.archetype(), payload.healthOnClaim(), payload.spiritOnClaim(),
                    payload.lifespanOnClaim(), payload.karmaOnClaim(), payload.battleHealthBonus(),
                    payload.battleSpiritBonus(), payload.combatDamageBonus(), payload.combatBlockBonus(),
                    payload.combatSpiritGain(), payload.combatPoisonBonus(), payload.battleWeight(),
                    payload.eliteWeight(), payload.treasureWeight(), payload.version(), payload.enabled());
            save(payload.category(), existing);
            return;
        }
        BuildConfigEntity created = switch (payload.category()) {
            case "功法" -> new SkillConfigEntity(payload.cardId(), payload.name(), payload.rarity(),
                    payload.description(), payload.effectText(), payload.archetype(), payload.healthOnClaim(),
                    payload.spiritOnClaim(), payload.lifespanOnClaim(), payload.karmaOnClaim(),
                    payload.battleHealthBonus(), payload.battleSpiritBonus(), payload.combatDamageBonus(),
                    payload.combatBlockBonus(), payload.combatSpiritGain(), payload.combatPoisonBonus(),
                    payload.battleWeight(), payload.eliteWeight(), payload.treasureWeight(), payload.enabled());
            case "法宝" -> new ItemConfigEntity(payload.cardId(), payload.name(), payload.rarity(),
                    payload.description(), payload.effectText(), payload.archetype(), payload.healthOnClaim(),
                    payload.spiritOnClaim(), payload.lifespanOnClaim(), payload.karmaOnClaim(),
                    payload.battleHealthBonus(), payload.battleSpiritBonus(), payload.combatDamageBonus(),
                    payload.combatBlockBonus(), payload.combatSpiritGain(), payload.combatPoisonBonus(),
                    payload.battleWeight(), payload.eliteWeight(), payload.treasureWeight(), payload.enabled());
            case "符箓" -> new TalismanConfigEntity(payload.cardId(), payload.name(), payload.rarity(),
                    payload.description(), payload.effectText(), payload.archetype(), payload.healthOnClaim(),
                    payload.spiritOnClaim(), payload.lifespanOnClaim(), payload.karmaOnClaim(),
                    payload.battleHealthBonus(), payload.battleSpiritBonus(), payload.combatDamageBonus(),
                    payload.combatBlockBonus(), payload.combatSpiritGain(), payload.combatPoisonBonus(),
                    payload.battleWeight(), payload.eliteWeight(), payload.treasureWeight(), payload.enabled());
            default -> throw new IllegalArgumentException("卡牌类别无效：" + payload.category());
        };
        created.setConfigVersion(payload.version());
        save(payload.category(), created);
    }

    private void save(String category, BuildConfigEntity entity) {
        switch (category) {
            case "功法" -> skillRepository.save((SkillConfigEntity) entity);
            case "法宝" -> itemRepository.save((ItemConfigEntity) entity);
            case "符箓" -> talismanRepository.save((TalismanConfigEntity) entity);
            default -> throw new IllegalArgumentException("卡牌类别无效：" + category);
        }
    }

    private String normalizeOperator(String operator) {
        if (blank(operator)) return "admin";
        return operator.trim().substring(0, Math.min(80, operator.trim().length()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record WeightedCard(String cardId, int weight) { }

    public record CardDefinition(String cardId, String category, String name, String rarity,
                                 String description, String effectText, String archetype,
                                 int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                                 int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                                 int combatDamageBonus, int combatBlockBonus, int combatSpiritGain,
                                 int combatPoisonBonus, int battleWeight, int eliteWeight,
                                 int treasureWeight, boolean enabled) { }

    public record CardConfigPayload(String cardId, String category, String name, String rarity,
                                    String description, String effectText, String archetype,
                                    int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                                    int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                                    int combatDamageBonus, int combatBlockBonus, int combatSpiritGain,
                                    int combatPoisonBonus, int battleWeight, int eliteWeight,
                                    int treasureWeight, int version, boolean enabled) { }

    public record CardConfigSnapshot(String cardId, String category, String name, String rarity,
                                     String description, String effectText, String archetype,
                                     int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                                     int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                                     int combatDamageBonus, int combatBlockBonus, int combatSpiritGain,
                                     int combatPoisonBonus, int battleWeight, int eliteWeight,
                                     int treasureWeight, int version, boolean enabled) { }

    public record CardValidationResult(boolean valid, int checkedCards, String message) { }

    public record CardImportResult(int importedCards, int activeCards, int disabledCards,
                                   String validationMessage) { }

    private record CardSeed(String cardId, String category, String name, String rarity,
                            String description, String effectText, String archetype,
                            int healthOnClaim, int spiritOnClaim, int lifespanOnClaim,
                            int karmaOnClaim, int battleHealthBonus, int battleSpiritBonus,
                            int combatDamageBonus, int combatBlockBonus, int combatSpiritGain,
                            int combatPoisonBonus, int battleWeight, int eliteWeight,
                            int treasureWeight, boolean enabled) { }
}
