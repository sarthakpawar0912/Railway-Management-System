package com.railway.bookingservice.FiegnClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentClient {

    @PostMapping("/payments/pay")
    Map<String, Object> pay(@RequestBody Map<String, Object> request);
}