package com.xiuxian.roguelike.api;

import com.xiuxian.roguelike.api.GameDtos.LeaderboardEntryView;
import com.xiuxian.roguelike.service.SettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final SettlementService settlementService;

    public LeaderboardController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public List<LeaderboardEntryView> top(@RequestParam(defaultValue = "10") int limit) {
        return settlementService.leaderboard(limit);
    }
}
