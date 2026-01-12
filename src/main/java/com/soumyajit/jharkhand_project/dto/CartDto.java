package com.soumyajit.jharkhand_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private String sessionId;
    private VendorInfoDto vendor;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private Integer totalItems;

    // --- NEW CALCULATED FIELDS ---
    private BigDecimal itemTotal;       // Cost of products only
    private BigDecimal deliveryCharge;  // Calculated delivery fee
    private String deliveryMessage;     // e.g. "Add ₹100 more for free delivery"
    // ----------------------------

    private LocalDateTime expiresAt;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorInfoDto {
        private Long vendorId;
        private String shopName;
        private String vendorPhone;
        private String vendorSlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long itemId;
        private Long productId;
        private String productName;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal priceAtAddition;
        private BigDecimal subtotal;
    }
}
