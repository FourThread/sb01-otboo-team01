package com.fourthread.ozang.module.domain.security.jwt;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {


}
