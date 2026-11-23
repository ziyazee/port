package com.porter.ksa.service;

import com.porter.ksa.dto.DriverAssignVehicleRequest;
import com.porter.ksa.dto.DriverCreateRequest;
import com.porter.ksa.dto.DriverResponse;
import com.porter.ksa.dto.DriverUpdateRequest;
import com.porter.ksa.entity.Driver;
import com.porter.ksa.entity.DriverStatus;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.repository.DriverRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository drivers;
    private final VehicleRepository vehicles;

    public DriverService(DriverRepository drivers, VehicleRepository vehicles) {
        this.drivers = drivers;
        this.vehicles = vehicles;
    }

    public List<DriverResponse> findAll(DriverStatus status) {
        List<Driver> driverList = status == null
                ? drivers.findAllByOrderByIdAsc()
                : drivers.findByStatusOrderByIdAsc(status);

        return driverList.stream()
                .map(this::toResponse)
                .toList();
    }

    public DriverResponse findById(Long id) {
        return toResponse(findDriver(id));
    }

    @Transactional
    public DriverResponse create(DriverCreateRequest request) {
        String fullName = requireText(request.fullName(), "Driver name is required");
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String nationalId = trimToNull(request.nationalId());
        String licenseNumber = trimToNull(request.licenseNumber());

        validateUniqueValues(null, phoneNumber, nationalId, licenseNumber);

        Driver driver = drivers.save(new Driver(
                fullName,
                phoneNumber,
                nationalId,
                licenseNumber,
                request.status()
        ));

        return toResponse(driver);
    }

    @Transactional
    public DriverResponse update(Long id, DriverUpdateRequest request) {
        Driver driver = findDriver(id);
        String fullName = requireText(request.fullName(), "Driver name is required");
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String nationalId = trimToNull(request.nationalId());
        String licenseNumber = trimToNull(request.licenseNumber());

        validateUniqueValues(id, phoneNumber, nationalId, licenseNumber);

        driver.update(fullName, phoneNumber, nationalId, licenseNumber, request.status());
        if (driver.getCurrentVehicle() != null) {
            driver.getCurrentVehicle().updateDriverName(driver.getFullName());
        }

        return toResponse(drivers.save(driver));
    }

    @Transactional
    public DriverResponse assignVehicle(Long id, DriverAssignVehicleRequest request) {
        Driver driver = findDriver(id);
        if (driver.getStatus() == DriverStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blocked driver cannot be assigned");
        }

        Vehicle vehicle = vehicles.findById(request.vehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        if (driver.getCurrentVehicle() != null && !driver.getCurrentVehicle().getId().equals(vehicle.getId())) {
            driver.getCurrentVehicle().updateDriverName(null);
        }

        drivers.findAll().stream()
                .filter(other -> !other.getId().equals(driver.getId()))
                .filter(other -> other.getCurrentVehicle() != null)
                .filter(other -> other.getCurrentVehicle().getId().equals(vehicle.getId()))
                .forEach(Driver::unassignVehicle);

        driver.assignToVehicle(vehicle);
        vehicle.updateDriverName(driver.getFullName());

        return toResponse(drivers.save(driver));
    }

    @Transactional
    public DriverResponse unassignVehicle(Long id) {
        Driver driver = findDriver(id);
        if (driver.getCurrentVehicle() != null) {
            driver.getCurrentVehicle().updateDriverName(null);
        }
        driver.unassignVehicle();

        return toResponse(drivers.save(driver));
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = findDriver(id);
        if (driver.getCurrentVehicle() != null) {
            driver.getCurrentVehicle().updateDriverName(null);
        }
        drivers.delete(driver);
    }

    private Driver findDriver(Long id) {
        return drivers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    private void validateUniqueValues(Long id, String phoneNumber, String nationalId, String licenseNumber) {
        if (id == null ? drivers.existsByPhoneNumber(phoneNumber) : drivers.existsByPhoneNumberAndIdNot(phoneNumber, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver phone number already exists");
        }

        if (nationalId != null
                && (id == null ? drivers.existsByNationalId(nationalId) : drivers.existsByNationalIdAndIdNot(nationalId, id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver national id already exists");
        }

        if (licenseNumber != null
                && (id == null ? drivers.existsByLicenseNumber(licenseNumber) : drivers.existsByLicenseNumberAndIdNot(licenseNumber, id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver license number already exists");
        }
    }

    private DriverResponse toResponse(Driver driver) {
        Vehicle currentVehicle = driver.getCurrentVehicle();
        return new DriverResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getPhoneNumber(),
                driver.getNationalId(),
                driver.getLicenseNumber(),
                driver.getStatus(),
                currentVehicle == null ? null : currentVehicle.getId(),
                currentVehicle == null ? null : currentVehicle.getFleetNumber(),
                currentVehicle == null ? null : currentVehicle.getPlateNumber(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String trimmed = requireText(phoneNumber, "Driver phone number is required");
        if (trimmed.startsWith("05")) {
            return "+966" + trimmed.substring(1);
        }

        return trimmed;
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
