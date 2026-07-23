package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.repository.RunMapNodeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RunMapService {

    private static final int TOTAL_FLOORS = 10;
    private static final int MIDDLE_NODES_PER_FLOOR = 3;

    private final RunMapNodeRepository nodeRepository;

    public RunMapService(RunMapNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public List<RunMapNodeEntity> generate(GameRunEntity run) {
        SplittableRandom random = new SplittableRandom(run.getSeed());
        List<List<RunMapNodeEntity>> floors = new ArrayList<>();

        for (int floor = 0; floor < TOTAL_FLOORS; floor++) {
            int count = floor == TOTAL_FLOORS - 1 ? 1 : MIDDLE_NODES_PER_FLOOR;
            List<RunMapNodeEntity> currentFloor = new ArrayList<>();
            for (int position = 0; position < count; position++) {
                String type = typeFor(floor, position, random);
                String contentId = EventCatalog.pickNodeContent(type, random);
                EventCatalog.EventMeta meta = EventCatalog.meta(contentId);
                String label = labelFor(type, contentId);
                String status = floor == 0 ? "AVAILABLE" : "LOCKED";
                currentFloor.add(new RunMapNodeEntity(
                        UUID.randomUUID().toString(),
                        run.getId(),
                        floor,
                        position,
                        type,
                        label,
                        meta.rarity(),
                        contentId,
                        status
                ));
            }
            floors.add(currentFloor);
        }

        for (int floor = 0; floor < floors.size() - 1; floor++) {
            connectFloors(floors.get(floor), floors.get(floor + 1), random);
        }

        List<RunMapNodeEntity> nodes = floors.stream().flatMap(List::stream).toList();
        return nodeRepository.saveAll(nodes);
    }

    public List<RunMapNodeEntity> getNodes(String runId) {
        return nodeRepository.findByRunIdOrderByFloorAscPositionAsc(runId);
    }

    public void saveNode(RunMapNodeEntity node) {
        nodeRepository.save(node);
    }

    public RunMapNodeEntity findNode(String runId, String nodeId) {
        return nodeRepository.findByIdAndRunId(nodeId, runId)
                .orElseThrow(() -> new IllegalArgumentException("找不到这局路线中的节点。"));
    }

    public void completeAndUnlock(RunMapNodeEntity current, List<RunMapNodeEntity> allNodes) {
        current.markCleared();
        List<String> nextIds = splitIds(current.getNextNodeIds());
        for (RunMapNodeEntity node : allNodes) {
            if (node.getFloor() == current.getFloor() && !node.getId().equals(current.getId())) {
                node.lock();
            }
            if (nextIds.contains(node.getId())) {
                node.unlock();
            }
        }
        nodeRepository.saveAll(allNodes);
    }

    public void enter(RunMapNodeEntity node) {
        if (!"AVAILABLE".equals(node.getStatus())) {
            throw new IllegalStateException("这个节点当前不可进入。");
        }
        node.markActive();
        nodeRepository.save(node);
    }

    public List<String> splitIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return List.of(ids.split(","));
    }

    private void connectFloors(List<RunMapNodeEntity> current, List<RunMapNodeEntity> next, SplittableRandom random) {
        for (RunMapNodeEntity node : current) {
            List<RunMapNodeEntity> targets = new ArrayList<>();
            targets.add(next.get(Math.min(node.getPosition(), next.size() - 1)));
            if (next.size() > 1 && random.nextInt(100) < 65) {
                RunMapNodeEntity extra = next.get(random.nextInt(next.size()));
                if (!targets.contains(extra)) {
                    targets.add(extra);
                }
            }
            node.setNextNodeIds(targets.stream().map(RunMapNodeEntity::getId).collect(Collectors.joining(",")));
        }

        for (RunMapNodeEntity target : next) {
            boolean reachable = current.stream()
                    .anyMatch(node -> splitIds(node.getNextNodeIds()).contains(target.getId()));
            if (!reachable) {
                RunMapNodeEntity source = current.get(random.nextInt(current.size()));
                String ids = source.getNextNodeIds();
                source.setNextNodeIds(ids.isBlank() ? target.getId() : ids + "," + target.getId());
            }
        }
    }

    private String typeFor(int floor, int position, SplittableRandom random) {
        if (floor == 0) {
            return position == 1 ? "EVENT" : "BATTLE";
        }
        if (floor == 1 && position == 1) {
            return "REST";
        }
        if (floor == 3 && position == 0) {
            return "SHOP";
        }
        if (floor == TOTAL_FLOORS - 1) {
            return "BOSS";
        }
        int roll = random.nextInt(100);
        if (roll < 36) return "BATTLE";
        if (roll < 48) return "EVENT";
        if (roll < 60) return "REST";
        if (roll < 72) return "SHOP";
        if (roll < 86) return "TREASURE";
        return "ELITE";
    }

    private String labelFor(String type, String contentId) {
        return switch (type) {
            case "BATTLE" -> "普通战斗";
            case "ELITE" -> "精英战斗";
            case "EVENT" -> "随机事件";
            case "REST" -> "休息闭关";
            case "SHOP" -> "坊市商店";
            case "TREASURE" -> "秘境宝藏";
            case "BOSS" -> "渡劫 Boss";
            default -> contentId;
        };
    }
}
