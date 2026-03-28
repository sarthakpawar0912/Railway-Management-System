package com.railway.bookingservice.DTO;

import com.railway.bookingservice.Enum.BookingStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {

    private Long id;
    private String userEmail;
    private String trainNumber;
    private String source;
    private String destination;

    // 🔥 NEW
    private String seatNumber;
    private String coach;
    private String seatType;

    private BookingStatus status;
    private String message;
}