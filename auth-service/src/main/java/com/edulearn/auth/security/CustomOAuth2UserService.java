package com.edulearn.auth.security;

import com.edulearn.auth.entity.User;
import com.edulearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Loads the OAuth2 user from the provider and creates/updates the
 * local User record so that JWT-based auth can be used post-login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider   = userRequest.getClientRegistration().getRegistrationId(); // "github" or "google"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId     = String.valueOf(attributes.get("id") != null ? attributes.get("id") : attributes.get("sub"));
        String email          = extractEmail(provider, attributes);
        String fullName       = extractName(provider, attributes);
        String profilePicUrl  = extractPicture(provider, attributes);

        if (email == null) {
            log.warn("OAuth2 provider '{}' did not return an email for providerId={}", provider, providerId);
            // Fall through — user may not have public email on GitHub
        }

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isEmpty() && email != null) {
            existing = userRepository.findByEmail(email);
        }

        if (existing.isPresent()) {
            User user = existing.get();
            user.setProvider(provider);
            user.setProviderId(providerId);
            if (profilePicUrl != null) user.setProfilePicUrl(profilePicUrl);
            userRepository.save(user);
        } else {
            User user = User.builder()
                    .fullName(fullName != null ? fullName : "OAuth User")
                    .email(email != null ? email : provider + "_" + providerId + "@oauth.local")
                    .role(User.Role.STUDENT)
                    .provider(provider)
                    .providerId(providerId)
                    .profilePicUrl(profilePicUrl)
                    .isActive(true)
                    .build();
            userRepository.save(user);
            log.info("Created new OAuth2 user: {} via {}", email, provider);
        }

        return oAuth2User;
    }

    private String extractEmail(String provider, Map<String, Object> attrs) {
        if ("google".equals(provider)) return (String) attrs.get("email");
        if ("github".equals(provider)) return (String) attrs.get("email");
        return null;
    }

    private String extractName(String provider, Map<String, Object> attrs) {
        if ("google".equals(provider)) return (String) attrs.get("name");
        if ("github".equals(provider)) {
            String name = (String) attrs.get("name");
            return name != null ? name : (String) attrs.get("login");
        }
        return null;
    }

    private String extractPicture(String provider, Map<String, Object> attrs) {
        if ("google".equals(provider)) return (String) attrs.get("picture");
        if ("github".equals(provider)) return (String) attrs.get("avatar_url");
        return null;
    }
}
