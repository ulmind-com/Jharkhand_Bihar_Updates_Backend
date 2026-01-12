package com.soumyajit.jharkhand_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppOrderResponse {
    private String whatsappUrl;
    private String formattedMessage;
    private String vendorPhone;
}
