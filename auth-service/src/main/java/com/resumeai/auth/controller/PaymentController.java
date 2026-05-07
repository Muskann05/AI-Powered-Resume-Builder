package com.resumeai.auth.controller;

import com.resumeai.auth.dto.CreatePaymentOrderRequest;
import com.resumeai.auth.dto.CreatePaymentOrderResponse;
import com.resumeai.auth.dto.PaymentVerifyResponse;
import com.resumeai.auth.dto.UserResponse;
import com.resumeai.auth.dto.VerifyPaymentRequest;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    public PaymentController(PaymentService paymentService, AuthService authService) {
        this.paymentService = paymentService;
        this.authService = authService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentOrderResponse> createOrder(Authentication authentication,
                                                                  @Valid @RequestBody CreatePaymentOrderRequest request) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(paymentService.createOrder(user.userId(), request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(Authentication authentication,
                                                               @Valid @RequestBody VerifyPaymentRequest request) {
        UserResponse user = authService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(paymentService.verifyPayment(user.userId(), request));
    }
}
