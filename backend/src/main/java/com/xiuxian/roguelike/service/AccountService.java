package com.xiuxian.roguelike.service;

import com.xiuxian.roguelike.api.AuthDtos.AccountView;
import com.xiuxian.roguelike.api.AuthDtos.CharacterRequest;
import com.xiuxian.roguelike.api.AuthDtos.CharacterView;
import com.xiuxian.roguelike.api.AuthDtos.LoginRequest;
import com.xiuxian.roguelike.api.AuthDtos.RegisterRequest;
import com.xiuxian.roguelike.api.AuthDtos.AuthResponse;
import com.xiuxian.roguelike.auth.AuthContext;
import com.xiuxian.roguelike.auth.AuthenticatedUser;
import com.xiuxian.roguelike.auth.JwtService;
import com.xiuxian.roguelike.auth.PasswordHasher;
import com.xiuxian.roguelike.domain.PlayerCharacterEntity;
import com.xiuxian.roguelike.domain.UserAccountEntity;
import com.xiuxian.roguelike.repository.PlayerCharacterRepository;
import com.xiuxian.roguelike.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final PlayerCharacterRepository characterRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final AuthContext authContext;

    public AccountService(UserAccountRepository userAccountRepository,
                          PlayerCharacterRepository characterRepository,
                          PasswordHasher passwordHasher, JwtService jwtService,
                          AuthContext authContext) {
        this.userAccountRepository = userAccountRepository;
        this.characterRepository = characterRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.authContext = authContext;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已经存在。");
        }
        UserAccountEntity account = new UserAccountEntity(
                UUID.randomUUID().toString(), username, passwordHasher.hash(request.password()));
        userAccountRepository.save(account);
        String characterName = request.characterName() == null || request.characterName().isBlank()
                ? username.substring(0, Math.min(16, username.length()))
                : request.characterName().trim();
        characterRepository.save(new PlayerCharacterEntity(
                UUID.randomUUID().toString(), account.getId(), characterName, "散修"));
        return issue(account);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccountEntity account = userAccountRepository.findByUsername(normalizeUsername(request.username()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!"ACTIVE".equals(account.getStatus()) || !passwordHasher.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issue(account);
    }

    @Transactional
    public AccountView current() {
        UserAccountEntity account = currentAccount();
        return toView(account);
    }

    @Transactional
    public AccountView createCharacter(CharacterRequest request) {
        UserAccountEntity account = currentAccount();
        characterRepository.save(new PlayerCharacterEntity(
                UUID.randomUUID().toString(), account.getId(), request.name().trim(), request.origin().trim()));
        return toView(account);
    }

    public PlayerCharacterEntity resolveCharacter(String requestedCharacterId) {
        String userId = authContext.requireUserId();
        if (requestedCharacterId != null && !requestedCharacterId.isBlank()) {
            return characterRepository.findByIdAndUserId(requestedCharacterId.trim(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("角色不属于当前账号。"));
        }
        return characterRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前账号还没有角色。"));
    }

    public AccountView toView(UserAccountEntity account) {
        List<CharacterView> characters = characterRepository.findByUserIdOrderByCreatedAtAsc(account.getId())
                .stream().map(character -> new CharacterView(character.getId(), character.getName(),
                        character.getOrigin(), character.getCreatedAt())).toList();
        return new AccountView(account.getId(), account.getUsername(), characters);
    }

    private AuthResponse issue(UserAccountEntity account) {
        AuthenticatedUser user = new AuthenticatedUser(account.getId(), account.getUsername());
        return new AuthResponse(jwtService.issue(user), "Bearer", jwtService.expirationMs(), toView(account));
    }

    private UserAccountEntity currentAccount() {
        String userId = authContext.requireUserId();
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("当前账号不存在。"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("用户名或密码错误。");
        }
    }
}
