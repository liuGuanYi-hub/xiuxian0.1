package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.ProgressDtos.AchievementView;
import com.xiuxian.roguelike.domain.AchievementRecordEntity;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunSettlementEntity;
import com.xiuxian.roguelike.domain.UserAccountEntity;
import com.xiuxian.roguelike.repository.AchievementRecordRepository;
import com.xiuxian.roguelike.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AchievementService {

    private static final Map<String, AchievementDefinition> CATALOG = catalog();

    private final AchievementRecordRepository achievementRecordRepository;
    private final UserAccountRepository userAccountRepository;

    public AchievementService(AchievementRecordRepository achievementRecordRepository,
                              UserAccountRepository userAccountRepository) {
        this.achievementRecordRepository = achievementRecordRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void awardFirstRun(String userId, String runId) {
        award(userId, "first_step", runId);
    }

    @Transactional
    public void evaluateSettlement(GameRunEntity run, RunSettlementEntity settlement) {
        String userId = run.getUserId();
        if (userId == null || userId.isBlank()) return;

        award(userId, "first_settlement", run.getId());
        if ("DEAD".equals(settlement.getStatus())) {
            award(userId, "fallen_once", run.getId());
        }
        if ("ASCENDED".equals(settlement.getStatus())) {
            award(userId, "ascension", run.getId());
        }
        if (settlement.getFloorReached() >= 5) {
            award(userId, "deep_tribulation", run.getId());
        }
        if (settlement.getEliteCount() >= 3) {
            award(userId, "elite_hunter", run.getId());
        }
        if (settlement.getActiveCards() >= 10) {
            award(userId, "build_master", run.getId());
        }

        UserAccountEntity account = userAccountRepository.findById(userId).orElse(null);
        if (account != null && account.getTotalCausalityEarned() >= 30) {
            award(userId, "causality_collector", run.getId());
        }
    }

    public List<AchievementView> views(String userId) {
        Map<String, AchievementRecordEntity> owned = achievementRecordRepository
                .findByUserIdOrderByAwardedAtAsc(userId).stream()
                .collect(java.util.stream.Collectors.toMap(AchievementRecordEntity::getAchievementId,
                        record -> record, (left, right) -> left));
        return CATALOG.values().stream().map(definition -> {
            AchievementRecordEntity record = owned.get(definition.id());
            return new AchievementView(definition.id(), definition.name(), definition.description(),
                    definition.conditionText(), record != null,
                    record == null ? null : record.getAwardedAt().toString());
        }).toList();
    }

    private void award(String userId, String achievementId, String runId) {
        if (userId == null || userId.isBlank() || !CATALOG.containsKey(achievementId)) return;
        if (achievementRecordRepository.findByUserIdAndAchievementId(userId, achievementId).isEmpty()) {
            achievementRecordRepository.save(new AchievementRecordEntity(userId, achievementId, runId));
        }
    }

    private static Map<String, AchievementDefinition> catalog() {
        Map<String, AchievementDefinition> definitions = new LinkedHashMap<>();
        definitions.put("first_step", new AchievementDefinition("first_step", "踏出第一步",
                "第一次创建修行存档，因果从此开始流动。", "开始一局新的修行"));
        definitions.put("first_settlement", new AchievementDefinition("first_settlement", "因果初结",
                "完成一次死亡或飞升结算，留下第一份轮回记录。", "完成一次结算"));
        definitions.put("fallen_once", new AchievementDefinition("fallen_once", "劫后余生",
                "即使道途断绝，结算后的记忆也会成为下一次修行的火种。", "经历一次死亡结算"));
        definitions.put("ascension", new AchievementDefinition("ascension", "白日飞升",
                "越过天关尽头，让一局修行以飞升收束。", "完成一次飞升结算"));
        definitions.put("deep_tribulation", new AchievementDefinition("deep_tribulation", "深入天关",
                "在层层劫难中走得足够深，触及第五层及以后。", "单局抵达第 5 层"));
        definitions.put("elite_hunter", new AchievementDefinition("elite_hunter", "猎尽强敌",
                "精英守关者的战利品，证明你的构筑经得起硬仗。", "单局击破 3 个精英节点"));
        definitions.put("build_master", new AchievementDefinition("build_master", "百炼成器",
                "收集足够多的功法、法宝与符箓，构筑开始自成一派。", "单局结算时拥有 10 张有效卡"));
        definitions.put("causality_collector", new AchievementDefinition("causality_collector", "因果富足",
                "多次轮回积攒的因果，终于足以撬动更长远的命数。", "账号累计获得 30 点因果"));
        return Collections.unmodifiableMap(definitions);
    }

    private record AchievementDefinition(String id, String name, String description, String conditionText) {
    }
}
