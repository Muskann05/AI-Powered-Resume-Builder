package com.resumeai.auth.repository;

import com.resumeai.auth.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
}
