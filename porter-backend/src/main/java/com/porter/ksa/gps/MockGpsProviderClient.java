package com.porter.ksa.gps;

import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockGpsProviderClient implements GpsProviderClient {

    private static final double[][] ROUTE = {
            {24.7136, 46.6753},
            {24.7224, 46.6901},
            {24.7358, 46.7068},
            {24.7487, 46.7242},
            {24.7606, 46.7395},
            {24.7741, 46.7561}
    };

    @Override
    public GpsProviderType provider() {
        return GpsProviderType.MOCK;
    }

    @Override
    public List<GpsLocationSnapshot> fetchLatestLocations(List<GpsDevice> devices) {
        Instant now = Instant.now();
        long tick = now.getEpochSecond() / 5;

        return devices.stream()
                .map(device -> locationFor(device, tick, now))
                .toList();
    }

    private GpsLocationSnapshot locationFor(GpsDevice device, long tick, Instant now) {
        int offset = Math.abs(device.getProviderVehicleId().hashCode()) % ROUTE.length;
        long deviceTick = tick + offset;
        int segmentSteps = 6;
        int segmentIndex = (int) ((deviceTick / segmentSteps) % ROUTE.length);
        int nextIndex = (segmentIndex + 1) % ROUTE.length;
        double fraction = (deviceTick % segmentSteps) / (double) segmentSteps;

        double[] start = ROUTE[segmentIndex];
        double[] end = ROUTE[nextIndex];
        double latitude = interpolate(start[0], end[0], fraction);
        double longitude = interpolate(start[1], end[1], fraction);
        double heading = bearingDegrees(start, end);
        double speed = 42 + (deviceTick % 5) * 3;

        return new GpsLocationSnapshot(
                provider(),
                device.getProviderVehicleId(),
                round(latitude),
                round(longitude),
                speed,
                Math.round(heading * 10.0) / 10.0,
                now
        );
    }

    private double interpolate(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }

    private double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    private double bearingDegrees(double[] start, double[] end) {
        double lat1 = Math.toRadians(start[0]);
        double lat2 = Math.toRadians(end[0]);
        double deltaLng = Math.toRadians(end[1] - start[1]);
        double x = Math.sin(deltaLng) * Math.cos(lat2);
        double y = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);
        return (Math.toDegrees(Math.atan2(x, y)) + 360) % 360;
    }
}
