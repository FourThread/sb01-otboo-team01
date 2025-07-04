package com.fourthread.ozang.module.domain.oauth.parser;

import com.fourthread.ozang.module.domain.oauth.dto.OAuthAttributes;
import java.util.Map;

public interface OAuthAttributesParser {

  boolean supports(String registrationId);

  OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes);

}
