package com.xiuxian.roguelike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.domain.RunMapNodeEntity;
import com.xiuxian.roguelike.repository.GameRunRepository;
import com.xiuxian.roguelike.repository.ItemConfigRepository;
import com.xiuxian.roguelike.repository.RunMapNodeRepository;
import com.xiuxian.roguelike.repository.SkillConfigRepository;
import com.xiuxian.roguelike.repository.TalismanConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SkillConfigRepository skillConfigRepository;

    @Autowired
    private ItemConfigRepository itemConfigRepository;

    @Autowired
    private TalismanConfigRepository talismanConfigRepository;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private RunMapNodeRepository runMapNodeRepository;

    @Test
    void startRunGeneratesConnectedSeedMap() throws Exception {
        JsonNode started = objectMapper.readTree(startRun());
        JsonNode nodes = started.get("map").get("nodes");

        assertEquals("awaiting_node", started.get("event").get("id").asText());
        assertEquals(10, started.get("map").get("totalFloors").asInt());
        assertEquals(28, nodes.size());
        assertEquals(3, countByStatus(nodes, "AVAILABLE"));
        assertEquals(1, countByType(nodes, "BOSS"));
        for (JsonNode node : nodes) {
            if (!"BOSS".equals(node.get("type").asText())) {
                assertTrue(node.get("nextNodeIds").size() > 0, "每个非 Boss 节点都应该有下一跳");
            }
        }
    }

    @Test
    void playerCanEnterChooseAndRestoreHistory() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();
        JsonNode entered = enterFirstAvailable(runId, startedRun);

        assertFalse(entered.get("currentNodeId").asText().isBlank());
        JsonNode chosen = entered.get("combat").isObject()
                ? finishCombat(runId, entered)
                : objectMapper.readTree(choose(runId, 0, "history-test-1"));
        assertEquals(runId, chosen.get("id").asText());
        assertEquals(1, chosen.get("turn").asInt());
        assertTrue(chosen.get("logs").size() >= 2);

        mockMvc.perform(get("/api/game/runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn").value(1))
                .andExpect(jsonPath("$.logs.length()").value(3));
    }

    @Test
    void battleChoiceCreatesThreeRewardsAndClaimAddsCard() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();
        JsonNode entered = enterFirstAvailable(runId, startedRun);
        assertTrue(entered.get("combat").isObject());
        assertEquals(1, entered.get("combat").get("turn").asInt());
        assertTrue(entered.get("combat").get("actions").size() >= 5);

        JsonNode restored = objectMapper.readTree(mockMvc.perform(get("/api/game/runs/{id}", runId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(entered.get("combat").get("id").asText(), restored.get("combat").get("id").asText());

        JsonNode afterGuard = combatAction(runId, "GUARD");
        assertEquals(2, afterGuard.get("combat").get("turn").asInt());
        JsonNode afterBattle = finishCombat(runId, afterGuard);
        assertEquals(1, afterBattle.get("turn").asInt());
        assertEquals(3, afterBattle.get("rewardOffers").size());
        assertEquals(2, afterBattle.get("build").size());
        assertEquals("REWARD", firstNode(afterBattle.get("map").get("nodes"), 0).get("status").asText());

        JsonNode claimed = claimFirstReward(runId, afterBattle);
        assertEquals(0, claimed.get("rewardOffers").size());
        assertEquals(3, claimed.get("build").size());
        assertTrue(countByStatus(claimed.get("map").get("nodes"), "AVAILABLE") > 0);
    }

    @Test
    void configSeedsSeventeenCardsAndStarterActivatesDanCultivatorSynergy() throws Exception {
        assertEquals(25, skillConfigRepository.count() + itemConfigRepository.count() + talismanConfigRepository.count());

        JsonNode started = objectMapper.readTree(startRun());
        assertEquals(2, started.get("buildStats").get("archetypeCounts").get("丹修").asInt());
        JsonNode danSynergy = findSynergy(started.get("buildStats").get("synergies"), "丹修");
        assertTrue(danSynergy.get("active").asBoolean());
        assertEquals(5, started.get("buildStats").get("battleSpiritBonus").asInt());
        assertEquals(2, started.get("buildStats").get("combatSpiritGain").asInt());
        assertTrue(started.get("build").get(0).has("archetype"));
    }

    @Test
    void shopCanBuyRefreshRemoveAndRestoreState() throws Exception {
        JsonNode current = objectMapper.readTree(startRun());
        String runId = current.get("id").asText();
        current = advanceToNodeType(runId, current, "SHOP");
        current = objectMapper.readTree(choose(runId, 0, UUID.randomUUID().toString()));

        assertEquals(3, current.get("shop").get("offers").size());
        JsonNode firstOffer = current.get("shop").get("offers").get(0);
        String shopNodeId = current.get("shop").get("nodeId").asText();
        assertEquals("普通", firstOffer.get("rarity").asText());
        assertEquals(20, firstOffer.get("price").asInt());
        int activeCardsBeforeBuy = current.get("buildStats").get("activeCards").asInt();

        current = buyShopOffer(runId, firstOffer.get("id").asText());
        assertEquals(40, current.get("spiritStones").asInt());
        assertEquals(activeCardsBeforeBuy + 1, current.get("buildStats").get("activeCards").asInt());

        current = refreshShop(runId);
        assertEquals(1, current.get("shop").get("refreshCount").asInt());
        assertEquals(15, current.get("shop").get("nextRefreshCost").asInt());
        assertEquals(30, current.get("spiritStones").asInt());

        String cardId = current.get("build").get(0).get("id").asText();
        current = removeShopCard(runId, cardId);
        assertEquals(0, current.get("spiritStones").asInt());
        assertEquals(activeCardsBeforeBuy, current.get("buildStats").get("activeCards").asInt());
        assertTrue(current.get("shop").get("removalUsed").asBoolean());

        JsonNode restored = objectMapper.readTree(mockMvc.perform(get("/api/game/runs/{id}", runId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(1, restored.get("shop").get("refreshCount").asInt());
        assertTrue(restored.get("shop").get("removalUsed").asBoolean());
        assertEquals(activeCardsBeforeBuy, restored.get("buildStats").get("activeCards").asInt());

        current = leaveShop(runId);
        assertEquals(activeCardsBeforeBuy, current.get("buildStats").get("activeCards").asInt());
        assertEquals("CLEARED", findNode(current.get("map").get("nodes"), shopNodeId).get("status").asText());
    }

    @Test
    void shopRefreshUsesTenThenFifteenSpiritStonesAndStopsAtTwo() throws Exception {
        JsonNode current = objectMapper.readTree(startRun());
        String runId = current.get("id").asText();
        current = advanceToNodeType(runId, current, "SHOP");
        current = objectMapper.readTree(choose(runId, 0, UUID.randomUUID().toString()));

        current = refreshShop(runId);
        assertEquals(50, current.get("spiritStones").asInt());
        current = refreshShop(runId);
        assertEquals(35, current.get("spiritStones").asInt());
        assertEquals(2, current.get("shop").get("refreshCount").asInt());
        assertEquals(0, current.get("shop").get("nextRefreshCost").asInt());

        mockMvc.perform(post("/api/game/runs/{id}/shops/refresh", runId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("本次坊市已经没有刷新次数了。"));
    }

    @Test
    void specialEventRemovalIsFreeAndLeavesHistoryBackedActiveCount() throws Exception {
        JsonNode started = objectMapper.readTree(startRun());
        String runId = started.get("id").asText();
        GameRunEntity run = gameRunRepository.findById(runId).orElseThrow();
        RunMapNodeEntity node = runMapNodeRepository.findByRunIdOrderByFloorAscPositionAsc(runId).get(0);
        run.setPendingRemovalNode(node.getId());
        node.markRemovalPending();
        gameRunRepository.save(run);
        runMapNodeRepository.save(node);

        String cardId = started.get("build").get(0).get("id").asText();
        JsonNode removed = objectMapper.readTree(mockMvc.perform(post("/api/game/runs/{id}/removals/{cardId}", runId, cardId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(60, removed.get("spiritStones").asInt());
        assertEquals(1, removed.get("buildStats").get("activeCards").asInt());
        assertTrue(removed.get("removal").isNull());
        assertEquals("CLEARED", findNode(removed.get("map").get("nodes"), node.getId()).get("status").asText());
    }

    @Test
    void restNodeCanUpgradeCardBeforeUnlockingRoute() throws Exception {
        JsonNode started = objectMapper.readTree(startRun());
        String runId = started.get("id").asText();
        JsonNode restNode = firstNodeByType(started.get("map").get("nodes"), "REST");
        JsonNode parent = firstNodePointingTo(started.get("map").get("nodes"), restNode.get("id").asText());
        assertTrue(parent != null, "固定的休息节点应该有上一跳");

        JsonNode current = enterNode(runId, parent.get("id").asText());
        current = current.get("combat").isObject()
                ? finishCombat(runId, current)
                : objectMapper.readTree(choose(runId, 0, "upgrade-prep-1"));
        if (current.get("rewardOffers").size() > 0) {
            current = claimFirstReward(runId, current);
        }
        assertEquals("AVAILABLE", findNode(current.get("map").get("nodes"), restNode.get("id").asText()).get("status").asText());

        current = enterNode(runId, restNode.get("id").asText());
        current = objectMapper.readTree(choose(runId, 0, "upgrade-rest-1"));
        assertTrue(current.get("build").size() >= 2, "starter 构筑至少应保留两张卡牌");
        assertEquals(current.get("build").size(), current.get("upgradeOptions").size());
        assertEquals("UPGRADE", findNode(current.get("map").get("nodes"), restNode.get("id").asText()).get("status").asText());

        JsonNode upgraded = upgradeFirstCard(runId, current);
        assertEquals(35, upgraded.get("spiritStones").asInt());
        assertEquals(1, upgraded.get("build").get(0).get("upgradeLevel").asInt());
        assertEquals(0, upgraded.get("upgradeOptions").size());
        assertEquals("CLEARED", findNode(upgraded.get("map").get("nodes"), restNode.get("id").asText()).get("status").asText());
    }

    @Test
    void duplicateRequestIdDoesNotAdvanceTheRunTwice() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();
        JsonNode eventNode = firstNodeByType(startedRun.get("map").get("nodes"), "EVENT");
        enterNode(runId, eventNode.get("id").asText());
        String body = "{\"choiceIndex\":0,\"requestId\":\"duplicate-test-1\"}";

        mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn").value(1));

        mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn").value(1))
                .andExpect(jsonPath("$.logs[2]").value("重复请求已忽略，本局状态没有重复结算。"));
    }

    @Test
    void invalidChoiceReturnsBadRequest() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();
        JsonNode eventNode = firstNodeByType(startedRun.get("map").get("nodes"), "EVENT");
        enterNode(runId, eventNode.get("id").asText());

        mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceIndex\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("无效的事件选项。"));
    }

    @Test
    void routeCanReachTheBossAfterMultipleDecisions() throws Exception {
        JsonNode current = objectMapper.readTree(startRun());
        String runId = current.get("id").asText();

        for (int decision = 0; decision < 32 && "RUNNING".equals(current.get("status").asText()); decision++) {
            if (current.get("rewardOffers").size() > 0) {
                current = claimFirstReward(runId, current);
                continue;
            }
            if (current.get("upgradeOptions").size() > 0) {
                current = skipUpgrade(runId);
                continue;
            }
            if (!current.get("shop").isNull()) {
                current = leaveShop(runId);
                continue;
            }
            if (!current.get("removal").isNull()) {
                current = skipSpecialRemoval(runId);
                continue;
            }
            if (!current.get("combat").isNull()) {
                current = finishCombat(runId, current);
                continue;
            }
            if (!current.get("currentNodeId").asText().isBlank()) {
                current = objectMapper.readTree(choose(runId, 2, UUID.randomUUID().toString()));
                continue;
            }
            JsonNode available = firstAvailable(current.get("map").get("nodes"));
            assertTrue(available != null, "运行中的路线应该始终存在可进入节点");
            current = enterFirstAvailable(runId, current);
        }

        assertTrue(current.get("turn").asInt() >= 9, "路线应该至少经过大部分楼层");
        assertTrue("ASCENDED".equals(current.get("status").asText())
                        || "DEAD".equals(current.get("status").asText()),
                "抵达 Boss 后应该进入结局或死亡状态");
    }

    private String startRun() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"顾长生\",\"origin\":\"散修\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.event.choices.length()").value(0))
                .andExpect(jsonPath("$.map.nodes.length()").value(28))
                .andExpect(jsonPath("$.spiritStones").value(60))
                .andExpect(jsonPath("$.build.length()").value(2))
                .andExpect(jsonPath("$.upgradeOptions.length()").value(0))
                .andExpect(jsonPath("$.rewardOffers.length()").value(0))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private JsonNode enterFirstAvailable(String runId, JsonNode current) throws Exception {
        JsonNode node = firstAvailable(current.get("map").get("nodes"));
        assertTrue(node != null, "应该找到可进入节点");
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/nodes/{nodeId}/enter", runId, node.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentNodeId").value(node.get("id").asText()))
                .andExpect(jsonPath("$.event.choices.length()").value(3))
                .andReturn();
        JsonNode entered = objectMapper.readTree(result.getResponse().getContentAsString());
        if (entered.get("combat").isObject()) {
            assertTrue(entered.get("combat").get("actions").size() >= 4);
        }
        return entered;
    }

    private JsonNode claimFirstReward(String runId, JsonNode current) throws Exception {
        JsonNode reward = current.get("rewardOffers").get(0);
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/rewards/{rewardId}/claim",
                        runId, reward.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardOffers.length()").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode leaveShop(String runId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/shops/leave", runId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode view = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(view.get("shop") == null || view.get("shop").isNull());
        return view;
    }

    private JsonNode skipSpecialRemoval(String runId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/removals/skip", runId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode view = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(view.get("removal") == null || view.get("removal").isNull());
        return view;
    }

    private JsonNode enterNode(String runId, String nodeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/nodes/{nodeId}/enter", runId, nodeId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode upgradeFirstCard(String runId, JsonNode current) throws Exception {
        JsonNode card = current.get("upgradeOptions").get(0);
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/upgrades/{cardId}", runId, card.get("id").asText()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode skipUpgrade(String runId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/upgrades/skip", runId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode finishCombat(String runId, JsonNode current) throws Exception {
        for (int turn = 0; turn < 60 && current.get("combat").isObject(); turn++) {
            String action = current.get("combat").get("enemyBlock").asInt() > 0
                    && current.get("spirit").asInt() >= 3 ? "STRIKE"
                    : "ATTACK".equals(current.get("combat").get("intent").asText()) ? "GUARD"
                    : current.get("spirit").asInt() >= 8 ? "TECHNIQUE"
                    : current.get("spirit").asInt() >= 3 ? "STRIKE" : "MEDITATE";
            current = combatAction(runId, action);
            if (!"RUNNING".equals(current.get("status").asText())) break;
        }
        assertFalse(current.get("combat").isObject(), "测试战斗应该在 60 回合内结束");
        return current;
    }

    private JsonNode combatAction(String runId, String action) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/combat/actions", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode buyShopOffer(String runId, String offerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/shops/{offerId}/buy", runId, offerId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refreshShop(String runId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/shops/refresh", runId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode removeShopCard(String runId, String cardId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs/{id}/shops/remove/{cardId}", runId, cardId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode advanceToNodeType(String runId, JsonNode current, String type) throws Exception {
        JsonNode target = firstNodeByType(current.get("map").get("nodes"), type);
        assertTrue(target != null, "路线图应该包含目标节点：" + type);
        for (int step = 0; step < 24; step++) {
            assertEquals("RUNNING", current.get("status").asText(), "前往目标节点的过程中不应提前结束");
            if (current.get("rewardOffers").size() > 0) {
                current = claimFirstReward(runId, current);
                continue;
            }
            if (current.get("upgradeOptions").size() > 0) {
                current = skipUpgrade(runId);
                continue;
            }
            if (!current.get("shop").isNull()) {
                current = leaveShop(runId);
                continue;
            }
            if (!current.get("removal").isNull()) {
                current = skipSpecialRemoval(runId);
                continue;
            }
            if (!current.get("combat").isNull()) {
                current = finishCombat(runId, current);
                continue;
            }
            if (!current.get("currentNodeId").asText().isBlank()) {
                current = objectMapper.readTree(choose(runId, 2, UUID.randomUUID().toString()));
                continue;
            }

            JsonNode targetState = findNode(current.get("map").get("nodes"), target.get("id").asText());
            if (targetState != null && "AVAILABLE".equals(targetState.get("status").asText())) {
                return enterNode(runId, target.get("id").asText());
            }
            JsonNode next = firstAvailableToward(current.get("map").get("nodes"), target.get("id").asText());
            assertTrue(next != null, "目标节点应该存在可达的相邻路径：" + type);
            current = enterNode(runId, next.get("id").asText());
        }
        throw new AssertionError("在测试步数内没有抵达目标节点：" + type);
    }

    private JsonNode firstAvailableToward(JsonNode nodes, String targetId) {
        for (JsonNode node : nodes) {
            if ("AVAILABLE".equals(node.get("status").asText()) && canReach(nodes, node.get("id").asText(), targetId)) {
                return node;
            }
        }
        return null;
    }

    private boolean canReach(JsonNode nodes, String fromId, String targetId) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(fromId);
        while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();
            if (!visited.add(currentId)) continue;
            if (targetId.equals(currentId)) return true;
            JsonNode current = findNode(nodes, currentId);
            if (current == null) continue;
            for (JsonNode next : current.get("nextNodeIds")) {
                queue.addLast(next.asText());
            }
        }
        return false;
    }

    private JsonNode findSynergy(JsonNode synergies, String archetype) {
        for (JsonNode synergy : synergies) {
            if (archetype.equals(synergy.get("archetype").asText())) return synergy;
        }
        return null;
    }

    private String choose(String runId, int choiceIndex, String requestId) throws Exception {
        return mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceIndex\":" + choiceIndex + ",\"requestId\":\"" + requestId + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private JsonNode firstAvailable(JsonNode nodes) {
        for (JsonNode node : nodes) {
            if ("AVAILABLE".equals(node.get("status").asText())) {
                return node;
            }
        }
        return null;
    }

    private JsonNode firstNode(JsonNode nodes, int floor) {
        for (JsonNode node : nodes) {
            if (node.get("floor").asInt() == floor) {
                return node;
            }
        }
        return null;
    }

    private JsonNode firstNodeByType(JsonNode nodes, String type) {
        for (JsonNode node : nodes) {
            if (type.equals(node.get("type").asText())) {
                return node;
            }
        }
        return null;
    }

    private JsonNode firstNodePointingTo(JsonNode nodes, String targetId) {
        for (JsonNode node : nodes) {
            if (node.get("floor").asInt() == 0 && node.get("nextNodeIds").toString().contains(targetId)) {
                return node;
            }
        }
        return null;
    }

    private JsonNode findNode(JsonNode nodes, String nodeId) {
        for (JsonNode node : nodes) {
            if (nodeId.equals(node.get("id").asText())) {
                return node;
            }
        }
        return null;
    }

    private int countByStatus(JsonNode nodes, String status) {
        int count = 0;
        for (JsonNode node : nodes) {
            if (status.equals(node.get("status").asText())) count++;
        }
        return count;
    }

    private int countByType(JsonNode nodes, String type) {
        int count = 0;
        for (JsonNode node : nodes) {
            if (type.equals(node.get("type").asText())) count++;
        }
        return count;
    }
}
