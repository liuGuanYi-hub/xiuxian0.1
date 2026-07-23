package com.xiuxian.roguelike.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "run_event", indexes = {
        @Index(name = "idx_run_event_run_turn", columnList = "run_id,turn")
})
public class RunEventEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(nullable = false)
    private int turn;

    @Column(nullable = false, length = 80)
    private String eventId;

    @Column(nullable = false, length = 120)
    private String eventTitle;

    @Column(nullable = false)
    private int choiceIndex;

    @Column(nullable = false, length = 200)
    private String choiceLabel;

    @Column(nullable = false)
    private int healthDelta;

    @Column(nullable = false)
    private int spiritDelta;

    @Column(nullable = false)
    private int lifespanDelta;

    @Column(nullable = false)
    private int karmaDelta;

    @Column(length = 36, unique = true)
    private String requestId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RunEventEntity() {
    }

    public RunEventEntity(String runId, int turn, String eventId, String eventTitle, int choiceIndex,
                          String choiceLabel, int healthDelta, int spiritDelta, int lifespanDelta,
                          int karmaDelta, String requestId) {
        this.id = java.util.UUID.randomUUID().toString();
        this.runId = runId;
        this.turn = turn;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.choiceIndex = choiceIndex;
        this.choiceLabel = choiceLabel;
        this.healthDelta = healthDelta;
        this.spiritDelta = spiritDelta;
        this.lifespanDelta = lifespanDelta;
        this.karmaDelta = karmaDelta;
        this.requestId = requestId;
        this.createdAt = LocalDateTime.now();
    }

    public String getRunId() { return runId; }
    public int getTurn() { return turn; }
    public String getEventTitle() { return eventTitle; }
    public String getChoiceLabel() { return choiceLabel; }
    public String getRequestId() { return requestId; }
}
