package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.GameDtos.CombatActionView;
import com.xiuxian.roguelike.api.GameDtos.CombatView;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunCombatEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.repository.RunCombatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CombatService {

    private static final int STRIKE_COST = 3;
    private static final int TECHNIQUE_COST = 8;

    private final RunCombatRepository combatRepository;
    private final BuildService buildService;

    public CombatService(RunCombatRepository combatRepository, BuildService buildService) {
        this.combatRepository = combatRepository;
        this.buildService = buildService;
    }

    @Transactional
    public RunCombatEntity start(GameRunEntity run, RunMapNodeEntity node) {
        BattleCatalog.EnemyEncounter encounter = BattleCatalog.pick(run, node);
        RunCombatEntity combat = new RunCombatEntity(run.getId(), node.getId(),
                encounter.definition().id(), encounter.definition().name(), encounter.definition().type(),
                encounter.definition().description(), encounter.maxHealth(), encounter.intent(), encounter.intentValue());
        return combatRepository.save(combat);
    }

    public RunCombatEntity active(String runId) {
        return combatRepository.findByRunIdAndStatus(runId, "ACTIVE").orElse(null);
    }

    @Transactional
    public CombatResult act(GameRunEntity run, RunCombatEntity combat, String action) {
        if (combat == null || !"ACTIVE".equals(combat.getStatus())) {
            throw new IllegalStateException("当前没有进行中的战斗。");
        }
        String normalized = action == null ? "" : action.trim().toUpperCase();
        BuildService.CombatModifier modifier = buildService.combatModifier(run.getId());
        List<String> logs = new ArrayList<>();

        switch (normalized) {
            case "STRIKE" -> {
                spendSpirit(run, STRIKE_COST);
                int damage = 11 + modifier.damage();
                int actual = dealDamage(combat, damage);
                logs.add("你挥出一式基础剑诀，造成 " + actual + " 点伤害。");
            }
            case "GUARD" -> {
                int block = 9 + modifier.block();
                combat.setPlayerBlock(combat.getPlayerBlock() + block);
                logs.add("你收束气机，获得 " + block + " 点护盾。");
            }
            case "MEDITATE" -> {
                int gain = 8 + modifier.spiritGain();
                run.applyBuildReward(2, gain, 0, 0);
                combat.setPlayerPoison(Math.max(0, combat.getPlayerPoison() - 2));
                logs.add("你调息一周天，灵力 +" + gain + "，气血 +2，并压低了中毒。");
            }
            case "TECHNIQUE" -> {
                spendSpirit(run, TECHNIQUE_COST);
                int damage = 18 + modifier.damage() + 4;
                int actual = dealDamage(combat, damage);
                int poison = 1 + modifier.poison();
                combat.setEnemyPoison(combat.getEnemyPoison() + poison);
                logs.add("你施展构筑战技，造成 " + actual + " 点伤害，并让敌人陷入 " + poison + " 层中毒。");
            }
            case "PURIFY" -> {
                spendSpirit(run, 2);
                int removed = combat.getPlayerPoison();
                combat.setPlayerPoison(0);
                int block = 4 + modifier.block() / 2;
                combat.setPlayerBlock(combat.getPlayerBlock() + block);
                logs.add("你以灵力净化经脉，清除 " + removed + " 层中毒并获得 " + block + " 点护盾。");
            }
            default -> throw new IllegalArgumentException("未知的战斗行动：" + action);
        }

        logs.forEach(combat::appendLog);
        if (combat.getHealth() <= 0) {
            combat.win();
            combat.appendLog("敌人倒下，战斗胜利。");
            combatRepository.save(combat);
            return new CombatResult(true, false, logs);
        }

        executeEnemyIntent(run, combat, logs);
        if (combat.getHealth() <= 0) {
            combat.win();
            combat.appendLog("敌人在中毒反噬中倒下，战斗胜利。");
            combatRepository.save(combat);
            return new CombatResult(true, false, logs);
        }
        if (run.getHealth() <= 0 || run.getLifespan() <= 0) {
            combat.lose();
            combat.appendLog("你的气血无法承受下一轮反噬。");
            combatRepository.save(combat);
            return new CombatResult(false, true, logs);
        }

        combat.nextTurn();
        BattleCatalog.EnemyDefinition enemy = BattleCatalog.find(combat.getEnemyId());
        BattleCatalog.Intent next = BattleCatalog.nextIntent(enemy, combat.getTurn(),
                run.getSeed() ^ combat.getNodeId().hashCode(), combat.getEnemyPower());
        combat.setIntent(next.kind(), next.value());
        combatRepository.save(combat);
        return new CombatResult(false, false, logs);
    }

    public CombatView toView(RunCombatEntity combat) {
        if (combat == null) return null;
        List<String> log = combat.getCombatLog() == null || combat.getCombatLog().isBlank()
                ? List.of() : List.of(combat.getCombatLog().split("\\n"));
        int from = Math.max(0, log.size() - 5);
        return new CombatView(
                combat.getId(), combat.getEnemyId(), combat.getEnemyName(), combat.getEnemyType(),
                combat.getEnemyDescription(), combat.getHealth(), combat.getMaxHealth(),
                combat.getEnemyBlock(), combat.getEnemyPoison(), combat.getPlayerBlock(), combat.getPlayerPoison(), combat.getTurn(),
                combat.getIntent(), combat.getIntentValue(), intentText(combat.getIntent(), combat.getIntentValue()),
                combat.getStatus(), actions(), log.subList(from, log.size())
        );
    }

    private List<CombatActionView> actions() {
        return List.of(
                new CombatActionView("STRIKE", "普通攻击", STRIKE_COST, "消耗 3 灵力，造成基础伤害"),
                new CombatActionView("GUARD", "守势", 0, "获得护盾，抵挡敌人本回合攻击"),
                new CombatActionView("MEDITATE", "调息", 0, "恢复灵力与少量气血，降低中毒"),
                new CombatActionView("TECHNIQUE", "构筑战技", TECHNIQUE_COST, "消耗 8 灵力，造成高额伤害并施加破绽"),
                new CombatActionView("PURIFY", "净脉", 2, "清除中毒并获得少量护盾")
        );
    }

    private void executeEnemyIntent(GameRunEntity run, RunCombatEntity combat, List<String> logs) {
        switch (combat.getIntent()) {
            case "ATTACK" -> {
                int incoming = combat.getIntentValue();
                int blocked = Math.min(combat.getPlayerBlock(), incoming);
                int damage = incoming - blocked;
                combat.setPlayerBlock(0);
                combat.setEnemyPower(0);
                run.applyBuildReward(-damage, 0, 0, 0);
                logs.add(combat.getEnemyName() + "发动攻击，造成 " + damage + " 点伤害（护盾抵挡 " + blocked + "）。");
            }
            case "DEFEND" -> {
                combat.setEnemyBlock(combat.getEnemyBlock() + combat.getIntentValue());
                logs.add(combat.getEnemyName() + "展开防御，获得 " + combat.getIntentValue() + " 点护盾。");
            }
            case "POISON" -> {
                combat.setPlayerPoison(combat.getPlayerPoison() + combat.getIntentValue());
                logs.add(combat.getEnemyName() + "释放阴毒，你获得 " + combat.getIntentValue() + " 层中毒。");
            }
            case "CHARGE" -> {
                combat.setEnemyPower(combat.getEnemyPower() + combat.getIntentValue());
                logs.add(combat.getEnemyName() + "蓄力完成，下一次攻击会更凶猛。");
            }
            default -> throw new IllegalStateException("未知敌人意图：" + combat.getIntent());
        }

        if (combat.getPlayerPoison() > 0) {
            int poisonDamage = combat.getPlayerPoison();
            run.applyBuildReward(-poisonDamage, 0, 0, 0);
            combat.setPlayerPoison(combat.getPlayerPoison() - 1);
            logs.add("中毒反噬，你损失 " + poisonDamage + " 点气血。");
        }
        if (combat.getEnemyPoison() > 0) {
            int poisonDamage = combat.getEnemyPoison();
            int actual = dealDamage(combat, poisonDamage);
            combat.setEnemyPoison(combat.getEnemyPoison() - 1);
            logs.add("敌方中毒反噬，损失 " + actual + " 点气血。");
        }
        logs.forEach(combat::appendLog);
    }

    private int dealDamage(RunCombatEntity combat, int damage) {
        int blocked = Math.min(combat.getEnemyBlock(), damage);
        combat.setEnemyBlock(combat.getEnemyBlock() - blocked);
        int actual = damage - blocked;
        combat.setHealth(combat.getHealth() - actual);
        return actual;
    }

    private void spendSpirit(GameRunEntity run, int amount) {
        if (run.getSpirit() < amount) {
            throw new IllegalStateException("灵力不足，需要 " + amount + " 点灵力。");
        }
        run.applyBuildReward(0, -amount, 0, 0);
    }

    private String intentText(String intent, int value) {
        return switch (intent) {
            case "ATTACK" -> "攻击 · 预计造成 " + value + " 点伤害";
            case "DEFEND" -> "防御 · 获得 " + value + " 点护盾";
            case "POISON" -> "施毒 · 施加 " + value + " 层中毒";
            case "CHARGE" -> "蓄力 · 攻击强度增加 " + value;
            default -> intent;
        };
    }

    public record CombatResult(boolean won, boolean lost, List<String> logs) {
    }
}
