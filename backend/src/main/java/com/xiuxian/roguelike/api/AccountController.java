package com.xiuxian.roguelike.api;

import com.xiuxian.roguelike.api.ProgressDtos.AccountProgressView;
import com.xiuxian.roguelike.service.PermanentProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final PermanentProgressService progressService;

    public AccountController(PermanentProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress")
    public AccountProgressView progress() {
        return progressService.current();
    }

    @PostMapping("/unlocks/{unlockId}")
    public AccountProgressView unlock(@PathVariable String unlockId) {
        return progressService.unlock(unlockId);
    }
}
