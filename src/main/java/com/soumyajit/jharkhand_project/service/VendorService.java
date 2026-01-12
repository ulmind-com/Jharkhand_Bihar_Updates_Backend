package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.VendorDto;
import com.soumyajit.jharkhand_project.dto.VendorRegistrationRequest;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.entity.Vendor;
import com.soumyajit.jharkhand_project.repository.ProductRepository;
import com.soumyajit.jharkhand_project.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    // --- REGISTER VENDOR (Updated for Cover Photo) ---
    @Transactional
    public VendorDto registerVendor(VendorRegistrationRequest request, User user, MultipartFile shopLogo, MultipartFile shopCover) {

        if (vendorRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("User already registered as vendor");
        }

        if (vendorRepository.existsByVendorPhone(request.getVendorPhone())) {
            throw new RuntimeException("Phone number already registered by another vendor");
        }

        String slug = generateUniqueSlug(request.getShopName());

        // Upload Logo
        String logoUrl = null;
        String logoPublicId = null;
        if (shopLogo != null && !shopLogo.isEmpty()) {
            CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(shopLogo);
            logoUrl = uploadResult.getUrl();
            logoPublicId = uploadResult.getPublicId();
        }

        // Upload Cover
        String coverUrl = null;
        String coverPublicId = null;
        if (shopCover != null && !shopCover.isEmpty()) {
            CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(shopCover);
            coverUrl = uploadResult.getUrl();
            coverPublicId = uploadResult.getPublicId();
        }

        Vendor vendor = Vendor.builder()
                .user(user)
                .shopName(request.getShopName())
                .shopDescription(request.getShopDescription())
                .vendorPhone(request.getVendorPhone())
                .vendorSlug(slug)
                .shopLogoUrl(logoUrl)
                .shopLogoPublicId(logoPublicId)
                .shopCoverUrl(coverUrl)          // Make sure Entity has this
                .shopCoverPublicId(coverPublicId) // Make sure Entity has this
                .vendorStatus(Vendor.VendorStatus.PENDING)
                .listingQuota(3)
                .shopAddress(request.getShopAddress())
                // --- ADDED DEFAULT DELIVERY SETTINGS ---
                .deliveryType(Vendor.DeliveryType.FREE) // Default to Free
                .deliveryCharge(0.0)
                .minFreeDeliveryAmount(0.0)
                // ---------------------------------------
                .build();

        vendor = vendorRepository.save(vendor);
        log.info("Vendor registration created for user: {} with slug: {}", user.getEmail(), slug);

        return convertToDto(vendor);
    }

    @Transactional(readOnly = true)
    public VendorDto getVendorByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));
        return convertToDto(vendor);
    }

    @Transactional(readOnly = true)
    public VendorDto getVendorBySlug(String vendorSlug) {
        Vendor vendor = vendorRepository.findByVendorSlug(vendorSlug)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getVendorStatus() != Vendor.VendorStatus.ACCEPTED) {
            throw new RuntimeException("Vendor is not active");
        }

        return convertToDto(vendor);
    }

    @Transactional(readOnly = true)
    public Page<VendorDto> getAllAcceptedVendors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Vendor> vendorPage = vendorRepository.findAllAcceptedVendors(pageable);
        return vendorPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<VendorDto> getAllPendingVendors() {
        return vendorRepository.findAllPendingVendors()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VendorDto approveVendor(Long vendorId, Long adminId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getVendorStatus() != Vendor.VendorStatus.PENDING) {
            throw new RuntimeException("Only pending vendors can be approved");
        }

        vendor.setVendorStatus(Vendor.VendorStatus.ACCEPTED);
        vendor.setApprovedAt(LocalDateTime.now());
        vendor.setApprovedByAdminId(adminId);
        vendor.setRejectionReason(null);

        vendor = vendorRepository.save(vendor);
        log.info("Vendor approved: {} by admin: {}", vendorId, adminId);
        return convertToDto(vendor);
    }

    @Transactional
    public VendorDto rejectVendor(Long vendorId, String reason, Long adminId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getVendorStatus() != Vendor.VendorStatus.PENDING) {
            throw new RuntimeException("Only pending vendors can be rejected");
        }

        vendor.setVendorStatus(Vendor.VendorStatus.REJECTED);
        vendor.setRejectionReason(reason);
        vendor.setApprovedByAdminId(adminId);

        vendor = vendorRepository.save(vendor);
        log.info("Vendor rejected: {} by admin: {}", vendorId, adminId);
        return convertToDto(vendor);
    }

    @Transactional
    public VendorDto updateListingQuota(Long vendorId, Integer newQuota) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (newQuota < 0) throw new RuntimeException("Quota cannot be negative");

        vendor.setListingQuota(newQuota);
        vendor = vendorRepository.save(vendor);
        log.info("Listing quota updated for vendor: {} to {}", vendorId, newQuota);
        return convertToDto(vendor);
    }

    // --- UPDATE TEXT DETAILS ---
    @Transactional
    public VendorDto updateVendorProfileDetails(Long userId, VendorRegistrationRequest request) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        vendor.setShopName(request.getShopName());
        vendor.setShopDescription(request.getShopDescription());
        vendor.setShopAddress(request.getShopAddress());

        if (request.getDeliveryType() != null) {
            vendor.setDeliveryType(request.getDeliveryType());
            vendor.setDeliveryCharge(request.getDeliveryCharge());
            vendor.setMinFreeDeliveryAmount(request.getMinFreeDeliveryAmount());
        }

        if (!vendor.getVendorPhone().equals(request.getVendorPhone())) {
            if (vendorRepository.existsByVendorPhone(request.getVendorPhone())) {
                throw new RuntimeException("Phone number already registered by another vendor");
            }
            vendor.setVendorPhone(request.getVendorPhone());
        }

        vendor = vendorRepository.save(vendor);
        log.info("Vendor details updated for user: {}", userId);
        return convertToDto(vendor);
    }

    // --- UPDATE LOGO ---
    @Transactional
    public VendorDto updateVendorShopLogo(Long userId, MultipartFile newShopLogo) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        if (newShopLogo == null || newShopLogo.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        if (vendor.getShopLogoPublicId() != null) {
            try {
                cloudinaryService.deleteImage(vendor.getShopLogoPublicId());
            } catch (Exception e) {
                log.warn("Failed to delete old logo: {}", e.getMessage());
            }
        }

        CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(newShopLogo);
        vendor.setShopLogoUrl(uploadResult.getUrl());
        vendor.setShopLogoPublicId(uploadResult.getPublicId());

        vendor = vendorRepository.save(vendor);
        log.info("Vendor logo updated for user: {}", userId);
        return convertToDto(vendor);
    }

    // --- UPDATE COVER PHOTO (New Method) ---
    @Transactional
    public VendorDto updateVendorShopCover(Long userId, MultipartFile newShopCover) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        if (newShopCover == null || newShopCover.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        // Delete old cover if exists
        if (vendor.getShopCoverPublicId() != null) {
            try {
                cloudinaryService.deleteImage(vendor.getShopCoverPublicId());
            } catch (Exception e) {
                log.warn("Failed to delete old cover: {}", e.getMessage());
            }
        }

        // Upload new cover
        CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(newShopCover);
        vendor.setShopCoverUrl(uploadResult.getUrl());
        vendor.setShopCoverPublicId(uploadResult.getPublicId());

        vendor = vendorRepository.save(vendor);
        log.info("Vendor cover updated for user: {}", userId);
        return convertToDto(vendor);
    }

    private String generateUniqueSlug(String shopName) {
        String baseSlug = shopName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = baseSlug;
        int counter = 1;
        while (vendorRepository.existsByVendorSlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private VendorDto convertToDto(Vendor vendor) {
        long productsCount = productRepository.countByVendorId(vendor.getId());
        return VendorDto.builder()
                .id(vendor.getId())
                .userId(vendor.getUser().getId())
                .email(vendor.getUser().getEmail())
                .firstName(vendor.getUser().getFirstName())
                .lastName(vendor.getUser().getLastName())
                .profileImageUrl(vendor.getUser().getProfileImageUrl())
                .shopName(vendor.getShopName())
                .shopDescription(vendor.getShopDescription())
                .vendorPhone(vendor.getVendorPhone())
                .shopAddress(vendor.getShopAddress())
                .vendorSlug(vendor.getVendorSlug())
                .shopLogoUrl(vendor.getShopLogoUrl())
                .shopCoverUrl(vendor.getShopCoverUrl())
                // MAP NEW DELIVERY FIELDS
                .deliveryType(vendor.getDeliveryType())
                .deliveryCharge(vendor.getDeliveryCharge())
                .minFreeDeliveryAmount(vendor.getMinFreeDeliveryAmount())
                .listingQuota(vendor.getListingQuota())
                .vendorStatus(vendor.getVendorStatus())
                .approvedAt(vendor.getApprovedAt())
                .rejectionReason(vendor.getRejectionReason())
                .createdAt(vendor.getCreatedAt())
                .productsCount((int) productsCount)
                .build();
    }
}