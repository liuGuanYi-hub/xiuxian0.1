package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.ProgressDtos.AccountProgressView;
import com.xiuxian.roguelike.api.ProgressDtos.AchievementView;
import com.xiuxian.roguelike.api.ProgressDtos.SettlementHistoryView;
import com.xiuxian.roguelike.api.ProgressDtos.UnlockView;
import com.xiuxian.roguelike.auth.AuthContext;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.UnlockRecordEntity;
import com.xiuxian.roguelike.domain.UserAccountEntity;
import com.xiuxian.roguelike.repository.GameRunRepository;
import com.xiuxian.roguelike.repository.RunSettlementRepository;
import com.xiuxian.roguelike.repository.UnlockRecordRepository;
import com.xiuxian.roguelike.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermanentProgressService {

    private static final Map<String, UnlockDefinition> CATALOG = catalog();

    private final UserAccountRepository userAccountRepository;
    private final UnlockRecordRepository unlockRecordRepository;
    private final GameRunRepository gameRunRepository;
    private final RunSettlementRepository settlementRepository;
    private final AchievementService achievementService;
    private final AuthContext authContext;

    public PermanentProgressService(UserAccountRepository userAccountRepository,
                                    UnlockRecordRepository unlockRecordRepository,
                                    GameRunRepository gameRunRepository,
                                    RunSettlementRepository settlementRepository,
                                    AchievementService achievementService,
                                    AuthContext authContext) {
        this.userAccountRepository = userAccountRepository;
        this.unlockRecordRepository = unlockRecordRepository;
        this.gameRunRepository = gameRunRepository;
        this.settlementRepository = settlementRepository;
        this.achievementService = achievementService;
        this.authContext = authContext;
    }

    public AccountProgressView current() {
        return view(authContext.requireUserId());
    }

    @Transactional
    public AccountProgressView unlock(String unlockId) {
        String userId = authContext.requireUserId();
        UnlockDefinition definition = definition(unlockId);
        if (unlockRecordRepository.findByUserIdAndUnlockId(userId, definition.id()).isPresent()) {
            throw new IllegalStateException("该永久解锁已经拥有。" );
        }
        UserAccountEntity account = account(userId);
        account.spendCausality(definition.cost());
        userAccountRepository.save(account);
        unlockRecordRepository.save(new UnlockRecordEntity(userId, definition.id(), definition.cost()));
        return view(userId);
    }

    @Transactional
    public void awardForSettlement(GameRunEntity run, int amount) {
        if (run.getUserId() == null || run.getUserId().isBlank() || amount <= 0) return;
        UserAccountEntity account = account(run.getUserId());
        account.addCausality(amount);
        userAccountRepository.save(account);
    }

    public StartingBonuses startingBonuses(String userId) {
        if (userId == null || userId.isBlank()) return StartingBonuses.ZERO;
        int health = 0;
        int spirit = 0;
        int lifespan = 0;
        int karma = 0;
        int spiritStones = 0;
        for (UnlockRecordEntity record : unlockRecordRepository.findByUserIdOrderByUnlockedAtAsc(userId)) {
            UnlockDefinition definition = CATALOG.get(record.getUnlockId());
            if (definition == null) continue;
            health += definition.healthBonus();
            spirit += definition.spiritBonus();
            lifespan += definition.lifespanBonus();
            karma += definition.karmaBonus();
            spiritStones += definition.spiritStonesBonus();
        }
        return new StartingBonuses(health, spirit, lifespan, karma, spiritStones);
    }

    public AccountProgressView view(String userId) {
        UserAccountEntity account = account(userId);
        List<String> owned = unlockRecordRepository.findByUserIdOrderByUnlockedAtAsc(userId).stream()
                .map(UnlockRecordEntity::getUnlockId).toList();
        List<UnlockView> unlocks = CATALOG.values().stream()
                .map(definition -> new UnlockView(definition.id(), definition.name(), definition.description(),
                        definition.cost(), owned.contains(definition.id()), definition.effectText()))
                .toList();
        List<SettlementHistoryView> history = settlementRepository
                .findByUserIdOrderBySettledAtDesc(userId, PageRequest.of(0, 10)).stream()
                .map(row -> new SettlementHistoryView(row.getRunId(), row.getCharacterId(), row.getPlayerName(),
                        row.getStatus(), row.getEndingTitle(), row.getScore(), row.getCausalityEarned(),
                        row.getSettledAt().toString()))
                .toList();
        List<AchievementView> achievements = achievementService.views(userId);
        int highestFloor = settlementRepository.findTopByUserIdOrderByFloorReachedDesc(userId)
                .map(row -> row.getFloorReached()).orElse(0);
        int bestScore = settlementRepository.findTopByUserIdOrderByScoreDesc(userId)
                .map(row -> row.getScore()).orElse(0);
        long ascendedRuns = settlementRepository.countByUserIdAndStatus(userId, "ASCENDED");
        long deadRuns = settlementRepository.countByUserIdAndStatus(userId, "DEAD");
        int achievementCount = (int) achievements.stream().filter(AchievementView::unlocked).count();
        int totalAchievementRewards = achievements.stream().filter(AchievementView::unlocked)
                .mapToInt(AchievementView::rewardCausality).sum();
        return new AccountProgressView(account.getCausalityPoints(), account.getTotalCausalityEarned(),
                account.getTotalCausalitySpent(), gameRunRepository.countByUserId(userId),
                settlementRepository.countByUserId(userId), ascendedRuns, deadRuns, highestFloor, bestScore,
                achievementCount, totalAchievementRewards, unlocks, achievements, history);
    }

    private UserAccountEntity account(String userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("当前账号不存在。"));
    }

    private UnlockDefinition definition(String unlockId) {
        if (unlockId == null || !CATALOG.containsKey(unlockId.trim())) {
            throw new IllegalArgumentException("未知的永久解锁。" );
        }
        return CATALOG.get(unlockId.trim());
    }

    private static Map<String, UnlockDefinition> catalog() {
        Map<String, UnlockDefinition> definitions = new LinkedHashMap<>();
        definitions.put("first_breath", new UnlockDefinition("first_breath", "先天气血", "以往轮回留下的肉身记忆。", 5,
                "+8 初始气血", 8, 0, 0, 0, 0));
        definitions.put("spirit_spring", new UnlockDefinition("spirit_spring", "灵泉回响", "识海中常驻一眼灵泉。", 15,
                "+5 初始灵力", 0, 5, 0, 0, 0));
        definitions.put("long_life", new UnlockDefinition("long_life", "长生余烬", "历经轮回后留下的寿元余烬。", 20,
                "+5 初始寿元", 0, 0, 5, 0, 0));
        definitions.put("karma_lens", new UnlockDefinition("karma_lens", "因果观", "看见选择背后的第一缕因果。", 25,
                "+2 初始因果", 0, 0, 0, 2, 0));
        definitions.put("wealth_memory", new UnlockDefinition("wealth_memory", "旧日宝藏", "记得曾经藏下的一笔灵石。", 30,
                "+20 初始灵石", 0, 0, 0, 0, 20));
        definitions.put("sword_bone", new UnlockDefinition("sword_bone", "剑骨初鸣", "历经数次轮回后，剑意沉入骨中。", 30,
                "+6 初始气血、+2 初始灵力", 6, 2, 0, 0, 0));
        definitions.put("alchemy_ember", new UnlockDefinition("alchemy_ember", "丹火余温", "丹炉熄灭之后，仍有一缕火种守护神魂。", 32,
                "+4 初始灵力、+2 初始寿元", 0, 4, 2, 0, 0));
        definitions.put("flesh_heart", new UnlockDefinition("flesh_heart", "不灭战躯", "以百战淬体，肉身在新局醒来时更难被击溃。", 35,
                "+12 初始气血", 12, 0, 0, 0, 0));
        definitions.put("karma_tide", new UnlockDefinition("karma_tide", "因果潮汐", "观尽众生来去，开局便携带一缕可转化的因果。", 40,
                "+3 初始因果", 0, 0, 0, 3, 0));
        return java.util.Collections.unmodifiableMap(definitions);
    }

    public record StartingBonuses(int health, int spirit, int lifespan, int karma, int spiritStones) {
        public static final StartingBonuses ZERO = new StartingBonuses(0, 0, 0, 0, 0);
    }

    private record UnlockDefinition(String id, String name, String description, int cost, String effectText,
                                    int healthBonus, int spiritBonus, int lifespanBonus, int karmaBonus,
                                    int spiritStonesBonus) {
    }
}
