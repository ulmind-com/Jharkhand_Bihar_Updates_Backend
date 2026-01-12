package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.AddToCartRequest;
import com.soumyajit.jharkhand_project.dto.CartDto;
import com.soumyajit.jharkhand_project.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        try {
            CartDto cart = cartService.addToCart(request);
            return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
        } catch (Exception e) {
            log.error("Error adding to cart", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<CartDto>> getCart(@PathVariable String sessionId) {
        try {
            CartDto cart = cartService.getCart(sessionId);
            return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cart));
        } catch (Exception e) {
            log.error("Error retrieving cart: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{sessionId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto>> updateCartItemQuantity(
            @PathVariable String sessionId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {

        try {
            CartDto cart = cartService.updateCartItemQuantity(sessionId, itemId, quantity);
            return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated", cart));
        } catch (Exception e) {
            log.error("Error updating cart item quantity", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDto>> removeCartItem(
            @PathVariable String sessionId,
            @PathVariable Long itemId) {

        try {
            CartDto cart = cartService.removeCartItem(sessionId, itemId);
            return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
        } catch (Exception e) {
            log.error("Error removing cart item", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<String>> clearCart(@PathVariable String sessionId) {
        try {
            cartService.clearCart(sessionId);
            return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
        } catch (Exception e) {
            log.error("Error clearing cart: {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
