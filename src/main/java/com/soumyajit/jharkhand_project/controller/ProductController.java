package com.soumyajit.jharkhand_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.CreateProductRequest;
import com.soumyajit.jharkhand_project.dto.ProductDto;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart("productImage") MultipartFile productImage,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            CreateProductRequest request = objectMapper.readValue(productJson, CreateProductRequest.class);

            ProductDto product = productService.createProduct(request, productImage, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Product created successfully", product));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllActiveProducts() {
        try {
            List<ProductDto> products = productService.getAllActiveProducts();
            return ResponseEntity.ok(ApiResponse.success("Products retrieved", products));
        } catch (Exception e) {
            log.error("Error retrieving products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve products"));
        }
    }

    @GetMapping("/discounted")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getDiscountedProducts() {
        try {
            List<ProductDto> products = productService.getDiscountedProducts();
            return ResponseEntity.ok(ApiResponse.success("Discounted products retrieved", products));
        } catch (Exception e) {
            log.error("Error retrieving discounted products", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve discounted products"));
        }
    }

    @GetMapping("/vendor/{vendorSlug}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductsByVendor(@PathVariable String vendorSlug) {
        try {
            List<ProductDto> products = productService.getProductsByVendorSlug(vendorSlug);
            return ResponseEntity.ok(ApiResponse.success("Vendor products retrieved", products));
        } catch (Exception e) {
            log.error("Error retrieving products for vendor: {}", vendorSlug, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve vendor products"));
        }
    }

    @GetMapping("/my-products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getMyProducts(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<ProductDto> products = productService.getMyProducts(user);
            return ResponseEntity.ok(ApiResponse.success("Your products retrieved", products));
        } catch (Exception e) {
            log.error("Error retrieving vendor's products", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable Long productId) {
        try {
            ProductDto product = productService.getProductById(productId);
            return ResponseEntity.ok(ApiResponse.success("Product retrieved", product));
        } catch (Exception e) {
            log.error("Error retrieving product: {}", productId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long productId,
            @RequestBody CreateProductRequest request, // Changed from RequestPart to RequestBody
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();

            // Image parameter remove kar diya, sirf text data pass hoga
            ProductDto product = productService.updateProduct(productId, request, user);

            return ResponseEntity.ok(ApiResponse.success("Product updated successfully (Image unchanged)", product));
        } catch (Exception e) {
            log.error("Error updating product: {}", productId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            productService.deleteProduct(productId, user);
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting product: {}", productId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{productId}/toggle-status")
    public ResponseEntity<ApiResponse<ProductDto>> toggleProductStatus(
            @PathVariable Long productId,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            ProductDto product = productService.toggleProductStatus(productId, user);
            return ResponseEntity.ok(ApiResponse.success("Product status toggled", product));
        } catch (Exception e) {
            log.error("Error toggling product status: {}", productId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
