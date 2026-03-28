package com.railway.paymentservice.Service;


import com.railway.paymentservice.DTO.PaymentRequest;
import com.railway.paymentservice.DTO.PaymentResponse;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);
}