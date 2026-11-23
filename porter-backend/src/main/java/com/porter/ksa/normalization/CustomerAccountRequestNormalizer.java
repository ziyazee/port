package com.porter.ksa.normalization;

import com.porter.ksa.dto.CustomerAccountCreateRequest;
import com.porter.ksa.dto.CustomerAccountUpdateRequest;
import com.porter.ksa.model.CustomerAccountProfileUpdate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomerAccountRequestNormalizer {

    public NormalizedCustomerAccountCreateFields normalizeCreate(CustomerAccountCreateRequest request) {
        return normalizeCreate(
                request.getCompanyName(),
                request.getCompanyRegistrationNumber(),
                request.getVatNumber(),
                request.getNationalAddress(),
                request.getPocContactNumber(),
                request.getPocEmailAddress(),
                request.getCommsEmailList(),
                request.getCreatedBy()
        );
    }

    public NormalizedCustomerAccountCreateFields normalizeCreate(
            String companyName,
            String companyRegistrationNumber,
            String vatNumber,
            String nationalAddress,
            String pocContactNumber,
            String pocEmailAddress,
            List<String> commsEmailList,
            String createdBy
    ) {
        return new NormalizedCustomerAccountCreateFields(
                trim(companyName),
                trim(companyRegistrationNumber),
                trim(vatNumber),
                trim(nationalAddress),
                normalizePhoneNumber(pocContactNumber),
                normalizeEmail(pocEmailAddress),
                normalizeEmailList(commsEmailList),
                trim(createdBy)
        );
    }

    public CustomerAccountProfileUpdate normalizeUpdate(CustomerAccountUpdateRequest request) {
        return new CustomerAccountProfileUpdate(
                trim(request.getCompanyName()),
                trim(request.getCompanyRegistrationNumber()),
                trim(request.getVatNumber()),
                trim(request.getNationalAddress()),
                normalizePhoneNumber(request.getPocContactNumber()),
                normalizeEmail(request.getPocEmailAddress()),
                normalizeEmailList(request.getCommsEmailList()),
                request.getAccountStatus(),
                trim(request.getUpdatedBy())
        );
    }

    public String normalizeAuditUser(String auditUser) {
        return trim(auditUser);
    }

    private List<String> normalizeEmailList(List<String> emailList) {
        if (emailList == null) {
            return List.of();
        }

        return emailList.stream()
                .map(this::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private String normalizeEmail(String email) {
        return trim(email).toLowerCase();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String trimmed = trim(phoneNumber);
        if (trimmed.startsWith("05")) {
            return "+966" + trimmed.substring(1);
        }

        return trimmed;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public record NormalizedCustomerAccountCreateFields(
            String companyName,
            String companyRegistrationNumber,
            String vatNumber,
            String nationalAddress,
            String pocContactNumber,
            String pocEmailAddress,
            List<String> commsEmailList,
            String createdBy
    ) {
    }

}
