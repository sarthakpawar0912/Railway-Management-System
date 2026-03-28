package com.railway.bookingservice.FiegnClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "TRAIN-SERVICE")
public interface TrainClient {

    @PostMapping("/trains/book-seat")
    Map<String, String> bookSeat(@RequestParam String trainNumber,
                                 @RequestParam String seatType);
}