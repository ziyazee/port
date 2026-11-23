package com.porter.ksa.service;

import com.porter.ksa.config.GpsModuleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GpsPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(GpsPollingScheduler.class);

    private final GpsModuleProperties properties;
    private final GpsTrackingService gpsTrackingService;

    public GpsPollingScheduler(GpsModuleProperties properties, GpsTrackingService gpsTrackingService) {
        this.properties = properties;
        this.gpsTrackingService = gpsTrackingService;
    }

    @Scheduled(fixedDelayString = "${porter.gps.polling-interval-ms:30000}")
    public void pollActiveDevices() {
        if (!properties.isPollingEnabled()) {
            return;
        }

        try {
            gpsTrackingService.pollActiveDevices();
        } catch (ResponseStatusException exception) {
            log.warn("GPS polling skipped: {}", exception.getReason());
        } catch (RuntimeException exception) {
            log.error("GPS polling failed", exception);
        }
    }
}
