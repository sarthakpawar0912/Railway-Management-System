package com.railway.paymentservice.Entity;

import com.railway.paymentservice.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;

    private String userEmail;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}