// Spring Boot 应用启动入口。
package com.coview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoViewApplication {

    // 启动 Spring Boot 应用。
    public static void main(String[] args) {
        SpringApplication.run(CoViewApplication.class, args);
    }
}
