package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "config_operation_log", indexes = {
        @Index(name = "idx_config_log_created_at", columnList = "created_at")
})
public class ConfigOperationLogEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "config_type", nullable = false, length = 20)
    private String configType;

    @Column(name = "config_id", length = 80)
    private String configId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false)
    private int configVersion;

    @Column(nullable = false, length = 80)
    private String operatorName;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ConfigOperationLogEntity() {
    }

    public ConfigOperationLogEntity(String configType, String configId, String action,
                                    int configVersion, String operatorName, String result,
                                    String message) {
        this.id = UUID.randomUUID().toString();
        this.configType = configType;
        this.configId = configId;
        this.action = action;
        this.configVersion = configVersion;
        this.operatorName = operatorName;
        this.result = result;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getConfigType() { return configType; }
    public String getConfigId() { return configId; }
    public String getAction() { return action; }
    public int getConfigVersion() { return configVersion; }
    public String getOperatorName() { return operatorName; }
    public String getResult() { return result; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
