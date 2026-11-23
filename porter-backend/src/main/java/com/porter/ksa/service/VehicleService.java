package com.porter.ksa.service;

import com.porter.ksa.dto.VehicleCreateRequest;
import com.porter.ksa.dto.VehicleDriverAssignmentResponse;
import com.porter.ksa.dto.VehicleAvailabilityResponse;
import com.porter.ksa.dto.VehicleResponse;
import com.porter.ksa.dto.VehicleUpdateRequest;
import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleDriverAssignment;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import com.porter.ksa.repository.GpsDeviceRepository;
import com.porter.ksa.repository.VehicleDriverAssignmentRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicles;
    private final VehicleDriverAssignmentRepository driverAssignments;
    private final GpsDeviceRepository gpsDevices;

    public VehicleService(
            VehicleRepository vehicles,
            VehicleDriverAssignmentRepository driverAssignments,
            GpsDeviceRepository gpsDevices
    ) {
        this.vehicles = vehicles;
        this.driverAssignments = driverAssignments;
        this.gpsDevices = gpsDevices;
    }

    public List<VehicleAvailabilityResponse> findAvailability() {
        return Arrays.stream(VehicleType.values())
                .map(this::availabilityFor)
                .toList();
    }

    public List<VehicleResponse> findAll(VehicleType vehicleType) {
        List<Vehicle> vehicleList = vehicleType == null
                ? vehicles.findAllByOrderByIdAsc()
                : vehicles.findTop20ByVehicleTypeOrderByIdAsc(vehicleType);

        return vehicleList
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VehicleResponse findById(Long id) {
        return toResponse(findVehicle(id));
    }

    public long countAvailable(VehicleType vehicleType) {
        return vehicles.countByVehicleTypeAndStatus(vehicleType, VehicleStatus.AVAILABLE);
    }

    @Transactional
    public VehicleResponse create(VehicleCreateRequest request) {
        String fleetNumber = trim(request.fleetNumber());
        String plateNumber = trim(request.plateNumber());
        String driverName = trimToNull(request.driverName());

        if (vehicles.existsByFleetNumber(fleetNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fleet number already exists");
        }
        if (vehicles.existsByPlateNumber(plateNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plate number already exists");
        }

        Vehicle vehicle = vehicles.save(new Vehicle(
                fleetNumber,
                plateNumber,
                request.vehicleType(),
                request.status(),
                driverName
        ));
        syncGpsDevice(vehicle, request.gpsProvider(), request.gpsProviderVehicleId());

        if (driverName != null) {
            driverAssignments.save(new VehicleDriverAssignment(vehicle, driverName));
        }

        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = findVehicle(id);
        String fleetNumber = trim(request.fleetNumber());
        String plateNumber = trim(request.plateNumber());
        String driverName = trimToNull(request.driverName());

        if (vehicles.existsByFleetNumberAndIdNot(fleetNumber, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fleet number already exists");
        }
        if (vehicles.existsByPlateNumberAndIdNot(plateNumber, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plate number already exists");
        }

        if (!Objects.equals(vehicle.getDriverName(), driverName)) {
            driverAssignments.findFirstByVehicleIdAndUnassignedAtIsNullOrderByAssignedAtDesc(id)
                    .ifPresent(VehicleDriverAssignment::close);
            if (driverName != null) {
                driverAssignments.save(new VehicleDriverAssignment(vehicle, driverName));
            }
        }

        vehicle.update(
                fleetNumber,
                plateNumber,
                request.vehicleType(),
                request.status(),
                driverName
        );
        syncGpsDevice(vehicle, request.gpsProvider(), request.gpsProviderVehicleId());

        return toResponse(vehicles.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findVehicle(id);
        driverAssignments.deleteByVehicleId(id);
        vehicles.delete(vehicle);
    }

    private VehicleAvailabilityResponse availabilityFor(VehicleType vehicleType) {
        return new VehicleAvailabilityResponse(
                vehicleType,
                vehicles.countByVehicleTypeAndStatus(vehicleType, VehicleStatus.AVAILABLE),
                vehicles.countByVehicleTypeAndStatus(vehicleType, VehicleStatus.ASSIGNED),
                vehicles.countByVehicleTypeAndStatus(vehicleType, VehicleStatus.MAINTENANCE),
                vehicles.countByVehicleTypeAndStatus(vehicleType, VehicleStatus.INACTIVE)
        );
    }

    private Vehicle findVehicle(Long id) {
        return vehicles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getFleetNumber(),
                vehicle.getPlateNumber(),
                vehicle.getVehicleType(),
                vehicle.getDriverName(),
                vehicle.getStatus(),
                vehicle.getGpsProvider(),
                vehicle.getGpsProviderVehicleId(),
                vehicle.getCurrentLatitude(),
                vehicle.getCurrentLongitude(),
                vehicle.getCurrentSpeedKph(),
                vehicle.getCurrentHeadingDegrees(),
                vehicle.getLastSeenAt(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt(),
                driverAssignments.findByVehicleIdOrderByAssignedAtDesc(vehicle.getId())
                        .stream()
                        .map(this::toDriverAssignmentResponse)
                        .toList()
        );
    }

    private void syncGpsDevice(Vehicle vehicle, GpsProviderType gpsProvider, String gpsProviderVehicleIdValue) {
        String gpsProviderVehicleId = trimToNull(gpsProviderVehicleIdValue);
        if (gpsProvider == null && gpsProviderVehicleId == null) {
            vehicle.updateGpsDevice(null, null);
            return;
        }

        if (gpsProvider == null || gpsProviderVehicleId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GPS provider and GPS provider vehicle id must be provided together"
            );
        }

        if (gpsDevices.existsByProviderAndProviderVehicleIdAndVehicleFleetNumberNot(
                gpsProvider,
                gpsProviderVehicleId,
                vehicle.getFleetNumber()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GPS provider vehicle id already exists");
        }

        GpsDevice gpsDevice = gpsDevices.findByProviderAndProviderVehicleId(gpsProvider, gpsProviderVehicleId)
                .orElseGet(() -> gpsDevices.save(new GpsDevice(
                        gpsProvider,
                        gpsProviderVehicleId,
                        vehicle.getFleetNumber()
                )));
        gpsDevice.updateVehicleFleetNumber(vehicle.getFleetNumber());
        gpsDevices.save(gpsDevice);
        vehicle.updateGpsDevice(gpsProvider, gpsProviderVehicleId);
    }

    private VehicleDriverAssignmentResponse toDriverAssignmentResponse(VehicleDriverAssignment assignment) {
        return new VehicleDriverAssignmentResponse(
                assignment.getId(),
                assignment.getDriverName(),
                assignment.getAssignedAt(),
                assignment.getUnassignedAt()
        );
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
