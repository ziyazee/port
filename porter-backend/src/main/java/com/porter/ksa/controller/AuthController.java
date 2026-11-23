package com.porter.ksa.controller;

import com.porter.ksa.dto.LoginRequest;
import com.porter.ksa.dto.LoginResponse;
import com.porter.ksa.dto.LogoutResponse;
import com.porter.ksa.dto.SignUpRequest;
import com.porter.ksa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/signup/admin")
    public LoginResponse signUpAdmin(@Valid @RequestBody SignUpRequest request) {
        return authService.signUpAdmin(request);
    }

    @PostMapping("/signup/operations")
    public LoginResponse signUpOperations(@Valid @RequestBody SignUpRequest request) {
        return authService.signUpOperations(request);
    }

    @PostMapping("/logout")
    public LogoutResponse logout() {
        return authService.logout();
    }
}
