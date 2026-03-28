package com.railway.paymentservice.Repository;


import com.railway.paymentservice.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}