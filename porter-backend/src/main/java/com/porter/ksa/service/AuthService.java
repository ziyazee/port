package com.porter.ksa.service;

import com.porter.ksa.dto.LoginRequest;
import com.porter.ksa.dto.LoginResponse;
import com.porter.ksa.dto.LogoutResponse;
import com.porter.ksa.dto.SignUpRequest;
import com.porter.ksa.entity.UserAccount;
import com.porter.ksa.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository users;

    public AuthService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        UserAccount account = users.findById(username).orElse(null);

        if (account == null || !account.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return toLoginResponse(account);
    }

    public LoginResponse signUpAdmin(SignUpRequest request) {
        return signUp(request, "ADMIN", "Admin");
    }

    public LoginResponse signUpOperations(SignUpRequest request) {
        return signUp(request, "OPERATIONS", "Operations");
    }

    public LogoutResponse logout() {
        return new LogoutResponse("ok");
    }

    private LoginResponse signUp(SignUpRequest request, String role, String defaultDisplayName) {
        String username = normalizeUsername(request.username());
        String password = requireValue(request.password(), "Password is required");
        String displayName = optionalValue(request.displayName(), defaultDisplayName);

        UserAccount account = new UserAccount(username, password, role, displayName);

        if (users.existsById(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        try {
            return toLoginResponse(users.save(account));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists", exception);
        }
    }

    private LoginResponse toLoginResponse(UserAccount account) {
        return new LoginResponse(account.getUsername(), account.getRole(), account.getDisplayName());
    }

    private String normalizeUsername(String username) {
        return requireValue(username, "Username is required").toLowerCase();
    }

    private String optionalValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private String requireValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return value.trim();
    }
}
