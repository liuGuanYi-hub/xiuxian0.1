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
    void playerCanStartChooseAndRestoreHistory() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();

        mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceIndex\":0,\"requestId\":\"history-test-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId))
                .andExpect(jsonPath("$.turn").value(1))
                .andExpect(jsonPath("$.health").value(100))
                .andExpect(jsonPath("$.logs.length()").value(2));

        mockMvc.perform(get("/api/game/runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn").value(1))
                .andExpect(jsonPath("$.logs.length()").value(3));
    }

    @Test
    void duplicateRequestIdDoesNotAdvanceTheRunTwice() throws Exception {
        JsonNode startedRun = objectMapper.readTree(startRun());
        String runId = startedRun.get("id").asText();
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

        mockMvc.perform(post("/api/game/runs/{id}/choices", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceIndex\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("无效的事件选项。"));
    }

    @Test
    void defaultPathCanReachLongFormAscension() throws Exception {
        JsonNode current = objectMapper.readTree(startRun());
        String runId = current.get("id").asText();

        for (int turn = 0; turn < 15 && "RUNNING".equals(current.get("status").asText()); turn++) {
            current = objectMapper.readTree(choose(runId, 0, UUID.randomUUID().toString()));
        }

        assertTrue(current.get("turn").asInt() >= 10, "完整路线应该至少有十次决策");
        assertEquals("ASCENDED", current.get("status").asText());
    }

    private String startRun() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/game/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"顾长生\",\"origin\":\"散修\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.event.choices.length()").value(3))
                .andReturn();
        return result.getResponse().getContentAsString();
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
}
