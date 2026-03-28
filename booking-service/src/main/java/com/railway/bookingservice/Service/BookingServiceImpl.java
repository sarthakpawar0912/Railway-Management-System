package com.railway.bookingservice.Service;

import com.railway.bookingservice.DTO.BookingRequest;
import com.railway.bookingservice.DTO.BookingResponse;
import com.railway.bookingservice.Entity.Booking;
import com.railway.bookingservice.Enum.BookingStatus;
import com.railway.bookingservice.FiegnClient.PaymentClient;
import com.railway.bookingservice.FiegnClient.TrainClient;
import com.railway.bookingservice.Repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository repo;

    @Autowired
    private TrainClient trainClient;

    @Autowired
    private PaymentClient paymentClient;

    @Override
    public BookingResponse bookTicket(BookingRequest request) {

        // 🔥 1. Seat booking
        Map<String, String> seatResponse =
                trainClient.bookSeat(request.getTrainNumber(), request.getSeatType());

        // 🔥 2. Save booking
        Booking booking = new Booking();
        booking.setUserEmail(request.getUserEmail());
        booking.setTrainNumber(request.getTrainNumber());
        booking.setSource(request.getSource());
        booking.setDestination(request.getDestination());

        booking.setSeatNumber(seatResponse.get("seatNumber"));
        booking.setCoach(seatResponse.get("coach"));
        booking.setSeatType(seatResponse.get("seatType"));

        booking.setStatus(BookingStatus.PENDING);

        Booking saved = repo.save(booking);

        // 🔥 3. CHECK WAITING BEFORE PAYMENT
        if ("WAITING".equalsIgnoreCase(seatResponse.get("status"))) {

            saved.setStatus(BookingStatus.WAITING);
            repo.save(saved);

            return map(saved, "Added to WAITING list");
        }

        // 🔥 4. Payment call (ONLY if seat available)
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("bookingId", saved.getId());
        paymentRequest.put("userEmail", saved.getUserEmail());
        paymentRequest.put("amount", 500);

        Map<String, Object> response = paymentClient.pay(paymentRequest);

        String paymentStatus = (String) response.get("status");

        // 🔥 5. Update status
        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            saved.setStatus(BookingStatus.CONFIRMED);
        } else {
            saved.setStatus(BookingStatus.FAILED);
        }

        repo.save(saved);

        return map(saved, "Booking " + saved.getStatus());
    }

    @Override
    public List<BookingResponse> getAllBookings() {
        return repo.findAll()
                .stream()
                .map(b -> map(b, "Fetched"))
                .toList();
    }

    @Override
    public BookingResponse getBookingById(Long id) {
        return map(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found")), "Fetched");
    }

    @Override
    public List<BookingResponse> getUserBookings(String email) {
        return repo.findByUserEmail(email)
                .stream()
                .map(b -> map(b, "Fetched"))
                .toList();
    }

    @Override
    public String cancelBooking(Long id) {

        Booking booking = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        repo.save(booking);

        return "Booking Cancelled Successfully";
    }

    // 🔥 COMMON MAPPER
    private BookingResponse map(Booking b, String msg) {
        return new BookingResponse(
                b.getId(),
                b.getUserEmail(),
                b.getTrainNumber(),
                b.getSource(),
                b.getDestination(),
                b.getSeatNumber(),
                b.getCoach(),
                b.getSeatType(),
                b.getStatus(),
                msg
        );
    }
}