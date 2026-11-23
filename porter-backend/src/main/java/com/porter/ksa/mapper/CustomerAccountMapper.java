package com.porter.ksa.mapper;

import com.porter.ksa.dto.CustomerAccountResponse;
import com.porter.ksa.dto.DocumentResponse;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.DocumentReference;
import org.springframework.stereotype.Component;

@Component
public class CustomerAccountMapper {

    public CustomerAccountResponse toResponse(CustomerAccount customerAccount) {
        return new CustomerAccountResponse(
                customerAccount.getId(),
                customerAccount.getCompanyName(),
                customerAccount.getCompanyRegistrationNumber(),
                customerAccount.getVatNumber(),
                customerAccount.getNationalAddress(),
                customerAccount.getPocContactNumber(),
                customerAccount.getPocEmailAddress(),
                customerAccount.getCommsEmailList(),
                toDocumentResponse(customerAccount.getRegistrationDocument()),
                toDocumentResponse(customerAccount.getVatDocument()),
                toDocumentResponse(customerAccount.getNationalAddressDocument()),
                customerAccount.getAccountStatus(),
                customerAccount.getCreatedAt(),
                customerAccount.getCreatedBy(),
                customerAccount.getUpdatedAt(),
                customerAccount.getUpdatedBy()
        );
    }

    private DocumentResponse toDocumentResponse(DocumentReference document) {
        if (document == null) {
            return null;
        }

        return new DocumentResponse(
                document.getStorageKey(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes()
        );
    }
}
