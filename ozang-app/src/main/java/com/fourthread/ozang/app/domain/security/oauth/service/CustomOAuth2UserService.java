package com.fourthread.ozang.app.domain.security.oauth.service;

import com.fourthread.ozang.app.domain.security.oauth.dto.OAuthAttributes;
import com.fourthread.ozang.app.domain.security.oauth.parser.OAuthAttributesParser;
import com.fourthread.ozang.core.domain.user.entity.User;
import com.fourthread.ozang.core.domain.user.entity.Profile;
import com.fourthread.ozang.core.domain.user.repository.UserRepository;
import com.fourthread.ozang.core.domain.user.dto.type.Items;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
            .orElseThrow(() -> new OAuth2AuthenticationException("지원하지 않는 OAuth 제공자: " + registrationId));

        OAuthAttributes attributes = parser.parse(userNameAttributeName, oauth2User.getAttributes());

        User user;
        try {
            user = saveOrUpdate(attributes);
        } catch (DataIntegrityViolationException e) {
            log.error("DB 제약 조건 위반: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("이미 등록된 사용자 정보가 있습니다.");
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 예외 발생: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("소셜 로그인 중 오류가 발생했습니다.");
        }

        if (user.getLocked()) {
            log.warn("[CustomOAuth2UserService] 잠긴 계정으로 OAuth 로그인 시도: {}", user.getEmail());
            throw new LockedException("계정이 잠겨있습니다.");
        }

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
                user.addOAuthProvider(provider);
                return userRepository.save(user);
            })
            .orElseGet(() -> {
                // name이 unique 제약 조건을 위반할 수 있으므로 try-catch는 상위에서 처리
                User newUser = new User(attributes.getName(), attributes.getEmail(), "");
                newUser.setProfile(new Profile(attributes.getName(), null, null, null, null, null));
                newUser.addOAuthProvider(provider);
                return userRepository.save(newUser);
            });
    }
}
