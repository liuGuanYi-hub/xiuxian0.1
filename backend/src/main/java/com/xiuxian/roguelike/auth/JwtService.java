package com.xiuxian.roguelike.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationMs;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.auth.jwt-secret}") String secret,
                      @Value("${app.auth.jwt-expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 个字符。 ");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    public String issue(AuthenticatedUser user) {
        try {
            long issuedAt = Instant.now().toEpochMilli();
            long expiresAt = issuedAt + expirationMs;
            String header = encode(objectMapper.writeValueAsBytes(objectMapper.createObjectNode()
                    .put("alg", "HS256")
                    .put("typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(objectMapper.createObjectNode()
                    .put("sub", user.userId())
                    .put("username", user.username())
                    .put("iat", issuedAt)
                    .put("exp", expiresAt)));
            String content = header + "." + payload;
            return content + "." + encode(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 生成失败。", exception);
        }
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return Optional.empty();
            String content = parts[0] + "." + parts[1];
            byte[] expected = sign(content);
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!payload.hasNonNull("sub") || !payload.hasNonNull("username") || !payload.hasNonNull("exp")) {
                return Optional.empty();
            }
            if (payload.get("exp").asLong() < Instant.now().toEpochMilli()) return Optional.empty();
            return Optional.of(new AuthenticatedUser(payload.get("sub").asText(), payload.get("username").asText()));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public long expirationMs() {
        return expirationMs;
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
