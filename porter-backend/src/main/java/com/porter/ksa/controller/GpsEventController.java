package com.porter.ksa.controller;

import com.porter.ksa.dto.GpsEventResponse;
import com.porter.ksa.service.GpsEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gps-events")
public class GpsEventController {

    private final GpsEventService gpsEventService;

    public GpsEventController(GpsEventService gpsEventService) {
        this.gpsEventService = gpsEventService;
    }

    @GetMapping("/tracking-sessions/{trackingSessionId}")
    public List<GpsEventResponse> findByTrackingSession(@PathVariable Long trackingSessionId) {
        return gpsEventService.findByTrackingSession(trackingSessionId);
    }
}
