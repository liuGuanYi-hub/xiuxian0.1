package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.BuildCardView;
import com.xiuxian.roguelike.api.GameDtos.BuildStatsView;
import com.xiuxian.roguelike.api.GameDtos.RemovalView;
import com.xiuxian.roguelike.api.GameDtos.RewardOfferView;
import com.xiuxian.roguelike.api.GameDtos.ShopOfferView;
import com.xiuxian.roguelike.api.GameDtos.ShopView;
import com.xiuxian.roguelike.api.GameDtos.SynergyView;
import com.xiuxian.roguelike.domain.BuildItemEntity;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RewardOfferEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.domain.RunShopEntity;
import com.xiuxian.roguelike.domain.RunShopOfferEntity;
import com.xiuxian.roguelike.repository.BuildItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

@Service
public class BuildService {

    private static final Map<String, SynergyDefinition> SYNERGIES = Map.of(
            "剑修", new SynergyDefinition("剑修", "剑修同道", "2 张：战斗额外获得 5 点气血；3 张：再获得 3 点灵力。", 5, 0, 0, 0, 0, 3),
            "丹修", new SynergyDefinition("丹修", "丹修同道", "2 张：战斗额外获得 4 点灵力；3 张：再获得 4 点气血。", 0, 4, 0, 0, 4, 0),
            "体修", new SynergyDefinition("体修", "体修同道", "2 张：战斗额外获得 8 点气血；3 张：再获得 1 点寿元。", 8, 0, 0, 1, 0, 0),
            "鬼修", new SynergyDefinition("鬼修", "鬼修同道", "2 张：战斗额外获得 6 点灵力；3 张：因果 +2，但额外消耗 1 点寿元。", 0, 6, -1, 2, 0, 0)
    );

    private final BuildItemRepository buildItemRepository;
    private final BuildConfigService configService;

    public BuildService(BuildItemRepository buildItemRepository, BuildConfigService configService) {
        this.buildItemRepository = buildItemRepository;
        this.configService = configService;
    }

    public void initialize(GameRunEntity run) {
        List<BuildItemEntity> starter = configService.starterCards().stream()
                .map(card -> toBuildItem(run.getId(), card, "STARTER"))
                .toList();
        buildItemRepository.saveAll(starter);
    }

    public List<BuildItemEntity> getBuild(String runId) {
        return buildItemRepository.findByRunIdAndStatusOrderByCreatedAtAsc(runId, "ACTIVE");
    }

    public List<BuildCardView> toViews(String runId) {
        return getBuild(runId).stream().map(this::toView).toList();
    }

    public List<BuildCardView> toUpgradeViews(String runId) {
        return toViews(runId);
    }

    public int activeCardCount(String runId) {
        return getBuild(runId).size();
    }

    public int upgradeCost(int currentLevel) {
        return 25 + currentLevel * 15;
    }

    public int upgradeHealthReward(BuildConfigService.CardDefinition card) {
        return card.healthOnClaim() == 0 ? 0 : Math.max(2, card.healthOnClaim() / 2);
    }

    public int upgradeSpiritReward(BuildConfigService.CardDefinition card) {
        return card.spiritOnClaim() == 0 ? 0 : Math.max(2, card.spiritOnClaim() / 2);
    }

    public int upgradeLifespanReward(BuildConfigService.CardDefinition card) {
        return card.lifespanOnClaim() == 0 ? 0 : Math.max(1, card.lifespanOnClaim() / 2);
    }

    public int upgradeKarmaReward(BuildConfigService.CardDefinition card) {
        return card.karmaOnClaim() == 0 ? 0 : Math.max(1, card.karmaOnClaim() / 2);
    }

    public List<RewardOfferEntity> createOffers(GameRunEntity run, RunMapNodeEntity node,
                                                 EventCatalog.EventMeta eventMeta, int completedEliteCount) {
        Map<String, Integer> archetypeCounts = archetypeCounts(run.getId());
        List<WeightedCard> pool = configService.poolFor(node.getType()).stream()
                .map(weighted -> new WeightedCard(weighted.cardId(),
                        adjustedRewardWeight(configService.get(weighted.cardId()), weighted.weight(), node,
                                eventMeta, completedEliteCount, archetypeCounts)))
                .filter(weighted -> weighted.weight() > 0)
                .toList();
        return createRewardOffers(run, node, pool);
    }

    public List<RunShopOfferEntity> createShopOffers(GameRunEntity run, RunShopEntity shop) {
        List<BuildConfigService.CardDefinition> cards = configService.all().stream()
                .filter(BuildConfigService.CardDefinition::enabled)
                .sorted(Comparator.comparing(BuildConfigService.CardDefinition::cardId))
                .toList();
        List<WeightedCard> pool = cards.stream()
                .map(card -> new WeightedCard(card.cardId(), shopWeight(card)))
                .toList();
        SplittableRandom random = new SplittableRandom(run.getSeed()
                ^ (long) shop.getNodeId().hashCode() * 31
                ^ (long) shop.getRefreshCount() * 0x9E3779B97F4A7C15L);
        List<RunShopOfferEntity> offers = new ArrayList<>();
        List<WeightedCard> remaining = new ArrayList<>(pool);
        for (int slot = 0; slot < 3 && !remaining.isEmpty(); slot++) {
            List<WeightedCard> candidates = remaining;
            if (slot == 0) {
                List<WeightedCard> commonCards = remaining.stream()
                        .filter(candidate -> "普通".equals(configService.get(candidate.cardId()).rarity()))
                        .toList();
                if (!commonCards.isEmpty()) candidates = commonCards;
            }
            WeightedCard selected = pick(candidates, random);
            remaining.removeIf(candidate -> candidate.cardId().equals(selected.cardId()));
            BuildConfigService.CardDefinition card = configService.get(selected.cardId());
            offers.add(new RunShopOfferEntity(shop.getId(), run.getId(), card.cardId(), card.category(),
                    card.archetype(), card.name(), card.rarity(), card.description(), card.effectText(),
                    priceFor(card.rarity()), slot, shop.getRefreshCount()));
        }
        return offers;
    }

    public boolean givesReward(String nodeType) {
        return "BATTLE".equals(nodeType) || "ELITE".equals(nodeType) || "TREASURE".equals(nodeType);
    }

    public BuildModifier modifier(String runId, String nodeType) {
        if (!"BATTLE".equals(nodeType) && !"ELITE".equals(nodeType)) {
            return new BuildModifier(0, 0, 0, 0);
        }
        BuildModifier modifier = modifierWithoutSynergy(runId);
        for (SynergyView synergy : buildStats(runId).synergies()) {
            if (!synergy.active()) continue;
            SynergyDefinition definition = SYNERGIES.get(synergy.archetype());
            modifier = modifier.plus(definition.healthBonus(), definition.spiritBonus(), 0, 0);
            if (synergy.count() >= 3) {
                modifier = modifier.plus(definition.healthThreeBonus(), definition.spiritThreeBonus(),
                        definition.lifespanThreeBonus(), definition.karmaThreeBonus());
            }
        }
        return modifier;
    }

    public BuildStatsView buildStats(String runId) {
        List<BuildItemEntity> build = getBuild(runId);
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Integer> archetypeCounts = new LinkedHashMap<>();
        for (BuildItemEntity item : build) {
            BuildConfigService.CardDefinition card = configService.get(item.getCardId());
            categoryCounts.merge(card.category(), 1, Integer::sum);
            archetypeCounts.merge(card.archetype(), 1, Integer::sum);
        }
        List<SynergyView> synergies = SYNERGIES.values().stream()
                .sorted(Comparator.comparing(SynergyDefinition::archetype))
                .map(definition -> new SynergyView(definition.archetype(), definition.title(),
                        archetypeCounts.getOrDefault(definition.archetype(), 0),
                        archetypeCounts.getOrDefault(definition.archetype(), 0) >= 2,
                        definition.effectText()))
                .toList();
        BuildModifier modifier = modifierWithoutSynergy(runId);
        for (SynergyView synergy : synergies) {
            if (!synergy.active()) continue;
            SynergyDefinition definition = SYNERGIES.get(synergy.archetype());
            modifier = modifier.plus(definition.healthBonus(), definition.spiritBonus(), 0, 0);
            if (synergy.count() >= 3) {
                modifier = modifier.plus(definition.healthThreeBonus(), definition.spiritThreeBonus(),
                        definition.lifespanThreeBonus(), definition.karmaThreeBonus());
            }
        }
        CombatModifier combatModifier = combatModifierWithoutSynergy(runId);
        for (SynergyView synergy : synergies) {
            if (!synergy.active()) continue;
            combatModifier = combatModifier.plus(synergyCombatBonus(synergy.archetype()));
        }
        return new BuildStatsView(build.size(), categoryCounts, archetypeCounts, synergies,
                modifier.healthDelta(), modifier.spiritDelta(), modifier.lifespanDelta(), modifier.karmaDelta(),
                combatModifier.damage(), combatModifier.block(), combatModifier.spiritGain(), combatModifier.poison());
    }

    public CombatModifier combatModifier(String runId) {
        CombatModifier modifier = combatModifierWithoutSynergy(runId);
        for (SynergyView synergy : buildStats(runId).synergies()) {
            if (synergy.active()) modifier = modifier.plus(synergyCombatBonus(synergy.archetype()));
        }
        return modifier;
    }

    public BuildItemEntity toBuildItem(String runId, RewardOfferEntity offer) {
        return new BuildItemEntity(runId, offer.getCardId(), offer.getCategory(), offer.getName(),
                offer.getRarity(), offer.getDescription(), offer.getEffectText(), offer.getNodeId());
    }

    public BuildItemEntity toBuildItem(String runId, RunShopOfferEntity offer) {
        return new BuildItemEntity(runId, offer.getCardId(), offer.getCategory(), offer.getName(),
                offer.getRarity(), offer.getDescription(), offer.getEffectText(), offer.getShopId());
    }

    public BuildItemEntity toBuildItem(String runId, BuildConfigService.CardDefinition card, String sourceNodeId) {
        return new BuildItemEntity(runId, card.cardId(), card.category(), card.name(), card.rarity(),
                card.description(), card.effectText(), sourceNodeId);
    }

    public RewardOfferView toRewardView(RewardOfferEntity offer) {
        return new RewardOfferView(offer.getId(), offer.getCardId(), offer.getCategory(),
                configService.get(offer.getCardId()).archetype(), offer.getName(), offer.getRarity(),
                offer.getDescription(), offer.getEffectText());
    }

    public ShopView toShopView(RunShopEntity shop, List<RunShopOfferEntity> offers) {
        List<ShopOfferView> views = offers.stream()
                .map(offer -> new ShopOfferView(offer.getId(), offer.getCardId(), offer.getCategory(),
                        offer.getArchetype(), offer.getName(), offer.getRarity(), offer.getDescription(),
                        offer.getEffectText(), offer.getPrice()))
                .toList();
        int nextCost = shop.getRefreshCount() >= shop.getRefreshLimit() ? 0 : 10 + shop.getRefreshCount() * 5;
        return new ShopView(shop.getId(), shop.getNodeId(), shop.getRefreshCount(), shop.getRefreshLimit(),
                nextCost, 30, shop.isRemovalUsed(), views);
    }

    public RemovalView toRemovalView(String source, List<BuildCardView> options) {
        return new RemovalView(source, "选择一张卡牌移出本局构筑", "SPECIAL_EVENT".equals(source) ? 0 : 30, options);
    }

    public int priceFor(String rarity) {
        return switch (rarity) {
            case "传说" -> 55;
            case "稀有" -> 35;
            default -> 20;
        };
    }

    private BuildModifier modifierWithoutSynergy(String runId) {
        int health = 0;
        int spirit = 0;
        for (BuildItemEntity item : getBuild(runId)) {
            BuildConfigService.CardDefinition card = configService.get(item.getCardId());
            int multiplier = item.getUpgradeLevel() + 1;
            health += card.battleHealthBonus() * multiplier;
            spirit += card.battleSpiritBonus() * multiplier;
            if (item.getUpgradeLevel() > 0 && card.battleHealthBonus() == 0 && card.battleSpiritBonus() == 0) {
                spirit += item.getUpgradeLevel();
            }
        }
        return new BuildModifier(health, spirit, 0, 0);
    }

    private CombatModifier combatModifierWithoutSynergy(String runId) {
        int damage = 0;
        int block = 0;
        int spiritGain = 0;
        int poison = 0;
        for (BuildItemEntity item : getBuild(runId)) {
            BuildConfigService.CardDefinition card = configService.get(item.getCardId());
            int multiplier = item.getUpgradeLevel() + 1;
            damage += card.combatDamageBonus() * multiplier;
            block += card.combatBlockBonus() * multiplier;
            spiritGain += card.combatSpiritGain() * multiplier;
            poison += card.combatPoisonBonus() * multiplier;
        }
        return new CombatModifier(damage, block, spiritGain, poison);
    }

    private CombatModifier synergyCombatBonus(String archetype) {
        return switch (archetype) {
            case "剑修" -> new CombatModifier(2, 0, 0, 0);
            case "丹修" -> new CombatModifier(0, 0, 2, 0);
            case "体修" -> new CombatModifier(0, 3, 0, 0);
            case "鬼修" -> new CombatModifier(0, 0, 0, 1);
            default -> new CombatModifier(0, 0, 0, 0);
        };
    }

    private List<RewardOfferEntity> createRewardOffers(GameRunEntity run, RunMapNodeEntity node,
                                                        List<WeightedCard> pool) {
        SplittableRandom random = new SplittableRandom(run.getSeed()
                ^ (long) node.getId().hashCode() ^ (long) run.getTurn() * 0x9E3779B97F4A7C15L);
        List<WeightedCard> remaining = new ArrayList<>(pool);
        List<RewardOfferEntity> offers = new ArrayList<>();
        while (offers.size() < 3 && !remaining.isEmpty()) {
            WeightedCard weighted = pick(remaining, random);
            remaining.removeIf(candidate -> candidate.cardId().equals(weighted.cardId()));
            BuildConfigService.CardDefinition card = configService.get(weighted.cardId());
            offers.add(new RewardOfferEntity(run.getId(), node.getId(), card.cardId(), card.category(),
                    card.name(), rewardRarity(card, node), card.description(), card.effectText()));
        }
        return offers;
    }

    private int adjustedRewardWeight(BuildConfigService.CardDefinition card, int baseWeight,
                                     RunMapNodeEntity node, EventCatalog.EventMeta eventMeta,
                                     int completedEliteCount, Map<String, Integer> archetypeCounts) {
        double weight = baseWeight;
        if ("ELITE".equals(node.getType())) weight *= isHighRarity(card) ? 1.35 : 1.05;
        if ("稀有".equals(node.getRarity())) weight *= isHighRarity(card) ? 1.20 : 0.95;
        if ("传说".equals(node.getRarity())) weight *= isHighRarity(card) ? 1.40 : 0.80;
        if (node.getFloor() >= 5 && isHighRarity(card)) weight *= 1.15;
        if (("稀有".equals(eventMeta.rarity()) || "传说".equals(eventMeta.rarity())) && isHighRarity(card)) {
            weight *= 1.25;
        }
        if (completedEliteCount == 0 && node.getFloor() >= 2 && isHighRarity(card)) weight *= 1.10;
        int count = archetypeCounts.getOrDefault(card.archetype(), 0);
        if (count == 1) weight *= 1.35;
        if (count == 2) weight *= 1.55;
        return Math.max(1, (int) Math.round(weight));
    }

    private boolean isHighRarity(BuildConfigService.CardDefinition card) {
        return "稀有".equals(card.rarity()) || "传说".equals(card.rarity());
    }

    private int shopWeight(BuildConfigService.CardDefinition card) {
        return switch (card.rarity()) {
            case "传说" -> 10;
            case "稀有" -> 30;
            default -> 60;
        };
    }

    private WeightedCard pick(List<WeightedCard> pool, SplittableRandom random) {
        int total = pool.stream().mapToInt(WeightedCard::weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (WeightedCard item : pool) {
            roll -= item.weight();
            if (roll < 0) return item;
        }
        return pool.get(pool.size() - 1);
    }

    private String rewardRarity(BuildConfigService.CardDefinition card, RunMapNodeEntity node) {
        if ("ELITE".equals(node.getType()) && "普通".equals(card.rarity())) return "稀有";
        return card.rarity();
    }

    private Map<String, Integer> archetypeCounts(String runId) {
        Map<String, Integer> counts = new HashMap<>();
        for (BuildItemEntity item : getBuild(runId)) {
            counts.merge(configService.get(item.getCardId()).archetype(), 1, Integer::sum);
        }
        return counts;
    }

    private BuildCardView toView(BuildItemEntity item) {
        BuildConfigService.CardDefinition card = configService.get(item.getCardId());
        String effectText = item.getEffectText();
        if (item.getUpgradeLevel() > 0) effectText += " · 强化等级 " + item.getUpgradeLevel();
        return new BuildCardView(item.getId(), item.getCardId(), item.getCategory(), card.archetype(),
                item.getName(), item.getRarity(), item.getDescription(), effectText, item.getUpgradeLevel());
    }

    public record BuildModifier(int healthDelta, int spiritDelta, int lifespanDelta, int karmaDelta) {
        public BuildModifier plus(int health, int spirit, int lifespan, int karma) {
            return new BuildModifier(healthDelta + health, spiritDelta + spirit,
                    lifespanDelta + lifespan, karmaDelta + karma);
        }
    }

    public record CombatModifier(int damage, int block, int spiritGain, int poison) {
        public CombatModifier plus(CombatModifier other) {
            return new CombatModifier(damage + other.damage, block + other.block,
                    spiritGain + other.spiritGain, poison + other.poison);
        }
    }

    private record WeightedCard(String cardId, int weight) { }

    private record SynergyDefinition(String archetype, String title, String effectText,
                                     int healthBonus, int spiritBonus, int lifespanThreeBonus,
                                     int karmaThreeBonus, int healthThreeBonus, int spiritThreeBonus) { }
}
