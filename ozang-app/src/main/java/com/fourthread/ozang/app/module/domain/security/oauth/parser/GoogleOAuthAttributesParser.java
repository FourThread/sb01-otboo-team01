package com.fourthread.ozang.module.domain.security.oauth.parser;

import com.fourthread.ozang.module.domain.security.oauth.dto.OAuthAttributes;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthAttributesParser implements OAuthAttributesParser {
  @Override
  public boolean supports(String registrationId) {
    return "google".equalsIgnoreCase(registrationId);
  }

  @Override
  public OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes) {
    return OAuthAttributes.builder()
        .registrationId("google")
        .name((String) attributes.get("name"))
        .email((String) attributes.get("email"))
        .picture((String) attributes.get("picture"))
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeName)
        .role(Role.USER)
        .build();
  }
}
