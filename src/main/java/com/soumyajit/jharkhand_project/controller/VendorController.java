package com.soumyajit.jharkhand_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.VendorDto;
import com.soumyajit.jharkhand_project.dto.VendorRegistrationRequest;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/vendors")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
@Slf4j
public class VendorController {

    private final VendorService vendorService;
    private final ObjectMapper objectMapper;

    // --- 1. REGISTER VENDOR (Updated to accept shopCover) ---
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorDto>> registerVendor(
            @RequestPart("vendor") String vendorJson,
            @RequestPart(value = "shopLogo", required = false) MultipartFile shopLogo,
            @RequestPart(value = "shopCover", required = false) MultipartFile shopCover, // Added Cover
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            VendorRegistrationRequest request = objectMapper.readValue(vendorJson, VendorRegistrationRequest.class);

            // Pass both logo and cover to service
            VendorDto vendor = vendorService.registerVendor(request, user, shopLogo, shopCover);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Vendor registration submitted for approval", vendor));
        } catch (Exception e) {
            log.error("Error registering vendor", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- 2. GET PROFILE ---
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<VendorDto>> getMyVendorProfile(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            VendorDto vendor = vendorService.getVendorByUserId(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Vendor profile retrieved", vendor));
        } catch (Exception e) {
            log.error("Error retrieving vendor profile", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- 3. UPDATE DETAILS (Text Only) ---
    @PutMapping("/me/details")
    public ResponseEntity<ApiResponse<VendorDto>> updateMyVendorDetails(
            @RequestBody VendorRegistrationRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            VendorDto vendor = vendorService.updateVendorProfileDetails(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Vendor details updated successfully", vendor));
        } catch (Exception e) {
            log.error("Error updating vendor details", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- 4. UPDATE LOGO ONLY ---
    @PutMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorDto>> updateMyShopLogo(
            @RequestParam("shopLogo") MultipartFile shopLogo,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            VendorDto vendor = vendorService.updateVendorShopLogo(user.getId(), shopLogo);
            return ResponseEntity.ok(ApiResponse.success("Shop logo updated successfully", vendor));
        } catch (Exception e) {
            log.error("Error updating shop logo", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- 5. UPDATE COVER PHOTO ONLY (New Endpoint) ---
    @PutMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorDto>> updateMyShopCover(
            @RequestParam("shopCover") MultipartFile shopCover,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            VendorDto vendor = vendorService.updateVendorShopCover(user.getId(), shopCover);
            return ResponseEntity.ok(ApiResponse.success("Shop cover updated successfully", vendor));
        } catch (Exception e) {
            log.error("Error updating shop cover", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- 6. GET ALL (Public, Paginated) ---
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VendorDto>>> getAllAcceptedVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<VendorDto> vendorPage = vendorService.getAllAcceptedVendors(page, size);
            return ResponseEntity.ok(ApiResponse.success("Vendors retrieved", vendorPage));
        } catch (Exception e) {
            log.error("Error retrieving vendors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve vendors"));
        }
    }

    // --- 7. GET BY SLUG (Public) ---
    @GetMapping("/{vendorSlug}")
    public ResponseEntity<ApiResponse<VendorDto>> getVendorBySlug(@PathVariable String vendorSlug) {
        try {
            VendorDto vendor = vendorService.getVendorBySlug(vendorSlug);
            return ResponseEntity.ok(ApiResponse.success("Vendor retrieved", vendor));
        } catch (Exception e) {
            log.error("Error retrieving vendor by slug: {}", vendorSlug, e);
            return ResponseEntity.notFound().build();
        }
    }
}