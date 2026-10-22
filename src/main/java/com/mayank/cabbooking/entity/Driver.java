package com.mayank.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "drivers", schema = "app",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_driver_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_driver_license",
                        columnNames = "license_number")})
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Auth user;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean available = false;

    @Column
    private Double rating = 0.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Driver() {
    }

    public Driver(Auth user, String licenseNumber) {
        this.user = user;
        this.licenseNumber = licenseNumber;
    }

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
    }

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}