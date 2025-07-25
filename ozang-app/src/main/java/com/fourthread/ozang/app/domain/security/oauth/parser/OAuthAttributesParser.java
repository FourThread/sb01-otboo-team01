package com.fourthread.ozang.app.domain.security.oauth.parser;

import com.fourthread.ozang.app.domain.security.oauth.dto.OAuthAttributes;
import java.util.Map;

public interface OAuthAttributesParser {

  boolean supports(String registrationId);

  OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes);

}
