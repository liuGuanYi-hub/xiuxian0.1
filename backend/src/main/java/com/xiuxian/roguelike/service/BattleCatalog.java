package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;

import java.util.List;
import java.util.SplittableRandom;

public final class BattleCatalog {

    private static final List<EnemyDefinition> COMMON = List.of(
            new EnemyDefinition("azure_wolf", "青鳞狼", "妖兽", "会在你露出破绽时连续扑击。", 36, 8, 7, 2, 2),
            new EnemyDefinition("mountain_bandit", "断岳劫修", "劫修", "粗重的灵压藏在一柄看似普通的铁尺里。", 42, 7, 10, 0, 3),
            new EnemyDefinition("paper_doll", "噬魂纸人", "邪祟", "纸面上写着你的名字，阴影会比本体先动。", 34, 6, 5, 3, 2)
    );

    private static final List<EnemyDefinition> ELITE = List.of(
            new EnemyDefinition("black_armor", "黑甲镇关将", "精英", "它不急着进攻，只等护甲叠到足够厚。", 74, 11, 14, 0, 4),
            new EnemyDefinition("blood_lotus", "血莲化身", "精英", "血莲每一次盛开，都会把中毒化成更猛烈的反噬。", 68, 10, 8, 5, 3),
            new EnemyDefinition("sword_ghost", "无名剑鬼", "精英", "它的剑招没有前式，只有不断逼近的后果。", 62, 13, 6, 1, 5)
    );

    private static final EnemyDefinition BOSS = new EnemyDefinition(
            "heaven_gate_dragon", "天关雷龙", "渡劫 Boss", "雷云在它鳞片之间游走，下一道天雷随时会落下。", 150, 18, 18, 6, 7
    );

    private BattleCatalog() {
    }

    public static EnemyEncounter pick(GameRunEntity run, RunMapNodeEntity node) {
        List<EnemyDefinition> pool = switch (node.getType()) {
            case "ELITE" -> ELITE;
            case "BOSS" -> List.of(BOSS);
            default -> COMMON;
        };
        SplittableRandom random = new SplittableRandom(run.getSeed()
                ^ node.getId().hashCode()
                ^ (long) node.getFloor() * 0x9E3779B97F4A7C15L);
        EnemyDefinition definition = pool.get(random.nextInt(pool.size()));
        int maxHealth = definition.maxHealth() + node.getFloor() * ("BOSS".equals(node.getType()) ? 8 : 3);
        Intent intent = nextIntent(definition, 1, random.nextLong(), 0);
        return new EnemyEncounter(definition, maxHealth, intent.kind(), intent.value());
    }

    public static Intent nextIntent(EnemyDefinition enemy, int turn, long seed, int power) {
        if (turn % 4 == 0) {
            return new Intent("CHARGE", Math.max(2, enemy.charge()));
        }
        SplittableRandom random = new SplittableRandom(seed ^ (long) turn * 0xD1342543DE82EF95L);
        int roll = random.nextInt(100);
        if (roll < 52) return new Intent("ATTACK", enemy.attack() + Math.min(power, 8));
        if (roll < 76) return new Intent("DEFEND", enemy.block() + turn / 3);
        if (roll < 92) return new Intent("POISON", enemy.poison() + Math.min(3, turn / 4));
        return new Intent("CHARGE", Math.max(2, enemy.charge() + power));
    }

    public static EnemyDefinition find(String enemyId) {
        return List.of(BOSS).stream()
                .filter(enemy -> enemy.id().equals(enemyId))
                .findFirst()
                .orElseGet(() -> allEnemies().stream()
                        .filter(enemy -> enemy.id().equals(enemyId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("战斗敌人不存在：" + enemyId)));
    }

    private static List<EnemyDefinition> allEnemies() {
        return java.util.stream.Stream.of(COMMON, ELITE)
                .flatMap(List::stream)
                .toList();
    }

    public record EnemyDefinition(String id, String name, String type, String description,
                                  int maxHealth, int attack, int block, int poison, int charge) {
    }

    public record EnemyEncounter(EnemyDefinition definition, int maxHealth, String intent, int intentValue) {
    }

    public record Intent(String kind, int value) {
    }
}
