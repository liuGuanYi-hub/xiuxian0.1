package com.xiuxian.roguelike.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public final class GameDtos {

    private GameDtos() {
    }

    public record StartRunRequest(
            @NotBlank(message = "道号不能为空") String playerName,
            @NotBlank(message = "出身不能为空") String origin
    ) {
    }

    public record ChoiceRequest(
            @NotNull(message = "必须选择一个选项")
            @PositiveOrZero(message = "选项编号不能为负数") Integer choiceIndex,
            String requestId
    ) {
    }

    public record ChoiceView(int index, String label, String hint) {
    }

    public record EventView(String id, String title, String description, List<ChoiceView> choices,
                            String rarity, boolean repeatable) {
    }

    public record MapNodeView(String id, int floor, int position, String type, String label,
                              String rarity, String status, List<String> nextNodeIds) {
    }

    public record RouteMapView(int totalFloors, List<MapNodeView> nodes) {
    }

    public record EndingView(String id, String title, String description) {
    }

    public record BuildCardView(String id, String cardId, String category, String archetype,
                                String name, String rarity, String description, String effectText,
                                int upgradeLevel) {
    }

    public record RewardOfferView(String id, String cardId, String category, String archetype,
                                  String name, String rarity, String description, String effectText) {
    }

    public record ShopOfferView(String id, String cardId, String category, String archetype,
                                String name, String rarity, String description, String effectText,
                                int price) {
    }

    public record ShopView(String id, String nodeId, int refreshCount, int refreshLimit,
                           int nextRefreshCost, int removalCost, boolean removalUsed,
                           List<ShopOfferView> offers) {
    }

    public record RemovalView(String source, String title, int cost, List<BuildCardView> options) {
    }

    public record SynergyView(String archetype, String title, int count, boolean active,
                              String effectText) {
    }

    public record BuildStatsView(int activeCards, Map<String, Integer> categoryCounts,
                                 Map<String, Integer> archetypeCounts, List<SynergyView> synergies,
                                 int battleHealthBonus, int battleSpiritBonus,
                                 int battleLifespanBonus, int battleKarmaBonus) {
    }

    public record GameRunView(
            String id,
            String playerName,
            String origin,
            String realm,
            int health,
            int spirit,
            int lifespan,
            int karma,
            int spiritStones,
            int turn,
            String status,
            String currentNodeId,
            int currentFloor,
            EventView event,
            RouteMapView map,
            EndingView ending,
            List<BuildCardView> build,
            BuildStatsView buildStats,
            List<BuildCardView> upgradeOptions,
            List<RewardOfferView> rewardOffers,
            ShopView shop,
            RemovalView removal,
            List<String> logs
    ) {
    }
}
