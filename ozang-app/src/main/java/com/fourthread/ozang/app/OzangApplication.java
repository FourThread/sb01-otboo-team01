package com.fourthread.ozang.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.fourthread.ozang.core",
    "com.fourthread.ozang.app"
})
@EnableJpaRepositories(basePackages = {
    "com.fourthread.ozang.core.domain",  // 모든 domain 하위의 repository 포함
    "com.fourthread.ozang.app.domain"
})
@EntityScan(basePackages = {
    "com.fourthread.ozang.core.domain", // Entity들도 스캔하도록 추가
    "com.fourthread.ozang.app.domain"
})
@EnableJpaAuditing
public class OzangApplication {

    public static void main(String[] args) {
        SpringApplication.run(OzangApplication.class, args);
    }

}
