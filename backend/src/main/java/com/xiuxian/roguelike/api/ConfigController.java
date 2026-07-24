package com.xiuxian.roguelike.api;

import com.xiuxian.roguelike.api.ConfigDtos.ConfigImportView;
import com.xiuxian.roguelike.api.ConfigDtos.ConfigValidationView;
import com.xiuxian.roguelike.api.ConfigDtos.CardConfigImportRequest;
import com.xiuxian.roguelike.api.ConfigDtos.CardConfigImportView;
import com.xiuxian.roguelike.api.ConfigDtos.CardConfigInput;
import com.xiuxian.roguelike.api.ConfigDtos.CardConfigListView;
import com.xiuxian.roguelike.api.ConfigDtos.EventConfigImportRequest;
import com.xiuxian.roguelike.api.ConfigDtos.EventConfigInput;
import com.xiuxian.roguelike.api.ConfigDtos.EventConfigListView;
import com.xiuxian.roguelike.service.EventConfigService;
import com.xiuxian.roguelike.service.BuildConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    private final EventConfigService eventConfigService;
    private final BuildConfigService buildConfigService;

    public ConfigController(EventConfigService eventConfigService, BuildConfigService buildConfigService) {
        this.eventConfigService = eventConfigService;
        this.buildConfigService = buildConfigService;
    }

    @GetMapping("/events")
    public EventConfigListView events(
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        EventConfigService.ValidationResult validation = eventConfigService.validateCurrent();
        return new EventConfigListView(eventConfigService.allConfigs(includeDisabled), validation.valid(),
                validation.checkedConfigs(), validation.message(), eventConfigService.activeEventCount());
    }

    @PostMapping("/events/import")
    public ConfigImportView importEvents(@Valid @RequestBody EventConfigImportRequest request) {
        EventConfigService.ImportResult result = eventConfigService.importConfigs(
                request.configs().stream().map(this::toPayload).toList(), request.operator());
        return new ConfigImportView(result.importedConfigs(), result.checkedConfigs(), result.activeEvents(),
                result.activeEndings(), result.validationMessage());
    }

    @PostMapping("/validate")
    public ConfigValidationView validate(@RequestParam(defaultValue = "admin") String operator) {
        EventConfigService.ValidationResult result = eventConfigService.validateAndLog(operator);
        return new ConfigValidationView(result.valid(), result.checkedConfigs(), result.message());
    }

    @PostMapping("/reload")
    public ConfigValidationView reload(@RequestParam(defaultValue = "admin") String operator) {
        EventConfigService.ValidationResult result = eventConfigService.reloadAndValidate(operator);
        return new ConfigValidationView(result.valid(), result.checkedConfigs(), result.message());
    }

    @GetMapping("/logs")
    public List<EventConfigService.OperationLogView> logs() {
        return eventConfigService.recentLogs();
    }

    @GetMapping("/cards")
    public CardConfigListView cards(
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        BuildConfigService.CardValidationResult validation = buildConfigService.validateCurrent();
        List<?> configs = buildConfigService.allConfigs(includeDisabled);
        int activeCards = (int) buildConfigService.allConfigs(false).size();
        int disabledCards = (int) buildConfigService.allConfigs(true).stream()
                .filter(card -> !card.enabled()).count();
        return new CardConfigListView(configs, validation.valid(), validation.checkedCards(),
                validation.message(), activeCards, disabledCards);
    }

    @PostMapping("/cards/import")
    public CardConfigImportView importCards(@Valid @RequestBody CardConfigImportRequest request) {
        BuildConfigService.CardImportResult result = buildConfigService.importConfigs(
                request.configs().stream().map(this::toCardPayload).toList(), request.operator());
        return new CardConfigImportView(result.importedCards(), result.activeCards(),
                result.disabledCards(), result.validationMessage());
    }

    private EventConfigService.EventConfigPayload toPayload(EventConfigInput input) {
        List<EventConfigService.ChoicePayload> choices = input.choices() == null
                ? List.of()
                : input.choices().stream().map(choice -> new EventConfigService.ChoicePayload(
                        choice.label(), choice.hint(), choice.healthDelta(), choice.spiritDelta(),
                        choice.lifespanDelta(), choice.karmaDelta(), choice.nextEventId(), choice.action()
                )).toList();
        return new EventConfigService.EventConfigPayload(
                input.configType(), input.eventId(), input.title(), input.description(), input.rarity(),
                input.repeatable(), input.version(), input.enabled(), choices,
                input.nextEventIds() == null ? List.of() : input.nextEventIds(),
                input.nodeWeights() == null ? java.util.Map.of() : input.nodeWeights()
        );
    }

    private BuildConfigService.CardConfigPayload toCardPayload(CardConfigInput input) {
        return new BuildConfigService.CardConfigPayload(input.cardId(), input.category(), input.name(),
                input.rarity(), input.description(), input.effectText(), input.archetype(),
                input.healthOnClaim(), input.spiritOnClaim(), input.lifespanOnClaim(), input.karmaOnClaim(),
                input.battleHealthBonus(), input.battleSpiritBonus(), input.combatDamageBonus(),
                input.combatBlockBonus(), input.combatSpiritGain(), input.combatPoisonBonus(),
                input.battleWeight(), input.eliteWeight(), input.treasureWeight(), input.version(),
                input.enabled());
    }
}
