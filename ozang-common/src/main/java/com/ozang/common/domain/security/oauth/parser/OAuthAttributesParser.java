package com.ozang.common.domain.security.oauth.parser;

import com.ozang.common.domain.security.oauth.dto.OAuthAttributes;
import java.util.Map;

public interface OAuthAttributesParser {

  boolean supports(String registrationId);

  OAuthAttributes parse(String userNameAttributeName, Map<String, Object> attributes);

}
