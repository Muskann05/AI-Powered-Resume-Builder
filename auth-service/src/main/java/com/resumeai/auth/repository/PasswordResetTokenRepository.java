package com.resumeai.auth.repository;

import com.resumeai.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserIdAndUsedFalse(String userId);
}
