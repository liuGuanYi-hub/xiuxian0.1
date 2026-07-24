package com.xiuxian.roguelike.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public final class ConfigDtos {

    private ConfigDtos() {
    }

    public record EventConfigImportRequest(
            @NotBlank(message = "操作人不能为空") String operator,
            @NotEmpty(message = "至少需要导入一条事件配置")
            List<@Valid EventConfigInput> configs
    ) {
    }

    public record EventConfigInput(
            @NotBlank(message = "配置类型不能为空") String configType,
            @NotBlank(message = "事件编号不能为空") String eventId,
            @NotBlank(message = "事件标题不能为空") String title,
            @NotBlank(message = "事件描述不能为空") String description,
            String rarity,
            boolean repeatable,
            int version,
            boolean enabled,
            List<ChoiceInput> choices,
            List<String> nextEventIds,
            Map<String, Integer> nodeWeights
    ) {
    }

    public record ChoiceInput(String label, String hint, int healthDelta, int spiritDelta,
                              int lifespanDelta, int karmaDelta, String nextEventId, String action) {
    }

    public record EventConfigListView(
            List<?> configs,
            boolean valid,
            int checkedConfigs,
            String validationMessage,
            int activeEvents
    ) {
    }

    public record ConfigImportView(
            int importedConfigs,
            int checkedConfigs,
            int activeEvents,
            int activeEndings,
            String validationMessage
    ) {
    }

    public record ConfigValidationView(boolean valid, int checkedConfigs, String message) {
    }

    public record CardConfigImportRequest(
            @NotBlank(message = "操作人不能为空") String operator,
            @NotEmpty(message = "至少需要导入一张卡牌配置")
            List<@Valid CardConfigInput> configs
    ) {
    }

    public record CardConfigInput(
            @NotBlank(message = "卡牌编号不能为空") String cardId,
            @NotBlank(message = "卡牌类别不能为空") String category,
            @NotBlank(message = "卡牌名称不能为空") String name,
            @NotBlank(message = "卡牌稀有度不能为空") String rarity,
            @NotBlank(message = "卡牌描述不能为空") String description,
            @NotBlank(message = "卡牌效果不能为空") String effectText,
            @NotBlank(message = "卡牌流派不能为空") String archetype,
            int healthOnClaim,
            int spiritOnClaim,
            int lifespanOnClaim,
            int karmaOnClaim,
            int battleHealthBonus,
            int battleSpiritBonus,
            int combatDamageBonus,
            int combatBlockBonus,
            int combatSpiritGain,
            int combatPoisonBonus,
            int battleWeight,
            int eliteWeight,
            int treasureWeight,
            int version,
            boolean enabled
    ) {
    }

    public record CardConfigListView(
            List<?> configs,
            boolean valid,
            int checkedCards,
            String validationMessage,
            int activeCards,
            int disabledCards
    ) {
    }

    public record CardConfigImportView(
            int importedCards,
            int activeCards,
            int disabledCards,
            String validationMessage
    ) {
    }
}
