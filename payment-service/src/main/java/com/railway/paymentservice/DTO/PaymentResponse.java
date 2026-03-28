package com.railway.paymentservice.DTO;

import lombok.*;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String status;
    private String message;
}