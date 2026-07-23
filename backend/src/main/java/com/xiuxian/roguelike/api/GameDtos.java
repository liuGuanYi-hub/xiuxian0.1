package com.xiuxian.roguelike.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

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

    public record BuildCardView(String id, String cardId, String category, String name,
                                String rarity, String description, String effectText) {
    }

    public record RewardOfferView(String id, String cardId, String category, String name,
                                  String rarity, String description, String effectText) {
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
            int turn,
            String status,
            String currentNodeId,
            int currentFloor,
            EventView event,
            RouteMapView map,
            EndingView ending,
            List<BuildCardView> build,
            List<RewardOfferView> rewardOffers,
            List<String> logs
    ) {
    }
}
