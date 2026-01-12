package com.soumyajit.jharkhand_project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_public_id")
    private String imagePublicId;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Builder.Default
    @Column(name = "has_discount", nullable = false)
    private Boolean hasDiscount = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 20, nullable = false)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "cart_add_count")
    @Builder.Default
    private Long cartAddCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StockStatus {
        IN_STOCK,
        OUT_OF_STOCK,
        LIMITED_STOCK
    }

    public BigDecimal getEffectivePrice() {
        return hasDiscount && discountedPrice != null ? discountedPrice : originalPrice;
    }
}
