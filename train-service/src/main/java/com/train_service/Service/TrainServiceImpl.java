package com.train_service.Service;

import com.train_service.DTO.TrainResponse;
import com.train_service.ENUM.SeatType;
import com.train_service.Entity.Train;
import com.train_service.Repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrainServiceImpl implements TrainService {

    @Autowired
    private TrainRepository repo;

    @Override
    public Train addTrain(Train train) {
        return repo.save(train);
    }

    @Override
    public List<Train> getAllTrains() {
        return repo.findAll();
    }

    // 🔥 ADVANCED SEARCH (SORT + DURATION + DISTANCE)
    @Override
    public List<TrainResponse> search(String source, String destination) {

        List<Train> trains = repo.findBySourceAndDestination(source, destination);

        return trains.stream()
                .sorted(Comparator.comparing(Train::getDepartureTime))
                .map(t -> {

                    String[] dep = t.getDepartureTime().split(":");
                    String[] arr = t.getArrivalTime().split(":");

                    int depMin = Integer.parseInt(dep[0]) * 60 + Integer.parseInt(dep[1]);
                    int arrMin = Integer.parseInt(arr[0]) * 60 + Integer.parseInt(arr[1]);

                    // 🔥 FIX: overnight train
                    if (arrMin < depMin) {
                        arrMin += 24 * 60;
                    }

                    int durationMin = arrMin - depMin;
                    int hr = durationMin / 60;
                    int min = durationMin % 60;

                    String duration = hr + "h " + min + "m";

                    int totalSeats =
                            t.getFirstAcSeats() +
                                    t.getSecondAcSeats() +
                                    t.getThirdAcSeats() +
                                    t.getSleeperSeats() +
                                    t.getGeneralSeats();

                    return new TrainResponse(
                            t.getTrainNumber(),
                            t.getTrainName(),
                            t.getSource(),
                            t.getDestination(),
                            t.getDepartureTime(),
                            t.getArrivalTime(),
                            duration,
                            t.getDistance(),
                            totalSeats
                    );
                })
                .toList();
    }

    @Override
    public Train getAvailability(String trainNumber) {
        return repo.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new RuntimeException("Train not found"));
    }

    // 🔥 FINAL SEAT + WAITING LOGIC
    @Override
    public Map<String, String> bookSeat(String trainNumber, String seatType) {

        Train t = repo.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new RuntimeException("Train not found"));

        Map<String, String> res = new HashMap<>();
        SeatType type = SeatType.valueOf(seatType.toUpperCase());

        switch (type) {

            case SLEEPER:
                if (t.getSleeperSeats() > 0) {
                    t.setSleeperSeats(t.getSleeperSeats() - 1);
                    res.put("seatNumber", "S-" + t.getSleeperSeats());
                    res.put("coach", "SL");
                    res.put("status", "CONFIRMED");
                } else {
                    t.setSleeperWaiting(t.getSleeperWaiting() + 1);
                    res.put("seatNumber", "WL-" + t.getSleeperWaiting());
                    res.put("coach", "WAITING");
                    res.put("status", "WAITING");
                }
                break;

            case FIRST_AC:
                if (t.getFirstAcSeats() > 0) {
                    t.setFirstAcSeats(t.getFirstAcSeats() - 1);
                    res.put("seatNumber", "A1-" + t.getFirstAcSeats());
                    res.put("coach", "1AC");
                    res.put("status", "CONFIRMED");
                } else {
                    t.setFirstAcWaiting(t.getFirstAcWaiting() + 1);
                    res.put("seatNumber", "WL-" + t.getFirstAcWaiting());
                    res.put("coach", "WAITING");
                    res.put("status", "WAITING");
                }
                break;

            case SECOND_AC:
                if (t.getSecondAcSeats() > 0) {
                    t.setSecondAcSeats(t.getSecondAcSeats() - 1);
                    res.put("seatNumber", "A2-" + t.getSecondAcSeats());
                    res.put("coach", "2AC");
                    res.put("status", "CONFIRMED");
                } else {
                    t.setSecondAcWaiting(t.getSecondAcWaiting() + 1);
                    res.put("seatNumber", "WL-" + t.getSecondAcWaiting());
                    res.put("coach", "WAITING");
                    res.put("status", "WAITING");
                }
                break;

            case THIRD_AC:
                if (t.getThirdAcSeats() > 0) {
                    t.setThirdAcSeats(t.getThirdAcSeats() - 1);
                    res.put("seatNumber", "A3-" + t.getThirdAcSeats());
                    res.put("coach", "3AC");
                    res.put("status", "CONFIRMED");
                } else {
                    t.setThirdAcWaiting(t.getThirdAcWaiting() + 1);
                    res.put("seatNumber", "WL-" + t.getThirdAcWaiting());
                    res.put("coach", "WAITING");
                    res.put("status", "WAITING");
                }
                break;

            case GENERAL:
                if (t.getGeneralSeats() > 0) {
                    t.setGeneralSeats(t.getGeneralSeats() - 1);
                    res.put("seatNumber", "G-" + t.getGeneralSeats());
                    res.put("coach", "GEN");
                    res.put("status", "CONFIRMED");
                } else {
                    t.setGeneralWaiting(t.getGeneralWaiting() + 1);
                    res.put("seatNumber", "WL-" + t.getGeneralWaiting());
                    res.put("coach", "WAITING");
                    res.put("status", "WAITING");
                }
                break;
        }

        repo.save(t);

        res.put("seatType", seatType.toUpperCase());

        return res;
    }
}