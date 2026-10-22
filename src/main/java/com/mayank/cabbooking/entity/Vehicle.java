package com.mayank.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vehicles",  schema = "app",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_registration",
                        columnNames = "registration_number"
                )
        })
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "vehicle_type", nullable = false, length = 30)
    private String vehicleType;

    @Column(length = 50)
    private String make;

    @Column(length = 50)
    private String model;

    @Column(length = 30)
    private String color;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (registrationNumber != null) {
            registrationNumber = registrationNumber.trim().toUpperCase();
        }

        if (vehicleType != null) {
            vehicleType = vehicleType.trim().toUpperCase();
        }
    }

    public Vehicle() {
    }


}