package com.xiuxian.roguelike.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * 当前阶段的事件内容目录。
 * 后续接入管理后台后，可以把这些定义迁移到 event_config 表。
 */
public final class EventCatalog {

    private static final Map<String, EventDefinition> EVENTS = Map.ofEntries(
            Map.entry("awaiting_node", event(
                    "awaiting_node", "等待选择", "前方的道路正在因你的选择展开。请先从路线图中选择一个相邻节点。"
            )),
            Map.entry("river", event(
                    "river", "黑水渡口", "暮色中的黑水河拦住去路。渡船老人不问灵石，只问你是否相信因果。",
                    choice("交出三枚灵石，请老人渡河", "稳妥，但会失去一点修行资源", 0, 5, 0, 1, "herb"),
                    choice("踏浪而行，以灵力强渡", "消耗灵力，可能更快抵达彼岸", -5, -8, 0, 0, "sect"),
                    choice("留下来替老人修补渡船", "耗费时间，却可能结下一段善缘", 0, 0, -3, 3, "cave")
            )),
            Map.entry("herb", event(
                    "herb", "百草谷口", "山谷里药香弥漫，几株灵草在月光下泛着微光。谷中弟子正为一味主药争执不休。",
                    choice("帮忙辨认灵草", "需要见识，也可能得到丹修的谢礼", 2, 2, -1, 2, "beast"),
                    choice("趁乱采走一株灵药", "眼下收益更高，但容易留下贪念", 8, 0, 0, -2, "library"),
                    choice("询问谷中长老求取指点", "耗费时间，却能让根基更加稳固", 5, 3, -2, 1, "market")
            )),
            Map.entry("sect", event(
                    "sect", "山门问心", "一座无名宗门挡在山路尽头。守门石碑映出你的来路，也映出你不愿面对的念头。",
                    choice("接受问心石考验", "直面心魔，可能获得宗门认可", -5, 5, -1, 4, "duel"),
                    choice("以剑意叩门", "锋芒会换来注意，也会换来挑战", -8, 8, 0, 1, "library"),
                    choice("绕过山门继续赶路", "不惹因果，但会错过部分传承", 0, 0, -2, -1, "market")
            )),
            Map.entry("cave", event(
                    "cave", "无名石窟", "石壁上的古篆在呼吸，洞底似乎藏着一件与自身有缘的东西。",
                    choice("点燃灵火，仔细搜寻", "需要灵力，但更容易找到真正的机缘", 0, -5, 0, 2, "seal"),
                    choice("直接取走洞口的玉简", "省下力气，但玉简上的气息有些不祥", -8, 2, 0, -2, "venom"),
                    choice("退后观察石壁上的阵纹", "谨慎会错过一些东西，也可能避开杀机", 0, 0, 1, 1, "vein")
            )),
            Map.entry("beast", event(
                    "beast", "赤岭兽潮", "山岭深处传来连续的低吼，兽潮正沿着山谷向你所在的方向移动。",
                    choice("借地势设阵抵挡", "消耗灵力换取稳妥的战果", -8, -2, 0, 3, "moon"),
                    choice("从兽潮缝隙中穿过", "速度很快，但肉身会承受冲击", -15, 0, 0, 1, "venom"),
                    choice("寻找兽王谈判", "以因果换取和平，结果无法预料", 0, 2, -2, 5, "library")
            )),
            Map.entry("duel", event(
                    "duel", "剑台旧约", "一个背负断剑的修士认出了你身上的旧日信物，邀你在雨幕中完成一场迟到的决斗。",
                    choice("正面接下这一剑", "以伤换取对方的认可", -18, 4, 0, 3, "moon"),
                    choice("以身法拖到对方力竭", "不够堂皇，却是活下来的办法", -6, -3, 0, 0, "venom"),
                    choice("放下兵刃，询问旧约真相", "放下执念，或许能看见更远的路", 3, 5, -2, 6, "library")
            )),
            Map.entry("seal", event(
                    "seal", "封印之下", "石窟最深处传来心跳声，九道符箓正在慢慢失去光泽。",
                    choice("加固封印", "消耗灵力，却能避免一场灾祸", 0, -10, -1, 5, "moon"),
                    choice("揭开一道符箓", "力量会立刻涌入体内，但代价也会跟来", -12, 12, -2, -4, "venom"),
                    choice("记下符文后离开", "暂时不碰危险，把秘密留到以后", 0, 0, 0, 1, "vein")
            )),
            Map.entry("market", event(
                    "market", "青石坊市", "坊市灯火通明，摊主们将真假难辨的机缘摆在你面前。",
                    choice("买下一颗来历不明的丹药", "效果未知，但修仙路上从不缺赌徒", 10, 0, -2, -1, "moon"),
                    choice("替人解决一桩小麻烦", "不一定赚钱，却能积累名声", 5, 0, -1, 3, "library"),
                    choice("什么也不买，观察众人", "节省资源，等待更好的机会", 0, 3, 0, 0, "vein")
            )),
            Map.entry("library", event(
                    "library", "残经阁", "雨声敲打着残经阁的窗棂。一本没有名字的古籍在书架尽头为你翻开一页。",
                    choice("记下运转灵力的口诀", "功法的雏形正在心中生根", 0, 8, -1, 2, "moon"),
                    choice("寻找与自身出身有关的记载", "真相往往比力量更沉重", 2, 3, -1, 4, "ghost"),
                    choice("合上古籍，避免沾染因果", "谨慎保命，但会少一份机缘", 4, 0, 0, 0, "vein")
            )),
            Map.entry("venom", event(
                    "venom", "百毒潭边", "雾气从黑色水潭升起，你的经脉开始发麻。潭心的莲花却散发着清明气息。",
                    choice("服下解毒丹强行渡过", "保住气血，但会消耗珍贵丹药", -5, 0, -1, 1, "moon"),
                    choice("采下毒莲，以毒攻毒", "高风险的修行方式，可能带来奇效", -20, 10, -2, -2, "ghost"),
                    choice("退回安全地带", "不冒险，至少不会在这里结束", 0, 0, -1, 0, "vein")
            )),
            Map.entry("moon", event(
                    "moon", "月下荒林", "林中传来微弱的呼救声。你看见一只受伤的白狐，和它身后的追兵。",
                    choice("救下白狐", "结下妖族因果，未来或许另有回报", -5, -3, -2, 6, "camp"),
                    choice("绕路离开", "少惹麻烦，但心中似乎留下了一根刺", 0, 0, -1, -1, "ghost"),
                    choice("反过来设伏追兵", "风险很高，胜利后能得到战利品", -12, 5, 0, 2, "vein")
            )),
            Map.entry("ghost", event(
                    "ghost", "黄泉渡影", "你在荒废的城池里看见一队没有影子的行人。他们都在等一个不会再回来的人。",
                    choice("替亡魂送出遗物", "消耗寿元，却能洗去一段旧因果", 3, 0, -5, 7, "camp"),
                    choice("跟随他们进入城心", "阴气会侵蚀肉身，但秘密也藏在那里", -12, 6, -2, 2, "vein"),
                    choice("立刻离开鬼城", "不与亡者争路，稳妥地保存自己", 0, 0, -1, 0, "camp")
            )),
            Map.entry("vein", event(
                    "vein", "地脉回响", "脚下的山脉像巨兽一样翻身，地底灵脉将一缕本源之气送到你面前。",
                    choice("引灵入体", "灵力大涨，但经脉会承受负担", -10, 15, -1, 2, "inner"),
                    choice("封存本源", "不急着使用，为渡劫留下底牌", 5, 5, 0, 1, "thunder"),
                    choice("将本源还给地脉", "因果减少，天地会记住你的选择", 0, 0, 2, 8, "auction")
            )),
            Map.entry("camp", event(
                    "camp", "雨夜篝火", "你在山腰点起篝火，同行者谈起各自的来处。火光之外，天劫的云层开始聚拢。",
                    choice("与同行者交换修行心得", "互相成就，灵力和因果都得到补益", 4, 6, -2, 3, "inner"),
                    choice("独自闭关一夜", "安静地整理收获，恢复部分气血", 12, 0, -3, 0, "thunder"),
                    choice("趁夜赶路", "提前靠近天关，却会损耗寿元", -5, 2, -5, 1, "auction")
            )),
            Map.entry("inner", event(
                    "inner", "心魔初醒", "你在识海中看见了另一个自己。它知道你所有没有说出口的欲望。",
                    choice("承认自己的恐惧", "心魔不会消失，但它不再能随意操控你", -5, 4, -1, 8, "thunder"),
                    choice("以杀意镇压心魔", "短时间内力量暴涨，心神却留下裂痕", -18, 12, -2, -4, "auction"),
                    choice("暂时封闭识海", "保守地拖延问题，换取一点喘息时间", 4, -4, -3, 1, "thunder")
            )),
            Map.entry("thunder", event(
                    "thunder", "九霄雷池", "第一道雷光落在远山，空气中满是金铁烧灼的味道。你必须选择渡劫的立足之处。",
                    choice("站上最高的山巅", "直面雷劫，成功后根基最为扎实", -18, -8, -3, 4, "auction"),
                    choice("借雷淬炼肉身", "气血会被反复撕裂，但体魄可能脱胎换骨", -25, -2, -2, 5, "auction"),
                    choice("寻找雷云间隙", "避开最猛烈的雷光，但会失去部分机缘", -8, 0, -1, 0, "auction")
            )),
            Map.entry("auction", event(
                    "auction", "天关黑市", "各方修士都在为渡劫准备最后的筹码。有人出售护心镜，也有人出售别人的命。",
                    choice("买下护心镜", "灵石换取安全，剩下的路会轻松一些", 10, -5, -1, 0, "trial_prep"),
                    choice("把一件因果深重的物品卖掉", "短期获利，代价由未来承担", 5, 0, -2, -5, "trial_prep"),
                    choice("什么也不买，保留全部底牌", "不依赖外物，只相信自己的选择", 0, 5, 0, 3, "trial_prep")
            )),
            Map.entry("starfall", event(
                    "starfall", "星陨荒原", "夜空突然裂开一道缝隙，一颗燃烧的星核坠落在荒原中央。附近的修士都说，那是天道遗落的眼睛。",
                    choice("靠近星核观察", "灵力会被星火灼烧，但可能看见未来的一角", -12, 12, -1, 5, "ancient_city"),
                    choice("以阵法封存星火", "把危险机缘变成可以携带的力量", -5, 5, 0, 3, "duel"),
                    choice("远离坠星之地", "不被天外因果牵连，也不会错过眼前的安全", 3, 0, 0, 0, "seal")
            )),
            Map.entry("ancient_city", event(
                    "ancient_city", "无名古城", "城门上没有文字，城中的石像却都朝着同一个方向跪拜。你的影子在这里比你先走了一步。",
                    choice("进入城主府", "古城最深的秘密，往往也藏着最深的危险", -14, 10, -2, 7, "dragon_vein"),
                    choice("寻找城中幸存的灯火", "也许还有人在等一个迟到很久的答案", 4, 3, -2, 5, "ghost"),
                    choice("拆下城门上的镇魂钉", "破坏旧秩序可以获得力量，也会放出东西", -10, 15, -3, -3, "venom")
            )),
            Map.entry("dragon_vein", event(
                    "dragon_vein", "龙脉裂隙", "大地深处传来龙吟，裂隙里涌出的不是灵石，而是一缕带着古老意志的金色血气。",
                    choice("引龙血淬体", "体魄会更强，但要承受龙意反噬", -22, 8, -1, 4, "sword_tomb"),
                    choice("聆听龙脉的记忆", "知道得越多，越难把自己当作普通修士", 0, 14, -2, 8, "vein"),
                    choice("将裂隙重新封闭", "放弃力量，换取一份天地认可", 8, 0, 1, 10, "inner")
            )),
            Map.entry("sword_tomb", event(
                    "sword_tomb", "万剑冢门", "无数残剑插在黑色山谷里，风吹过时像有万名剑修同时叹息。最深处的一柄剑正在等你。",
                    choice("拔出无名古剑", "剑会替你斩开一条路，也会要求你付出代价", -20, 12, -2, 4, "heavenly_omen"),
                    choice("向万剑行礼后离开", "不取不求，反而可能得到剑冢的认可", 5, 5, 0, 7, "thunder"),
                    choice("以自身剑意回应山谷", "让残剑记住你的名字", -8, 10, -1, 5, "auction")
            )),
            Map.entry("heavenly_omen", event(
                    "heavenly_omen", "天机显圣", "云层中浮出一只巨大的眼睛。它没有开口，却把你所有未完成的因果照得一清二楚。",
                    choice("请求天机指路", "提前看见一条未来，但会失去一部分自由", 0, 12, -3, 6, "trial_prep"),
                    choice("遮住天机之眼", "不接受安排，代价是承受天地的注视", -15, 8, -1, -2, "trial_prep"),
                    choice("向天机献上一段记忆", "换取一次改变命数的机会", 8, 4, -5, 9, "trial_prep")
            )),
            Map.entry("trial_prep", event(
                    "trial_prep", "筑基天关前", "天门就在云海尽头。最后一夜，你可以整理自己的道心，也可以把一切押在下一步。",
                    choice("稳固根基，准备渡劫", "减少风险，保留较多寿元", 8, -4, -2, 2, "trial"),
                    choice("燃烧寿元换取灵力", "突破机会更大，但生命正在倒计时", -5, 15, -8, 1, "trial"),
                    choice("以因果之力观测天门", "看见更多可能，也背负更多天地注视", -8, 5, -3, 8, "trial")
            )),
            Map.entry("trial", event(
                    "trial", "筑基雷劫", "雷海压下，过往十余次选择同时回响。你已经没有办法把责任交给任何人。",
                    choice("以积累的灵力强行冲关", "灵力越多，成功的可能越高", -18, -14, -4, 2, "tribulation"),
                    choice("以肉身硬抗九道天雷", "气血越厚，越可能活着走出雷池", -28, -4, -3, 3, "tribulation"),
                    choice("以因果之力换取突破", "善恶终有代价，突破也不例外", -20, 0, -8, 10, "tribulation")
            )),
            Map.entry("tribulation", event(
                    "tribulation", "天门回响", "最后一道雷光没有落下。天门之外传来一个声音，问你究竟为何修仙。",
                    choice("为了守住重要的人", "执念让道心变得坚定，也让因果变得沉重", 5, 0, -2, 8, "gate"),
                    choice("为了看见天地尽头", "求知没有尽头，修行也没有尽头", 0, 8, -2, 3, "gate"),
                    choice("为了证明自己不会认输", "锋芒可以破局，却也最容易伤到自己", -8, 5, -1, -1, "gate")
            )),
            Map.entry("gate", event(
                    "gate", "天门之外", "你回首人间，曾经做出的每个选择都化成了脚下的路。",
                    choice("踏入天门", "结束本轮旅程，带着经历飞升", 0, 0, 0, 5, "finish"),
                    choice("回望人间", "放弃飞升，换取一次重来的机会", 10, 0, 0, -3, "finish"),
                    choice("斩断因果", "不问天地，不问来路", -30, 0, 0, 10, "finish")
            )),
            Map.entry("finish", event(
                    "finish", "旅程终点", "这一局的故事已经写完。真正的修仙路，还在下一次选择之后。"
            ))
    );

    private static final Map<String, List<String>> NEXT_EVENT_POOLS = Map.ofEntries(
            Map.entry("river", List.of("herb", "sect", "cave")),
            Map.entry("herb", List.of("beast", "duel", "seal")),
            Map.entry("sect", List.of("beast", "duel", "seal")),
            Map.entry("cave", List.of("beast", "duel", "seal")),
            Map.entry("beast", List.of("moon", "ghost", "venom")),
            Map.entry("duel", List.of("moon", "ghost", "venom")),
            Map.entry("seal", List.of("moon", "ghost", "venom")),
            Map.entry("moon", List.of("camp", "inner", "vein")),
            Map.entry("ghost", List.of("camp", "inner", "vein")),
            Map.entry("venom", List.of("camp", "inner", "vein")),
            Map.entry("camp", List.of("thunder", "auction")),
            Map.entry("inner", List.of("thunder", "auction")),
            Map.entry("vein", List.of("thunder", "auction")),
            Map.entry("thunder", List.of("trial_prep")),
            Map.entry("auction", List.of("trial_prep")),
            Map.entry("trial_prep", List.of("trial")),
            Map.entry("trial", List.of("tribulation")),
            Map.entry("tribulation", List.of("gate")),
            Map.entry("gate", List.of("finish"))
    );

    private static final Map<String, List<WeightedContent>> NODE_CONTENT_POOLS = Map.of(
            "BATTLE", List.of(
                    weighted("beast", 60), weighted("duel", 25), weighted("seal", 12), weighted("ancient_city", 3)
            ),
            "ELITE", List.of(
                    weighted("sword_tomb", 35), weighted("dragon_vein", 25), weighted("trial", 25), weighted("heavenly_omen", 15)
            ),
            "EVENT", List.of(
                    weighted("moon", 30), weighted("ghost", 25), weighted("venom", 20), weighted("starfall", 15), weighted("ancient_city", 10)
            ),
            "REST", List.of(
                    weighted("camp", 70), weighted("inner", 20), weighted("trial_prep", 10)
            ),
            "SHOP", List.of(
                    weighted("market", 70), weighted("auction", 20), weighted("cave", 10)
            ),
            "TREASURE", List.of(
                    weighted("cave", 45), weighted("vein", 25), weighted("dragon_vein", 20), weighted("sword_tomb", 10)
            ),
            "BOSS", List.of(
                    weighted("trial", 70), weighted("tribulation", 20), weighted("gate", 10)
            )
    );

    private static final Map<String, EventMeta> EVENT_META = Map.ofEntries(
            Map.entry("starfall", new EventMeta("稀有", false)),
            Map.entry("ancient_city", new EventMeta("稀有", false)),
            Map.entry("dragon_vein", new EventMeta("传说", false)),
            Map.entry("sword_tomb", new EventMeta("稀有", false)),
            Map.entry("heavenly_omen", new EventMeta("传说", false)),
            Map.entry("market", new EventMeta("普通", true)),
            Map.entry("camp", new EventMeta("普通", true)),
            Map.entry("auction", new EventMeta("稀有", true)),
            Map.entry("trial", new EventMeta("Boss", false)),
            Map.entry("tribulation", new EventMeta("Boss", false)),
            Map.entry("gate", new EventMeta("终局", false))
    );

    private static final Map<String, EndingDefinition> ENDINGS = Map.of(
            "heavenly_ascension", endingConfig("heavenly_ascension", "天门飞升", "你以纯粹灵力贯通天门，成为云海之上的新客。"),
            "red_dust_sage", endingConfig("red_dust_sage", "红尘道君", "你没有斩断人间，反而把一路结下的因果炼成了自己的道。"),
            "demon_sovereign", endingConfig("demon_sovereign", "逆命魔尊", "你以反噬和执念为薪，踏出了一条无人敢走的逆命之路。"),
            "free_wanderer", endingConfig("free_wanderer", "逍遥散仙", "你放弃了天门尽头的答案，带着一身故事回到天地之间。"),
            "causality_breaker", endingConfig("causality_breaker", "断因绝果", "你斩断了所有既定因果，连天道也无法再为你写下结局。"),
            "fallen_path", endingConfig("fallen_path", "道途断绝", "这一世的寿元和肉身都走到了尽头，但因果簿仍在等待下一次落笔。")
    );

    private EventCatalog() {
    }

    public static EventDefinition get(String id) {
        EventDefinition event = EVENTS.get(id);
        if (event == null) {
            throw new IllegalStateException("事件配置不存在：" + id);
        }
        return event;
    }

    public static EventMeta meta(String id) {
        return EVENT_META.getOrDefault(id, new EventMeta("普通", false));
    }

    public static String pickNodeContent(String nodeType, SplittableRandom random) {
        List<WeightedContent> candidates = NODE_CONTENT_POOLS.getOrDefault(nodeType, NODE_CONTENT_POOLS.get("EVENT"));
        int totalWeight = candidates.stream().mapToInt(WeightedContent::weight).sum();
        int roll = random.nextInt(totalWeight);
        for (WeightedContent candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return candidate.eventId();
            }
        }
        return candidates.get(candidates.size() - 1).eventId();
    }

    public static EndingDefinition ending(String id) {
        EndingDefinition ending = ENDINGS.get(id);
        if (ending == null) {
            throw new IllegalStateException("结局配置不存在：" + id);
        }
        return ending;
    }

    private static EndingDefinition endingConfig(String id, String title, String description) {
        return new EndingDefinition(id, title, description);
    }

    private static WeightedContent weighted(String eventId, int weight) {
        return new WeightedContent(eventId, weight);
    }

    /**
     * 根据本局 seed、回合和已访问节点选择下一关，保证同一局可复盘，不同局顺序不同。
     */
    public static String chooseNextEvent(String currentEventId, String preferredNextEventId,
                                         int choiceIndex, long seed, int turn, Set<String> visitedEventIds) {
        List<String> candidates = NEXT_EVENT_POOLS.get(currentEventId);
        if (candidates == null || candidates.isEmpty()) {
            return preferredNextEventId;
        }

        List<String> available = new ArrayList<>();
        for (String candidate : candidates) {
            if (!visitedEventIds.contains(candidate)) {
                available.add(candidate);
            }
        }
        if (available.isEmpty()) {
            available.addAll(candidates);
        }

        long mixed = seed
                ^ (long) currentEventId.hashCode() * 31
                ^ (long) turn * 0x9E3779B97F4A7C15L
                ^ (long) (choiceIndex + 1) * 0xC2B2AE3D27D4EB4FL;
        int randomIndex = Math.floorMod((int) (mixed ^ (mixed >>> 32)), available.size());
        if (preferredNextEventId != null
                && available.contains(preferredNextEventId)
                && Math.floorMod((int) mixed, 4) == 0) {
            return preferredNextEventId;
        }
        return available.get(randomIndex);
    }

    private static EventDefinition event(String id, String title, String description, ChoiceDefinition... choices) {
        return new EventDefinition(id, title, description, List.of(choices));
    }

    private static ChoiceDefinition choice(String label, String hint, int healthDelta, int spiritDelta,
                                           int lifespanDelta, int karmaDelta, String nextEventId) {
        return new ChoiceDefinition(label, hint, healthDelta, spiritDelta, lifespanDelta, karmaDelta, nextEventId);
    }

    public record EventDefinition(String id, String title, String description, List<ChoiceDefinition> choices) {
    }

    public record EventMeta(String rarity, boolean repeatable) {
    }

    public record WeightedContent(String eventId, int weight) {
    }

    public record EndingDefinition(String id, String title, String description) {
    }

    public record ChoiceDefinition(String label, String hint, int healthDelta, int spiritDelta,
                                   int lifespanDelta, int karmaDelta, String nextEventId) {
    }
}
