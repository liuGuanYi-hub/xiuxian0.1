package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.ChoiceRequest;
import com.xiuxian.roguelike.api.GameDtos.ChoiceView;
import com.xiuxian.roguelike.api.GameDtos.EventView;
import com.xiuxian.roguelike.api.GameDtos.GameRunView;
import com.xiuxian.roguelike.api.GameDtos.StartRunRequest;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunEventEntity;
import com.xiuxian.roguelike.repository.GameRunRepository;
import com.xiuxian.roguelike.repository.RunEventRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class GameService {

    private final GameRunRepository gameRunRepository;
    private final RunEventRepository runEventRepository;

    public GameService(GameRunRepository gameRunRepository, RunEventRepository runEventRepository) {
        this.gameRunRepository = gameRunRepository;
        this.runEventRepository = runEventRepository;
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
                "river"
        );
        gameRunRepository.save(run);
        return toView(run, List.of());
    }

    @Transactional
    public GameRunView get(String id) {
        GameRunEntity run = findRun(id);
        return toView(run, List.of("存档已恢复，当前是第 " + (run.getTurn() + 1) + " 回合。"));
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

        if (!"RUNNING".equals(run.getStatus())) {
            throw new IllegalStateException("这局旅程已经结束，请重新开始。");
        }

        EventCatalog.EventDefinition event = EventCatalog.get(run.getCurrentEventId());
        int choiceIndex = request.choiceIndex();
        if (choiceIndex < 0 || choiceIndex >= event.choices().size()) {
            throw new IllegalArgumentException("无效的事件选项。");
        }

        EventCatalog.ChoiceDefinition choice = event.choices().get(choiceIndex);
        List<RunEventEntity> history = runEventRepository.findByRunIdOrderByTurnAsc(run.getId());
        Set<String> visitedEventIds = new HashSet<>();
        history.forEach(item -> visitedEventIds.add(item.getEventId()));
        visitedEventIds.add(run.getCurrentEventId());

        Outcome outcome = resolveOutcome(run, event);
        int healthDelta = choice.healthDelta() + outcome.healthDelta();
        int spiritDelta = choice.spiritDelta() + outcome.spiritDelta();
        int lifespanDelta = choice.lifespanDelta() + outcome.lifespanDelta();
        int karmaDelta = choice.karmaDelta() + outcome.karmaDelta();
        String nextEvent = EventCatalog.chooseNextEvent(
                run.getCurrentEventId(),
                choice.nextEventId(),
                choiceIndex,
                run.getSeed(),
                run.getTurn() + 1,
                visitedEventIds
        );
        String nextStatus = "RUNNING";
        int nextTurn = run.getTurn() + 1;

        if ("finish".equals(nextEvent)) {
            nextStatus = "ASCENDED";
        } else if (run.getHealth() + healthDelta <= 0
                || run.getLifespan() + lifespanDelta <= 0) {
            nextStatus = "DEAD";
            nextEvent = "finish";
        }

        run.applyChoice(
                healthDelta,
                spiritDelta,
                lifespanDelta,
                karmaDelta,
                nextEvent,
                realmForTurn(nextTurn),
                nextStatus
        );
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

        List<String> transientLogs = new ArrayList<>();
        if ("DEAD".equals(nextStatus)) {
            transientLogs.add("你的肉身或寿元无法支撑下一步，修仙路在此断绝。");
        } else if ("ASCENDED".equals(nextStatus)) {
            transientLogs.add("你完成了这一轮旅程，因果簿上留下了新的名字。");
        }
        return toView(run, transientLogs);
    }

    private GameRunEntity findRun(String id) {
        return gameRunRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到这局修仙旅程：" + id));
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
        if (turn >= 10) {
            return "筑基一层";
        }
        if (turn >= 7) {
            return "炼气后期";
        }
        if (turn >= 4) {
            return "炼气中期";
        }
        return "炼气一层";
    }

    private GameRunView toView(GameRunEntity run, List<String> transientLogs) {
        EventCatalog.EventDefinition event = EventCatalog.get(run.getCurrentEventId());
        List<ChoiceView> choices = new ArrayList<>();
        for (int i = 0; i < event.choices().size(); i++) {
            EventCatalog.ChoiceDefinition choice = event.choices().get(i);
            choices.add(new ChoiceView(i, choice.label(), choice.hint()));
        }

        List<String> logs = new ArrayList<>();
        logs.add("你以“" + run.getPlayerName() + "”之名踏上修仙路。");
        for (RunEventEntity history : runEventRepository.findByRunIdOrderByTurnAsc(run.getId())) {
            String line = "第 " + history.getTurn() + " 回合 · " + history.getEventTitle() + "：" + history.getChoiceLabel();
            if (history.getResultNote() != null) {
                line += " · " + history.getResultNote();
            }
            logs.add(line);
        }
        logs.addAll(transientLogs);

        return new GameRunView(
                run.getId(),
                run.getPlayerName(),
                run.getOrigin(),
                run.getRealm(),
                run.getHealth(),
                run.getSpirit(),
                run.getLifespan(),
                run.getKarma(),
                run.getTurn(),
                run.getStatus(),
                new EventView(event.id(), event.title(), event.description(), choices),
                logs
        );
    }

    private Outcome resolveOutcome(GameRunEntity run, EventCatalog.EventDefinition event) {
        long mixedSeed = run.getSeed()
                ^ (long) run.getTurn() * 0x9E3779B97F4A7C15L
                ^ event.id().hashCode();
        int roll = new Random(mixedSeed).nextInt(100);
        int luckyThreshold = Math.min(35, 18 + Math.max(-5, Math.min(12, run.getKarma())));
        if (roll < luckyThreshold) {
            return new Outcome(4, 5, 0, 1, "随机机缘：你在这次选择中额外获得了一缕灵气。");
        }
        if (roll >= 92) {
            return new Outcome(-6, -2, 0, -1, "随机变数：暗处的因果突然反噬了你的选择。");
        }
        return new Outcome(0, 0, 0, 0, null);
    }

    private record Outcome(int healthDelta, int spiritDelta, int lifespanDelta, int karmaDelta, String note) {
    }
}
