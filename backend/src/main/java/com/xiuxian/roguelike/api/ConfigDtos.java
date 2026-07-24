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
}
