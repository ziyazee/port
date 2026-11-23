package com.porter.ksa.service;

import com.porter.ksa.dto.GpsDeviceCreateRequest;
import com.porter.ksa.dto.GpsDeviceResponse;
import com.porter.ksa.dto.GpsPollResponse;
import com.porter.ksa.dto.GpsTrackingResponse;
import com.porter.ksa.dto.GpsUpdateRequest;
import com.porter.ksa.dto.MapDetailsResponse;
import com.porter.ksa.dto.WialonCurrentLocationResponse;
import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsLatestLocation;
import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.gps.GpsLocationSnapshot;
import com.porter.ksa.gps.GpsProviderClient;
import com.porter.ksa.gps.WialonCurrentLocation;
import com.porter.ksa.gps.WialonGpsProviderClient;
import com.porter.ksa.maps.GoogleMapsReverseGeocodingClient;
import com.porter.ksa.repository.GpsDeviceRepository;
import com.porter.ksa.repository.GpsLatestLocationRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GpsTrackingService {

    private static final Logger log = LoggerFactory.getLogger(GpsTrackingService.class);
    private final GpsDeviceRepository gpsDevices;
    private final GpsLatestLocationRepository latestLocations;
    private final VehicleRepository vehicles;
    private final Map<GpsProviderType, GpsProviderClient> providerClients;
    private final GoogleMapsReverseGeocodingClient googleMaps;

    public GpsTrackingService(
            GpsDeviceRepository gpsDevices,
            GpsLatestLocationRepository latestLocations,
            VehicleRepository vehicles,
            List<GpsProviderClient> providerClients,
            GoogleMapsReverseGeocodingClient googleMaps
    ) {
        this.gpsDevices = gpsDevices;
        this.latestLocations = latestLocations;
        this.vehicles = vehicles;
        this.googleMaps = googleMaps;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(GpsProviderClient::provider, Function.identity()));
    }

    public GpsDeviceResponse createDevice(GpsDeviceCreateRequest request) {
        GpsProviderType provider = request.provider();
        String providerVehicleId = requireText(request.providerVehicleId(), "Provider vehicle id is required");
        String vehicleFleetNumber = trimToNull(request.vehicleFleetNumber());

        if (gpsDevices.existsByProviderAndProviderVehicleId(provider, providerVehicleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GPS device already exists");
        }

        GpsDevice device = gpsDevices.save(new GpsDevice(provider, providerVehicleId, vehicleFleetNumber));
        syncVehicleGpsDevice(device);
        return toDeviceResponse(device);
    }

    @Transactional(readOnly = true)
    public List<GpsDeviceResponse> findDevices() {
        return gpsDevices.findAll()
                .stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    public GpsPollResponse pollActiveDevices() {
        List<GpsDevice> activeDevices = gpsDevices.findByActiveTrueOrderByIdAsc();
        int locationsUpdated = 0;

        Map<GpsProviderType, List<GpsDevice>> devicesByProvider = activeDevices.stream()
                .collect(Collectors.groupingBy(GpsDevice::getProvider));

        for (Map.Entry<GpsProviderType, List<GpsDevice>> entry : devicesByProvider.entrySet()) {
            GpsProviderClient providerClient = providerClients.get(entry.getKey());
            if (providerClient == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GPS provider is not configured");
            }

            List<GpsLocationSnapshot> snapshots;
            try {
                snapshots = providerClient.fetchLatestLocations(entry.getValue());
            } catch (UnsupportedOperationException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            }

            for (GpsLocationSnapshot snapshot : snapshots) {
                upsertLatestLocation(snapshot);
                locationsUpdated++;
            }
        }

        return new GpsPollResponse(activeDevices.size(), locationsUpdated, Instant.now());
    }

    public WialonCurrentLocationResponse findCurrentWialonLocation(Long unitId) {
        GpsProviderClient providerClient = providerClients.get(GpsProviderType.WIALON);
        if (!(providerClient instanceof WialonGpsProviderClient wialonClient)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wialon GPS provider is not configured");
        }

        WialonCurrentLocation location = wialonClient.fetchCurrentLocationByUnitId(unitId);
        gpsDevices.findByProviderAndProviderVehicleId(GpsProviderType.WIALON, String.valueOf(location.wialonUnitId()))
                .ifPresent(device -> upsertLatestLocation(location.toGpsLocationSnapshot()));

        MapDetailsResponse mapDetails = googleMaps.reverseGeocode(location.latitude(), location.longitude())
                .orElse(null);
        return toWialonResponse(location, mapDetails);
    }

    public GpsTrackingResponse receive(GpsUpdateRequest request) {
        String vehicleFleetNumber = requireText(request.vehicleFleetNumber(), "Vehicle fleet number is required");
        GpsDevice device = gpsDevices.findFirstByVehicleFleetNumberOrderByIdAsc(vehicleFleetNumber)
                .orElseGet(() -> gpsDevices.save(new GpsDevice(
                        GpsProviderType.MOCK,
                        vehicleFleetNumber,
                        vehicleFleetNumber
                )));

        GpsLocationSnapshot snapshot = new GpsLocationSnapshot(
                device.getProvider(),
                device.getProviderVehicleId(),
                request.latitude(),
                request.longitude(),
                request.speedKph(),
                request.headingDegrees(),
                request.recordedAt() == null ? Instant.now() : request.recordedAt()
        );
        GpsTrackingResponse response = toResponse(upsertLatestLocation(snapshot));

        log.info(
                "GPS update received: vehicle={} lat={} lng={} speedKph={} heading={} recordedAt={}",
                vehicleFleetNumber,
                request.latitude(),
                request.longitude(),
                request.speedKph(),
                request.headingDegrees(),
                snapshot.recordedAt()
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<GpsTrackingResponse> findLive() {
        return latestLocations.findAllByOrderByGpsDeviceVehicleFleetNumberAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GpsTrackingResponse findLiveForVehicle(String vehicleFleetNumber) {
        GpsDevice device = gpsDevices.findFirstByVehicleFleetNumberOrderByIdAsc(vehicleFleetNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No GPS device found for vehicle"));

        return latestLocations.findByGpsDeviceId(device.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No GPS data found for vehicle"));
    }

    @Transactional(readOnly = true)
    public List<GpsTrackingResponse> findHistory(String vehicleFleetNumber, int limit) {
        return List.of(findLiveForVehicle(vehicleFleetNumber));
    }

    private GpsLatestLocation upsertLatestLocation(GpsLocationSnapshot snapshot) {
        GpsDevice device = gpsDevices.findByProviderAndProviderVehicleId(
                        snapshot.provider(),
                        snapshot.providerVehicleId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GPS device not found"));
        Instant receivedAt = Instant.now();
        GpsLatestLocation location = latestLocations.findByGpsDeviceId(device.getId())
                .orElseGet(() -> new GpsLatestLocation(
                        device,
                        snapshot.latitude(),
                        snapshot.longitude(),
                        snapshot.speedKph(),
                        snapshot.headingDegrees(),
                        snapshot.recordedAt(),
                        receivedAt
                ));

        location.update(
                snapshot.latitude(),
                snapshot.longitude(),
                snapshot.speedKph(),
                snapshot.headingDegrees(),
                snapshot.recordedAt(),
                receivedAt
        );

        GpsLatestLocation savedLocation = latestLocations.save(location);
        syncVehicleLatestLocation(savedLocation);
        return savedLocation;
    }

    private void syncVehicleGpsDevice(GpsDevice device) {
        if (device.getVehicleFleetNumber() == null || device.getVehicleFleetNumber().isBlank()) {
            return;
        }

        vehicles.findByFleetNumber(device.getVehicleFleetNumber())
                .ifPresent(vehicle -> {
                    vehicle.updateGpsDevice(device.getProvider(), device.getProviderVehicleId());
                    vehicles.save(vehicle);
                });
    }

    private void syncVehicleLatestLocation(GpsLatestLocation location) {
        GpsDevice device = location.getGpsDevice();
        if (device.getVehicleFleetNumber() == null || device.getVehicleFleetNumber().isBlank()) {
            return;
        }

        vehicles.findByFleetNumber(device.getVehicleFleetNumber())
                .ifPresent(vehicle -> {
                    vehicle.updateGpsDevice(device.getProvider(), device.getProviderVehicleId());
                    vehicle.updateLatestLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getSpeedKph(),
                            location.getHeadingDegrees(),
                            location.getProviderRecordedAt()
                    );
                    vehicles.save(vehicle);
                });
    }

    private GpsTrackingResponse toResponse(GpsLatestLocation location) {
        GpsDevice device = location.getGpsDevice();
        return new GpsTrackingResponse(
                location.getId(),
                device.getId(),
                device.getProvider(),
                device.getProviderVehicleId(),
                device.getVehicleFleetNumber(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeedKph(),
                location.getHeadingDegrees(),
                location.getProviderRecordedAt(),
                location.getReceivedAt()
        );
    }

    private WialonCurrentLocationResponse toWialonResponse(
            WialonCurrentLocation location,
            MapDetailsResponse mapDetails
    ) {
        return new WialonCurrentLocationResponse(
                GpsProviderType.WIALON,
                location.wialonUnitId(),
                location.unitName(),
                location.latitude(),
                location.longitude(),
                location.speedKph(),
                location.headingDegrees(),
                location.recordedAt(),
                location.receivedAt(),
                mapDetails
        );
    }

    private GpsDeviceResponse toDeviceResponse(GpsDevice device) {
        return new GpsDeviceResponse(
                device.getId(),
                device.getProvider(),
                device.getProviderVehicleId(),
                device.getVehicleFleetNumber(),
                device.isActive(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
