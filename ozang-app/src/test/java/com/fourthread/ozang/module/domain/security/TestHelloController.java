package com.fourthread.ozang.module.domain.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestHelloController {

  @GetMapping("/hello")
  public String hello() {
    return "Hello World!";
  }
}
