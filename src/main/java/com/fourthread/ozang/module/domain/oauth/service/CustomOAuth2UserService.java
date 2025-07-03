package com.fourthread.ozang.module.domain.oauth.service;

import com.fourthread.ozang.module.domain.oauth.dto.OAuthAttributes;
import com.fourthread.ozang.module.domain.oauth.parser.OAuthAttributesParser;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.dto.type.Items;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final List<OAuthAttributesParser> parsers;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributesParser parser = parsers.stream()
            .filter(p -> p.supports(registrationId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + registrationId));

        OAuthAttributes attributes = parser.parse(userNameAttributeName, oauth2User.getAttributes());
        User user = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
            attributes.getAttributes(),
            attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        Items provider = Items.valueOf(attributes.getRegistrationId().toUpperCase());
        return userRepository.findByEmail(attributes.getEmail())
            .map(user -> {
                user.addOAuthProvider(provider); // 기존 사용자에게 provider 추가
                return userRepository.save(user); // 업데이트 저장
            })
            .orElseGet(() -> {
                User newUser = new User(attributes.getName(), attributes.getEmail(), "");
                newUser.setProfile(new Profile(attributes.getName(), null, null, null, null, null));
                newUser.addOAuthProvider(provider);
                return userRepository.save(newUser);
            });
    }
}