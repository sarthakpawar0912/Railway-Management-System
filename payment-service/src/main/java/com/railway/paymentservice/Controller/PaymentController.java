package com.railway.paymentservice.Controller;


import com.railway.paymentservice.DTO.PaymentRequest;
import com.railway.paymentservice.DTO.PaymentResponse;
import com.railway.paymentservice.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService service;

    @PostMapping("/pay")
    public PaymentResponse pay(@RequestBody PaymentRequest request) {
        return service.processPayment(request);
    }
}