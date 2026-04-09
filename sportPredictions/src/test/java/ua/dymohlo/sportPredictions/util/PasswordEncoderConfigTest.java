package ua.dymohlo.sportPredictions.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncoderConfigTest {

    @Test
    void encoderPassword_returnsHashedPassword() {
        String raw = "myPassword123";
        String hashed = PasswordEncoderConfig.encoderPassword(raw);

        assertThat(hashed).isNotNull();
        assertThat(hashed).isNotEqualTo(raw);
        assertThat(hashed).startsWith("$2a$");
    }

    @Test
    void encoderPassword_twoCallsProduceDifferentHashes() {
        String raw = "samePassword";
        String hash1 = PasswordEncoderConfig.encoderPassword(raw);
        String hash2 = PasswordEncoderConfig.encoderPassword(raw);

        // BCrypt uses random salt each time
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void checkPassword_correctPassword_returnsTrue() {
        String raw = "correctPassword";
        String hashed = PasswordEncoderConfig.encoderPassword(raw);

        assertThat(PasswordEncoderConfig.checkPassword(raw, hashed)).isTrue();
    }

    @Test
    void checkPassword_wrongPassword_returnsFalse() {
        String raw = "correctPassword";
        String hashed = PasswordEncoderConfig.encoderPassword(raw);

        assertThat(PasswordEncoderConfig.checkPassword("wrongPassword", hashed)).isFalse();
    }

    @Test
    void checkPassword_emptyString_returnsFalse() {
        String hashed = PasswordEncoderConfig.encoderPassword("somePassword");
        assertThat(PasswordEncoderConfig.checkPassword("", hashed)).isFalse();
    }

    @Test
    void constructor_throwsUnsupportedOperationException() throws Exception {
        var constructor = PasswordEncoderConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
