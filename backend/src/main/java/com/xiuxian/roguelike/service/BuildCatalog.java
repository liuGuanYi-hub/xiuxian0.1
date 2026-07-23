package com.xiuxian.roguelike.service;

import java.util.List;
import java.util.Map;

public final class BuildCatalog {

    private static final Map<String, CardDefinition> CARDS = Map.ofEntries(
            Map.entry("qi_guiding", card("qi_guiding", "功法", "引气诀", "普通", "最基础的引气法门，胜在稳定。", "战斗结算额外获得 1 点灵力。", 0, 2, 0, 0, 0, 1)),
            Map.entry("healing_talisman", card("healing_talisman", "符箓", "回春符", "普通", "温养经脉的常用符箓。", "获得时恢复 8 点气血。", 8, 0, 0, 0, 0, 0)),
            Map.entry("sword_manual", card("sword_manual", "功法", "裂风剑诀", "稀有", "剑气先行，适合主动寻找战斗的修士。", "战斗结算额外获得 4 点气血。", 0, 4, 0, 0, 4, 0)),
            Map.entry("taixu_manual", card("taixu_manual", "功法", "太虚纳灵诀", "稀有", "把周天灵气化作可反复调用的底蕴。", "获得时获得 8 点灵力；战斗额外获得 2 点灵力。", 0, 8, 0, 0, 0, 2)),
            Map.entry("body_manual", card("body_manual", "功法", "百炼锻体篇", "普通", "以肉身为炉，越是险境越能站稳。", "获得时获得 10 点气血；战斗额外获得 3 点气血。", 10, 0, 0, 0, 3, 0)),
            Map.entry("heavenly_sword", card("heavenly_sword", "法宝", "天阙剑胚", "传说", "尚未开锋的天外剑胚，正在等待自己的主人。", "战斗额外获得 8 点气血。", 0, 10, 0, 0, 8, 0)),
            Map.entry("phoenix_robe", card("phoenix_robe", "法宝", "涅槃羽衣", "传说", "每一次破损都在为下一次重生积蓄力量。", "获得时获得 15 点气血。", 15, 0, 0, 0, 0, 0)),
            Map.entry("demon_blood", card("demon_blood", "功法", "赤血逆命经", "传说", "把反噬炼成锋刃，强大而危险。", "获得时因果 -4；战斗额外获得 10 点气血。", 0, 0, 0, -4, 10, 0)),
            Map.entry("dragon_core", card("dragon_core", "法宝", "龙脉残核", "传说", "龙脉中留下的一枚残缺核心。", "获得时获得 12 点灵力；战斗额外获得 6 点灵力。", 0, 12, 0, 0, 0, 6)),
            Map.entry("karma_mirror", card("karma_mirror", "法宝", "照因镜", "稀有", "镜中映出的不是面容，而是每一个被忽略的选择。", "获得时因果 +6。", 0, 0, 0, 6, 0, 0)),
            Map.entry("void_talisman", card("void_talisman", "符箓", "虚空遁符", "稀有", "把一次无法躲开的灾厄推迟到未来。", "获得时获得 5 点寿元；战斗额外获得 4 点灵力。", 0, 0, 5, 0, 0, 4)),
            Map.entry("seal_talisman", card("seal_talisman", "符箓", "镇厄符", "普通", "压住身边一缕躁动的天机。", "获得时因果 +3。", 0, 0, 0, 3, 0, 0)),
            Map.entry("spirit_talisman", card("spirit_talisman", "符箓", "聚灵符", "稀有", "一张可以反复温养的聚灵符。", "获得时获得 6 点灵力。", 0, 6, 0, 0, 0, 0)),
            Map.entry("star_manual", card("star_manual", "功法", "摘星换命诀", "稀有", "借星火淬炼神识，也把一部分命数交给夜空。", "获得时灵力 +5、因果 +2；战斗额外获得 3 点灵力。", 0, 5, 0, 2, 0, 3)),
            Map.entry("ancient_robe", card("ancient_robe", "法宝", "古城守心袍", "稀有", "穿上它的人会听见旧城最后一次呼吸。", "获得时气血 +8、寿元 +3。", 8, 0, 3, 0, 0, 0)),
            Map.entry("nine_heavens", card("nine_heavens", "符箓", "九霄雷引符", "传说", "把天雷引到敌人身上，而不是自己的道心里。", "获得时灵力 +18；战斗额外获得 8 点灵力。", 0, 18, 0, 0, 0, 8)),
            Map.entry("thunder_talisman", card("thunder_talisman", "符箓", "掌心雷符", "稀有", "适合在战斗最胶着时撕开的符箓。", "战斗额外获得 2 点气血和 6 点灵力。", 0, 0, 0, 0, 2, 6))
    );

    private static final Map<String, List<WeightedCard>> POOLS = Map.of(
            "BATTLE", List.of(
                    weighted("sword_manual", 24), weighted("taixu_manual", 20), weighted("body_manual", 18),
                    weighted("healing_talisman", 12), weighted("thunder_talisman", 12), weighted("seal_talisman", 8),
                    weighted("spirit_talisman", 6)
            ),
            "ELITE", List.of(
                    weighted("heavenly_sword", 18), weighted("phoenix_robe", 16), weighted("demon_blood", 12),
                    weighted("dragon_core", 16), weighted("karma_mirror", 12), weighted("void_talisman", 10),
                    weighted("star_manual", 10), weighted("nine_heavens", 6)
            ),
            "TREASURE", List.of(
                    weighted("spirit_talisman", 20), weighted("star_manual", 18), weighted("ancient_robe", 18),
                    weighted("karma_mirror", 16), weighted("healing_talisman", 12), weighted("nine_heavens", 10),
                    weighted("void_talisman", 6)
            )
    );

    private BuildCatalog() {
    }

    public static CardDefinition get(String cardId) {
        CardDefinition card = CARDS.get(cardId);
        if (card == null) {
            throw new IllegalArgumentException("构筑卡牌不存在：" + cardId);
        }
        return card;
    }

    public static List<CardDefinition> starterCards() {
        return List.of(get("qi_guiding"), get("healing_talisman"));
    }

    public static List<WeightedCard> poolFor(String nodeType) {
        return POOLS.getOrDefault(nodeType, POOLS.get("BATTLE"));
    }

    private static CardDefinition card(String cardId, String category, String name, String rarity,
                                       String description, String effectText, int healthOnClaim,
                                       int spiritOnClaim, int lifespanOnClaim, int karmaOnClaim,
                                       int battleHealthBonus, int battleSpiritBonus) {
        return new CardDefinition(cardId, category, name, rarity, description, effectText,
                healthOnClaim, spiritOnClaim, lifespanOnClaim, karmaOnClaim,
                battleHealthBonus, battleSpiritBonus);
    }

    private static WeightedCard weighted(String cardId, int weight) {
        return new WeightedCard(cardId, weight);
    }

    public record WeightedCard(String cardId, int weight) {
    }

    public record CardDefinition(String cardId, String category, String name, String rarity,
                                 String description, String effectText, int healthOnClaim,
                                 int spiritOnClaim, int lifespanOnClaim, int karmaOnClaim,
                                 int battleHealthBonus, int battleSpiritBonus) {
    }
}
