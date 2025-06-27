package com.fourthread.ozang.module.domain.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorResponse;
import com.fourthread.ozang.module.domain.security.SecurityMatchers;
import com.fourthread.ozang.module.domain.security.UserDetailsImpl;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (isPermitAll(request)) {
      filterChain.doFilter(request, response);
      return;
    }
  }

  private Optional<String> resolveAccessToken(HttpServletRequest request) {
    String prefix = "Bearer ";
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .filter(header -> header.startsWith(prefix))
        .map(header -> header.substring(prefix.length()));
  }

  private void authenticate(String token, HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    try {
      UserDto userDto = jwtService.parse(token).userDto();
      UserDetailsImpl userDetails = new UserDetailsImpl(userDto, null);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      SecurityContextHolder.getContext().setAuthentication(authentication);
      chain.doFilter(request, response);

    } catch (Exception e) {
      log.warn("Authentication failed during token parsing: {}", e.getMessage());
      handleUnauthorized(request, response);
    }
  }

  private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response,) {
    try {
      String token = resolveAccessToken(request).orElse("UNKNOWN");
      jwtService.invalidateJwtSession(token);

      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ErrorResponse errorResponse = new ErrorResponse("Invalid token", "accessToken : {}" + token,
          null);

      response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    } catch (IOException e) {
      log.error("Failed to write!");
    }
  }

  private boolean isPermitAll(HttpServletRequest request) {
    return Arrays.stream(Arrays.stream(SecurityMatchers.PUBLIC_MATCHERS)
        .anyMatch(requestMatcher -> requestMatcher.matches(request)));
  }
}
