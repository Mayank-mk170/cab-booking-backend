
package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.Driver;
import java.time.LocalDateTime;

public class DriverResponse {

    private Long id;
    private Long userId;
    private String licenseNumber;
    private boolean verified;
    private boolean available;
    private Double rating;
    private LocalDateTime createdAt;

    public DriverResponse() {
    }

    public static DriverResponse fromEntity(Driver driver) {
        DriverResponse response = new DriverResponse();

        response.setId(driver.getId());
        response.setUserId(driver.getUser().getId());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setVerified(driver.isVerified());
        response.setAvailable(driver.isAvailable());
        response.setRating(driver.getRating());
        response.setCreatedAt(driver.getCreatedAt());

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Double getRating() { return rating; }
    public void setRating(Double rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
