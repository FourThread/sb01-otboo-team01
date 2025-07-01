package com.fourthread.ozang.module.domain.oauth.service;

import com.fourthread.ozang.module.domain.user.dto.type.Items;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.List;
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
    String email = oauth2User.getAttribute("email");
    String name = oauth2User.getAttribute("name");


    // DB에 사용자 저장 or 갱신
    User user = userRepository.findByEmail(email)
        .map(existingUser -> {
          if (!existingUser.getLinkedOAuthProviders().contains(Items.valueOf(provider.toUpperCase()))) {
            existingUser.getLinkedOAuthProviders().add(Items.valueOf(provider.toUpperCase()));
          }
          return existingUser;
        })
        .orElseGet(() -> {
          Profile proflie = new Profile(name, null, null, null, null, null);
          User newUser = new User(
              name,
              email,
              null,
              Role.USER
          );
          newUser.setProfile(proflie);
          newUser.getLinkedOAuthProviders().add(Items.valueOf(provider.toUpperCase()));
          return userRepository.save(newUser);
        });

    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")),
        oauth2User.getAttributes(),
        "email"
    );
  }
}

