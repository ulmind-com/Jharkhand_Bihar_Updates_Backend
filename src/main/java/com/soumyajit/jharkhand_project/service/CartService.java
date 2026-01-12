package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.AddToCartRequest;
import com.soumyajit.jharkhand_project.dto.CartDto;
import com.soumyajit.jharkhand_project.entity.CartItem;
import com.soumyajit.jharkhand_project.entity.CartSession;
import com.soumyajit.jharkhand_project.entity.Product;
import com.soumyajit.jharkhand_project.entity.Vendor;
import com.soumyajit.jharkhand_project.repository.CartItemRepository;
import com.soumyajit.jharkhand_project.repository.CartSessionRepository;
import com.soumyajit.jharkhand_project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartSessionRepository cartSessionRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    private static final int CART_EXPIRY_HOURS = 24;

    @Transactional
    public CartDto addToCart(AddToCartRequest request) {

        // 1. Fetch Product (Essential)
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validation (Fast in-memory checks)
        if (!product.getIsActive()) throw new RuntimeException("Product is not available");
        if (product.getVendor().getVendorStatus() != Vendor.VendorStatus.ACCEPTED) throw new RuntimeException("Vendor is not active");
        if (product.getStockStatus() == Product.StockStatus.OUT_OF_STOCK) throw new RuntimeException("Product is out of stock");

        String sessionId = request.getSessionId();
        CartSession cartSession;

        // 2. Handle Session
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
            cartSession = createNewCartSession(sessionId, product.getVendor());
        } else {
            Optional<CartSession> existingSession = cartSessionRepository.findBySessionId(sessionId);

            if (existingSession.isPresent()) {
                cartSession = existingSession.get();

                if (cartSession.isExpired()) {
                    // Optimization: Use bulk delete query instead of fetching and deleting
                    cartItemRepository.deleteByCartSessionId(cartSession.getId());
                    cartSession = createNewCartSession(sessionId, product.getVendor());
                } else {
                    // Check vendor mismatch
                    if (!cartSession.getVendor().getId().equals(product.getVendor().getId())) {

                        // --- OPTIMIZATION 1: Lightweight Check ---
                        // Instead of loading ALL items (heavy), just check if ANY exist (fast)
                        boolean hasItems = cartItemRepository.existsByCartSessionId(cartSession.getId());

                        if (!hasItems) {
                            // Cart is empty in DB, swap vendor
                            cartSession.setVendor(product.getVendor());
                            cartSessionRepository.save(cartSession);
                        } else {
                            throw new RuntimeException("Cart contains items from another vendor. Clear cart first.");
                        }
                    }
                }
            } else {
                cartSession = createNewCartSession(sessionId, product.getVendor());
            }
        }

        // 3. Add/Update Item
        // Try to find item in current session
        Optional<CartItem> existingItem = cartItemRepository.findByCartSessionIdAndProductId(
                cartSession.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cartSession(cartSession)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceAtAddition(product.getEffectivePrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        // --- OPTIMIZATION 2: Async Analytics ---
        // Run this in background so user doesn't wait for it
        CompletableFuture.runAsync(() -> {
            productRepository.incrementCartAddCount(product.getId());
        });

        // 4. Return
        // Note: If getCart() is still slow, we should manually construct CartDto here
        // using the 'cartSession' object we already have, to avoid another DB call.
        return getCart(cartSession.getSessionId());
    }

    @Transactional(readOnly = true)
    public CartDto getCart(String sessionId) {
        CartSession cartSession = cartSessionRepository.findBySessionIdWithItems(sessionId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cartSession.isExpired()) {
            throw new RuntimeException("Cart has expired");
        }

        return convertToDto(cartSession);
    }

    @Transactional
    public CartDto updateCartItemQuantity(String sessionId, Long itemId, Integer newQuantity) {
        if (newQuantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        CartSession cartSession = cartSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCartSession().getId().equals(cartSession.getId())) {
            throw new RuntimeException("Item does not belong to this cart");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        log.info("Updated cart item {} quantity to {}", itemId, newQuantity);

        return convertToDto(cartSession);
    }

    @Transactional
    public CartDto removeCartItem(String sessionId, Long itemId) {
        // 1. Fetch Session
        CartSession cartSession = cartSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // 2. Fetch Item & Validate
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCartSession().getId().equals(cartSession.getId())) {
            throw new RuntimeException("Item does not belong to this cart");
        }

        // 3. DELETE & FLUSH
        // Flush is CRITICAL here: It forces the DB to remove the row immediately
        // so the next SQL check sees the correct state.
        cartItemRepository.delete(item);
        cartItemRepository.flush();

        log.info("Removed cart item: {}", itemId);

        // 4. OPTIMIZED Empty Check (SQL Query vs Java List Load)
        // Runs "SELECT 1" instead of fetching all item data
        boolean hasItems = cartItemRepository.existsByCartSessionId(cartSession.getId());

        if (!hasItems) {
            // Cart is empty -> Kill Session
            cartSessionRepository.delete(cartSession);
            log.info("Deleted empty cart session: {}", sessionId);

            // Return Clean Empty DTO
            CartDto emptyCart = new CartDto();
            emptyCart.setSessionId(null);
            emptyCart.setItems(new ArrayList<>());
            emptyCart.setTotalAmount(BigDecimal.ZERO);
            return emptyCart;
        }

        // 5. Return Updated Cart
        // Using getCart ensures we fetch the fresh list from DB correctly
        return getCart(sessionId);
    }

    @Transactional
    public void clearCart(String sessionId) {
        CartSession cartSession = cartSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cartItemRepository.deleteByCartSessionId(cartSession.getId());
        cartSessionRepository.delete(cartSession);
        log.info("Cleared cart session: {}", sessionId);
    }

    // Scheduled task to clean up expired carts (runs daily at 2 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        LocalDateTime now = LocalDateTime.now();
        cartSessionRepository.deleteExpiredSessions(now);
        log.info("Cleaned up expired cart sessions");
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private CartSession createNewCartSession(String sessionId, Vendor vendor) {
        CartSession cartSession = CartSession.builder()
                .sessionId(sessionId)
                .vendor(vendor)
                .expiresAt(LocalDateTime.now().plusHours(CART_EXPIRY_HOURS))
                .build();

        return cartSessionRepository.save(cartSession);
    }

    // --- UPDATED CONVERT TO DTO (With Delivery Logic) ---
    private CartDto convertToDto(CartSession cartSession) {
        // 1. Map Items
        List<CartDto.CartItemDto> items = cartSession.getItems().stream()
                .map(item -> CartDto.CartItemDto.builder()
                        .itemId(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .imageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .priceAtAddition(item.getPriceAtAddition())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        // 2. Calculate Item Total (Sum of products only)
        BigDecimal itemTotal = items.stream()
                .map(CartDto.CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .mapToInt(CartDto.CartItemDto::getQuantity)
                .sum();

        // 3. Calculate Delivery Charge
        BigDecimal deliveryCharge = BigDecimal.ZERO;
        String deliveryMessage = "Free Delivery";

        Vendor vendor = cartSession.getVendor();

        if (vendor.getDeliveryType() == Vendor.DeliveryType.FIXED) {
            double charge = vendor.getDeliveryCharge() != null ? vendor.getDeliveryCharge() : 0.0;
            deliveryCharge = BigDecimal.valueOf(charge);
            deliveryMessage = "Standard delivery charge applied";
        }
        else if (vendor.getDeliveryType() == Vendor.DeliveryType.CONDITIONAL) {
            double minAmount = vendor.getMinFreeDeliveryAmount() != null ? vendor.getMinFreeDeliveryAmount() : 0.0;
            double charge = vendor.getDeliveryCharge() != null ? vendor.getDeliveryCharge() : 0.0;

            // Check if cart total meets the minimum amount
            if (itemTotal.doubleValue() >= minAmount) {
                deliveryCharge = BigDecimal.ZERO;
                deliveryMessage = "🎉 You've unlocked FREE Delivery!";
            } else {
                deliveryCharge = BigDecimal.valueOf(charge);
                double remaining = minAmount - itemTotal.doubleValue();
                deliveryMessage = String.format("Add items worth ₹%.0f more for FREE Delivery!", remaining);
            }
        }

        // 4. Calculate Grand Total
        BigDecimal grandTotal = itemTotal.add(deliveryCharge);

        CartDto.VendorInfoDto vendorInfo = CartDto.VendorInfoDto.builder()
                .vendorId(vendor.getId())
                .shopName(vendor.getShopName())
                .vendorPhone(vendor.getVendorPhone())
                .vendorSlug(vendor.getVendorSlug())
                .build();

        return CartDto.builder()
                .sessionId(cartSession.getSessionId())
                .vendor(vendorInfo)
                .items(items)
                .itemTotal(itemTotal)             // New Field (Product cost)
                .deliveryCharge(deliveryCharge)   // New Field (Shipping cost)
                .totalAmount(grandTotal)          // Updated (Total + Shipping)
                .deliveryMessage(deliveryMessage) // New Field (Message to user)
                .totalItems(totalItems)
                .expiresAt(cartSession.getExpiresAt())
                .build();
    }
}
