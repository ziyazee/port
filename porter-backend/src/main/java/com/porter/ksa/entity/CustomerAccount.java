package com.porter.ksa.entity;

import com.porter.ksa.model.CustomerAccountProfileUpdate;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, length = 80)
    private String companyRegistrationNumber;

    @Column(nullable = false, length = 80)
    private String vatNumber;

    @Column(nullable = false)
    private String nationalAddress;

    @Column(nullable = false, length = 20)
    private String pocContactNumber;

    @Column(nullable = false)
    private String pocEmailAddress;

    @ElementCollection
    @CollectionTable(name = "customer_account_comms_emails", joinColumns = @JoinColumn(name = "customer_account_id"))
    @Column(name = "email", nullable = false)
    private List<String> commsEmailList = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "storageKey", column = @Column(name = "registration_document_storage_key", length = 500)),
            @AttributeOverride(name = "originalFilename", column = @Column(name = "registration_document_filename")),
            @AttributeOverride(name = "contentType", column = @Column(name = "registration_document_content_type", length = 120)),
            @AttributeOverride(name = "sizeBytes", column = @Column(name = "registration_document_size_bytes"))
    })
    private DocumentReference registrationDocument;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "storageKey", column = @Column(name = "vat_document_storage_key", length = 500)),
            @AttributeOverride(name = "originalFilename", column = @Column(name = "vat_document_filename")),
            @AttributeOverride(name = "contentType", column = @Column(name = "vat_document_content_type", length = 120)),
            @AttributeOverride(name = "sizeBytes", column = @Column(name = "vat_document_size_bytes"))
    })
    private DocumentReference vatDocument;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "storageKey", column = @Column(name = "national_address_document_storage_key", length = 500)),
            @AttributeOverride(name = "originalFilename", column = @Column(name = "national_address_document_filename")),
            @AttributeOverride(name = "contentType", column = @Column(name = "national_address_document_content_type", length = 120)),
            @AttributeOverride(name = "sizeBytes", column = @Column(name = "national_address_document_size_bytes"))
    })
    private DocumentReference nationalAddressDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccountStatus accountStatus = AccountStatus.CREATED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    protected CustomerAccount() {
    }

    public CustomerAccount(
            String companyName,
            String companyRegistrationNumber,
            String vatNumber,
            String nationalAddress,
            String pocContactNumber,
            String pocEmailAddress,
            List<String> commsEmailList,
            DocumentReference registrationDocument,
            DocumentReference vatDocument,
            DocumentReference nationalAddressDocument,
            String createdBy
    ) {
        this.companyName = companyName;
        this.companyRegistrationNumber = companyRegistrationNumber;
        this.vatNumber = vatNumber;
        this.nationalAddress = nationalAddress;
        this.pocContactNumber = pocContactNumber;
        this.pocEmailAddress = pocEmailAddress;
        this.commsEmailList = new ArrayList<>(commsEmailList);
        this.registrationDocument = registrationDocument;
        this.vatDocument = vatDocument;
        this.nationalAddressDocument = nationalAddressDocument;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    @PrePersist
    void beforeCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyRegistrationNumber() {
        return companyRegistrationNumber;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public String getNationalAddress() {
        return nationalAddress;
    }

    public String getPocContactNumber() {
        return pocContactNumber;
    }

    public String getPocEmailAddress() {
        return pocEmailAddress;
    }

    public List<String> getCommsEmailList() {
        return List.copyOf(commsEmailList);
    }

    public DocumentReference getRegistrationDocument() {
        return registrationDocument;
    }

    public DocumentReference getVatDocument() {
        return vatDocument;
    }

    public DocumentReference getNationalAddressDocument() {
        return nationalAddressDocument;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void updateProfile(CustomerAccountProfileUpdate profileUpdate) {
        this.companyName = profileUpdate.companyName();
        this.companyRegistrationNumber = profileUpdate.companyRegistrationNumber();
        this.vatNumber = profileUpdate.vatNumber();
        this.nationalAddress = profileUpdate.nationalAddress();
        this.pocContactNumber = profileUpdate.pocContactNumber();
        this.pocEmailAddress = profileUpdate.pocEmailAddress();
        this.commsEmailList = new ArrayList<>(profileUpdate.commsEmailList());
        this.accountStatus = profileUpdate.accountStatus();
        this.updatedBy = profileUpdate.updatedBy();
    }

    public void replaceRegistrationDocument(DocumentReference registrationDocument) {
        this.registrationDocument = registrationDocument;
    }

    public void replaceVatDocument(DocumentReference vatDocument) {
        this.vatDocument = vatDocument;
    }

    public void replaceNationalAddressDocument(DocumentReference nationalAddressDocument) {
        this.nationalAddressDocument = nationalAddressDocument;
    }

    public void updateStatus(AccountStatus accountStatus, String updatedBy) {
        this.accountStatus = accountStatus;
        this.updatedBy = updatedBy;
    }
}
