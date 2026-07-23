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

    public record EventView(String id, String title, String description, List<ChoiceView> choices) {
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
            EventView event,
            List<String> logs
    ) {
    }
}
