package com.railway.bookingservice.Repository;

import com.railway.bookingservice.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserEmail(String email);

    // 🔥 SEARCH APIs
    List<Booking> findByTrainNumber(String trainNumber);

    List<Booking> findBySource(String source);

    List<Booking> findByDestination(String destination);

    List<Booking> findBySourceAndDestination(String source, String destination);
}