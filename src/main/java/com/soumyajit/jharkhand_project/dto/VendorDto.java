package com.soumyajit.jharkhand_project.dto;

import com.soumyajit.jharkhand_project.entity.Vendor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDto {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String shopName;
    private String shopDescription;
    private String vendorPhone;

    private Vendor.DeliveryType deliveryType;
    private Double deliveryCharge;
    private Double minFreeDeliveryAmount;

    private String shopAddress;
    private String vendorSlug;
    private String shopLogoUrl;
    private String shopCoverUrl;
    private Integer listingQuota;
    private Vendor.VendorStatus vendorStatus;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private Integer productsCount;
}
