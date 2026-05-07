package com.resumeai.auth.service;

import com.resumeai.auth.dto.CreatePaymentOrderRequest;
import com.resumeai.auth.dto.CreatePaymentOrderResponse;
import com.resumeai.auth.dto.PaymentVerifyResponse;
import com.resumeai.auth.dto.VerifyPaymentRequest;
import com.resumeai.auth.entity.PaymentTransaction;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.enums.PaymentStatus;
import com.resumeai.auth.enums.SubscriptionPlan;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.ResourceNotFoundException;
import com.resumeai.auth.messaging.PlanChangedEvent;
import com.resumeai.auth.repository.PaymentTransactionRepository;
import com.resumeai.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final AuthEventPublisher authEventPublisher;
    private final RestClient razorpayRestClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.payment.premium.amount}")
    private Integer premiumAmount;

    @Value("${app.payment.currency}")
    private String currency;

    @Value("${app.payment.provider:dummy}")
    private String paymentProvider;

    public PaymentServiceImpl(PaymentTransactionRepository paymentTransactionRepository,
                              UserRepository userRepository,
                              AuthEventPublisher authEventPublisher,
                              RestClient razorpayRestClient) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.authEventPublisher = authEventPublisher;
        this.razorpayRestClient = razorpayRestClient;
    }

    @Override
    public CreatePaymentOrderResponse createOrder(String userId, CreatePaymentOrderRequest request) {
        log.info("Creating payment order userId={} plan={} provider={}", userId, request.plan(), paymentProvider);
        validatePremiumPlan(request.plan());

        User user = getActiveUser(userId);
        if (SubscriptionPlan.PREMIUM == user.getSubscriptionPlan()) {
            throw new BadRequestException("User is already on PREMIUM plan");
        }

        String receipt = "premium_" + userId.substring(0, Math.min(8, userId.length())) + "_" + System.currentTimeMillis();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", premiumAmount);
        body.put("currency", currency);
        body.put("receipt", receipt);

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("userId", userId);
        notes.put("plan", request.plan());
        body.put("notes", notes);

        String providerOrderId;
        if ("razorpay".equalsIgnoreCase(paymentProvider)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = razorpayRestClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new BadRequestException("Failed to create Razorpay order");
            }
            providerOrderId = String.valueOf(response.get("id"));
        } else {
            providerOrderId = "dummy_order_" + System.currentTimeMillis();
            log.info("Using dummy payment provider orderId={}", providerOrderId);
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(userId);
        transaction.setPlan(request.plan().toUpperCase());
        transaction.setAmount(premiumAmount);
        transaction.setCurrency(currency);
        transaction.setReceipt(receipt);
        transaction.setRazorpayOrderId(providerOrderId);
        transaction.setStatus(PaymentStatus.CREATED);
        paymentTransactionRepository.save(transaction);

        return new CreatePaymentOrderResponse(
                razorpayKeyId,
                transaction.getRazorpayOrderId(),
                premiumAmount,
                currency,
                transaction.getPlan()
        );
    }

    @Override
    public PaymentVerifyResponse verifyPayment(String userId, VerifyPaymentRequest request) {
        log.info("Verifying payment userId={} orderId={} provider={}",
                userId, request.razorpayOrderId(), paymentProvider);
        validatePremiumPlan(request.plan());

        PaymentTransaction transaction = paymentTransactionRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Payment does not belong to current user");
        }

        if (PaymentStatus.VERIFIED == transaction.getStatus()) {
            return new PaymentVerifyResponse(true, "Payment already verified");
        }

        if (!"razorpay".equalsIgnoreCase(paymentProvider)) {
            transaction.setRazorpayPaymentId(request.razorpayPaymentId());
            transaction.setRazorpaySignature(request.razorpaySignature());
            transaction.setStatus(PaymentStatus.VERIFIED);
            transaction.setVerifiedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            upgradeUserToPremium(userId);
            log.info("Dummy payment verified userId={} orderId={}", userId, request.razorpayOrderId());
            return new PaymentVerifyResponse(true, "Dummy payment verified and subscription updated to PREMIUM");
        }

        String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
        String expectedSignature = hmacSha256Hex(payload, razorpayKeySecret);

        if (!expectedSignature.equals(request.razorpaySignature())) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setRazorpayPaymentId(request.razorpayPaymentId());
            transaction.setRazorpaySignature(request.razorpaySignature());
            paymentTransactionRepository.save(transaction);
            throw new BadRequestException("Invalid payment signature");
        }

        transaction.setRazorpayPaymentId(request.razorpayPaymentId());
        transaction.setRazorpaySignature(request.razorpaySignature());
        transaction.setStatus(PaymentStatus.VERIFIED);
        transaction.setVerifiedAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        upgradeUserToPremium(userId);
        log.info("Razorpay payment verified userId={} orderId={}", userId, request.razorpayOrderId());
        return new PaymentVerifyResponse(true, "Payment verified and subscription updated to PREMIUM");
    }

    private void upgradeUserToPremium(String userId) {
        User user = getActiveUser(userId);
        SubscriptionPlan oldPlan = user.getSubscriptionPlan();
        user.setSubscriptionPlan(SubscriptionPlan.PREMIUM);
        userRepository.save(user);

        if (oldPlan != SubscriptionPlan.PREMIUM) {
            authEventPublisher.publishPlanChanged(
                    new PlanChangedEvent(
                            user.getUserId(),
                            oldPlan != null ? oldPlan.name() : null,
                            SubscriptionPlan.PREMIUM.name(),
                            user.getUserId(),
                            LocalDateTime.now()
                    )
            );
        }
    }

    private void validatePremiumPlan(String plan) {
        if (!"PREMIUM".equalsIgnoreCase(plan)) {
            throw new BadRequestException("Only PREMIUM payment is supported right now");
        }
    }

    private User getActiveUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("User account is inactive");
        }

        return user;
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to verify payment signature");
        }
    }
}
