package com.xiuxian.roguelike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        JsonNode chosen = objectMapper.readTree(choose(runId, 0, "history-test-1"));
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
        enterFirstAvailable(runId, startedRun);

        JsonNode afterBattle = objectMapper.readTree(choose(runId, 0, "reward-test-1"));
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
    void restNodeCanUpgradeCardBeforeUnlockingRoute() throws Exception {
        JsonNode started = objectMapper.readTree(startRun());
        String runId = started.get("id").asText();
        JsonNode restNode = firstNodeByType(started.get("map").get("nodes"), "REST");
        JsonNode parent = firstNodePointingTo(started.get("map").get("nodes"), restNode.get("id").asText());
        assertTrue(parent != null, "固定的休息节点应该有上一跳");

        JsonNode current = enterNode(runId, parent.get("id").asText());
        current = objectMapper.readTree(choose(runId, 0, "upgrade-prep-1"));
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
        enterFirstAvailable(runId, startedRun);
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
        enterFirstAvailable(runId, startedRun);

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
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
