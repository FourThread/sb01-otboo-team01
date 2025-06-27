package com.fourthread.ozang.module.domain.user.controller.auth;

import com.fourthread.ozang.module.domain.security.dto.JwtToken;
import com.fourthread.ozang.module.domain.user.dto.request.LoginRequest;
import com.fourthread.ozang.module.domain.user.service.AuthenticationService;
import com.fourthread.ozang.module.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  @PostMapping("/sign-in")
  public ResponseEntity<String> signIn(
      @RequestBody @Validated LoginRequest request
  ) {
    JwtToken jwtToken = authenticationService.signIn(request);
    return ResponseEntity
        .ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(jwtToken.accessToken());
  }

  @PostMapping("/sign-out")
  public ResponseEntity<Void> signOut(@RequestHeader("Authorization") String bearerToken) {
    String token = bearerToken.replace("Bearer ", "");

    authenticationService.logout(token);

    return ResponseEntity.noContent().build();
  }
}
