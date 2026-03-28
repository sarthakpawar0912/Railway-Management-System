package com.train_service.Controller;

import com.train_service.DTO.TrainResponse;
import com.train_service.Entity.Train;
import com.train_service.Service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trains")
public class TrainController {

    @Autowired
    private TrainService service;

    @PostMapping("/add")
    public Train add(@RequestBody Train train) {
        return service.addTrain(train);
    }

    @GetMapping("/search")
    public List<TrainResponse> search(
            @RequestParam String source,
            @RequestParam String destination) {
        return service.search(source, destination);
    }

    // 🔥 UPDATED RETURN TYPE
    @PostMapping("/book-seat")
    public Map<String, String> bookSeat(
            @RequestParam String trainNumber,
            @RequestParam String seatType) {

        return service.bookSeat(trainNumber, seatType);
    }

    @GetMapping("/availability/{trainNumber}")
    public Train availability(@PathVariable String trainNumber) {
        return service.getAvailability(trainNumber);
    }

    @GetMapping("/all")
    public List<Train> all() {
        return service.getAllTrains();
    }
}