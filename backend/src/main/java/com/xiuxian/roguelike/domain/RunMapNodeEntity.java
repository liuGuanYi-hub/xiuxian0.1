package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "run_map_node", indexes = {
        @Index(name = "idx_map_node_run_floor", columnList = "run_id,floor_number,position_number")
})
public class RunMapNodeEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "floor_number", nullable = false)
    private int floor;

    @Column(name = "position_number", nullable = false)
    private int position;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(nullable = false, length = 80)
    private String contentId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "next_node_ids", nullable = false, columnDefinition = "TEXT")
    private String nextNodeIds;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RunMapNodeEntity() {
    }

    public RunMapNodeEntity(String id, String runId, int floor, int position, String type,
                            String label, String rarity, String contentId, String status) {
        this.id = id;
        this.runId = runId;
        this.floor = floor;
        this.position = position;
        this.type = type;
        this.label = label;
        this.rarity = rarity;
        this.contentId = contentId;
        this.status = status;
        this.nextNodeIds = "";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getRunId() { return runId; }
    public int getFloor() { return floor; }
    public int getPosition() { return position; }
    public String getType() { return type; }
    public String getLabel() { return label; }
    public String getRarity() { return rarity; }
    public String getContentId() { return contentId; }
    public String getStatus() { return status; }
    public String getNextNodeIds() { return nextNodeIds; }

    public void setNextNodeIds(String nextNodeIds) {
        this.nextNodeIds = nextNodeIds;
    }

    public void markActive() {
        this.status = "ACTIVE";
    }

    public void markCleared() {
        this.status = "CLEARED";
    }

    public void unlock() {
        if ("LOCKED".equals(this.status)) {
            this.status = "AVAILABLE";
        }
    }

    public void lock() {
        if ("AVAILABLE".equals(this.status)) {
            this.status = "LOCKED";
        }
    }
}
