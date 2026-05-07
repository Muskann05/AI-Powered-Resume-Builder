package com.resumeai.auth.security;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.enums.AuthProvider;
import com.resumeai.auth.enums.Role;
import com.resumeai.auth.enums.SubscriptionPlan;
import com.resumeai.auth.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        AuthProvider provider = resolveProvider(registrationId);

        if ((name == null || name.isBlank()) && oauthUser.getAttribute("given_name") != null) {
            String givenName = oauthUser.getAttribute("given_name");
            String familyName = oauthUser.getAttribute("family_name");
            name = (givenName + " " + (familyName != null ? familyName : "")).trim();
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user"),
                    "Email not found from " + registrationId
            );
        }

        final String resolvedName = (name != null && !name.isBlank())
                ? name
                : defaultDisplayName(provider);

        User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(() -> {
            User u = new User();
            u.setEmail(email.toLowerCase());
            u.setFullName(resolvedName);
            u.setPasswordHash("OAUTH2_USER");
            u.setPhone(null);
            u.setRole(Role.USER);
            u.setProvider(provider);
            u.setSubscriptionPlan(SubscriptionPlan.FREE);
            u.setIsActive(true);
            return u;
        });

        if (user.getProvider() == AuthProvider.LOCAL) {
            user.setProvider(provider);
        }

        userRepository.save(user);
        return oauthUser;
    }

    private AuthProvider resolveProvider(String registrationId) {
        if ("linkedin".equalsIgnoreCase(registrationId)) {
            return AuthProvider.LINKEDIN;
        }
        return AuthProvider.GOOGLE;
    }

    private String defaultDisplayName(AuthProvider provider) {
        return provider == AuthProvider.LINKEDIN ? "LinkedIn User" : "Google User";
    }
}
