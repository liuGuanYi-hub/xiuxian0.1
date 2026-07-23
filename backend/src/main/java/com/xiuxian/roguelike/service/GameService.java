package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.ChoiceRequest;
import com.xiuxian.roguelike.api.GameDtos.ChoiceView;
import com.xiuxian.roguelike.api.GameDtos.EndingView;
import com.xiuxian.roguelike.api.GameDtos.EventView;
import com.xiuxian.roguelike.api.GameDtos.GameRunView;
import com.xiuxian.roguelike.api.GameDtos.MapNodeView;
import com.xiuxian.roguelike.api.GameDtos.RouteMapView;
import com.xiuxian.roguelike.api.GameDtos.StartRunRequest;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunEventEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.repository.GameRunRepository;
import com.xiuxian.roguelike.repository.RunEventRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class GameService {

    private final GameRunRepository gameRunRepository;
    private final RunEventRepository runEventRepository;
    private final RunMapService runMapService;

    public GameService(GameRunRepository gameRunRepository, RunEventRepository runEventRepository,
                       RunMapService runMapService) {
        this.gameRunRepository = gameRunRepository;
        this.runEventRepository = runEventRepository;
        this.runMapService = runMapService;
    }

    @Transactional
    public GameRunView start(StartRunRequest request) {
        String id = UUID.randomUUID().toString();
        long seed = System.nanoTime();
        GameRunEntity run = new GameRunEntity(
                id,
                request.playerName().trim(),
                request.origin().trim(),
                seed,
                "awaiting_node"
        );
        gameRunRepository.save(run);
        runMapService.generate(run);
        return toView(run, List.of("路线图已经生成，选择第一层的相邻节点开始修行。"));
    }

    @Transactional
    public GameRunView get(String id) {
        GameRunEntity run = findRun(id);
        return toView(run, List.of("存档已恢复，当前在第 " + (run.getCurrentFloor() + 1) + " 层路线。"));
    }

    @Transactional
    public synchronized GameRunView enterNode(String id, String nodeId) {
        GameRunEntity run = findRun(id);
        ensureRunning(run);
        if (!run.getCurrentNodeId().isBlank()) {
            throw new IllegalStateException("请先完成当前节点。");
        }

        RunMapNodeEntity node = runMapService.findNode(run.getId(), nodeId);
        runMapService.enter(node);
        run.enterNode(node.getId(), node.getFloor(), node.getContentId());
        gameRunRepository.save(run);
        return toView(run, List.of("你进入了第 " + (node.getFloor() + 1) + " 层的" + node.getLabel() + "。"));
    }

    @Transactional
    public synchronized GameRunView choose(String id, ChoiceRequest request) {
        GameRunEntity run = findRun(id);
        String requestId = normalizeRequestId(request.requestId());

        if (requestId != null) {
            RunEventEntity previous = runEventRepository.findByRequestId(requestId).orElse(null);
            if (previous != null) {
                if (!run.getId().equals(previous.getRunId())) {
                    throw new IllegalArgumentException("请求编号已经用于另一局旅程。");
                }
                return toView(run, List.of("重复请求已忽略，本局状态没有重复结算。"));
            }
        }

        ensureRunning(run);
        if (run.getCurrentNodeId().isBlank()) {
            throw new IllegalStateException("请先从路线图中选择一个节点。");
        }

        RunMapNodeEntity node = runMapService.findNode(run.getId(), run.getCurrentNodeId());
        if (!"ACTIVE".equals(node.getStatus())) {
            throw new IllegalStateException("当前节点不是可结算状态。");
        }

        EventCatalog.EventDefinition event = EventCatalog.get(run.getCurrentEventId());
        int choiceIndex = request.choiceIndex();
        if (choiceIndex < 0 || choiceIndex >= event.choices().size()) {
            throw new IllegalArgumentException("无效的事件选项。");
        }

        EventCatalog.ChoiceDefinition choice = event.choices().get(choiceIndex);
        Outcome outcome = resolveOutcome(run, event, node);
        int healthDelta = choice.healthDelta() + outcome.healthDelta();
        int spiritDelta = choice.spiritDelta() + outcome.spiritDelta();
        int lifespanDelta = choice.lifespanDelta() + outcome.lifespanDelta();
        int karmaDelta = choice.karmaDelta() + outcome.karmaDelta();
        int nextTurn = run.getTurn() + 1;
        boolean dead = run.getHealth() + healthDelta <= 0 || run.getLifespan() + lifespanDelta <= 0;
        boolean boss = "BOSS".equals(node.getType());
        String nextStatus = dead ? "DEAD" : (boss ? "ASCENDED" : "RUNNING");
        String endingId = dead ? "fallen_path" : (boss ? resolveEnding(run, choiceIndex, healthDelta, spiritDelta, karmaDelta) : null);

        run.applyChoice(
                healthDelta,
                spiritDelta,
                lifespanDelta,
                karmaDelta,
                "awaiting_node",
                realmForTurn(nextTurn),
                nextStatus,
                endingId
        );
        run.clearNode();
        gameRunRepository.save(run);
        runEventRepository.save(new RunEventEntity(
                run.getId(),
                run.getTurn(),
                event.id(),
                event.title(),
                choiceIndex,
                choice.label(),
                healthDelta,
                spiritDelta,
                lifespanDelta,
                karmaDelta,
                requestId,
                outcome.note()
        ));

        List<RunMapNodeEntity> allNodes = runMapService.getNodes(run.getId());
        if ("RUNNING".equals(nextStatus)) {
            runMapService.completeAndUnlock(node, allNodes);
        } else {
            node.markCleared();
            runMapService.saveNode(node);
        }

        List<String> transientLogs = new ArrayList<>();
        if (outcome.note() != null) {
            transientLogs.add(outcome.note());
        }
        if (dead) {
            transientLogs.add("你的肉身或寿元无法支撑下一步，修仙路在此断绝。");
        } else if (boss) {
            transientLogs.add("你完成了渡劫，新的结局正在因果簿上显现。");
        }
        return toView(run, transientLogs);
    }

    private GameRunEntity findRun(String id) {
        return gameRunRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到这局修仙旅程：" + id));
    }

    private void ensureRunning(GameRunEntity run) {
        if (!"RUNNING".equals(run.getStatus())) {
            throw new IllegalStateException("这局旅程已经结束，请重新开始。");
        }
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        if (requestId.length() > 36) {
            throw new IllegalArgumentException("请求编号长度不能超过 36 个字符。");
        }
        return requestId.trim();
    }

    private String realmForTurn(int turn) {
        if (turn >= 10) return "筑基一层";
        if (turn >= 7) return "炼气后期";
        if (turn >= 4) return "炼气中期";
        return "炼气一层";
    }

    private String resolveEnding(GameRunEntity run, int choiceIndex, int healthDelta, int spiritDelta, int karmaDelta) {
        int karma = run.getKarma() + karmaDelta;
        int spirit = run.getSpirit() + spiritDelta;
        if (choiceIndex == 2 && karma >= 8) return "causality_breaker";
        if (karma <= -5) return "demon_sovereign";
        if (choiceIndex == 1) return "free_wanderer";
        if (karma >= 15) return "red_dust_sage";
        if (spirit >= 42 || healthDelta >= 10) return "heavenly_ascension";
        return "heavenly_ascension";
    }

    private GameRunView toView(GameRunEntity run, List<String> transientLogs) {
        EventCatalog.EventDefinition event = EventCatalog.get(run.getCurrentEventId());
        EventCatalog.EventMeta meta = EventCatalog.meta(event.id());
        List<ChoiceView> choices = new ArrayList<>();
        for (int i = 0; i < event.choices().size(); i++) {
            EventCatalog.ChoiceDefinition choice = event.choices().get(i);
            choices.add(new ChoiceView(i, choice.label(), choice.hint()));
        }

        List<String> logs = new ArrayList<>();
        logs.add("你以“" + run.getPlayerName() + "”之名踏上修仙路。");
        for (RunEventEntity history : runEventRepository.findByRunIdOrderByTurnAsc(run.getId())) {
            String line = "第 " + history.getTurn() + " 回合 · " + history.getEventTitle() + "：" + history.getChoiceLabel();
            if (history.getResultNote() != null) line += " · " + history.getResultNote();
            logs.add(line);
        }
        logs.addAll(transientLogs);

        List<MapNodeView> nodes = runMapService.getNodes(run.getId()).stream()
                .map(node -> new MapNodeView(
                        node.getId(),
                        node.getFloor(),
                        node.getPosition(),
                        node.getType(),
                        node.getLabel(),
                        node.getRarity(),
                        node.getStatus(),
                        runMapService.splitIds(node.getNextNodeIds())
                ))
                .toList();
        RouteMapView map = new RouteMapView(10, nodes);
        EndingView ending = run.getEndingId() == null ? null : toEndingView(run.getEndingId());

        return new GameRunView(
                run.getId(), run.getPlayerName(), run.getOrigin(), run.getRealm(),
                run.getHealth(), run.getSpirit(), run.getLifespan(), run.getKarma(),
                run.getTurn(), run.getStatus(), run.getCurrentNodeId(), run.getCurrentFloor(),
                new EventView(event.id(), event.title(), event.description(), choices, meta.rarity(), meta.repeatable()),
                map, ending, logs
        );
    }

    private EndingView toEndingView(String endingId) {
        EventCatalog.EndingDefinition ending = EventCatalog.ending(endingId);
        return new EndingView(ending.id(), ending.title(), ending.description());
    }

    private Outcome resolveOutcome(GameRunEntity run, EventCatalog.EventDefinition event, RunMapNodeEntity node) {
        long mixedSeed = run.getSeed()
                ^ (long) run.getTurn() * 0x9E3779B97F4A7C15L
                ^ event.id().hashCode()
                ^ node.getId().hashCode();
        int roll = new Random(mixedSeed).nextInt(100);
        int luckyThreshold = Math.min(35, 18 + Math.max(-5, Math.min(12, run.getKarma())));
        if (roll < luckyThreshold) return new Outcome(4, 5, 0, 1, "随机机缘：你在节点中发现了一缕额外灵气。");
        if (roll >= 92) return new Outcome(-6, -2, 0, -1, "随机变数：节点深处的因果突然反噬了你的选择。");
        return new Outcome(0, 0, 0, 0, null);
    }

    private record Outcome(int healthDelta, int spiritDelta, int lifespanDelta, int karmaDelta, String note) {
    }
}
