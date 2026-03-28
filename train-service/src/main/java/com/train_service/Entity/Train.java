package com.train_service.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String trainNumber;
    private String trainName;

    private String source;
    private String destination;

    private String departureTime; // HH:mm
    private String arrivalTime;   // HH:mm

    private int distance;

    private int firstAcSeats;
    private int secondAcSeats;
    private int thirdAcSeats;
    private int sleeperSeats;
    private int generalSeats;

    // 🔥 WAITING COUNTERS
    private int firstAcWaiting;
    private int secondAcWaiting;
    private int thirdAcWaiting;
    private int sleeperWaiting;
    private int generalWaiting;
}