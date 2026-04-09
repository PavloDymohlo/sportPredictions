package ua.dymohlo.sportPredictions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.dto.response.AuthResponse;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;
import ua.dymohlo.sportPredictions.service.AuthService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for authentication endpoints.
 * Security is active. @WithMockUser satisfies Spring Security even when WebSecurityConfig
 * is not auto-loaded by the test slice. .with(csrf()) satisfies CSRF validation.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ─── POST /api/v0/auth/register ───────────────────────────────────────────

    @Test
    @WithMockUser
    void register_success_returns200WithUserNameAndLanguage() throws Exception {
        AuthResponse auth = AuthResponse.builder()
                .userName("alice").language("en").token("jwt-token").build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(auth);

        mockMvc.perform(post("/api/v0/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName", is("alice")))
                .andExpect(jsonPath("$.language", is("en")));
    }

    @Test
    @WithMockUser
    void register_success_setsJwtCookie() throws Exception {
        AuthResponse auth = AuthResponse.builder()
                .userName("alice").language("en").token("my-jwt-token").build();
        when(authService.register(any())).thenReturn(auth);

        mockMvc.perform(post("/api/v0/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("jwt=my-jwt-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @WithMockUser
    void register_duplicateUser_returns400() throws Exception {
        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("This username already exists!"));

        mockMvc.perform(post("/api/v0/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass"))))
                .andExpect(status().is4xxClientError());
    }

    // ─── POST /api/v0/auth/login ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void login_success_returns200WithUserInfo() throws Exception {
        AuthResponse auth = AuthResponse.builder()
                .userName("bob").language("uk").token("tok").build();
        when(authService.loginIn(any(LoginInRequest.class))).thenReturn(auth);

        mockMvc.perform(post("/api/v0/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginInRequest("bob", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName", is("bob")))
                .andExpect(jsonPath("$.language", is("uk")));
    }

    @Test
    @WithMockUser
    void login_success_setsJwtCookie() throws Exception {
        AuthResponse auth = AuthResponse.builder()
                .userName("bob").language("en").token("login-token").build();
        when(authService.loginIn(any())).thenReturn(auth);

        mockMvc.perform(post("/api/v0/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginInRequest("bob", "pass"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("jwt=login-token")));
    }

    @Test
    @WithMockUser
    void login_wrongCredentials_returns400() throws Exception {
        when(authService.loginIn(any()))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        mockMvc.perform(post("/api/v0/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginInRequest("bob", "wrong"))))
                .andExpect(status().is4xxClientError());
    }

    // ─── POST /api/v0/auth/logout ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void logout_returns200AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v0/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verifyNoInteractions(authService);
    }
}
