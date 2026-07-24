package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_config", indexes = {
        @Index(name = "idx_event_config_type_enabled", columnList = "config_type,enabled")
})
public class EventConfigEntity {

    @Id
    @Column(name = "event_id", length = 80, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "config_type", nullable = false, length = 20)
    private String configType;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false)
    private boolean repeatable;

    @Column(name = "config_version", nullable = false)
    private int configVersion;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "choices_json", nullable = false, columnDefinition = "TEXT")
    private String choicesJson;

    @Column(name = "next_event_ids_json", nullable = false, columnDefinition = "TEXT")
    private String nextEventIdsJson;

    @Column(name = "node_weights_json", nullable = false, columnDefinition = "TEXT")
    private String nodeWeightsJson;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EventConfigEntity() {
    }

    public EventConfigEntity(String eventId, String configType, String title, String description,
                             String rarity, boolean repeatable, int configVersion, boolean enabled,
                             String choicesJson, String nextEventIdsJson, String nodeWeightsJson,
                             String updatedBy) {
        this.eventId = eventId;
        this.configType = configType;
        this.title = title;
        this.description = description;
        this.rarity = rarity;
        this.repeatable = repeatable;
        this.configVersion = configVersion;
        this.enabled = enabled;
        this.choicesJson = choicesJson;
        this.nextEventIdsJson = nextEventIdsJson;
        this.nodeWeightsJson = nodeWeightsJson;
        this.updatedBy = updatedBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getEventId() { return eventId; }
    public String getConfigType() { return configType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRarity() { return rarity; }
    public boolean isRepeatable() { return repeatable; }
    public int getConfigVersion() { return configVersion; }
    public boolean isEnabled() { return enabled; }
    public String getChoicesJson() { return choicesJson; }
    public String getNextEventIdsJson() { return nextEventIdsJson; }
    public String getNodeWeightsJson() { return nodeWeightsJson; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(String configType, String title, String description, String rarity,
                       boolean repeatable, int configVersion, boolean enabled,
                       String choicesJson, String nextEventIdsJson, String nodeWeightsJson,
                       String updatedBy) {
        this.configType = configType;
        this.title = title;
        this.description = description;
        this.rarity = rarity;
        this.repeatable = repeatable;
        this.configVersion = configVersion;
        this.enabled = enabled;
        this.choicesJson = choicesJson;
        this.nextEventIdsJson = nextEventIdsJson;
        this.nodeWeightsJson = nodeWeightsJson;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
