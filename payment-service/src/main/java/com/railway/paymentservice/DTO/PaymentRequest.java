package com.railway.paymentservice.DTO;

import lombok.*;

@Data
public class PaymentRequest {
    private Long bookingId;
    private String userEmail;
    private double amount;
}