package com.fourthread.ozang.module.domain.user.controller.api;

import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.dto.request.UserCreateRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> createUser(
      @RequestBody @Validated UserCreateRequest request
  ) {

    UserDto user = userService.createUser(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

//  @PatchMapping("/{userId}/role")
//  public ResponseEntity<UserDto> changeUserRole(
//      @PathVariable(name = "userId")UUID userId,
//      @RequestBody Use
//  )
}
