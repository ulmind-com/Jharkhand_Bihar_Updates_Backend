package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.CreateProductRequest;
import com.soumyajit.jharkhand_project.dto.ProductDto;
import com.soumyajit.jharkhand_project.entity.Product;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.entity.Vendor;
import com.soumyajit.jharkhand_project.repository.CartItemRepository;
import com.soumyajit.jharkhand_project.repository.ProductRepository;
import com.soumyajit.jharkhand_project.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final CloudinaryService cloudinaryService;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public ProductDto createProduct(CreateProductRequest request, MultipartFile productImage, User user) {

        // Get vendor profile
        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        // Check if vendor is accepted
        if (vendor.getVendorStatus() != Vendor.VendorStatus.ACCEPTED) {
            throw new RuntimeException("Only accepted vendors can add products");
        }

        // Check listing quota
        long currentProductCount = productRepository.countByVendorId(vendor.getId());
        if (currentProductCount >= vendor.getListingQuota()) {
            throw new RuntimeException("Product listing quota exceeded. Current limit: " + vendor.getListingQuota());
        }

        // Validate image
        if (productImage == null || productImage.isEmpty()) {
            throw new RuntimeException("Product image is required");
        }

        // Upload image to Cloudinary
        CloudinaryService.CloudinaryUploadResult uploadResult = cloudinaryService.uploadImageWithPublicId(productImage);

        // Validate discount
        if (request.getHasDiscount() && request.getDiscountedPrice() != null) {
            if (request.getDiscountedPrice().compareTo(request.getOriginalPrice()) >= 0) {
                throw new RuntimeException("Discounted price must be less than original price");
            }
        }

        // Create product
        Product product = Product.builder()
                .vendor(vendor)
                .productName(request.getProductName())
                .description(request.getDescription())
                .imageUrl(uploadResult.getUrl())
                .imagePublicId(uploadResult.getPublicId())
                .originalPrice(request.getOriginalPrice())
                .discountedPrice(request.getDiscountedPrice())
                .hasDiscount(request.getHasDiscount() != null && request.getHasDiscount())
                .stockStatus(Product.StockStatus.valueOf(request.getStockStatus()))
                .isActive(true)
                .viewCount(0L)
                .cartAddCount(0L)
                .build();

        product = productRepository.save(product);
        log.info("Product created: {} by vendor: {}", product.getId(), vendor.getId());

        return convertToDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllActiveProducts() {
        return productRepository.findAllActiveProductsFromAcceptedVendors()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByVendorSlug(String vendorSlug) {
        return productRepository.findByVendorSlugAndIsActiveTrue(vendorSlug)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getMyProducts(User user) {
        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        return productRepository.findAllByVendorId(vendor.getId())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Increment view count
        productRepository.incrementViewCount(productId);

        return convertToDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getDiscountedProducts() {
        return productRepository.findAllProductsWithDiscount()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateProduct(Long productId, CreateProductRequest request, User user) {

        // 1. Validate Vendor
        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        // 2. Validate Product Ownership
        Product product = productRepository.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission"));

        // 3. Update basic info (Text Fields Only)
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setDiscountedPrice(request.getDiscountedPrice());

        // Check null for boolean/Enum to avoid NullPointerExceptions
        if (request.getHasDiscount() != null) {
            product.setHasDiscount(request.getHasDiscount());
        }
        if (request.getStockStatus() != null) {
            product.setStockStatus(Product.StockStatus.valueOf(request.getStockStatus()));
        }

        // 4. Validate Price Logic (Discount < Original)
        if (Boolean.TRUE.equals(product.getHasDiscount()) && product.getDiscountedPrice() != null) {
            if (product.getDiscountedPrice().compareTo(product.getOriginalPrice()) >= 0) {
                throw new RuntimeException("Discounted price must be less than original price");
            }
        }

        // *** IMAGE UPDATE LOGIC REMOVED ***
        // Agar future mein image update chahiye, toh ek alag API banana "/{productId}/image"

        product = productRepository.save(product);
        log.info("Product details updated: {} by vendor: {}", productId, vendor.getId());

        return convertToDto(product);
    }

    @Transactional
    public void deleteProduct(Long productId, User user) {
        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        Product product = productRepository.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission"));

        // 2. DELETE FROM CART FIRST (Yeh naya logic hai)
        // Isse pehle ki product dlt ho, usse saare carts se hata do
        cartItemRepository.deleteAllByProductId(productId);

        // 3. Delete image from Cloudinary
        if (product.getImagePublicId() != null) {
            cloudinaryService.deleteImage(product.getImagePublicId());
        }

        // 4. Finally Delete Product
        productRepository.delete(product);
        log.info("Product and associated cart items deleted: {} by vendor: {}", productId, vendor.getId());
    }

    @Transactional
    public ProductDto toggleProductStatus(Long productId, User user) {
        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        Product product = productRepository.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission"));

        product.setIsActive(!product.getIsActive());
        product = productRepository.save(product);
        log.info("Product status toggled: {} to {}", productId, product.getIsActive());

        return convertToDto(product);
    }

    private ProductDto convertToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .vendorId(product.getVendor().getId())
                .vendorName(product.getVendor().getUser().getFirstName() + " " + product.getVendor().getUser().getLastName())
                .shopName(product.getVendor().getShopName())
                .vendorSlug(product.getVendor().getVendorSlug())
                .vendorPhone(product.getVendor().getVendorPhone())
                .productName(product.getProductName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .originalPrice(product.getOriginalPrice())
                .discountedPrice(product.getDiscountedPrice())
                .hasDiscount(product.getHasDiscount())
                .effectivePrice(product.getEffectivePrice())
                .stockStatus(product.getStockStatus())
                .isActive(product.getIsActive())
                .viewCount(product.getViewCount())
                .cartAddCount(product.getCartAddCount())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
