package com.xiuxian.roguelike.service;

import java.util.List;

/**
 * 事件配置的数据结构。实际内容由 EventConfigService 从 event_config 加载。
 */
public final class EventCatalog {

    private EventCatalog() {
    }

    public record EventDefinition(String id, String title, String description, List<ChoiceDefinition> choices) {
    }

    public record EventMeta(String rarity, boolean repeatable) {
    }

    public record WeightedContent(String eventId, int weight) {
    }

    public record EndingDefinition(String id, String title, String description) {
    }

    public record ChoiceDefinition(String label, String hint, int healthDelta, int spiritDelta,
                                   int lifespanDelta, int karmaDelta, String nextEventId, String action) {
    }
}
