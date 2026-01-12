package com.soumyajit.jharkhand_project.dto;

import com.soumyajit.jharkhand_project.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private String shopName;
    private String vendorSlug;
    private String vendorPhone;
    private String productName;
    private String description;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Boolean hasDiscount;
    private BigDecimal effectivePrice;
    private Product.StockStatus stockStatus;
    private Boolean isActive;
    private Long viewCount;
    private Long cartAddCount;
    private LocalDateTime createdAt;
}
