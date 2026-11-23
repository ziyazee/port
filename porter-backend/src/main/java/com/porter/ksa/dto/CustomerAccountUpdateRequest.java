package com.porter.ksa.dto;

import com.porter.ksa.entity.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class CustomerAccountUpdateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be 255 characters or fewer")
    private String companyName;

    @NotBlank(message = "Company registration number is required")
    @Size(max = 80, message = "Company registration number must be 80 characters or fewer")
    private String companyRegistrationNumber;

    @NotBlank(message = "VAT number is required")
    @Size(max = 80, message = "VAT number must be 80 characters or fewer")
    private String vatNumber;

    @NotBlank(message = "National address is required")
    @Size(max = 500, message = "National address must be 500 characters or fewer")
    private String nationalAddress;

    @NotBlank(message = "POC contact number is required")
    @Pattern(regexp = "^(?:\\+9665|05)\\d{8}$", message = "POC contact number must be a valid KSA mobile number")
    private String pocContactNumber;

    @NotBlank(message = "POC email address is required")
    @Email(message = "POC email address must be valid")
    private String pocEmailAddress;

    private List<@Email(message = "Comms email address must be valid") String> commsEmailList = new ArrayList<>();

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;

    @NotBlank(message = "Updated by is required")
    @Size(max = 120, message = "Updated by must be 120 characters or fewer")
    private String updatedBy;

    private MultipartFile registrationDocument;
    private MultipartFile vatDocument;
    private MultipartFile nationalAddressDocument;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyRegistrationNumber() {
        return companyRegistrationNumber;
    }

    public void setCompanyRegistrationNumber(String companyRegistrationNumber) {
        this.companyRegistrationNumber = companyRegistrationNumber;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getNationalAddress() {
        return nationalAddress;
    }

    public void setNationalAddress(String nationalAddress) {
        this.nationalAddress = nationalAddress;
    }

    public String getPocContactNumber() {
        return pocContactNumber;
    }

    public void setPocContactNumber(String pocContactNumber) {
        this.pocContactNumber = pocContactNumber;
    }

    public String getPocEmailAddress() {
        return pocEmailAddress;
    }

    public void setPocEmailAddress(String pocEmailAddress) {
        this.pocEmailAddress = pocEmailAddress;
    }

    public List<String> getCommsEmailList() {
        return commsEmailList;
    }

    public void setCommsEmailList(List<String> commsEmailList) {
        this.commsEmailList = commsEmailList == null ? new ArrayList<>() : commsEmailList;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public MultipartFile getRegistrationDocument() {
        return registrationDocument;
    }

    public void setRegistrationDocument(MultipartFile registrationDocument) {
        this.registrationDocument = registrationDocument;
    }

    public MultipartFile getVatDocument() {
        return vatDocument;
    }

    public void setVatDocument(MultipartFile vatDocument) {
        this.vatDocument = vatDocument;
    }

    public MultipartFile getNationalAddressDocument() {
        return nationalAddressDocument;
    }

    public void setNationalAddressDocument(MultipartFile nationalAddressDocument) {
        this.nationalAddressDocument = nationalAddressDocument;
    }
}
