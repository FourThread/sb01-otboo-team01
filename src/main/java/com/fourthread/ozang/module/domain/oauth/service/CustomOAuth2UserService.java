package com.fourthread.ozang.module.domain.oauth.service;

import com.fourthread.ozang.module.domain.oauth.dto.OAuthAttributes;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.dto.type.Items;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oauth2User.getAttributes());
        
        User user = saveOrUpdate(attributes);
        
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        Optional<User> userOptional = userRepository.findByEmail(attributes.getEmail());

        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            // 새로운 사용자 생성
            User newUser = new User(attributes.getName(), attributes.getEmail(), "");
            
            // 빈 프로필 생성하기 (UserServiceImpl과 동일한 로직)
            Profile emptyProfile = new Profile(attributes.getName(), null, null, null, null, null);
            newUser.setProfile(emptyProfile);
            
            // OAuth 제공자 정보 추가
            Items oauthProvider = getOAuthProviderFromRegistrationId(attributes.getRegistrationId());
            newUser.addOAuthProvider(oauthProvider);
            
            return userRepository.save(newUser);
        }
    }
    
    private Items getOAuthProviderFromRegistrationId(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> Items.KAKAO;
            case "google" -> Items.GOOGLE;
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }
} 