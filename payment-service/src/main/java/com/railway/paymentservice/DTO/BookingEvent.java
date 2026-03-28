package com.railway.paymentservice.DTO;

import lombok.Data;
import java.io.Serializable;

@Data
public class BookingEvent implements Serializable {
    private Long bookingId;
    private String userEmail;
    private String trainNumber;
    private String source;
    private String destination;
}