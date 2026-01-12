package com.soumyajit.jharkhand_project.dto;

import com.soumyajit.jharkhand_project.entity.Vendor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VendorRegistrationRequest {

    @NotBlank(message = "Shop name is required")
    @Size(min = 3, max = 100, message = "Shop name must be between 3 and 100 characters")
    private String shopName;

    @Size(max = 1000, message = "Shop description cannot exceed 1000 characters")
    private String shopDescription;

    @NotBlank(message = "WhatsApp phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String vendorPhone;

    private String shopAddress;

    private Vendor.DeliveryType deliveryType;
    private Double deliveryCharge;
    private Double minFreeDeliveryAmount;
}
