package com.fourthread.ozang;

import com.fourthread.ozang.module.domain.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class OZangApplicationTests {

    @Test
    void contextLoads() {
    }

}
