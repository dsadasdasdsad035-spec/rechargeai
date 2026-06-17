package com.wildai;

import com.wildai.common.config.WildAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(WildAiProperties.class)
public class WildAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WildAiApplication.class, args);
    }
}
