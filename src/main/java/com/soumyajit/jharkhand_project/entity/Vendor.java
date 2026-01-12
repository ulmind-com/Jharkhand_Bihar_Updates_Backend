package com.soumyajit.jharkhand_project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_vendor_slug", columnList = "vendor_slug"),
        @Index(name = "idx_vendor_status", columnList = "vendor_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "shop_description", length = 1000)
    private String shopDescription;

    @Column(name = "vendor_phone", nullable = false, length = 10)
    private String vendorPhone;

    @Column(name = "vendor_slug", unique = true, nullable = false)
    private String vendorSlug;

    @Column(name = "shop_address", length = 500)
    private String shopAddress;

    // --- LOGO FIELDS ---
    @Column(name = "shop_logo_url")
    private String shopLogoUrl;

    @Column(name = "shop_logo_public_id")
    private String shopLogoPublicId;

    // --- COVER PHOTO FIELDS ---
    @Column(name = "shop_cover_url")
    private String shopCoverUrl;

    @Column(name = "shop_cover_public_id")
    private String shopCoverPublicId;

    // --- DELIVERY SETTINGS (NEW) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", length = 20)
    @Builder.Default
    private DeliveryType deliveryType = DeliveryType.FREE;

    @Column(name = "delivery_charge")
    private Double deliveryCharge;

    @Column(name = "min_free_delivery_amount")
    private Double minFreeDeliveryAmount;
    // -------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_status", length = 20, nullable = false)
    @Builder.Default
    private VendorStatus vendorStatus = VendorStatus.PENDING;

    @Column(name = "listing_quota", nullable = false)
    @Builder.Default
    private Integer listingQuota = 3;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    public enum VendorStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    public enum DeliveryType {
        FREE,
        FIXED,
        CONDITIONAL
    }

    public boolean isAccepted() {
        return vendorStatus == VendorStatus.ACCEPTED;
    }
}