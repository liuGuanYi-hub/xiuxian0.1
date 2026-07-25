package com.xiuxian.roguelike.api;

import com.xiuxian.roguelike.api.AuthDtos.AccountView;
import com.xiuxian.roguelike.api.AuthDtos.CharacterRequest;
import com.xiuxian.roguelike.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final AccountService accountService;

    public PlayerController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AccountView me() {
        return accountService.current();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@Valid @RequestBody CharacterRequest request) {
        return accountService.createCharacter(request);
    }
}
