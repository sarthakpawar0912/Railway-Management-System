package com.train_service.Repository;

import com.train_service.Entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    List<Train> findBySourceAndDestination(String source, String destination);

    Optional<Train> findByTrainNumber(String trainNumber); // 🔥 FIX
}