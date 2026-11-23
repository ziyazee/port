package com.porter.ksa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.porter.ksa.controller.AuthController;
import com.porter.ksa.dto.LoginRequest;
import com.porter.ksa.dto.LoginResponse;
import com.porter.ksa.dto.LogoutResponse;
import com.porter.ksa.dto.SignUpRequest;
import com.porter.ksa.entity.UserAccount;
import com.porter.ksa.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class AuthControllerTests {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserAccountRepository userAccounts;

    @BeforeEach
    void resetUsers() {
        userAccounts.deleteAll();
        userAccounts.save(new UserAccount("admin", "admin123", "ADMIN", "Admin"));
        userAccounts.save(new UserAccount("operations", "ops123", "OPERATIONS", "Operations"));
    }

    @Test
    void loginReturnsAdminRoleForValidAdminCredentials() {
        LoginResponse response = authController.login(
                new LoginRequest("admin", "admin123")
        );

        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void loginThrowsUnauthorizedForInvalidCredentials() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authController.login(new LoginRequest("admin", "wrong"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void signUpAdminCreatesAdminAccountThatCanLogin() {
        LoginResponse signUpResponse = authController.signUpAdmin(
                new SignUpRequest("NewAdmin", "secret", "New Admin")
        );

        assertEquals("newadmin", signUpResponse.username());
        assertEquals("ADMIN", signUpResponse.role());

        LoginResponse loginResponse = authController.login(
                new LoginRequest("newadmin", "secret")
        );

        assertEquals("New Admin", loginResponse.displayName());
    }

    @Test
    void signUpOperationsCreatesOperationsAccountThatCanLogin() {
        LoginResponse signUpResponse = authController.signUpOperations(
                new SignUpRequest("dispatcher", "secret", "Dispatcher")
        );

        assertEquals("dispatcher", signUpResponse.username());
        assertEquals("OPERATIONS", signUpResponse.role());

        LoginResponse loginResponse = authController.login(
                new LoginRequest("dispatcher", "secret")
        );

        assertEquals("OPERATIONS", loginResponse.role());
    }

    @Test
    void signUpThrowsConflictForExistingUsername() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authController.signUpAdmin(
                        new SignUpRequest("admin", "secret", "Duplicate")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void signUpThrowsBadRequestForBlankUsername() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authController.signUpOperations(
                        new SignUpRequest(" ", "secret", "Invalid")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void logoutReturnsOkStatus() {
        LogoutResponse response = authController.logout();

        assertEquals("ok", response.status());
    }
}
