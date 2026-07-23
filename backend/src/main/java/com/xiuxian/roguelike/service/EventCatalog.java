package com.xiuxian.roguelike.service;

import java.util.List;
import java.util.Map;

/**
 * 当前阶段的事件内容目录。
 * 后续接入管理后台后，可以把这些定义迁移到 event_config 表。
 */
public final class EventCatalog {

    private static final Map<String, EventDefinition> EVENTS = Map.ofEntries(
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

    private EventCatalog() {
    }

    public static EventDefinition get(String id) {
        EventDefinition event = EVENTS.get(id);
        if (event == null) {
            throw new IllegalStateException("事件配置不存在：" + id);
        }
        return event;
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

    public record ChoiceDefinition(String label, String hint, int healthDelta, int spiritDelta,
                                   int lifespanDelta, int karmaDelta, String nextEventId) {
    }
}

