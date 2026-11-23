package com.porter.ksa.controller;

import com.porter.ksa.dto.CustomerAccountCreateRequest;
import com.porter.ksa.dto.CustomerAccountResponse;
import com.porter.ksa.dto.CustomerAccountStatusUpdateRequest;
import com.porter.ksa.dto.CustomerAccountUpdateRequest;
import com.porter.ksa.service.CustomerAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-accounts")
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;

    public CustomerAccountController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAccountResponse create(@Valid @ModelAttribute CustomerAccountCreateRequest request) {
        return customerAccountService.create(request);
    }

    @GetMapping
    public List<CustomerAccountResponse> findAll() {
        return customerAccountService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerAccountResponse findById(@PathVariable Long id) {
        return customerAccountService.findById(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomerAccountResponse update(
            @PathVariable Long id,
            @Valid @ModelAttribute CustomerAccountUpdateRequest request
    ) {
        return customerAccountService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public CustomerAccountResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerAccountStatusUpdateRequest request
    ) {
        return customerAccountService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerAccountService.delete(id);
    }
}
