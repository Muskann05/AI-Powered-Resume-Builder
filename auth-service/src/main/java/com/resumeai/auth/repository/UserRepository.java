package com.resumeai.auth.repository;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.enums.Role;
import com.resumeai.auth.enums.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    List<User> findAllByRole(Role role);
    List<User> findBySubscriptionPlan(SubscriptionPlan subscriptionPlan);
    List<User> findByIsActive(Boolean isActive);
    void deleteByUserId(String userId);
}
