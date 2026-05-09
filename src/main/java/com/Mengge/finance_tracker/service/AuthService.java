package com.Mengge.finance_tracker.service;

import com.Mengge.finance_tracker.dto.auth.AuthResponse;
import com.Mengge.finance_tracker.dto.auth.LoginRequest;
import com.Mengge.finance_tracker.dto.auth.RegisterRequest;
import com.Mengge.finance_tracker.entity.User;
import com.Mengge.finance_tracker.repository.UserRepository;
import com.Mengge.finance_tracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        UserDetails principal = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_USER")
            .build();
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, jwtService.getExpirationMs());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        UserDetails principal = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_USER")
            .build();
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, jwtService.getExpirationMs());
    }
}
