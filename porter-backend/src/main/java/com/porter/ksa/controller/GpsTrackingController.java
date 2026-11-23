package com.porter.ksa.controller;

import com.porter.ksa.dto.GpsDeviceCreateRequest;
import com.porter.ksa.dto.GpsDeviceResponse;
import com.porter.ksa.dto.GpsPollResponse;
import com.porter.ksa.dto.GpsTrackingResponse;
import com.porter.ksa.dto.GpsUpdateRequest;
import com.porter.ksa.dto.WialonCurrentLocationResponse;
import com.porter.ksa.service.GpsTrackingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gps")
public class GpsTrackingController {

    private final GpsTrackingService gpsTrackingService;

    public GpsTrackingController(GpsTrackingService gpsTrackingService) {
        this.gpsTrackingService = gpsTrackingService;
    }

    @PostMapping("/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public GpsTrackingResponse receive(@Valid @RequestBody GpsUpdateRequest request) {
        return gpsTrackingService.receive(request);
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public GpsDeviceResponse createDevice(@Valid @RequestBody GpsDeviceCreateRequest request) {
        return gpsTrackingService.createDevice(request);
    }

    @GetMapping("/devices")
    public List<GpsDeviceResponse> findDevices() {
        return gpsTrackingService.findDevices();
    }

    @PostMapping("/poll")
    public GpsPollResponse poll() {
        return gpsTrackingService.pollActiveDevices();
    }

    @GetMapping("/wialon/units/{unitId}/current-location")
    public WialonCurrentLocationResponse findCurrentWialonLocation(@PathVariable Long unitId) {
        return gpsTrackingService.findCurrentWialonLocation(unitId);
    }

    @GetMapping("/live")
    public List<GpsTrackingResponse> findLive() {
        return gpsTrackingService.findLive();
    }

    @GetMapping("/live/{vehicleFleetNumber}")
    public GpsTrackingResponse findLiveForVehicle(@PathVariable String vehicleFleetNumber) {
        return gpsTrackingService.findLiveForVehicle(vehicleFleetNumber);
    }

    @GetMapping("/vehicles/{vehicleFleetNumber}/history")
    public List<GpsTrackingResponse> findHistory(
            @PathVariable String vehicleFleetNumber,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return gpsTrackingService.findHistory(vehicleFleetNumber, limit);
    }
}
