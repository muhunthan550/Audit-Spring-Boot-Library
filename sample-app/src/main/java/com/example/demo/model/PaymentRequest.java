package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String paymentId;
    private Double amount;
    private String currency;
    private String customerId;

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "paymentId='" + paymentId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}