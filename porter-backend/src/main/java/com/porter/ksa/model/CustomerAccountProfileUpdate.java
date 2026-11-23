package com.porter.ksa.model;

import com.porter.ksa.entity.AccountStatus;
import java.util.List;

public record CustomerAccountProfileUpdate(
        String companyName,
        String companyRegistrationNumber,
        String vatNumber,
        String nationalAddress,
        String pocContactNumber,
        String pocEmailAddress,
        List<String> commsEmailList,
        AccountStatus accountStatus,
        String updatedBy
) {
}
