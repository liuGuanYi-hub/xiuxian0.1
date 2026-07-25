package com.xiuxian.roguelike.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "^[A-Za-z0-9_]{3,40}$", message = "用户名只能使用 3-40 位字母、数字或下划线")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 120, message = "密码长度需要在 8-120 位之间")
            String password,
            @Size(max = 16, message = "道号不能超过 16 个字符")
            String characterName
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record CharacterRequest(
            @NotBlank(message = "道号不能为空")
            @Size(max = 16, message = "道号不能超过 16 个字符")
            String name,
            @NotBlank(message = "出身不能为空") String origin
    ) {
    }

    public record CharacterView(String id, String name, String origin, LocalDateTime createdAt) {
    }

    public record AccountView(String id, String username, List<CharacterView> characters) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInMs, AccountView account) {
    }
}
