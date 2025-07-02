package com.fourthread.ozang.module.domain.oauth.service;

import com.fourthread.ozang.module.domain.user.dto.type.Items;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;

  @Transactional
  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);
    String provider = userRequest.getClientRegistration().getRegistrationId();

    // 사용자 정보 추출
    String email;
    String name;

    if ("google".equals(provider)) {
      email = oauth2User.getAttribute("email");
      name = oauth2User.getAttribute("name");
    } else if ("kakao".equals(provider)) {
      Map<String, Object> kakaoAccount = oauth2User.getAttribute("kakao_account");
      Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
      email = (String) kakaoAccount.get("email");
      name = (String) profile.get("nickname");
    } else {
      throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
    }

    if (email == null) {
      throw new OAuth2AuthenticationException("Email is missing from OAuth2 response");
    }

    // 사용자 조회 또는 생성
    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
      Profile profileEntity = new Profile(name, null, null, null, null, null);
      user = new User(name, email, null, Role.USER);
      user.setProfile(profileEntity);
      user.getLinkedOAuthProviders().add(Items.valueOf(provider.toUpperCase()));
      userRepository.save(user);
    } else {
      if (!user.getLinkedOAuthProviders().contains(Items.valueOf(provider.toUpperCase()))) {
        user.getLinkedOAuthProviders().add(Items.valueOf(provider.toUpperCase()));
      }
    }

    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")),
        oauth2User.getAttributes(),
        "email"
    );
  }
}