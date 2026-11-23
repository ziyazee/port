package com.porter.ksa.service;

import com.porter.ksa.dto.CustomerAccountCreateRequest;
import com.porter.ksa.dto.CustomerAccountResponse;
import com.porter.ksa.dto.CustomerAccountStatusUpdateRequest;
import com.porter.ksa.dto.CustomerAccountUpdateRequest;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.DocumentReference;
import com.porter.ksa.mapper.CustomerAccountMapper;
import com.porter.ksa.normalization.CustomerAccountRequestNormalizer;
import com.porter.ksa.normalization.CustomerAccountRequestNormalizer.NormalizedCustomerAccountCreateFields;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.storage.DocumentStorage;
import com.porter.ksa.storage.DocumentType;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerAccountService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAccountService.class);
    private final CustomerAccountRepository customerAccounts;
    private final DocumentStorage documentStorage;
    private final CustomerAccountRequestNormalizer requestNormalizer;
    private final CustomerAccountMapper customerAccountMapper;

    public CustomerAccountService(
            CustomerAccountRepository customerAccounts,
            DocumentStorage documentStorage,
            CustomerAccountRequestNormalizer requestNormalizer,
            CustomerAccountMapper customerAccountMapper
    ) {
        this.customerAccounts = customerAccounts;
        this.documentStorage = documentStorage;
        this.requestNormalizer = requestNormalizer;
        this.customerAccountMapper = customerAccountMapper;
    }

    public CustomerAccountResponse create(CustomerAccountCreateRequest request) {
        NormalizedCustomerAccountCreateFields fields = requestNormalizer.normalizeCreate(request);
        log.atDebug().log("Creating customer account: {}", fields);
        CustomerAccount customerAccount = new CustomerAccount(
                fields.companyName(),
                fields.companyRegistrationNumber(),
                fields.vatNumber(),
                fields.nationalAddress(),
                fields.pocContactNumber(),
                fields.pocEmailAddress(),
                fields.commsEmailList(),
                null,
                null,
                null,
                fields.createdBy()
        );
        CustomerAccount savedCustomerAccount = customerAccounts.saveAndFlush(customerAccount);

        replaceDocumentIfPresent(
                savedCustomerAccount.getId(),
                request.getRegistrationDocument(),
                DocumentType.REGISTRATION,
                savedCustomerAccount::replaceRegistrationDocument
        );
        replaceDocumentIfPresent(
                savedCustomerAccount.getId(),
                request.getVatDocument(),
                DocumentType.VAT,
                savedCustomerAccount::replaceVatDocument
        );
        replaceDocumentIfPresent(
                savedCustomerAccount.getId(),
                request.getNationalAddressDocument(),
                DocumentType.NATIONAL_ADDRESS,
                savedCustomerAccount::replaceNationalAddressDocument
        );

        return customerAccountMapper.toResponse(customerAccounts.save(savedCustomerAccount));
    }

    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> findAll() {
        return customerAccounts.findAllWithCommsEmailList()
                .stream()
                .map(customerAccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerAccountResponse findById(Long id) {
        return customerAccountMapper.toResponse(findAccount(id));
    }

    public CustomerAccountResponse update(Long id, CustomerAccountUpdateRequest request) {
        CustomerAccount customerAccount = findAccount(id);
        customerAccount.updateProfile(requestNormalizer.normalizeUpdate(request));

        replaceDocumentIfPresent(
                customerAccount.getId(),
                request.getRegistrationDocument(),
                DocumentType.REGISTRATION,
                customerAccount::replaceRegistrationDocument
        );
        replaceDocumentIfPresent(
                customerAccount.getId(),
                request.getVatDocument(),
                DocumentType.VAT,
                customerAccount::replaceVatDocument
        );
        replaceDocumentIfPresent(
                customerAccount.getId(),
                request.getNationalAddressDocument(),
                DocumentType.NATIONAL_ADDRESS,
                customerAccount::replaceNationalAddressDocument
        );

        return customerAccountMapper.toResponse(customerAccounts.save(customerAccount));
    }

    public CustomerAccountResponse updateStatus(Long id, CustomerAccountStatusUpdateRequest request) {
        CustomerAccount customerAccount = findAccount(id);
        customerAccount.updateStatus(request.accountStatus(), requestNormalizer.normalizeAuditUser(request.updatedBy()));

        return customerAccountMapper.toResponse(customerAccounts.save(customerAccount));
    }

    public void delete(Long id) {
        CustomerAccount customerAccount = findAccount(id);
        customerAccounts.delete(customerAccount);
    }

    private CustomerAccount findAccount(Long id) {
        return customerAccounts.findByIdWithCommsEmailList(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account not found"));
    }

    private void replaceDocumentIfPresent(
            Long customerAccountId,
            MultipartFile file,
            DocumentType documentType,
            Consumer<DocumentReference> replaceDocument
    ) {
        if (file != null && !file.isEmpty()) {
            replaceDocument.accept(documentStorage.store(customerAccountId, documentType, file));
        }
    }
}
