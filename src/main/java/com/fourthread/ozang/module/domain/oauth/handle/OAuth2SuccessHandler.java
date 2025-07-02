package com.fourthread.ozang.module.domain.oauth.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtPayloadDto;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.user.dto.type.Items;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = extractEmail(oAuth2User.getAttributes());
        log.info("[OAuth2SuccessHandler] 추출된 이메일: {}", email);
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        User user = userOptional.get();
        log.info("[OAuth2SuccessHandler] OAuth2 로그인 성공 - 사용자: {}", email);
        
        // OAuth 제공자 정보 저장
        String registrationId = extractRegistrationId(authentication);
        Items oauthProvider = getOAuthProvider(registrationId);
        user.addOAuthProvider(oauthProvider);
        userRepository.save(user);
        log.info("[OAuth2SuccessHandler] OAuth 제공자 추가: {}", oauthProvider);
        
        // JWT 토큰 생성
        JwtPayloadDto payloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());
        log.info("[OAuth2SuccessHandler] 이전 토큰을 무효화합니다");
        jwtService.invalidateJwtTokenByEmail(payloadDto.email());
        
        JwtToken jwtSession = jwtService.registerJwtToken(payloadDto);
        log.info("[OAuth2SuccessHandler] 새로운 Access Token을 발급합니다");
        
        String refreshToken = jwtSession.getRefreshToken();
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/api/auth");
        response.addCookie(refreshTokenCookie);
        
        // 백엔드 메인 페이지로 리다이렉트
        response.sendRedirect("/");
    }
    
    private String extractEmail(Map<String, Object> attributes) {
        // 카카오의 경우 kakao_account 안에 이메일이 있음
        if (attributes.get("kakao_account") instanceof Map<?, ?> kakaoAccount) {
            Object email = kakaoAccount.get("email");
            if (email instanceof String e) return e;
        }
        
        // 구글의 경우 직접 이메일이 있음
        if (attributes.containsKey("email")) {
            return (String) attributes.get("email");
        }
        
        // 이메일이 없는 경우 기본값 반환 (실제로는 예외 처리 필요)
        return "unknown@example.com";
    }
    
    private String extractRegistrationId(Authentication authentication) {
        // OAuth2User의 attributes에서 registrationId를 추출
        // 실제로는 OAuth2AuthenticationToken에서 가져와야 함
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            return oauth2Token.getAuthorizedClientRegistrationId();
        }

        return "unknown";
    }
    
    private Items getOAuthProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> Items.KAKAO;
            case "google" -> Items.GOOGLE;
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }
} 