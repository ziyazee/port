package com.porter.ksa.controller;

import com.porter.ksa.dto.DriverAssignVehicleRequest;
import com.porter.ksa.dto.DriverCreateRequest;
import com.porter.ksa.dto.DriverResponse;
import com.porter.ksa.dto.DriverUpdateRequest;
import com.porter.ksa.entity.DriverStatus;
import com.porter.ksa.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public List<DriverResponse> findAll(@RequestParam(required = false) DriverStatus status) {
        return driverService.findAll(status);
    }

    @GetMapping("/{id}")
    public DriverResponse findById(@PathVariable Long id) {
        return driverService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse create(@Valid @RequestBody DriverCreateRequest request) {
        return driverService.create(request);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable Long id, @Valid @RequestBody DriverUpdateRequest request) {
        return driverService.update(id, request);
    }

    @PostMapping("/{id}/assign-vehicle")
    public DriverResponse assignVehicle(
            @PathVariable Long id,
            @Valid @RequestBody DriverAssignVehicleRequest request
    ) {
        return driverService.assignVehicle(id, request);
    }

    @PostMapping("/{id}/unassign-vehicle")
    public DriverResponse unassignVehicle(@PathVariable Long id) {
        return driverService.unassignVehicle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        driverService.delete(id);
    }
}
