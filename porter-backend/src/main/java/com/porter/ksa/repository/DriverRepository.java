package com.porter.ksa.repository;

import com.porter.ksa.entity.Driver;
import com.porter.ksa.entity.DriverStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    List<Driver> findAllByOrderByIdAsc();

    List<Driver> findByStatusOrderByIdAsc(DriverStatus status);

    Optional<Driver> findByCurrentVehicle_Id(Long vehicleId);
}
