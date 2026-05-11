package com.Mengge.finance_tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Mengge.finance_tracker.dto.auth.AuthResponse;
import com.Mengge.finance_tracker.dto.auth.LoginRequest;
import com.Mengge.finance_tracker.dto.auth.RegisterRequest;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.repository.UserRepository;
import com.Mengge.finance_tracker.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_throwsWhenEmailTaken() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(
            () -> authService.register(new RegisterRequest("N", "a@b.com", "secret12"))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already in use");
    }

    @Test
    void register_encodesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("encoded");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse resp = authService.register(new RegisterRequest("N", "new@b.com", "secret12"));

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.expiresInMs()).isEqualTo(3600000L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_returnsTokenAfterAuthentication() {
        User user = new User();
        user.setId(1L);
        user.setEmail("log@b.com");
        user.setPassword("encoded");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(new UsernamePasswordAuthenticationToken("log@b.com", null));
        when(userRepository.findByEmail("log@b.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("login-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse resp = authService.login(new LoginRequest("log@b.com", "secret12"));

        assertThat(resp.token()).isEqualTo("login-jwt");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
