package com.train_service.Service;

import com.train_service.DTO.TrainResponse;
import com.train_service.Entity.Train;

import java.util.List;
import java.util.Map;

public interface TrainService {

    Train addTrain(Train train);

    List<Train> getAllTrains();



    List<TrainResponse> search(String source, String destination);
    // 🔥 CHANGE HERE
    Map<String, String> bookSeat(String trainNumber, String seatType);

    Train getAvailability(String trainNumber);
}