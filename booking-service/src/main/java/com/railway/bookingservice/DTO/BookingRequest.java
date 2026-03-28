package com.railway.bookingservice.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private String userEmail;
    private String trainNumber;
    private String source;
    private String destination;

    private String seatType; // 🔥 NEW
}