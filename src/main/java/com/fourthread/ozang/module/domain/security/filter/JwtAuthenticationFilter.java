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
import java.util.Map;
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
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain
  ) throws IOException, ServletException {
    Optional<String> optionalAccessToken = resolveAccessToken(request);
    if (optionalAccessToken.isPresent() && !isPermitAll(request)) {
      String accessToken = optionalAccessToken.get();
      if (jwtService.validate(accessToken)) {
        UserDto userDto = jwtService.parse(accessToken).userDto();
        UserDetailsImpl userDetails = new UserDetailsImpl(userDto, null);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);

      } else {
        jwtService.invalidateJwtToken(accessToken);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse("Invalid token", null, null);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  private Optional<String> resolveAccessToken(HttpServletRequest request) {
    String prefix = "Bearer ";
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .map(value -> {
          if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
          } else {
            return null;
          }
        });
  }

  private boolean isPermitAll(HttpServletRequest request) {
    return Arrays.stream(SecurityMatchers.PUBLIC_MATCHERS)
        .anyMatch(requestMatcher -> requestMatcher.matches(request));
  }
}
