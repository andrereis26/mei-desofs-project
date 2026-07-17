package com.desofs.project.user.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.shared.ratelimit.AuthRateLimitGuard;
import com.desofs.project.user.dtos.AuthResponse;
import com.desofs.project.user.dtos.LoginRequest;
import com.desofs.project.user.dtos.RegisterRequest;
import com.desofs.project.user.dtos.UserDto;
import com.desofs.project.user.services.IUserService;
import com.desofs.project.user.services.ITokenRevocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final IUserService userService;
    private final JwtService jwtService;
    private final AuthRateLimitGuard authRateLimitGuard;
    private final ITokenRevocationService tokenRevocationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authRateLimitGuard.checkLoginIdentityLimit(request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails user = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        String role = user.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("USER");

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(role)
                .expiresIn(jwtService.getExpiration())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        authRateLimitGuard.checkRegisterUsernameLimit(request.getUsername());
        authRateLimitGuard.checkRegisterEmailLimit(request.getEmail());
        UserDto user = userService.register(request);
        String token = jwtService.generateToken(user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .expiresIn(jwtService.getExpiration())
                .build());

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        String token = extractToken(authHeader);
        tokenRevocationService.revoke(token, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Invalid token");
        }
        return authHeader.substring(7);
    }
}
