package com.xiuxian.roguelike.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.roguelike.domain.ConfigOperationLogEntity;
import com.xiuxian.roguelike.domain.EventConfigEntity;
import com.xiuxian.roguelike.repository.ConfigOperationLogRepository;
import com.xiuxian.roguelike.repository.EventConfigRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

@Service
public class EventConfigService {

    private static final Set<String> NODE_TYPES = Set.of(
            "BATTLE", "ELITE", "EVENT", "REST", "SHOP", "TREASURE", "BOSS"
    );

    private final EventConfigRepository eventConfigRepository;
    private final ConfigOperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;
    private volatile ConfigState state = ConfigState.empty();

    public EventConfigService(EventConfigRepository eventConfigRepository,
                              ConfigOperationLogRepository operationLogRepository,
                              ObjectMapper objectMapper) {
        this.eventConfigRepository = eventConfigRepository;
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public synchronized void initialize() {
        try {
            List<EventConfigPayload> seeds = objectMapper.readValue(
                    new ClassPathResource("event-config.json").getInputStream(),
                    new TypeReference<>() { }
            );
            int inserted = 0;
            for (EventConfigPayload seed : seeds) {
                if (!eventConfigRepository.existsById(seed.eventId())) {
                    eventConfigRepository.save(toEntity(seed, "bootstrap"));
                    inserted++;
                }
            }
            reload();
            ValidationResult validation = validateCurrent();
            if (!validation.valid()) {
                log("EVENT", null, "INITIALIZE", 0, "bootstrap", "FAILED", validation.message());
                throw new IllegalStateException("事件配置初始化校验失败：" + validation.message());
            }
            log("EVENT", null, "INITIALIZE", 1, "bootstrap", "SUCCESS",
                    "事件配置已加载，补充 " + inserted + " 条，当前启用 " + state.events().size() + " 个事件。");
        } catch (IOException exception) {
            throw new IllegalStateException("事件配置初始化失败。", exception);
        }
    }

    public EventCatalog.EventDefinition get(String id) {
        EventCatalog.EventDefinition event = state.events().get(id);
        if (event == null) {
            reload();
            event = state.events().get(id);
        }
        if (event == null) {
            throw new IllegalStateException("事件配置不存在或已停用：" + id);
        }
        return event;
    }

    public EventCatalog.EventMeta meta(String id) {
        return state.metas().getOrDefault(id, new EventCatalog.EventMeta("普通", false));
    }

    public String pickNodeContent(String nodeType, SplittableRandom random) {
        List<EventCatalog.WeightedContent> candidates = state.nodePools()
                .getOrDefault(nodeType, state.nodePools().getOrDefault("EVENT", List.of()));
        if (candidates.isEmpty()) {
            throw new IllegalStateException("事件节点权重配置为空：" + nodeType);
        }
        int totalWeight = candidates.stream().mapToInt(EventCatalog.WeightedContent::weight).sum();
        int roll = random.nextInt(totalWeight);
        for (EventCatalog.WeightedContent candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return candidate.eventId();
            }
        }
        return candidates.get(candidates.size() - 1).eventId();
    }

    public EventCatalog.EndingDefinition ending(String id) {
        EventCatalog.EndingDefinition ending = state.endings().get(id);
        if (ending == null) {
            throw new IllegalStateException("结局配置不存在或已停用：" + id);
        }
        return ending;
    }

    public String chooseNextEvent(String currentEventId, String preferredNextEventId,
                                  int choiceIndex, long seed, int turn, Set<String> visitedEventIds) {
        List<String> candidates = state.nextEventPools().get(currentEventId);
        if (candidates == null || candidates.isEmpty()) {
            return preferredNextEventId;
        }

        List<String> available = new ArrayList<>();
        for (String candidate : candidates) {
            if (!visitedEventIds.contains(candidate)) {
                available.add(candidate);
            }
        }
        if (available.isEmpty()) {
            available.addAll(candidates);
        }

        long mixed = seed
                ^ (long) currentEventId.hashCode() * 31
                ^ (long) turn * 0x9E3779B97F4A7C15L
                ^ (long) (choiceIndex + 1) * 0xC2B2AE3D27D4EB4FL;
        int randomIndex = Math.floorMod((int) (mixed ^ (mixed >>> 32)), available.size());
        if (preferredNextEventId != null
                && available.contains(preferredNextEventId)
                && Math.floorMod((int) mixed, 4) == 0) {
            return preferredNextEventId;
        }
        return available.get(randomIndex);
    }

    @Transactional
    public synchronized ImportResult importConfigs(List<EventConfigPayload> payloads, String operatorName) {
        if (payloads == null || payloads.isEmpty()) {
            throw new IllegalArgumentException("至少需要导入一条事件配置。");
        }
        String operator = normalizeOperator(operatorName);
        Map<String, EventConfigEntity> candidates = new LinkedHashMap<>();
        for (EventConfigEntity row : eventConfigRepository.findAllByOrderByEventIdAsc()) {
            candidates.put(row.getEventId(), row);
        }
        for (EventConfigPayload payload : payloads) {
            validatePayloadShape(payload);
            candidates.put(payload.eventId(), toEntity(payload, operator));
        }

        ValidationResult validation = validateRows(candidates.values());
        if (!validation.valid()) {
            log("EVENT", null, "IMPORT", 0, operator, "FAILED", validation.message());
            throw new IllegalArgumentException("事件配置校验失败：" + validation.message());
        }

        for (EventConfigPayload payload : payloads) {
            EventConfigEntity existing = eventConfigRepository.findById(payload.eventId()).orElse(null);
            if (existing == null) {
                eventConfigRepository.save(toEntity(payload, operator));
            } else {
                existing.update(payload.configType(), payload.title(), payload.description(), payload.rarity(),
                        payload.repeatable(), payload.version(), payload.enabled(),
                        json(payload.choices()), json(payload.nextEventIds()), json(payload.nodeWeights()), operator);
                eventConfigRepository.save(existing);
            }
            log(payload.configType(), payload.eventId(), "IMPORT", payload.version(), operator,
                    "SUCCESS", "导入配置成功。");
        }
        reload();
        return new ImportResult(payloads.size(), validation.checkedConfigs(), state.events().size(),
                state.endings().size(), validation.message());
    }

    public synchronized void reload() {
        List<EventConfigEntity> rows = eventConfigRepository.findByEnabledTrueOrderByEventIdAsc();
        state = buildState(rows);
    }

    public ValidationResult validateCurrent() {
        return validateRows(eventConfigRepository.findByEnabledTrueOrderByEventIdAsc());
    }

    @Transactional
    public synchronized ValidationResult validateAndLog(String operatorName) {
        ValidationResult result = validateCurrent();
        log("EVENT", null, "VALIDATE", 0, operatorName, result.valid() ? "SUCCESS" : "FAILED", result.message());
        return result;
    }

    @Transactional
    public synchronized ValidationResult reloadAndValidate(String operatorName) {
        reload();
        ValidationResult result = validateCurrent();
        log("EVENT", null, "RELOAD", 0, operatorName, result.valid() ? "SUCCESS" : "FAILED", result.message());
        return result;
    }

    public List<ConfigSnapshot> allConfigs(boolean includeDisabled) {
        List<EventConfigEntity> rows = includeDisabled
                ? eventConfigRepository.findAllByOrderByEventIdAsc()
                : eventConfigRepository.findByEnabledTrueOrderByEventIdAsc();
        return rows.stream().map(this::toSnapshot).toList();
    }

    public List<OperationLogView> recentLogs() {
        return operationLogRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(row -> new OperationLogView(row.getId(), row.getConfigType(), row.getConfigId(),
                        row.getAction(), row.getConfigVersion(), row.getOperatorName(), row.getResult(),
                        row.getMessage(), row.getCreatedAt().toString()))
                .toList();
    }

    public int activeEventCount() {
        return state.events().size();
    }

    public ConfigStatus status() {
        List<EventConfigEntity> rows = eventConfigRepository.findByEnabledTrueOrderByEventIdAsc();
        int activeEvents = (int) rows.stream().filter(row -> "EVENT".equals(row.getConfigType())).count();
        int activeEndings = (int) rows.stream().filter(row -> "ENDING".equals(row.getConfigType())).count();
        int maxVersion = rows.stream().mapToInt(EventConfigEntity::getConfigVersion).max().orElse(0);
        ValidationResult validation = validateRows(rows);
        return new ConfigStatus(activeEvents, activeEndings, maxVersion, validation.valid());
    }

    private ConfigState buildState(List<EventConfigEntity> rows) {
        Map<String, EventCatalog.EventDefinition> events = new HashMap<>();
        Map<String, EventCatalog.EventMeta> metas = new HashMap<>();
        Map<String, EventCatalog.EndingDefinition> endings = new HashMap<>();
        Map<String, List<String>> nextPools = new HashMap<>();
        Map<String, List<EventCatalog.WeightedContent>> nodePools = new HashMap<>();

        for (EventConfigEntity row : rows) {
            ParsedConfig parsed = parse(row);
            if ("ENDING".equals(row.getConfigType())) {
                endings.put(row.getEventId(), new EventCatalog.EndingDefinition(
                        row.getEventId(), row.getTitle(), row.getDescription()));
                continue;
            }
            events.put(row.getEventId(), new EventCatalog.EventDefinition(
                    row.getEventId(), row.getTitle(), row.getDescription(), parsed.choices()));
            metas.put(row.getEventId(), new EventCatalog.EventMeta(row.getRarity(), row.isRepeatable()));
            nextPools.put(row.getEventId(), parsed.nextEventIds());
            for (Map.Entry<String, Integer> weight : parsed.nodeWeights().entrySet()) {
                nodePools.computeIfAbsent(weight.getKey(), ignored -> new ArrayList<>())
                        .add(new EventCatalog.WeightedContent(row.getEventId(), weight.getValue()));
            }
        }
        nodePools.replaceAll((key, value) -> List.copyOf(value));
        nextPools.replaceAll((key, value) -> List.copyOf(value));
        return new ConfigState(Map.copyOf(events), Map.copyOf(metas), Map.copyOf(endings),
                Map.copyOf(nextPools), Map.copyOf(nodePools));
    }

    private ValidationResult validateRows(Iterable<EventConfigEntity> rows) {
        List<EventConfigEntity> activeRows = new ArrayList<>();
        for (EventConfigEntity row : rows) {
            if (row.isEnabled()) activeRows.add(row);
        }
        Set<String> ids = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (EventConfigEntity row : activeRows) {
            if (!ids.add(row.getEventId())) errors.add("重复配置编号 " + row.getEventId());
            if (row.getConfigVersion() <= 0) errors.add(row.getEventId() + " 的版本号必须大于 0");
            if (blank(row.getTitle()) || blank(row.getDescription())) errors.add(row.getEventId() + " 缺少标题或描述");
            try {
                ParsedConfig parsed = parse(row);
                if ("EVENT".equals(row.getConfigType())) {
                    if (parsed.nodeWeights().values().stream().anyMatch(weight -> weight == null || weight <= 0)) {
                        errors.add(row.getEventId() + " 存在非正节点权重");
                    }
                    if (parsed.nodeWeights().entrySet().stream().anyMatch(entry -> !NODE_TYPES.contains(entry.getKey()))) {
                        errors.add(row.getEventId() + " 存在未知节点类型");
                    }
                }
            } catch (RuntimeException exception) {
                errors.add(row.getEventId() + " JSON 无法解析：" + exception.getMessage());
            }
        }

        Set<String> activeIds = ids;
        for (EventConfigEntity row : activeRows) {
            try {
                ParsedConfig parsed = parse(row);
                for (String nextId : parsed.nextEventIds()) {
                    if (!activeIds.contains(nextId)) errors.add(row.getEventId() + " 指向不存在的事件 " + nextId);
                }
                for (EventCatalog.ChoiceDefinition choice : parsed.choices()) {
                    if (!activeIds.contains(choice.nextEventId())) {
                        errors.add(row.getEventId() + " 选项指向不存在的事件 " + choice.nextEventId());
                    }
                }
            } catch (RuntimeException ignored) {
                // 第一轮已经报告 JSON 解析错误，这里避免重复刷屏。
            }
        }
        if (!activeIds.contains("awaiting_node")) errors.add("缺少 awaiting_node 入口事件");
        for (String nodeType : NODE_TYPES) {
            if (!"awaiting_node".equals(nodeType)
                    && activeRows.stream().noneMatch(row -> "EVENT".equals(row.getConfigType())
                    && parseSafely(row).nodeWeights().getOrDefault(nodeType, 0) > 0)) {
                errors.add("节点类型 " + nodeType + " 没有可用事件权重");
            }
        }
        return new ValidationResult(errors.isEmpty(), activeRows.size(),
                errors.isEmpty() ? "配置校验通过。" : String.join("；", errors));
    }

    private ParsedConfig parseSafely(EventConfigEntity row) {
        try {
            return parse(row);
        } catch (RuntimeException exception) {
            return new ParsedConfig(List.of(), List.of(), Map.of());
        }
    }

    private ParsedConfig parse(EventConfigEntity row) {
        try {
            List<ChoicePayload> choices = objectMapper.readValue(row.getChoicesJson(), new TypeReference<>() { });
            List<String> nextEventIds = objectMapper.readValue(row.getNextEventIdsJson(), new TypeReference<>() { });
            Map<String, Integer> nodeWeights = objectMapper.readValue(row.getNodeWeightsJson(), new TypeReference<>() { });
            List<EventCatalog.ChoiceDefinition> definitions = choices.stream()
                    .map(choice -> new EventCatalog.ChoiceDefinition(choice.label(), choice.hint(),
                            choice.healthDelta(), choice.spiritDelta(), choice.lifespanDelta(),
                            choice.karmaDelta(), choice.nextEventId(), choice.action()))
                    .toList();
            return new ParsedConfig(definitions, nextEventIds, nodeWeights);
        } catch (IOException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    private EventConfigEntity toEntity(EventConfigPayload payload, String operator) {
        return new EventConfigEntity(payload.eventId(), payload.configType(), payload.title(), payload.description(),
                payload.rarity(), payload.repeatable(), payload.version(), payload.enabled(),
                json(payload.choices()), json(payload.nextEventIds()), json(payload.nodeWeights()), operator);
    }

    private ConfigSnapshot toSnapshot(EventConfigEntity row) {
        ParsedConfig parsed = parse(row);
        return new ConfigSnapshot(row.getEventId(), row.getConfigType(), row.getTitle(), row.getDescription(),
                row.getRarity(), row.isRepeatable(), row.getConfigVersion(), row.isEnabled(),
                parsed.choices(), parsed.nextEventIds(), parsed.nodeWeights(), row.getUpdatedBy(),
                row.getUpdatedAt().toString());
    }

    private void validatePayloadShape(EventConfigPayload payload) {
        if (payload == null || blank(payload.eventId()) || blank(payload.configType())
                || blank(payload.title()) || blank(payload.description())) {
            throw new IllegalArgumentException("事件配置缺少编号、类型、标题或描述。");
        }
        if (!Set.of("EVENT", "ENDING").contains(payload.configType())) {
            throw new IllegalArgumentException("事件配置类型只能是 EVENT 或 ENDING。");
        }
        if (payload.version() <= 0) throw new IllegalArgumentException("配置版本号必须大于 0。");
        if (payload.choices() == null || payload.nextEventIds() == null || payload.nodeWeights() == null) {
            throw new IllegalArgumentException("事件选项、后继事件和节点权重不能为空。");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("事件配置 JSON 序列化失败。", exception);
        }
    }

    private void log(String configType, String configId, String action, int version,
                     String operator, String result, String message) {
        operationLogRepository.save(new ConfigOperationLogEntity(configType, configId, action,
                version, normalizeOperator(operator), result, message));
    }

    private String normalizeOperator(String operator) {
        if (blank(operator)) return "system";
        return operator.trim().substring(0, Math.min(80, operator.trim().length()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record EventConfigPayload(String configType, String eventId, String title, String description,
                                     String rarity, boolean repeatable, int version, boolean enabled,
                                     List<ChoicePayload> choices, List<String> nextEventIds,
                                     Map<String, Integer> nodeWeights) {
    }

    public record ChoicePayload(String label, String hint, int healthDelta, int spiritDelta,
                                int lifespanDelta, int karmaDelta, String nextEventId, String action) {
    }

    public record ValidationResult(boolean valid, int checkedConfigs, String message) {
    }

    public record ImportResult(int importedConfigs, int checkedConfigs, int activeEvents,
                               int activeEndings, String validationMessage) {
    }

    public record ConfigSnapshot(String eventId, String configType, String title, String description,
                                 String rarity, boolean repeatable, int version, boolean enabled,
                                 List<EventCatalog.ChoiceDefinition> choices, List<String> nextEventIds,
                                 Map<String, Integer> nodeWeights, String updatedBy, String updatedAt) {
    }

    public record OperationLogView(String id, String configType, String configId, String action,
                                   int version, String operatorName, String result, String message,
                                   String createdAt) {
    }

    public record ConfigStatus(int activeEvents, int activeEndings, int maxVersion, boolean valid) {
    }

    private record ParsedConfig(List<EventCatalog.ChoiceDefinition> choices, List<String> nextEventIds,
                                Map<String, Integer> nodeWeights) {
    }

    private record ConfigState(Map<String, EventCatalog.EventDefinition> events,
                               Map<String, EventCatalog.EventMeta> metas,
                               Map<String, EventCatalog.EndingDefinition> endings,
                               Map<String, List<String>> nextEventPools,
                               Map<String, List<EventCatalog.WeightedContent>> nodePools) {
        private static ConfigState empty() {
            return new ConfigState(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
