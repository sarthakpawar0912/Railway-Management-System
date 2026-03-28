package com.railway.bookingservice.Controller;

import com.railway.bookingservice.DTO.*;
import com.railway.bookingservice.Service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService service;

    @PostMapping("/book")
    public BookingResponse book(@RequestBody BookingRequest request) {
        return service.bookTicket(request);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id) {
        return service.getBookingById(id);
    }

    @GetMapping("/user")
    public List<BookingResponse> get(@RequestParam String email) {
        return service.getUserBookings(email);
    }

    @GetMapping("/all")
    public List<BookingResponse> all() {
        return service.getAllBookings();
    }

    @DeleteMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        return service.cancelBooking(id);
    }
}