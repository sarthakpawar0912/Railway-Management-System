package com.railway.paymentservice.Service;

import com.railway.paymentservice.DTO.*;
import com.railway.paymentservice.Entity.Payment;
import com.railway.paymentservice.Enum.PaymentStatus;
import com.railway.paymentservice.Repository.PaymentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository repo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 🔥 REST API (optional but required for controller)
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {

        Payment payment = new Payment();
        payment.setBookingId(request.getBookingId());
        payment.setUserEmail(request.getUserEmail());
        payment.setAmount(request.getAmount());

        if (request.getAmount() > 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        Payment saved = repo.save(payment);

        return new PaymentResponse(
                saved.getId(),
                saved.getStatus().name(),
                "Payment " + saved.getStatus()
        );
    }

    // 🔥 ASYNC LISTENER
    @RabbitListener(queues = "bookingQueue")
    public void receiveBooking(BookingEvent event) {

        System.out.println("🔥 Received booking: " + event.getBookingId());

        Payment payment = new Payment();
        payment.setBookingId(event.getBookingId());
        payment.setUserEmail(event.getUserEmail());
        payment.setAmount(500);

        if (payment.getAmount() > 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        repo.save(payment);

        // 🔥 SEND BACK RESULT
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setBookingId(event.getBookingId());
        paymentEvent.setStatus(payment.getStatus().name());

        rabbitTemplate.convertAndSend("paymentQueue", paymentEvent);

        System.out.println("✅ Payment processed & event sent");
    }
}