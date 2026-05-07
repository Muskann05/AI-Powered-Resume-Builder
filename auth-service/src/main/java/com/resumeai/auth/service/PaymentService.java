package com.resumeai.auth.service;

import com.resumeai.auth.dto.CreatePaymentOrderRequest;
import com.resumeai.auth.dto.CreatePaymentOrderResponse;
import com.resumeai.auth.dto.PaymentVerifyResponse;
import com.resumeai.auth.dto.VerifyPaymentRequest;

public interface PaymentService {
    CreatePaymentOrderResponse createOrder(String userId, CreatePaymentOrderRequest request);
    PaymentVerifyResponse verifyPayment(String userId, VerifyPaymentRequest request);
}
