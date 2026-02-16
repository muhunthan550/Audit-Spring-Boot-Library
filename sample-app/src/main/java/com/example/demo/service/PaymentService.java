package com.example.demo.service;

import com.example.audit.annotation.Auditable;
import com.example.demo.model.PaymentRequest;
import com.example.demo.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PaymentService {


    @Auditable(action = "CREATE_PAYMENT")
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Processing payment: {}", request);

        // Simulate payment processing
        String transactionId = UUID.randomUUID().toString();

        PaymentResponse response = new PaymentResponse(
                request.getPaymentId(),
                "SUCCESS",
                transactionId,
                "Payment processed successfully"
        );

        log.info("Payment created: {}", response);
        return response;
    }


    @Auditable(action = "GET_PAYMENT", captureReturnValue = false)
    public PaymentResponse getPayment(String paymentId) {
        log.info("Retrieving payment: {}", paymentId);

        return new PaymentResponse(
                paymentId,
                "SUCCESS",
                "TXN-" + paymentId,
                "Payment retrieved"
        );
    }

    @Auditable(action = "CANCEL_PAYMENT")
    public void cancelPayment(String paymentId) {
        log.info("Cancelling payment: {}", paymentId);

        // Simulate cancellation
        if (paymentId == null || paymentId.isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        log.info("Payment cancelled: {}", paymentId);
    }

    @Auditable(action = "REFUND_PAYMENT", captureParameters = true, captureReturnValue = true)
    public PaymentResponse refundPayment(String paymentId, Double amount) {
        log.info("Refunding payment: {} amount: {}", paymentId, amount);

        return new PaymentResponse(
                paymentId,
                "REFUNDED",
                "REFUND-" + UUID.randomUUID(),
                "Refund processed: $" + amount
        );
    }
}