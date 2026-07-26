package com.xiuxian.roguelike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.roguelike.domain.GameRunEntity;
import com.xiuxian.roguelike.repository.GameRunRepository;
import com.xiuxian.roguelike.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.required=true")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRunRepository gameRunRepository;

    @Autowired
    private SettlementService settlementService;

    @Test
    void registerLoginAndQueryCurrentAccount() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode registered = register(username, "云海散人");
        String token = registered.get("token").asText();
        String characterId = registered.get("account").get("characters").get(0).get("id").asText();

        assertFalse(token.isBlank());
        assertNotNull(characterId);
        assertEquals(username, registered.get("account").get("username").asText());

        JsonNode loggedIn = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertFalse(loggedIn.get("token").asText().isBlank());

        JsonNode current = objectMapper.readTree(mockMvc.perform(get("/api/players/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(username, current.get("username").asText());
        assertEquals(1, current.get("characters").size());
        assertEquals(characterId, current.get("characters").get(0).get("id").asText());
    }

    @Test
    void runCanOnlyBeReadByItsOwningAccount() throws Exception {
        JsonNode owner = register("owner_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), "守心客");
        String ownerToken = owner.get("token").asText();
        String characterId = owner.get("account").get("characters").get(0).get("id").asText();
        JsonNode started = objectMapper.readTree(mockMvc.perform(post("/api/game/runs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"伪造名\",\"origin\":\"丹鼎童子\",\"characterId\":\""
                                + characterId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String runId = started.get("id").asText();

        JsonNode other = register("other_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), "旁观者");
        String otherToken = other.get("token").asText();

        mockMvc.perform(get("/api/game/runs/{id}", runId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/game/runs/{id}", runId))
                .andExpect(status().isUnauthorized());

        JsonNode ownRuns = objectMapper.readTree(mockMvc.perform(get("/api/game/runs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertTrue(ownRuns.isArray());
        assertEquals(runId, ownRuns.get(0).get("id").asText());
        assertEquals(characterId, ownRuns.get(0).get("characterId").asText());
        assertEquals("守心客", ownRuns.get(0).get("playerName").asText());
    }

    @Test
    void settlementAwardsAccountCausalityAndUnlockChangesNextRun() throws Exception {
        String username = "progress_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode owner = register(username, "轮回客");
        String token = owner.get("token").asText();
        String characterId = owner.get("account").get("characters").get(0).get("id").asText();

        JsonNode started = objectMapper.readTree(mockMvc.perform(post("/api/game/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"轮回客\",\"origin\":\"散修\",\"characterId\":\""
                                + characterId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String runId = started.get("id").asText();
        assertEquals(1, started.get("accountProgress").get("achievementCount").asInt());
        assertEquals(2, started.get("accountProgress").get("totalAchievementRewards").asInt());
        GameRunEntity run = gameRunRepository.findById(runId).orElseThrow();
        run.applyChoice(0, 0, 0, 0, "awaiting_node", "炼气二层", "DEAD", "fallen_path");
        gameRunRepository.save(run);
        settlementService.ensure(run);

        JsonNode progress = objectMapper.readTree(mockMvc.perform(get("/api/account/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertTrue(progress.get("causalityPoints").asInt() > 0);
        assertEquals(1, progress.get("completedRuns").asInt());
        assertEquals(0, progress.get("ascendedRuns").asInt());
        assertEquals(1, progress.get("deadRuns").asInt());
        assertEquals(1, progress.get("highestFloor").asInt());
        assertTrue(progress.get("bestScore").asInt() > 0);
        assertTrue(progress.get("achievementCount").asInt() >= 3);
        assertEquals(9, progress.get("totalAchievementRewards").asInt());
        assertTrue(achievementUnlocked(progress, "first_step"));
        assertTrue(achievementUnlocked(progress, "first_settlement"));
        assertTrue(achievementUnlocked(progress, "fallen_once"));
        assertTrue(progress.get("recentSettlements").size() >= 1);

        JsonNode settledRun = objectMapper.readTree(mockMvc.perform(get("/api/game/runs/{id}", runId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(settledRun.get("settlement").get("score").asInt(),
                settledRun.get("settlement").get("scoreBreakdown").get("total").asInt());
        JsonNode restoredProgress = objectMapper.readTree(mockMvc.perform(get("/api/account/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(progress.get("causalityPoints").asInt(), restoredProgress.get("causalityPoints").asInt());
        assertEquals(progress.get("totalAchievementRewards").asInt(), restoredProgress.get("totalAchievementRewards").asInt());

        JsonNode unlocked = objectMapper.readTree(mockMvc.perform(post("/api/account/unlocks/first_breath")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        boolean firstBreathUnlocked = false;
        for (JsonNode item : unlocked.get("unlocks")) {
            if ("first_breath".equals(item.get("id").asText())) {
                firstBreathUnlocked = item.get("unlocked").asBoolean();
            }
        }
        assertTrue(firstBreathUnlocked);

        JsonNode nextRun = objectMapper.readTree(mockMvc.perform(post("/api/game/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"伪造名\",\"origin\":\"伪造出身\",\"characterId\":\""
                                + characterId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertEquals(108, nextRun.get("health").asInt());

        mockMvc.perform(post("/api/account/unlocks/first_breath")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private JsonNode register(String username, String characterName) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\",\"characterName\":\""
                                + characterName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private boolean achievementUnlocked(JsonNode progress, String achievementId) {
        for (JsonNode achievement : progress.get("achievements")) {
            if (achievementId.equals(achievement.get("id").asText())) {
                return achievement.get("unlocked").asBoolean();
            }
        }
        return false;
    }
}
