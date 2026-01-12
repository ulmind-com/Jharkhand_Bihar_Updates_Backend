package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.CartDto;
import com.soumyajit.jharkhand_project.dto.WhatsAppOrderRequest;
import com.soumyajit.jharkhand_project.dto.WhatsAppOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final CartService cartService;

    @Transactional
    public WhatsAppOrderResponse generateWhatsAppOrder(WhatsAppOrderRequest request) {

        // Get cart
        CartDto cart = cartService.getCart(request.getSessionId());

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Generate formatted message
        String formattedMessage = generateOrderMessage(cart, request);

        // Generate WhatsApp URL
        String whatsappUrl = generateWhatsAppUrl(cart.getVendor().getVendorPhone(), formattedMessage);

        log.info("Generated WhatsApp order for session: {} to vendor: {}",
                request.getSessionId(), cart.getVendor().getShopName());

        // ✅ Clear cart after generating order (keeps database clean)
        try {
            cartService.clearCart(request.getSessionId());
            log.info("Cart cleared after WhatsApp order generation: {}", request.getSessionId());
        } catch (Exception e) {
            log.warn("Failed to clear cart after order generation: {}", e.getMessage());
            // Don't fail the order if cart cleanup fails
        }

        return WhatsAppOrderResponse.builder()
                .whatsappUrl(whatsappUrl)
                .formattedMessage(formattedMessage)
                .vendorPhone(cart.getVendor().getVendorPhone())
                .build();
    }

    private String generateOrderMessage(CartDto cart, WhatsAppOrderRequest request) {
        StringBuilder message = new StringBuilder();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Header
        message.append("🛒 *New Order Request*\n\n");

        // Items
        message.append("📦 *Items:*\n");
        int itemNumber = 1;
        for (CartDto.CartItemDto item : cart.getItems()) {
            message.append(itemNumber++).append(". ")
                    .append(item.getProductName()).append("\n");
            message.append("   Qty: ")
                    .append(item.getQuantity())
                    .append(" × ")
                    .append(currencyFormat.format(item.getPriceAtAddition()))
                    .append(" = ")
                    .append(currencyFormat.format(item.getSubtotal()))
                    .append("\n\n");
        }

        // --- UPDATED BILL SUMMARY (WITH DELIVERY BREAKDOWN) ---
        message.append("💰 *Bill Summary:*\n");

        // 1. Items Total
        BigDecimal itemTotal = cart.getItemTotal() != null ? cart.getItemTotal() : BigDecimal.ZERO;
        message.append("Items Total: ").append(currencyFormat.format(itemTotal)).append("\n");

        // 2. Delivery Charge
        BigDecimal deliveryCharge = cart.getDeliveryCharge() != null ? cart.getDeliveryCharge() : BigDecimal.ZERO;
        String deliveryText = (deliveryCharge.compareTo(BigDecimal.ZERO) > 0)
                ? currencyFormat.format(deliveryCharge)
                : "FREE";

        message.append("Delivery: ").append(deliveryText).append("\n");

        message.append("━━━━━━━━━━━━━━━\n");

        // 3. Grand Total
        BigDecimal grandTotal = cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
        message.append("*Grand Total: ").append(currencyFormat.format(grandTotal)).append("*\n\n");
        // ------------------------------------------------------

        // Customer Details
        message.append("👤 *Customer Details:*\n");
        message.append("Name: ").append(request.getCustomerName()).append("\n");
        message.append("Phone: ").append(request.getCustomerPhone()).append("\n\n");

        // Delivery Address
        message.append("📍 *Delivery Address:*\n");
        message.append(request.getDeliveryAddress()).append("\n\n");

        // Exact Location (if provided)
        if (request.getExactLocation() != null && !request.getExactLocation().isEmpty()) {
            message.append("🗁 *Map Link:*\n");
            message.append(request.getExactLocation()).append("\n\n");
        }

        message.append("Please confirm this order. Thank you! 🙏");

        return message.toString();
    }

    private String generateWhatsAppUrl(String vendorPhone, String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
            return String.format("https://wa.me/91%s?text=%s", vendorPhone, encodedMessage);
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding WhatsApp message", e);
            throw new RuntimeException("Failed to generate WhatsApp URL");
        }
    }
}