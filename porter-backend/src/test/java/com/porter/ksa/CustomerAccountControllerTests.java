package com.porter.ksa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.porter.ksa.controller.CustomerAccountController;
import com.porter.ksa.dto.CustomerAccountCreateRequest;
import com.porter.ksa.dto.CustomerAccountResponse;
import com.porter.ksa.dto.CustomerAccountStatusUpdateRequest;
import com.porter.ksa.dto.CustomerAccountUpdateRequest;
import com.porter.ksa.entity.AccountStatus;
import com.porter.ksa.repository.CustomerAccountRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "porter.document-storage.local.base-path=${java.io.tmpdir}/porter-test-documents")
class CustomerAccountControllerTests {

    @Autowired
    private CustomerAccountController customerAccountController;

    @Autowired
    private CustomerAccountRepository customerAccounts;

    @Autowired
    private Validator validator;

    @BeforeEach
    void resetCustomers() {
        customerAccounts.deleteAll();
    }

    @Test
    void createCustomerAccountPersistsSubmittedDataAndDocuments() {
        CustomerAccountResponse response = customerAccountController.create(validCreateRequest());

        assertNotNull(response.id());
        assertEquals("Acme Logistics", response.companyName());
        assertEquals("CR-12345", response.companyRegistrationNumber());
        assertEquals("VAT-98765", response.vatNumber());
        assertEquals("Riyadh 12211", response.nationalAddress());
        assertEquals("+966512345678", response.pocContactNumber());
        assertEquals("ops@acme.example", response.pocEmailAddress());
        assertEquals("finance@acme.example", response.commsEmailList().get(0));
        assertEquals("dispatch@acme.example", response.commsEmailList().get(1));
        assertEquals("customer-documents/" + response.id() + "/registrationDocument", response.registrationDocument().storageKey());
        assertEquals("customer-documents/" + response.id() + "/vatDocument", response.vatDocument().storageKey());
        assertEquals("customer-documents/" + response.id() + "/nationalAddressDocument", response.nationalAddressDocument().storageKey());
        assertEquals("CREATED", response.accountStatus().name());
        assertNotNull(response.createdAt());
        assertEquals("admin", response.createdBy());
        assertNotNull(response.updatedAt());
        assertEquals("admin", response.updatedBy());
    }

    @Test
    void createCustomerAccountRejectsNonKsaPocContactNumber() {
        CustomerAccountCreateRequest request = validCreateRequest();
        request.setPocContactNumber("+15551234567");

        Set<ConstraintViolation<CustomerAccountCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("pocContactNumber")));
    }

    @Test
    void createCustomerAccountAllowsMissingDocuments() {
        CustomerAccountCreateRequest request = validCreateRequest();
        request.setRegistrationDocument(null);
        request.setVatDocument(null);
        request.setNationalAddressDocument(null);

        CustomerAccountResponse response = customerAccountController.create(request);

        assertNotNull(response.id());
        assertNull(response.registrationDocument());
        assertNull(response.vatDocument());
        assertNull(response.nationalAddressDocument());
    }

    @Test
    void findAllReturnsCustomerAccountsWithoutOrderCounts() {
        CustomerAccountResponse created = customerAccountController.create(validCreateRequest());

        List<CustomerAccountResponse> accounts = customerAccountController.findAll();

        assertEquals(1, accounts.size());
        assertEquals(created.id(), accounts.get(0).id());
        assertEquals("Acme Logistics", accounts.get(0).companyName());
    }

    @Test
    void findByIdReturnsCustomerAccount() {
        CustomerAccountResponse created = customerAccountController.create(validCreateRequest());

        CustomerAccountResponse found = customerAccountController.findById(created.id());

        assertEquals(created.id(), found.id());
        assertEquals("Acme Logistics", found.companyName());
    }

    @Test
    void updateCustomerAccountChangesProfileStatusAndOptionalDocument() {
        CustomerAccountResponse created = customerAccountController.create(validCreateRequest());

        CustomerAccountResponse updated = customerAccountController.update(created.id(), validUpdateRequest());

        assertEquals(created.id(), updated.id());
        assertEquals("Acme Freight", updated.companyName());
        assertEquals("CR-54321", updated.companyRegistrationNumber());
        assertEquals("VAT-11111", updated.vatNumber());
        assertEquals("Jeddah 23333", updated.nationalAddress());
        assertEquals("+966598765432", updated.pocContactNumber());
        assertEquals("support@acme.example", updated.pocEmailAddress());
        assertEquals(List.of("billing@acme.example"), updated.commsEmailList());
        assertEquals(AccountStatus.PENDING_FOR_VERIFICATION, updated.accountStatus());
        assertEquals("operations", updated.updatedBy());
        assertNotNull(updated.vatDocument().storageKey());
        assertEquals(created.registrationDocument().storageKey(), updated.registrationDocument().storageKey());
        assertEquals(created.nationalAddressDocument().storageKey(), updated.nationalAddressDocument().storageKey());
    }

    @Test
    void updateCustomerAccountStatusChangesOnlyStatusAndAuditUser() {
        CustomerAccountResponse created = customerAccountController.create(validCreateRequest());

        CustomerAccountResponse updated = customerAccountController.updateStatus(
                created.id(),
                new CustomerAccountStatusUpdateRequest(AccountStatus.VERIFIED, "verifier")
        );

        assertEquals(created.id(), updated.id());
        assertEquals("Acme Logistics", updated.companyName());
        assertEquals(AccountStatus.VERIFIED, updated.accountStatus());
        assertEquals("verifier", updated.updatedBy());
    }

    @Test
    void updateCustomerAccountStatusRejectsUnknownCustomerAccount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> customerAccountController.updateStatus(
                        999L,
                        new CustomerAccountStatusUpdateRequest(AccountStatus.DEACTIVATED, "admin")
                )
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void deleteCustomerAccountRemovesIt() {
        CustomerAccountResponse created = customerAccountController.create(validCreateRequest());

        customerAccountController.delete(created.id());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> customerAccountController.findById(created.id())
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void findByIdRejectsUnknownCustomerAccount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> customerAccountController.findById(999L)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    private CustomerAccountCreateRequest validCreateRequest() {
        CustomerAccountCreateRequest request = new CustomerAccountCreateRequest();
        request.setRegistrationDocument(pdf("registrationDocument", "registration.pdf"));
        request.setVatDocument(pdf("vatDocument", "vat.pdf"));
        request.setNationalAddressDocument(pdf("nationalAddressDocument", "national-address.pdf"));
        request.setCompanyName("Acme Logistics");
        request.setCompanyRegistrationNumber("CR-12345");
        request.setVatNumber("VAT-98765");
        request.setNationalAddress("Riyadh 12211");
        request.setPocContactNumber("0512345678");
        request.setPocEmailAddress("Ops@Acme.example");
        request.setCommsEmailList(List.of("finance@acme.example", "dispatch@acme.example"));
        request.setCreatedBy("admin");
        return request;
    }

    private CustomerAccountUpdateRequest validUpdateRequest() {
        CustomerAccountUpdateRequest request = new CustomerAccountUpdateRequest();
        request.setCompanyName("Acme Freight");
        request.setCompanyRegistrationNumber("CR-54321");
        request.setVatNumber("VAT-11111");
        request.setNationalAddress("Jeddah 23333");
        request.setPocContactNumber("+966598765432");
        request.setPocEmailAddress("Support@Acme.example");
        request.setCommsEmailList(List.of("billing@acme.example", "billing@acme.example"));
        request.setAccountStatus(AccountStatus.PENDING_FOR_VERIFICATION);
        request.setUpdatedBy("operations");
        request.setVatDocument(pdf("vatDocument", "vat-updated.pdf"));
        return request;
    }

    private MockMultipartFile pdf(String partName, String filename) {
        return new MockMultipartFile(partName, filename, "application/pdf", "document".getBytes());
    }
}
