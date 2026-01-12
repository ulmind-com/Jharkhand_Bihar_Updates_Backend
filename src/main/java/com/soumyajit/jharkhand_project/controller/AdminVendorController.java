package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.VendorDto;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/vendors")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RequiredArgsConstructor
@Slf4j
public class AdminVendorController {

    private final VendorService vendorService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<VendorDto>>> getPendingVendors() {
        try {
            List<VendorDto> vendors = vendorService.getAllPendingVendors();
            return ResponseEntity.ok(ApiResponse.success("Pending vendors retrieved", vendors));
        } catch (Exception e) {
            log.error("Error retrieving pending vendors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve pending vendors"));
        }
    }

    @PostMapping("/{vendorId}/approve")
    public ResponseEntity<ApiResponse<VendorDto>> approveVendor(
            @PathVariable Long vendorId,
            Authentication authentication) {

        try {
            User admin = (User) authentication.getPrincipal();
            VendorDto vendor = vendorService.approveVendor(vendorId, admin.getId());
            return ResponseEntity.ok(ApiResponse.success("Vendor approved successfully", vendor));
        } catch (Exception e) {
            log.error("Error approving vendor: {}", vendorId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{vendorId}/reject")
    public ResponseEntity<ApiResponse<VendorDto>> rejectVendor(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        try {
            User admin = (User) authentication.getPrincipal();
            String rejectionReason = reason != null ? reason : "Does not meet requirements";
            VendorDto vendor = vendorService.rejectVendor(vendorId, rejectionReason, admin.getId());
            return ResponseEntity.ok(ApiResponse.success("Vendor rejected", vendor));
        } catch (Exception e) {
            log.error("Error rejecting vendor: {}", vendorId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{vendorId}/quota")
    public ResponseEntity<ApiResponse<VendorDto>> updateListingQuota(
            @PathVariable Long vendorId,
            @RequestParam Integer quota) {

        try {
            VendorDto vendor = vendorService.updateListingQuota(vendorId, quota);
            return ResponseEntity.ok(ApiResponse.success("Listing quota updated", vendor));
        } catch (Exception e) {
            log.error("Error updating listing quota for vendor: {}", vendorId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<VendorDto>>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            // Call the paginated service method
            Page<VendorDto> vendors = vendorService.getAllAcceptedVendors(page, size);

            return ResponseEntity.ok(ApiResponse.success("All vendors retrieved", vendors));
        } catch (Exception e) {
            log.error("Error retrieving all vendors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve vendors"));
        }
    }
}
