package ua.dymohlo.sportPredictions.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private static final String SECRET =
            "test-secret-key-which-is-long-enough-for-hmac-256-algorithm-padding";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        // Manually invoke @PostConstruct
        ReflectionTestUtils.invokeMethod(jwtUtils, "init");
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtils.generateToken("testUser");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String username = "alice";
        String token = jwtUtils.generateToken(username);

        assertThat(jwtUtils.extractUsername(token)).isEqualTo(username);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtils.generateToken("bob");
        assertThat(jwtUtils.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtils.generateToken("charlie");
        // corrupt the signature
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtUtils.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertThat(jwtUtils.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtUtils.isTokenValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void generateToken_differentUsersGetDifferentTokens() {
        String tokenAlice = jwtUtils.generateToken("alice");
        String tokenBob = jwtUtils.generateToken("bob");
        assertThat(tokenAlice).isNotEqualTo(tokenBob);
    }

    @Test
    void jwtCookieName_isJwt() {
        assertThat(JwtUtils.JWT_COOKIE_NAME).isEqualTo("jwt");
    }
}
