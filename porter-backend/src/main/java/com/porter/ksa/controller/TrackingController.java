package com.porter.ksa.controller;

import com.porter.ksa.dto.TrackingModeUpdateRequest;
import com.porter.ksa.dto.TrackingSessionCreateRequest;
import com.porter.ksa.dto.TrackingSessionResponse;
import com.porter.ksa.service.TrackingLiveUpdateService;
import com.porter.ksa.service.TrackingSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingSessionService trackingSessionService;
    private final TrackingLiveUpdateService liveUpdateService;

    public TrackingController(
            TrackingSessionService trackingSessionService,
            TrackingLiveUpdateService liveUpdateService
    ) {
        this.trackingSessionService = trackingSessionService;
        this.liveUpdateService = liveUpdateService;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackingSessionResponse start(@Valid @RequestBody TrackingSessionCreateRequest request) {
        return trackingSessionService.start(request);
    }

    @GetMapping("/sessions/active")
    public List<TrackingSessionResponse> findActive() {
        return trackingSessionService.findActive();
    }

    @GetMapping("/order-vehicles/{orderVehicleId}/session")
    public TrackingSessionResponse findByOrderVehicle(@PathVariable Long orderVehicleId) {
        return trackingSessionService.findByOrderVehicle(orderVehicleId);
    }

    @PatchMapping("/sessions/{id}/mode")
    public TrackingSessionResponse updateMode(
            @PathVariable Long id,
            @Valid @RequestBody TrackingModeUpdateRequest request
    ) {
        return trackingSessionService.updateMode(id, request);
    }

    @PostMapping("/sessions/{id}/stop")
    public TrackingSessionResponse stop(@PathVariable Long id) {
        return trackingSessionService.stop(id);
    }

    @GetMapping("/live")
    public SseEmitter live() {
        return liveUpdateService.subscribe();
    }
}
