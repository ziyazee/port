package com.porter.ksa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "porter.gps")
public class GpsModuleProperties {

    private boolean pollingEnabled = true;
    private boolean trackingSchedulerEnabled = true;
    private long pollingIntervalMs = 30000;
    private boolean autoStartTracking = true;
    private int defaultGeofenceRadiusMeters = 150;
    private double arrivalMaxSpeedKph = 5.0;
    private long maxLocationAgeSeconds = 120;
    private long deliveryMinimumDwellSeconds = 60;

    public boolean isPollingEnabled() {
        return pollingEnabled;
    }

    public void setPollingEnabled(boolean pollingEnabled) {
        this.pollingEnabled = pollingEnabled;
    }

    public boolean isTrackingSchedulerEnabled() {
        return trackingSchedulerEnabled;
    }

    public void setTrackingSchedulerEnabled(boolean trackingSchedulerEnabled) {
        this.trackingSchedulerEnabled = trackingSchedulerEnabled;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public boolean isAutoStartTracking() {
        return autoStartTracking;
    }

    public void setAutoStartTracking(boolean autoStartTracking) {
        this.autoStartTracking = autoStartTracking;
    }

    public int getDefaultGeofenceRadiusMeters() {
        return defaultGeofenceRadiusMeters;
    }

    public void setDefaultGeofenceRadiusMeters(int defaultGeofenceRadiusMeters) {
        this.defaultGeofenceRadiusMeters = defaultGeofenceRadiusMeters;
    }

    public double getArrivalMaxSpeedKph() {
        return arrivalMaxSpeedKph;
    }

    public void setArrivalMaxSpeedKph(double arrivalMaxSpeedKph) {
        this.arrivalMaxSpeedKph = arrivalMaxSpeedKph;
    }

    public long getMaxLocationAgeSeconds() {
        return maxLocationAgeSeconds;
    }

    public void setMaxLocationAgeSeconds(long maxLocationAgeSeconds) {
        this.maxLocationAgeSeconds = maxLocationAgeSeconds;
    }

    public long getDeliveryMinimumDwellSeconds() {
        return deliveryMinimumDwellSeconds;
    }

    public void setDeliveryMinimumDwellSeconds(long deliveryMinimumDwellSeconds) {
        this.deliveryMinimumDwellSeconds = deliveryMinimumDwellSeconds;
    }
}
