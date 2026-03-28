package com.train_service.DTO;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainResponse {

    private String trainNumber;
    private String trainName;

    private String source;
    private String destination;

    private String departureTime;
    private String arrivalTime;

    private String duration; // 🔥 calculated
    private int distance;    // 🔥 km

    private int totalSeats;  // 🔥 summary
}