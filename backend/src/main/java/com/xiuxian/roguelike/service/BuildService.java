package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.BuildCardView;
import com.xiuxian.roguelike.api.GameDtos.RewardOfferView;
import com.xiuxian.roguelike.domain.BuildItemEntity;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RewardOfferEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.repository.BuildItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

@Service
public class BuildService {

    private final BuildItemRepository buildItemRepository;

    public BuildService(BuildItemRepository buildItemRepository) {
        this.buildItemRepository = buildItemRepository;
    }

    public void initialize(GameRunEntity run) {
        List<BuildItemEntity> starter = BuildCatalog.starterCards().stream()
                .map(card -> toBuildItem(run.getId(), card, "STARTER"))
                .toList();
        buildItemRepository.saveAll(starter);
    }

    public List<BuildItemEntity> getBuild(String runId) {
        return buildItemRepository.findByRunIdOrderByCreatedAtAsc(runId);
    }

    public List<BuildCardView> toViews(String runId) {
        return getBuild(runId).stream()
                .map(item -> new BuildCardView(
                        item.getId(), item.getCardId(), item.getCategory(), item.getName(),
                        item.getRarity(), item.getDescription(), item.getEffectText()
                ))
                .toList();
    }

    public List<RewardOfferEntity> createOffers(GameRunEntity run, RunMapNodeEntity node) {
        List<BuildCatalog.WeightedCard> pool = new ArrayList<>(BuildCatalog.poolFor(node.getType()));
        Set<String> selected = new HashSet<>();
        SplittableRandom random = new SplittableRandom(
                run.getSeed() ^ (long) node.getId().hashCode() ^ (long) run.getTurn() * 0x9E3779B97F4A7C15L
        );
        List<RewardOfferEntity> offers = new ArrayList<>();
        while (offers.size() < 3 && !pool.isEmpty()) {
            BuildCatalog.WeightedCard weighted = pick(pool, random);
            pool.removeIf(candidate -> candidate.cardId().equals(weighted.cardId()));
            if (!selected.add(weighted.cardId())) {
                continue;
            }
            BuildCatalog.CardDefinition card = BuildCatalog.get(weighted.cardId());
            offers.add(new RewardOfferEntity(
                    run.getId(), node.getId(), card.cardId(), card.category(), card.name(),
                    rewardRarity(card, node), card.description(), card.effectText()
            ));
        }
        return offers;
    }

    public boolean givesReward(String nodeType) {
        return "BATTLE".equals(nodeType) || "ELITE".equals(nodeType) || "TREASURE".equals(nodeType);
    }

    public BuildModifier modifier(String runId, String nodeType) {
        if (!"BATTLE".equals(nodeType) && !"ELITE".equals(nodeType)) {
            return new BuildModifier(0, 0);
        }
        int health = 0;
        int spirit = 0;
        for (BuildItemEntity item : getBuild(runId)) {
            BuildCatalog.CardDefinition card = BuildCatalog.get(item.getCardId());
            health += card.battleHealthBonus();
            spirit += card.battleSpiritBonus();
        }
        return new BuildModifier(health, spirit);
    }

    public BuildItemEntity toBuildItem(String runId, RewardOfferEntity offer) {
        return new BuildItemEntity(
                runId, offer.getCardId(), offer.getCategory(), offer.getName(), offer.getRarity(),
                offer.getDescription(), offer.getEffectText(), offer.getNodeId()
        );
    }

    public BuildItemEntity toBuildItem(String runId, BuildCatalog.CardDefinition card, String sourceNodeId) {
        return new BuildItemEntity(
                runId, card.cardId(), card.category(), card.name(), card.rarity(),
                card.description(), card.effectText(), sourceNodeId
        );
    }

    public RewardOfferView toRewardView(RewardOfferEntity offer) {
        return new RewardOfferView(
                offer.getId(), offer.getCardId(), offer.getCategory(), offer.getName(),
                offer.getRarity(), offer.getDescription(), offer.getEffectText()
        );
    }

    private BuildCatalog.WeightedCard pick(List<BuildCatalog.WeightedCard> pool, SplittableRandom random) {
        int total = pool.stream().mapToInt(BuildCatalog.WeightedCard::weight).sum();
        int roll = random.nextInt(total);
        for (BuildCatalog.WeightedCard item : pool) {
            roll -= item.weight();
            if (roll < 0) return item;
        }
        return pool.get(pool.size() - 1);
    }

    private String rewardRarity(BuildCatalog.CardDefinition card, RunMapNodeEntity node) {
        if ("ELITE".equals(node.getType()) && "普通".equals(card.rarity())) {
            return "稀有";
        }
        return card.rarity();
    }

    public record BuildModifier(int healthDelta, int spiritDelta) {
    }
}
