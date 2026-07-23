package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "run_shop", indexes = {
        @Index(name = "idx_run_shop_run_node", columnList = "run_id,node_id", unique = true)
})
public class RunShopEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "node_id", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "refresh_count", nullable = false)
    private int refreshCount;

    @Column(name = "refresh_limit", nullable = false)
    private int refreshLimit;

    @Column(name = "removal_used", nullable = false)
    private boolean removalUsed;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected RunShopEntity() {
    }

    public RunShopEntity(String runId, String nodeId) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.nodeId = nodeId;
        this.refreshCount = 0;
        this.refreshLimit = 2;
        this.removalUsed = false;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public String getNodeId() { return nodeId; }
    public int getRefreshCount() { return refreshCount; }
    public int getRefreshLimit() { return refreshLimit; }
    public boolean isRemovalUsed() { return removalUsed; }
    public String getStatus() { return status; }

    public void refresh() {
        this.refreshCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void useRemoval() {
        this.removalUsed = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = "CLOSED";
        this.updatedAt = LocalDateTime.now();
    }
}
