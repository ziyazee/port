package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class TransportOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private CustomerAccount customerAccount;

    @Column(nullable = false, length = 500)
    private String fromLocation;

    @Column(nullable = false, length = 500)
    private String toLocation;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double dropLatitude;

    private Double dropLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 1000)
    private String loadDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CargoSpecification cargoSpecification;

    @Column(length = 80)
    private String cargoHeight;

    @Column(length = 80)
    private String cargoWeight;

    @Column(length = 120)
    private String cargoDimensions;

    @Column(nullable = false)
    private int quantityUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus orderStatus = OrderStatus.CREATED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    protected TransportOrder() {
    }

    public TransportOrder(
            CustomerAccount customerAccount,
            String fromLocation,
            String toLocation,
            Double pickupLatitude,
            Double pickupLongitude,
            Double dropLatitude,
            Double dropLongitude,
            VehicleType vehicleType,
            String loadDescription,
            CargoSpecification cargoSpecification,
            String cargoHeight,
            String cargoWeight,
            String cargoDimensions,
            int quantityUnits,
            String createdBy
    ) {
        this.customerAccount = customerAccount;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropLatitude = dropLatitude;
        this.dropLongitude = dropLongitude;
        this.vehicleType = vehicleType;
        this.loadDescription = loadDescription;
        this.cargoSpecification = cargoSpecification;
        this.cargoHeight = cargoHeight;
        this.cargoWeight = cargoWeight;
        this.cargoDimensions = cargoDimensions;
        this.quantityUnits = quantityUnits;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public TransportOrder(
            CustomerAccount customerAccount,
            String fromLocation,
            String toLocation,
            VehicleType vehicleType,
            String loadDescription,
            CargoSpecification cargoSpecification,
            String cargoHeight,
            String cargoWeight,
            String cargoDimensions,
            int quantityUnits,
            String createdBy
    ) {
        this(
                customerAccount,
                fromLocation,
                toLocation,
                null,
                null,
                null,
                null,
                vehicleType,
                loadDescription,
                cargoSpecification,
                cargoHeight,
                cargoWeight,
                cargoDimensions,
                quantityUnits,
                createdBy
        );
    }

    @PrePersist
    void beforeCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public Double getDropLatitude() {
        return dropLatitude;
    }

    public Double getDropLongitude() {
        return dropLongitude;
    }

    public boolean hasTrackingCoordinates() {
        return pickupLatitude != null
                && pickupLongitude != null
                && dropLatitude != null
                && dropLongitude != null;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getLoadDescription() {
        return loadDescription;
    }

    public CargoSpecification getCargoSpecification() {
        return cargoSpecification;
    }

    public String getCargoHeight() {
        return cargoHeight;
    }

    public String getCargoWeight() {
        return cargoWeight;
    }

    public String getCargoDimensions() {
        return cargoDimensions;
    }

    public int getQuantityUnits() {
        return quantityUnits;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void updateDetails(
            CustomerAccount customerAccount,
            String fromLocation,
            String toLocation,
            Double pickupLatitude,
            Double pickupLongitude,
            Double dropLatitude,
            Double dropLongitude,
            VehicleType vehicleType,
            String loadDescription,
            CargoSpecification cargoSpecification,
            String cargoHeight,
            String cargoWeight,
            String cargoDimensions,
            int quantityUnits,
            String updatedBy
    ) {
        this.customerAccount = customerAccount;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropLatitude = dropLatitude;
        this.dropLongitude = dropLongitude;
        this.vehicleType = vehicleType;
        this.loadDescription = loadDescription;
        this.cargoSpecification = cargoSpecification;
        this.cargoHeight = cargoHeight;
        this.cargoWeight = cargoWeight;
        this.cargoDimensions = cargoDimensions;
        this.quantityUnits = quantityUnits;
        this.updatedBy = updatedBy;
    }

    public void updateStatus(OrderStatus orderStatus, String updatedBy) {
        this.orderStatus = orderStatus;
        this.updatedBy = updatedBy;
    }
}
