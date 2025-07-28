package com.fourthread.ozang.app;

import com.fourthread.ozang.app.domain.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "ADMIN_USERNAME=test-admin",
                "ADMIN_EMAIL=test-admin@mail.com",
                "ADMIN_PASSWORD=test-pass",
                "JWT_SECRET=d12d12d21d21d12d2",
                "KAKAO_API_KEY=test",
                "WEATHER_API_KEY=dwqqdd11",
                "cloud.aws.credentials.access-key=testAccessKey",
                "cloud.aws.credentials.secret-key=testSecretKey",
                "cloud.aws.region.static=ap-northeast-2"
        }
)
@TestPropertySource(properties = {
        "AWS_ACCESS_KEY=testAccessKey",
        "AWS_SECRET_KEY=testSecretKey",
        "cloud.aws.region.static=ap-northeast-2"
})

@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OzangApplicationTests {

    @Test
    void contextLoads() {
    }

}

