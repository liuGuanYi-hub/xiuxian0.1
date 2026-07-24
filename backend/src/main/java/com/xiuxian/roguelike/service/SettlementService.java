package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.LeaderboardEntryView;
import com.xiuxian.roguelike.api.GameDtos.SettlementView;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.domain.RunSettlementEntity;
import com.xiuxian.roguelike.repository.RunSettlementRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class SettlementService {

    private final RunSettlementRepository settlementRepository;
    private final RunMapService runMapService;
    private final BuildService buildService;
    private final EventConfigService eventConfigService;

    public SettlementService(RunSettlementRepository settlementRepository, RunMapService runMapService,
                             BuildService buildService, EventConfigService eventConfigService) {
        this.settlementRepository = settlementRepository;
        this.runMapService = runMapService;
        this.buildService = buildService;
        this.eventConfigService = eventConfigService;
    }

    @Transactional
    public synchronized RunSettlementEntity ensure(GameRunEntity run) {
        if ("RUNNING".equals(run.getStatus())) {
            return null;
        }
        return settlementRepository.findByRunId(run.getId()).orElseGet(() -> {
            List<RunMapNodeEntity> nodes = runMapService.getNodes(run.getId());
            int eliteCount = (int) nodes.stream()
                    .filter(node -> "ELITE".equals(node.getType()) && "CLEARED".equals(node.getStatus()))
                    .count();
            int activeCards = buildService.activeCardCount(run.getId());
            String endingId = run.getEndingId() == null ? "fallen_path" : run.getEndingId();
            String endingTitle = endingTitle(endingId);
            int score = score(run, activeCards, eliteCount);
            return settlementRepository.save(new RunSettlementEntity(
                    run.getId(), run.getPlayerName(), run.getOrigin(), run.getStatus(), endingId, endingTitle,
                    run.getCurrentFloor() + 1, run.getTurn(), run.getHealth(), run.getSpirit(),
                    run.getLifespan(), run.getKarma(), run.getSpiritStones(), activeCards, eliteCount,
                    score, run.getSeed()
            ));
        });
    }

    public SettlementView toView(RunSettlementEntity settlement) {
        if (settlement == null) return null;
        return new SettlementView(settlement.getRunId(), settlement.getPlayerName(), settlement.getOrigin(),
                settlement.getStatus(), settlement.getEndingId(), settlement.getEndingTitle(),
                settlement.getFloorReached(), settlement.getTurn(), settlement.getHealth(),
                settlement.getSpirit(), settlement.getLifespan(), settlement.getKarma(),
                settlement.getSpiritStones(), settlement.getActiveCards(), settlement.getEliteCount(),
                settlement.getScore(), settlement.getSettledAt().toString());
    }

    public List<LeaderboardEntryView> leaderboard(int requestedLimit) {
        int limit = Math.max(1, Math.min(50, requestedLimit));
        List<RunSettlementEntity> rows = settlementRepository
                .findAllByOrderByScoreDescSettledAtAsc(PageRequest.of(0, limit));
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    RunSettlementEntity row = rows.get(index);
                    return new LeaderboardEntryView(index + 1, row.getRunId(), row.getPlayerName(),
                            row.getOrigin(), row.getStatus(), row.getEndingTitle(), row.getFloorReached(),
                            row.getTurn(), row.getScore(), row.getSettledAt().toString());
                }).toList();
    }

    private int score(GameRunEntity run, int activeCards, int eliteCount) {
        int score = (run.getCurrentFloor() + 1) * 100;
        score += run.getTurn() * 15;
        score += run.getHealth();
        score += run.getSpirit() * 2;
        score += run.getLifespan();
        score += Math.max(0, run.getKarma()) * 4;
        score += run.getSpiritStones();
        score += activeCards * 25;
        score += eliteCount * 120;
        if ("ASCENDED".equals(run.getStatus())) score += 1000;
        return score;
    }

    private String endingTitle(String endingId) {
        try {
            return eventConfigService.ending(endingId).title();
        } catch (RuntimeException ignored) {
            return "道途断绝";
        }
    }
}
