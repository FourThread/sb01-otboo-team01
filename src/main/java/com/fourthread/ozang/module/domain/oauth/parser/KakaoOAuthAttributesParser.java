package com.fourthread.ozang.module.domain.oauth.parser;

import com.fourthread.ozang.module.domain.oauth.dto.OAuthAttributes;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthAttributesParser implements OAuthAttributesParser {
  @Override
  public boolean supports(String registrationId) {
    return "kakao".equalsIgnoreCase(registrationId);
  }

  @Override
  public OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes) {
    Object accountObj = attributes.get("kakao_account");
    if (!(accountObj instanceof Map<?, ?> accountRaw)) {
      throw new IllegalArgumentException("Invalid kakao_account format");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> kakaoAccount = (Map<String, Object>) accountRaw;

    Object profileObj = kakaoAccount.get("profile");
    if (!(profileObj instanceof Map<?, ?> profileRaw)) {
      throw new IllegalArgumentException("Invalid profile format in kakao_account");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> profile = (Map<String, Object>) profileRaw;

    return OAuthAttributes.builder()
        .registrationId("kakao")
        .name((String) profile.get("nickname"))
        .email((String) kakaoAccount.get("email"))
        .picture((String) profile.get("profile_image_url"))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .role(Role.USER)
        .build();
  }
}
