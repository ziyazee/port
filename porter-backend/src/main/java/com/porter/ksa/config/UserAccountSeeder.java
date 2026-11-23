package com.porter.ksa.config;

import com.porter.ksa.entity.UserAccount;
import com.porter.ksa.repository.UserAccountRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAccountSeeder {

    @Bean
    ApplicationRunner seedUserAccounts(UserAccountRepository userAccounts) {
        return args -> {
            createIfMissing(userAccounts, new UserAccount("admin", "admin123", "ADMIN", "Admin"));
            createIfMissing(userAccounts, new UserAccount("operations", "ops123", "OPERATIONS", "Operations"));
        };
    }

    private void createIfMissing(UserAccountRepository userAccounts, UserAccount account) {
        if (!userAccounts.existsById(account.getUsername())) {
            userAccounts.save(account);
        }
    }
}
