package com.xiuxian.roguelike.api;

import java.util.List;

public final class ProgressDtos {

    private ProgressDtos() {
    }

    public record UnlockView(String id, String name, String description, int cost,
                             boolean unlocked, String effectText) {
    }

    public record SettlementHistoryView(String runId, String characterId, String playerName,
                                        String status, String endingTitle, int score,
                                        int causalityEarned, String settledAt) {
    }

    public record AccountProgressView(int causalityPoints, int totalCausalityEarned,
                                      int totalCausalitySpent, long totalRuns,
                                      long completedRuns, List<UnlockView> unlocks,
                                      List<SettlementHistoryView> recentSettlements) {
    }
}
