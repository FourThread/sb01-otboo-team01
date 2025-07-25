package com.fourthread.ozang.module.domain.security.oauth.parser;

import com.fourthread.ozang.module.domain.security.oauth.dto.OAuthAttributes;
import java.util.Map;

public interface OAuthAttributesParser {

  boolean supports(String registrationId);

  OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes);

}
