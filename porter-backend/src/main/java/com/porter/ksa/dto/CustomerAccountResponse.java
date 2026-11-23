package com.porter.ksa.dto;

import com.porter.ksa.entity.AccountStatus;
import java.time.Instant;
import java.util.List;

public record CustomerAccountResponse(
        Long id,
        String companyName,
        String companyRegistrationNumber,
        String vatNumber,
        String nationalAddress,
        String pocContactNumber,
        String pocEmailAddress,
        List<String> commsEmailList,
        DocumentResponse registrationDocument,
        DocumentResponse vatDocument,
        DocumentResponse nationalAddressDocument,
        AccountStatus accountStatus,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
