package com.example.demo.controller;

import com.example.audit.annotation.Auditable;
import com.example.demo.model.PaymentRequest;
import com.example.demo.model.PaymentResponse;
import com.example.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping
    @Auditable(action = "API_CREATE_PAYMENT")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        log.info("API: Creating payment for customer: {}", request.getCustomerId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Auditable(action = "API_GET_PAYMENT")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
        log.info("API: Retrieving payment: {}", id);
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Auditable(action = "API_CANCEL_PAYMENT")
    public ResponseEntity<Void> cancelPayment(@PathVariable String id) {
        log.info("API: Cancelling payment: {}", id);
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refund")
    @Auditable(action = "API_REFUND_PAYMENT")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable String id,
            @RequestParam Double amount) {
        log.info("API: Refunding payment: {} amount: {}", id, amount);
        PaymentResponse response = paymentService.refundPayment(id, amount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is healthy");
    }
}