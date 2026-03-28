package com.railway.bookingservice.Entity;

import com.railway.bookingservice.Enum.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String trainNumber;
    private String source;
    private String destination;

    // 🔥 NEW FIELDS
    private String seatNumber;
    private String coach;
    private String seatType;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;
}