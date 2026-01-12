package com.soumyajit.jharkhand_project.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String productName;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Original price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0.01", message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    private Boolean hasDiscount = false;

    private String stockStatus = "IN_STOCK";
}
