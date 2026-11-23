package com.porter.ksa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.porter.ksa.controller.CustomerAccountController;
import com.porter.ksa.controller.DocumentController;
import com.porter.ksa.controller.OrderController;
import com.porter.ksa.dto.CustomerAccountCreateRequest;
import com.porter.ksa.dto.CustomerAccountResponse;
import com.porter.ksa.dto.DocumentRecordResponse;
import com.porter.ksa.dto.OrderCreateRequest;
import com.porter.ksa.dto.OrderResponse;
import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.DocumentRecordRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = "porter.document-storage.local.base-path=${java.io.tmpdir}/porter-test-documents")
class DocumentControllerTests {

    @Autowired
    private DocumentController documentController;

    @Autowired
    private CustomerAccountController customerAccountController;

    @Autowired
    private OrderController orderController;

    @Autowired
    private DocumentRecordRepository documents;

    @Autowired
    private TransportOrderRepository orders;

    @Autowired
    private CustomerAccountRepository customerAccounts;

    @BeforeEach
    void resetData() {
        deleteTestData();
    }

    @AfterEach
    void cleanUpData() {
        deleteTestData();
    }

    private void deleteTestData() {
        documents.deleteAll();
        orders.deleteAll();
        customerAccounts.deleteAll();
    }

    @Test
    void uploadListsAndDeletesCustomerDocument() {
        CustomerAccountResponse customer = customerAccountController.create(validCustomerRequest());

        DocumentRecordResponse uploaded = documentController.upload(
                DocumentOwnerType.CUSTOMER_ACCOUNT,
                customer.id(),
                "PURCHASE_ORDER",
                "operations",
                pdf("file", "purchase-order.pdf")
        );

        assertNotNull(uploaded.id());
        assertEquals(DocumentOwnerType.CUSTOMER_ACCOUNT, uploaded.ownerType());
        assertEquals(customer.id(), uploaded.ownerId());
        assertEquals("PURCHASE_ORDER", uploaded.documentType());
        assertEquals("purchase-order.pdf", uploaded.originalFilename());
        assertEquals("operations", uploaded.uploadedBy());
        assertTrue(uploaded.storageKey().startsWith("documents/customer-account/" + customer.id() + "/PURCHASE_ORDER/"));

        List<DocumentRecordResponse> found = documentController.findByOwner(DocumentOwnerType.CUSTOMER_ACCOUNT, customer.id());

        assertEquals(1, found.size());
        assertEquals(uploaded.id(), found.get(0).id());

        documentController.delete(uploaded.id());

        assertTrue(documentController.findByOwner(DocumentOwnerType.CUSTOMER_ACCOUNT, customer.id()).isEmpty());
    }

    @Test
    void uploadListsOrderDocument() {
        CustomerAccountResponse customer = customerAccountController.create(validCustomerRequest());
        OrderResponse order = orderController.create(new OrderCreateRequest(
                customer.id(),
                "Dammam Industrial",
                "Riyadh Dry Port",
                null,
                null,
                null,
                null,
                VehicleType.LOWBED,
                "Steel pipes",
                CargoSpecification.REGULAR_CARGO,
                "0",
                "0",
                "0",
                1,
                List.of(),
                "operations"
        ));

        DocumentRecordResponse uploaded = documentController.upload(
                DocumentOwnerType.ORDER,
                order.id(),
                "CUSTOMER_REQUEST",
                "operations",
                pdf("file", "whatsapp-request.pdf")
        );

        assertEquals(DocumentOwnerType.ORDER, uploaded.ownerType());
        assertEquals(order.id(), uploaded.ownerId());
        assertEquals("CUSTOMER_REQUEST", uploaded.documentType());
        assertTrue(uploaded.storageKey().startsWith("documents/order/" + order.id() + "/CUSTOMER_REQUEST/"));

        List<DocumentRecordResponse> found = documentController.findByOwner(DocumentOwnerType.ORDER, order.id());

        assertEquals(1, found.size());
        assertEquals(uploaded.id(), found.get(0).id());
    }

    private CustomerAccountCreateRequest validCustomerRequest() {
        CustomerAccountCreateRequest request = new CustomerAccountCreateRequest();
        request.setRegistrationDocument(pdf("registrationDocument", "registration.pdf"));
        request.setVatDocument(pdf("vatDocument", "vat.pdf"));
        request.setNationalAddressDocument(pdf("nationalAddressDocument", "national-address.pdf"));
        request.setCompanyName("Acme Logistics");
        request.setCompanyRegistrationNumber("CR-12345");
        request.setVatNumber("VAT-98765");
        request.setNationalAddress("Riyadh 12211");
        request.setPocContactNumber("0512345678");
        request.setPocEmailAddress("ops@acme.example");
        request.setCommsEmailList(List.of("finance@acme.example"));
        request.setCreatedBy("admin");
        return request;
    }

    private MockMultipartFile pdf(String partName, String filename) {
        return new MockMultipartFile(partName, filename, "application/pdf", "document".getBytes());
    }
}
