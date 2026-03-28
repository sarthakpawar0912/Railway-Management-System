package com.railway.paymentservice.DTO;

import lombok.Data;
import java.io.Serializable;

@Data
public class PaymentEvent implements Serializable {
    private Long bookingId;
    private String status;
}