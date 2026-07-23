package com.xiuxian.roguelike.api;

import com.xiuxian.roguelike.api.GameDtos.ChoiceRequest;
import com.xiuxian.roguelike.api.GameDtos.GameRunView;
import com.xiuxian.roguelike.api.GameDtos.StartRunRequest;
import com.xiuxian.roguelike.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game/runs")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRunView start(@Valid @RequestBody StartRunRequest request) {
        return gameService.start(request);
    }

    @GetMapping("/{id}")
    public GameRunView get(@PathVariable String id) {
        return gameService.get(id);
    }

    @PostMapping("/{id}/choices")
    public GameRunView choose(@PathVariable String id, @Valid @RequestBody ChoiceRequest request) {
        return gameService.choose(id, request);
    }

    @PostMapping("/{id}/nodes/{nodeId}/enter")
    public GameRunView enterNode(@PathVariable String id, @PathVariable String nodeId) {
        return gameService.enterNode(id, nodeId);
    }

    @PostMapping("/{id}/rewards/{rewardId}/claim")
    public GameRunView claimReward(@PathVariable String id, @PathVariable String rewardId) {
        return gameService.claimReward(id, rewardId);
    }

    @PostMapping("/{id}/upgrades/{cardId}")
    public GameRunView upgradeCard(@PathVariable String id, @PathVariable String cardId) {
        return gameService.upgradeCard(id, cardId);
    }

    @PostMapping("/{id}/upgrades/skip")
    public GameRunView skipUpgrade(@PathVariable String id) {
        return gameService.skipUpgrade(id);
    }

    @PostMapping("/{id}/removals/{cardId}")
    public GameRunView removeSpecialCard(@PathVariable String id, @PathVariable String cardId) {
        return gameService.removeSpecialCard(id, cardId);
    }

    @PostMapping("/{id}/removals/skip")
    public GameRunView skipSpecialRemoval(@PathVariable String id) {
        return gameService.skipSpecialRemoval(id);
    }

    @PostMapping("/{id}/shops/{offerId}/buy")
    public GameRunView buyShopOffer(@PathVariable String id, @PathVariable String offerId) {
        return gameService.buyShopOffer(id, offerId);
    }

    @PostMapping("/{id}/shops/refresh")
    public GameRunView refreshShop(@PathVariable String id) {
        return gameService.refreshShop(id);
    }

    @PostMapping("/{id}/shops/remove/{cardId}")
    public GameRunView removeShopCard(@PathVariable String id, @PathVariable String cardId) {
        return gameService.removeShopCard(id, cardId);
    }

    @PostMapping("/{id}/shops/leave")
    public GameRunView leaveShop(@PathVariable String id) {
        return gameService.leaveShop(id);
    }
}
