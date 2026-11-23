package com.porter.ksa.controller;

import com.porter.ksa.dto.VehicleAvailabilitySearchResponse;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.service.VehicleAvailabilityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle-availability")
public class VehicleAvailabilityController {

    private final VehicleAvailabilityService availabilityService;

    public VehicleAvailabilityController(VehicleAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<VehicleAvailabilitySearchResponse> search(
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) Double pickupLatitude,
            @RequestParam(required = false) Double pickupLongitude,
            @RequestParam(defaultValue = "true") boolean includeAssigned
    ) {
        return availabilityService.search(vehicleType, pickupLatitude, pickupLongitude, includeAssigned);
    }
}
