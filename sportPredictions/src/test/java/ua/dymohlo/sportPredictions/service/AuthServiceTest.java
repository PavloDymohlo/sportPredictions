package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.dto.response.AuthResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;
import ua.dymohlo.sportPredictions.util.PasswordEncoderConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_newUser_savesAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest("alice", "pass123");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtils.generateToken("alice")).thenReturn("token-alice");

        AuthResponse response = authService.register(request);

        assertThat(response.getUserName()).isEqualTo("alice");
        assertThat(response.getToken()).isEqualTo("token-alice");
        assertThat(response.getLanguage()).isEqualTo("en");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUserName()).isEqualTo("alice");
        assertThat(saved.getRankingPosition()).isEqualTo(1L);
        // password must be hashed
        assertThat(saved.getPassword()).isNotEqualTo("pass123");
        assertThat(PasswordEncoderConfig.checkPassword("pass123", saved.getPassword())).isTrue();
    }

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest("alice", "pass123");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void register_rankingPositionIsCountPlusOne() {
        RegisterRequest request = new RegisterRequest("newUser", "pass");
        when(userRepository.findByUserName("newUser")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateToken(any())).thenReturn("tok");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRankingPosition()).isEqualTo(6L);
    }

    // ─── loginIn ──────────────────────────────────────────────────────────────

    @Test
    void loginIn_correctCredentials_returnsResponse() {
        String raw = "secret";
        String hashed = PasswordEncoderConfig.encoderPassword(raw);
        User user = User.builder()
                .id(1L)
                .userName("alice")
                .password(hashed)
                .language("uk")
                .build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken("alice")).thenReturn("tok123");

        LoginInRequest request = new LoginInRequest("alice", raw);
        AuthResponse response = authService.loginIn(request);

        assertThat(response.getUserName()).isEqualTo("alice");
        assertThat(response.getLanguage()).isEqualTo("uk");
        assertThat(response.getToken()).isEqualTo("tok123");
    }

    @Test
    void loginIn_userNotFound_throwsIllegalArgumentException() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginIn(new LoginInRequest("ghost", "any")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginIn_wrongPassword_throwsIllegalArgumentException() {
        String hashed = PasswordEncoderConfig.encoderPassword("correctPass");
        User user = User.builder().userName("alice").password(hashed).build();
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.loginIn(new LoginInRequest("alice", "wrongPass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
