package com.resumeai.auth.entity;

import com.resumeai.auth.enums.AuthProvider;
import com.resumeai.auth.enums.Role;
import com.resumeai.auth.enums.SubscriptionPlan;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 36)
    private String userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 25)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 20)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (userId == null) userId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        if (provider == null) provider = AuthProvider.LOCAL;
        if (subscriptionPlan == null) subscriptionPlan = SubscriptionPlan.FREE;
        if (isActive == null) isActive = true;
    }

    public String getUserId() { 
    	return userId; 
    }
    public void setUserId(String userId) { 
    	this.userId = userId; 
    }
    
    public String getFullName() { 
    	return fullName; 
    }
    public void setFullName(String fullName) { 
    	this.fullName = fullName; 
    }
    public String getEmail() { 
    	return email; 
    }
    public void setEmail(String email) { 
    	this.email = email; 
    }
    public String getPasswordHash() { 
    	return passwordHash; 
    }
    public void setPasswordHash(String passwordHash) { 
    	this.passwordHash = passwordHash; 
    }
    public String getPhone() { 
    	return phone; 
    }
    public void setPhone(String phone) { 
    	this.phone = phone; 
    }
    public Role getRole() { 
    	return role; 
    }
    public void setRole(Role role) { 
    	this.role = role; 
    }
    public AuthProvider getProvider() { 
    	return provider; 
    }
    public void setProvider(AuthProvider provider) { 
    	this.provider = provider; 
    }
    public Boolean getIsActive() { 
    	return isActive; 
    }
    public void setIsActive(Boolean active) { 
    	isActive = active; 
    }
    public SubscriptionPlan getSubscriptionPlan() { 
    	return subscriptionPlan; 
    }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { 
    	this.subscriptionPlan = subscriptionPlan; 
    }
    public LocalDateTime getCreatedAt() { 
    	return createdAt; 
    }
    public void setCreatedAt(LocalDateTime createdAt) { 
    	this.createdAt = createdAt; 
    }
}
