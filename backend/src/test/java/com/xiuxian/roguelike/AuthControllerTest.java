package com.xiuxian.roguelike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private JsonNode register(String username, String characterName) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\",\"characterName\":\""
                                + characterName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }
}
