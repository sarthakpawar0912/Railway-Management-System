package com.railway.bookingservice.Service;

import com.railway.bookingservice.DTO.*;

import java.util.List;

public interface BookingService {

    BookingResponse bookTicket(BookingRequest request);

    List<BookingResponse> getAllBookings();

    BookingResponse getBookingById(Long id);

    List<BookingResponse> getUserBookings(String email);

    String cancelBooking(Long id);
}