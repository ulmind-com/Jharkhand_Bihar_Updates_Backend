package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.WhatsAppOrderRequest;
import com.soumyajit.jharkhand_project.dto.WhatsAppOrderResponse;
import com.soumyajit.jharkhand_project.service.WhatsAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
@Slf4j
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping("/generate-order")
    public ResponseEntity<ApiResponse<WhatsAppOrderResponse>> generateWhatsAppOrder(
            @Valid @RequestBody WhatsAppOrderRequest request) {

        try {
            WhatsAppOrderResponse response = whatsAppService.generateWhatsAppOrder(request);
            return ResponseEntity.ok(ApiResponse.success("WhatsApp order URL generated", response));
        } catch (Exception e) {
            log.error("Error generating WhatsApp order", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
